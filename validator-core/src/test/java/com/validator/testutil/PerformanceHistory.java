package com.validator.testutil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The performance history of the validator: docs/performance/history.csv, one row per timed subject (a model, or the
 * total of a test suite) per recorded run, in the order the runs were recorded. The figure to compare across runs is
 * the ratio of a subject's time to the reference workload's time in the same run (see {@link PerformanceReference}):
 * it stays about the same when the whole machine is slower and grows when the validator is.
 *
 * <p>Rows are read by column name, so a column added later reads as empty in older rows, and appending to a file
 * with an older header rewrites it with the current columns.</p>
 */
public final class PerformanceHistory {

    /** The CSV columns, in file order. */
    public static final List<String> COLUMNS = List.of("recorded_utc", "version", "commit", "jdk", "os", "arch",
        "cpus", "load_avg", "reference_ms", "slowdown", "subject", "measure", "files", "symbols", "timed_ms", "ratio",
        "note");

    /** Measure of a model built once, the first time in the JVM (JIT compilation and parser caches still cold). */
    public static final String FIRST_BUILD = "first-build";
    /** Measure of a test suite: the sum of its files' first builds. */
    public static final String FIRST_BUILDS_TOTAL = "first-builds-total";
    /** Prefix of the measure of a model built again on the warm JVM: the median of N builds. */
    public static final String WARM_MEDIAN_PREFIX = "warm-median-of-";
    /** Prefix of a subject that is a whole test suite rather than one model. */
    public static final String SUITE_PREFIX = "suite:";

    /**
     * One timed subject of one recorded run. The run's fields (time, version, commit, machine, reference) repeat on
     * each of its rows, so that every row stands on its own in a spreadsheet.
     *
     * @param recordedUtc  when the run was recorded, an ISO-8601 instant in UTC; it identifies the run
     * @param version      the Maven project version
     * @param commit       the short git SHA, marked -dirty for uncommitted changes
     * @param jdk          the Java runtime
     * @param os           the operating system
     * @param arch         the JVM's architecture
     * @param cpus         processors available to the JVM
     * @param loadAverage  the system load average over the last minute when the run ended; NaN if unknown
     * @param referenceMs  the reference workload's median in this run
     * @param slowdown     the reference over its nominal time (at least 1): how much slower than at full speed the
     *                     machine was
     * @param subject      a model file name, or {@value #SUITE_PREFIX} and a test-suite folder
     * @param measure      how the time was taken: {@value #FIRST_BUILD}, {@value #FIRST_BUILDS_TOTAL} or
     *                     {@value #WARM_MEDIAN_PREFIX}N
     * @param files        the number of model files timed (1 for a model)
     * @param symbols      the symbols built from them
     * @param timedMs      the time
     * @param ratio        the time over the reference median: the figure that compares across runs and machines
     * @param note         free text, such as how busy the machine was
     */
    public record Row(String recordedUtc, String version, String commit, String jdk, String os, String arch, int cpus,
                      double loadAverage, double referenceMs, double slowdown, String subject, String measure,
                      int files, int symbols, double timedMs, double ratio, String note) {

        /** The key that identifies a subject's figure as comparable across runs. */
        String subjectKey() {
            return subject + " / " + measure;
        }

        boolean sameRun(Row other) {
            return recordedUtc.equals(other.recordedUtc) && version.equals(other.version)
                && commit.equals(other.commit);
        }
    }

    /** One recorded run: the rows recorded together, in file order. */
    public record Run(List<Row> rows) {
        public Run {
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("A run has at least one row");
            }
            rows = List.copyOf(rows);
        }

        public String version() {
            return rows.get(0).version();
        }

        public String commit() {
            return rows.get(0).commit();
        }

        public String recordedUtc() {
            return rows.get(0).recordedUtc();
        }

        public Row first() {
            return rows.get(0);
        }

