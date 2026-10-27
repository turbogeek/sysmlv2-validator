package com.validator.cli.compare;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * The result files of one run, written the same way by this validator ({@link ValidatorRunner}), the pilot runner
 * (conformance/pilot/PilotRunner.java) and the Cameo macro (conformance/cameo/CameoConformanceRunner.groovy), so
 * that {@link ConformanceComparison} and {@link PerfComparison} can read them all.
 *
 * <ul>
 *   <li>{@code results.csv}: one row per model, repetition and phase ({@link #RESULTS_HEADER});</li>
 *   <li>{@code diagnostics.csv}: what the tool reported for each model, from its first repetition
 *       ({@link #DIAGNOSTICS_HEADER});</li>
 *   <li>{@code environment.properties}: the tool, the machine and the JVM.</li>
 * </ul>
 */
public final class Results {

    /** Header of results.csv. */
    public static final String RESULTS_HEADER =
        "tool,tool_version,host,run_id,file_id,rep,phase,ms,elements,errors,warnings,status,detail";
    /** Header of diagnostics.csv. */
    public static final String DIAGNOSTICS_HEADER = "tool,file_id,member,source,severity,line,column,code,message";
    /** The file id of a row that belongs to the whole run, such as startup. */
    public static final String RUN = "*";

    private Results() {
    }

    /**
     * One timed phase.
     *
     * @param fileId the manifest id, or {@link #RUN}
     * @param rep the repetition, from 1
     * @param phase what was timed
     * @param ms elapsed milliseconds, or -1 if it did not run
     * @param elements elements built, or -1
     * @param errors errors reported, or -1
     * @param warnings warnings reported, or -1
     * @param status ok, failed, skipped or unavailable
     * @param detail free text: the reason for a failure, the heap in use, ...
     */
    public record Row(String fileId, int rep, String phase, double ms, long elements, long errors, long warnings,
            String status, String detail) {
    }

    /**
     * Write results.csv.
     *
     * @param file the CSV file
     * @param tool the tool name
     * @param toolVersion its version
     * @param host the machine
     * @param runId the run's id
     * @param rows the rows
     * @throws IOException if it cannot be written
     */
    public static void writeResults(Path file, String tool, String toolVersion, String host, String runId,
            List<Row> rows) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(RESULTS_HEADER + "\n");
            for (Row r : rows) {
                w.write(csv(tool, toolVersion, host, runId, r.fileId(), Integer.toString(r.rep()), r.phase(),
                    String.format(java.util.Locale.ROOT, "%.3f", r.ms()), Long.toString(r.elements()),
                    Long.toString(r.errors()), Long.toString(r.warnings()), r.status(), r.detail()) + "\n");
            }
        }
    }

    /**
     * Read results.csv.
     *
     * @param file the CSV file
     * @return the rows, and the run's tool, version and host in {@code info}
     * @throws IOException if it cannot be read or is malformed
     */
    public static Run readResults(Path file) throws IOException {
        List<List<String>> records = parseCsv(Files.readString(file, StandardCharsets.UTF_8));
        if (records.isEmpty() || !String.join(",", records.get(0)).equals(RESULTS_HEADER)) {
            throw new IOException(file + ": not a results.csv (header " + RESULTS_HEADER + ")");
        }
        List<Row> rows = new ArrayList<>();
        String tool = "";
        String version = "";
        String host = "";
        for (List<String> f : records.subList(1, records.size())) {
            if (f.size() != 13) {
                throw new IOException(file + ": expected 13 columns, found " + f.size() + ": " + f);
            }
            tool = f.get(0);
            version = f.get(1);
            host = f.get(2);
            rows.add(new Row(f.get(4), Integer.parseInt(f.get(5)), f.get(6), Double.parseDouble(f.get(7)),
                Long.parseLong(f.get(8)), Long.parseLong(f.get(9)), Long.parseLong(f.get(10)), f.get(11), f.get(12)));
        }
        return new Run(tool, version, host, rows);
    }

    /**
     * The rows of one results.csv.
     *
     * @param tool the tool
     * @param version its version
     * @param host the machine
     * @param rows the rows
     */
    public record Run(String tool, String version, String host, List<Row> rows) {

        /**
         * The rows of a model and phase that ran, by repetition.
         *
         * @param fileId the model
         * @param phase the phase
         * @return its rows with status ok, in repetition order
         */
        public List<Row> ok(String fileId, String phase) {
            List<Row> result = new ArrayList<>();
            for (Row r : rows) {
                if (r.fileId().equals(fileId) && r.phase().equals(phase) && "ok".equals(r.status())) {
                    result.add(r);
                }
            }
            result.sort((a, b) -> Integer.compare(a.rep(), b.rep()));
            return result;
        }

        /**
         * The first row of a model and phase, whatever its status.
         *
         * @param fileId the model
         * @param phase the phase
         * @return the row, or null
         */
        public Row first(String fileId, String phase) {
            Row first = null;
            for (Row r : rows) {
                if (r.fileId().equals(fileId) && r.phase().equals(phase) && (first == null || r.rep() < first.rep())) {
                    first = r;
                }
            }
            return first;
        }
    }

    /**
     * Timing summary of one model and phase: the first repetition (the model's first run) and the median of the
     * later ones (warm). With one repetition, both are that repetition.
     *
     * @param first ms of repetition 1, or NaN if it did not succeed
     * @param warmMedian median ms of the later repetitions that succeeded; {@code first} if there are none
     * @param reps how many repetitions succeeded
     */
    public record Timing(double first, double warmMedian, int reps) {

        /**
         * Summarize rows.
         *
         * @param rows the successful rows of one model and phase
         * @return the timing, or null if there are none
         */
        public static Timing of(List<Row> rows) {
            if (rows.isEmpty()) {
                return null;
            }
            double first = Double.NaN;
            List<Double> warm = new ArrayList<>();
            for (Row r : rows) {
                if (r.rep() == 1) {
                    first = r.ms();
                } else {
                    warm.add(r.ms());
                }
            }
            if (warm.isEmpty()) {
                return new Timing(first, first, rows.size());
            }
            Collections.sort(warm);
            int n = warm.size();
            double median = n % 2 == 1 ? warm.get(n / 2) : (warm.get(n / 2 - 1) + warm.get(n / 2)) / 2;
            return new Timing(first, median, rows.size());
        }

        /** True when only one run was timed (a large model), so first and warm are the same run. */
        public boolean single() {
            return reps <= 1;
        }

        /** "first / warm", or one number for a single run. */
        public String cell() {
            return single() && !Double.isNaN(first) ? format(first) : format(first) + " / " + format(warmMedian);
        }

        /**
         * Milliseconds for a report: no decimals from 100 ms, one below; "–" for NaN.
         *
         * @param ms milliseconds
         * @return the text
         */
        public static String format(double ms) {
            if (Double.isNaN(ms)) {
                return "–";
            }
            return ms >= 100 ? String.format(java.util.Locale.ROOT, "%.0f", ms)
                : String.format(java.util.Locale.ROOT, "%.1f", ms);
        }
    }

    /**
     * One reported problem.
     *
     * @param fileId the model
     * @param member the file inside a KPAR the problem is in; empty for a plain file
     * @param source where it came from, e.g. syntax, linking, validation
     * @param severity error, warning or info
     * @param line line (1-based) within the file or member, or 0 if unknown
     * @param column column, or 0
     * @param code rule or error code
     * @param message the message
     */
    public record Diagnostic(String fileId, String member, String source, String severity, int line, int column,
            String code, String message) {

        /** "member:line", or the line alone for a plain file; null if the line is unknown. */
        public String place() {
            if (line <= 0) {
                return null;
            }
            return member == null || member.isEmpty() ? Integer.toString(line) : member + ":" + line;
        }
    }

    /**
     * Write diagnostics.csv.
     *
     * @param file the CSV file
     * @param tool the tool name
     * @param diagnostics the diagnostics
     * @throws IOException if it cannot be written
     */
    public static void writeDiagnostics(Path file, String tool, List<Diagnostic> diagnostics) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(DIAGNOSTICS_HEADER + "\n");
            for (Diagnostic d : diagnostics) {
                w.write(csv(tool, d.fileId(), d.member(), d.source(), d.severity(), Integer.toString(d.line()),
                    Integer.toString(d.column()), d.code(), d.message()) + "\n");
            }
        }
    }

    /**
     * Read diagnostics.csv.
     *
     * @param file the CSV file; a missing file reads as empty
     * @return the diagnostics
     * @throws IOException if it cannot be read or is malformed
     */
    public static List<Diagnostic> readDiagnostics(Path file) throws IOException {
        List<Diagnostic> result = new ArrayList<>();
        if (!Files.isRegularFile(file)) {
            return result;
        }
        List<List<String>> records = parseCsv(Files.readString(file, StandardCharsets.UTF_8));
        if (records.isEmpty() || !String.join(",", records.get(0)).equals(DIAGNOSTICS_HEADER)) {
            throw new IOException(file + ": not a diagnostics.csv (header " + DIAGNOSTICS_HEADER + ")");
        }
        for (List<String> f : records.subList(1, records.size())) {
            if (f.size() != 9) {
                throw new IOException(file + ": expected 9 columns, found " + f.size() + ": " + f);
            }
            result.add(new Diagnostic(f.get(1), f.get(2), f.get(3), f.get(4).toLowerCase(java.util.Locale.ROOT),
                parseInt(f.get(5)), parseInt(f.get(6)), f.get(7), f.get(8)));
        }
        return result;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Write environment.properties, keys sorted.
     *
     * @param file the file
     * @param values the properties
     * @throws IOException if it cannot be written
     */
    public static void writeEnvironment(Path file, Map<String, String> values) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(values).entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue().replace("\\", "\\\\").replace("\n", " "))
                .append('\n');
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Read environment.properties.
     *
     * @param file the file; a missing file reads as empty
     * @return the properties, in key order
     * @throws IOException if it cannot be read
     */
    public static Map<String, String> readEnvironment(Path file) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.isRegularFile(file)) {
            return result;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        for (String key : new TreeMap<>(p).keySet().stream().map(Object::toString).toList()) {
            result.put(key, p.getProperty(key));
        }
        return result;
    }

    /** One CSV record: fields quoted when they hold a comma, a quote or a line break. */
    static String csv(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            String f = fields[i] == null ? "" : fields[i];
            if (i > 0) {
                sb.append(',');
            }
            if (f.indexOf(',') >= 0 || f.indexOf('"') >= 0 || f.indexOf('\n') >= 0 || f.indexOf('\r') >= 0) {
                sb.append('"').append(f.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(f);
            }
        }
        return sb.toString();
    }

    /** Parse CSV text into records (RFC 4180: quoted fields, doubled quotes, line breaks inside quotes). */
    static List<List<String>> parseCsv(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
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
                any = true;
            } else if (c == ',') {
                record.add(field.toString());
                field.setLength(0);
                any = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                if (any || field.length() > 0) {
                    record.add(field.toString());
                    records.add(record);
                }
                record = new ArrayList<>();
                field.setLength(0);
                any = false;
            } else {
                field.append(c);
                any = true;
            }
        }
        if (any || field.length() > 0) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
