package com.validator.testutil;

import com.validator.testutil.PerformanceHistory.Row;
import com.validator.testutil.PerformanceHistory.Run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps the per-version tables of docs/performance/README.md in step with docs/performance/history.csv. Only the
 * part between {@link #BEGIN} and {@link #END} is generated; the prose around it is edited by hand. The timing test
 * regenerates it whenever it appends to the history, PerformanceHistoryTest fails when the committed README and
 * history disagree, and after editing the history by hand it is regenerated with:
 * java validator-core/src/test/java/com/validator/testutil/PerformanceReadme.java
 */
public final class PerformanceReadme {

    /** The history, relative to the repository root. */
    public static final String HISTORY_FILE = "docs/performance/history.csv";
    /** The README, relative to the repository root. */
    public static final String README_FILE = "docs/performance/README.md";

    static final String BEGIN = "<!-- BEGIN GENERATED from history.csv by com.validator.testutil.PerformanceReadme;"
        + " do not edit by hand -->";
    static final String END = "<!-- END GENERATED -->";

    private PerformanceReadme() {
    }

    /** Regenerates the README of the repository the working directory is in. */
    public static void main(String[] args) {
        Path root = TestPaths.repositoryRoot();
        regenerate(root.resolve(HISTORY_FILE), root.resolve(README_FILE));
        System.out.println("Regenerated " + root.resolve(README_FILE));
    }

    /** Rewrites the generated part of {@code readme} from {@code history}. */
    public static void regenerate(Path history, Path readme) {
        try {
            String text = Files.readString(readme, StandardCharsets.UTF_8);
            Files.writeString(readme, update(text, PerformanceHistory.read(history)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot update " + readme, e);
        }
    }

    /** The README text with its generated part replaced by the tables of {@code history}. */
    public static String update(String readme, PerformanceHistory history) {
        int begin = readme.indexOf(BEGIN);
        int end = readme.indexOf(END);
        if (begin < 0 || end < begin) {
            throw new IllegalArgumentException("The README has no " + BEGIN + " ... " + END + " section");
        }
        return readme.substring(0, begin + BEGIN.length()) + "\n" + render(history) + readme.substring(end);
    }

    /** The generated part of the README now, or nothing if it has none. */
    public static Optional<String> generatedPart(String readme) {
        int begin = readme.indexOf(BEGIN);
        int end = readme.indexOf(END);
        return begin < 0 || end < begin ? Optional.empty()
            : Optional.of(readme.substring(begin + BEGIN.length() + 1, end));
    }

    /**
     * The tables for the latest run of each version: when, where and on what it was recorded, then its ratio to the
     * reference per subject. A subject timed another way in some version (first build against warm median) gets a
     * column per measure, because the two ratios do not compare.
     */
    public static String render(PerformanceHistory history) {
        List<Run> latest = new ArrayList<>();
        for (String version : history.versions()) {
            history.latestRun(version).ifPresent(latest::add);
        }
        StringBuilder md = new StringBuilder();
        if (latest.isEmpty()) {
            return md.append("No runs recorded yet.\n").toString();
        }
        Map<String, Integer> runCounts = history.runCounts();

        md.append("### Latest run of each version\n\n");
        md.append("| Version | Runs | Recorded (UTC) | Commit | JDK | OS / arch | CPUs | Load | Reference ms"
            + " | Slowdown | Note |\n");
        md.append("|---|---:|---|---|---|---|---:|---:|---:|---:|---|\n");
        for (Run run : latest) {
            Row r = run.first();
            md.append(cells(r.version(), Integer.toString(runCounts.get(r.version())), r.recordedUtc(), r.commit(),
                r.jdk(), r.os() + " " + r.arch(), Integer.toString(r.cpus()),
                Double.isNaN(r.loadAverage()) ? "-" : String.format(Locale.ROOT, "%.1f", r.loadAverage()),
                String.format(Locale.ROOT, "%.1f", r.referenceMs()), String.format(Locale.ROOT, "%.2f", r.slowdown()),
                r.note()));
        }

        Map<String, String> columns = new LinkedHashMap<>();
        for (Run run : latest) {
            for (Row r : run.rows()) {
                columns.putIfAbsent(r.subjectKey(), r.subject() + " (" + r.measure() + ")");
            }
        }
        md.append("\n### Ratio to the reference, latest run of each version\n\n");
        md.append("Time over the reference workload's time in the same run; lower is faster. `-`: not timed in that"
            + " version.\n\n");
        List<String> header = new ArrayList<>(List.of("Version"));
        header.addAll(columns.values());
        md.append(cells(header.toArray(String[]::new)));
        md.append("|---").append("|---:".repeat(columns.size())).append("|\n");
        for (Run run : latest) {
            List<String> row = new ArrayList<>(List.of(run.version()));
            for (String key : columns.keySet()) {
                row.add(run.rows().stream().filter(r -> r.subjectKey().equals(key)).findFirst()
                    .map(r -> PerformanceComparison.ratio(r.ratio())).orElse("-"));
            }
            md.append(cells(row.toArray(String[]::new)));
        }
        return md.toString();
    }

    private static String cells(String... values) {
        StringBuilder line = new StringBuilder("|");
        for (String v : values) {
            line.append(' ').append(v.replace("|", "\\|")).append(" |");
        }
        return line.append('\n').toString();
    }
}