        /** This run's row for a subject, if it timed the subject. */
        public Optional<Row> row(String subject) {
            return rows.stream().filter(r -> r.subject().equals(subject)).findFirst();
        }
    }

    /** One subject's time in the current run, before it becomes a row. */
    public record Timing(String subject, String measure, int files, int symbols, double timedMs) {
    }

    private final List<Row> rows;

    public PerformanceHistory(List<Row> rows) {
        this.rows = List.copyOf(rows);
    }

    /**
     * The rows of a new run, rounded to the precision the CSV keeps so that they compare the same before and after
     * they are written. The ratio is each time over the reference median.
     */
    public static List<Row> newRun(BuildInfo build, Instant recorded, double referenceMs, double slowdown,
                                   List<Timing> timings, String note) {
        String when = recorded.truncatedTo(ChronoUnit.SECONDS).toString();
        double load = build.loadAverage() < 0 ? Double.NaN : round(build.loadAverage(), 2);
        String oneLineNote = note == null ? "" : note.replaceAll("[\\r\\n]+", " ").trim();
        List<Row> rows = new ArrayList<>();
        for (Timing t : timings) {
            rows.add(new Row(when, build.version(), build.commit(), build.jdk(), build.os(), build.arch(),
                build.cpus(), load, round(referenceMs, 2), round(slowdown, 2), t.subject(), t.measure(), t.files(),
                t.symbols(), round(t.timedMs(), 1), round(t.timedMs() / referenceMs, 3), oneLineNote));
        }
        return rows;
    }

    public List<Row> rows() {
        return rows;
    }

    /** Reads a history file; a missing file is an empty history. */
    public static PerformanceHistory read(Path file) {
        if (!Files.exists(file)) {
            return new PerformanceHistory(List.of());
        }
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    /** Parses history CSV text with a header line. Blank text is an empty history. */
    public static PerformanceHistory parse(String csv) {
        List<List<String>> records = parseCsv(csv);
        if (records.isEmpty()) {
            return new PerformanceHistory(List.of());
        }
        Map<String, Integer> index = new HashMap<>();
        List<String> header = records.get(0);
        for (int i = 0; i < header.size(); i++) {
            index.put(header.get(i).trim(), i);
        }
        for (String required : List.of("recorded_utc", "version", "subject", "ratio")) {
            if (!index.containsKey(required)) {
                throw new IllegalArgumentException("History header has no " + required + " column: " + header);
            }
        }
        List<Row> rows = new ArrayList<>();
        for (int line = 1; line < records.size(); line++) {
            List<String> record = records.get(line);
            if (record.size() == 1 && record.get(0).isBlank()) {
                continue;
            }
            Function<String, String> field = name -> {
                Integer i = index.get(name);
                return i == null || i >= record.size() ? "" : record.get(i);
            };
            try {
                rows.add(new Row(field.apply("recorded_utc"), field.apply("version"), field.apply("commit"),
                    field.apply("jdk"), field.apply("os"), field.apply("arch"), parseInt(field.apply("cpus")),
                    parseDouble(field.apply("load_avg")), parseDouble(field.apply("reference_ms")),
                    parseDouble(field.apply("slowdown")), field.apply("subject"), field.apply("measure"),
                    parseInt(field.apply("files")), parseInt(field.apply("symbols")),
                    parseDouble(field.apply("timed_ms")), parseDouble(field.apply("ratio")), field.apply("note")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "History line " + (line + 1) + " has a malformed number: " + record, e);
            }
        }
        return new PerformanceHistory(rows);
    }

    /** Formats rows as CSV lines (no header), each ending with a newline. */
    public static String formatRows(List<Row> rows) {
        StringBuilder csv = new StringBuilder();
        for (Row r : rows) {
            List<String> fields = List.of(r.recordedUtc(), r.version(), r.commit(), r.jdk(), r.os(), r.arch(),
                Integer.toString(r.cpus()), formatNumber(r.loadAverage(), 2), formatNumber(r.referenceMs(), 2),
                formatNumber(r.slowdown(), 2), r.subject(), r.measure(), Integer.toString(r.files()),
                Integer.toString(r.symbols()), formatNumber(r.timedMs(), 1), formatNumber(r.ratio(), 3), r.note());
            csv.append(String.join(",", fields.stream().map(PerformanceHistory::quote).toList())).append('\n');
        }
        return csv.toString();
    }

    /** The header line, ending with a newline. */
    public static String header() {
        return String.join(",", COLUMNS) + "\n";
    }

    /** The whole history as CSV text with a header. */
    public String toCsv() {
        return header() + formatRows(rows);
    }

    /**
     * Appends rows to a history file, creating it with a header if needed. A file with another header (an older set
     * of columns) is rewritten with the current columns first.
     */
    public static void append(Path file, List<Row> newRows) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            String existing = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
            if (existing.isBlank()) {
                Files.writeString(file, header() + formatRows(newRows), StandardCharsets.UTF_8);
            } else if (!existing.startsWith(header())) {
                List<Row> all = new ArrayList<>(parse(existing).rows());
                all.addAll(newRows);
                Files.writeString(file, new PerformanceHistory(all).toCsv(), StandardCharsets.UTF_8);
            } else {
                String separator = existing.endsWith("\n") ? "" : "\n";
                Files.writeString(file, separator + formatRows(newRows), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + file, e);
        }
    }

    /** The recorded runs in file order. Rows of one run are adjacent; they share time, version and commit. */
    public List<Run> runs() {
        List<Run> runs = new ArrayList<>();
        List<Row> current = new ArrayList<>();
        for (Row row : rows) {
            if (!current.isEmpty() && !current.get(0).sameRun(row)) {
                runs.add(new Run(current));
                current = new ArrayList<>();
            }
            current.add(row);
        }
        if (!current.isEmpty()) {
            runs.add(new Run(current));
        }
        return runs;
    }

    /** The recorded versions in the order they were first recorded. */
    public List<String> versions() {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        rows.forEach(r -> versions.add(r.version()));
        return new ArrayList<>(versions);
    }

    public boolean hasVersion(String version) {
        return rows.stream().anyMatch(r -> r.version().equals(version));
    }

    /**
     * The version recorded before {@code version}: the one first recorded just before it, or, when {@code version}
     * has no rows yet, the last version recorded. Versions are ordered by when they were first recorded rather than
     * by their numbers, so a history is read the way it was made.
     */
    public Optional<String> previousVersion(String version) {
        List<String> versions = versions();
        int i = versions.indexOf(version);
        if (i < 0) {
            return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(versions.size() - 1));
        }
        return i == 0 ? Optional.empty() : Optional.of(versions.get(i - 1));
    }

    /** The most recently recorded run of a version. */
    public Optional<Run> latestRun(String version) {
        List<Run> runs = runs();
        Collections.reverse(runs);
        return runs.stream().filter(r -> r.version().equals(version)).findFirst();
    }

    /** The most recently recorded run of any version. */
    public Optional<Run> lastRun() {
        List<Run> runs = runs();
        return runs.isEmpty() ? Optional.empty() : Optional.of(runs.get(runs.size() - 1));
    }

    /** The number of recorded runs of each version, in the order the versions were first recorded. */
    public Map<String, Integer> runCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        runs().forEach(r -> counts.merge(r.version(), 1, Integer::sum));
        return counts;
    }

    /**
     * When the timing test appends its run to the history, from the system property perf.record: {@code true}
     * always, {@code false} never, unset (AUTO) only for the first run of a project version, so that every version
     * is recorded once without each ordinary build modifying the file.
     */
    public enum RecordMode {
        AUTO, ALWAYS, NEVER;

        /** System property that selects the mode. */
        public static final String PROPERTY = "perf.record";

        public static RecordMode fromProperty(String value) {
            if (value == null || value.isBlank() || value.equalsIgnoreCase("auto")) {
                return AUTO;
            }
            if (value.equalsIgnoreCase("true")) {
                return ALWAYS;
            }
            if (value.equalsIgnoreCase("false")) {
                return NEVER;
            }
            throw new IllegalArgumentException(PROPERTY + " must be true, false or auto, not " + value);
        }
    }

    /** Whether to append this run, and why. */
    public record Decision(boolean append, String reason) {
    }

    /**
     * Decides whether to append a run. A run of an unknown version (outside Maven) is never recorded. In AUTO mode a
     * run that failed a check is not recorded either, so that a regression does not silently become the next run's
     * baseline (recording it with perf.record=true is how a slowdown is accepted), and neither is a run whose ratios
     * cannot be trusted (a busy machine, a coverage agent), since its inflated ratios would hide the next regression:
     * the version is then recorded by its first trustworthy run.
     *
     * @param mode         the recording mode
     * @param build        the version and commit measured
     * @param history      the history so far
     * @param failedChecks true when the run regressed against the history or exceeded a budget
     * @param untrusted    why the run's ratios cannot be trusted, or empty (see PerformanceComparison.untrusted)
     */
    public static Decision decide(RecordMode mode, BuildInfo build, PerformanceHistory history, boolean failedChecks,
                                  Optional<String> untrusted) {
        if (mode == RecordMode.NEVER) {
            return new Decision(false, RecordMode.PROPERTY + "=false");
        }
        if (!build.versionKnown()) {
            return new Decision(false, "the project version is unknown (run the test through Maven)");
        }
        if (mode == RecordMode.ALWAYS) {
            return new Decision(true, RecordMode.PROPERTY + "=true"
                + (failedChecks ? ", although the run failed a check: it is the baseline from now on" : ""));
        }
        if (history.hasVersion(build.version())) {
            return new Decision(false, "version " + build.version() + " is already recorded (record again with -D"
                + RecordMode.PROPERTY + "=true)");
        }
        if (failedChecks) {
            return new Decision(false, "first run of version " + build.version() + " failed a check; fix it, or"
                + " accept the figures with -D" + RecordMode.PROPERTY + "=true");
        }
        if (untrusted.isPresent()) {
            return new Decision(false, untrusted.get() + "; version " + build.version()
                + " is recorded by its first run without that");
        }
        return new Decision(true, "first run of version " + build.version());
    }

    /** Formats a number for the CSV with a fixed number of decimals; NaN or a negative load is an empty field. */
    static String formatNumber(double value, int decimals) {
        if (Double.isNaN(value) || value < 0) {
            return "";
        }
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** Rounds to the precision the CSV keeps, so that a run compares the same in memory and after reading it back. */
    public static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }

    private static double parseDouble(String s) {
        return s.isBlank() ? Double.NaN : Double.parseDouble(s.trim());
    }

    private static int parseInt(String s) {
        return s.isBlank() ? 0 : Integer.parseInt(s.trim());
    }

    /** Quotes a CSV field when it holds a comma, quote or line break (RFC 4180). */
    static String quote(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /** Splits CSV text into records of fields (RFC 4180: quoted fields may hold commas, quotes and line breaks). */
    static List<List<String>> parseCsv(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            any = true;
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                record.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
                any = false;
            } else {
                field.append(c);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unterminated quoted field in history CSV");
        }
        if (any) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
