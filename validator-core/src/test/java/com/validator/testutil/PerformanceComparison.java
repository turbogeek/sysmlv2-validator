package com.validator.testutil;

import com.validator.testutil.PerformanceHistory.Row;
import com.validator.testutil.PerformanceHistory.Run;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Compares a run's ratios to the reference with a recorded run of the performance history, subject by subject, and
 * says which differences are clear regressions rather than measurement noise.
 */
public final class PerformanceComparison {

    private PerformanceComparison() {
    }

    /**
     * When a ratio counts as clearly worse than its baseline: it is more than {@code factor} times the baseline and
     * more than {@code minRatioIncrease} above it. The factor covers the run-to-run noise of a ratio; the increase
     * keeps a model that takes a few milliseconds, whose ratio is well below 1, from failing on a few milliseconds of
     * scheduling or garbage collection.
     *
     * <p>A run made while the machine was busy, with a load average above {@code busyLoadPerCpu} times its
     * processors, is compared but cannot fail (see {@link #untrusted}): under that much contention the JIT compiler
     * and garbage collector threads starve and a first build slows down far more than the reference workload, whose
     * short bursts still get a processor. All three come from perf-reference.properties, with the noise they were
     * derived from.</p>
     */
    public record Threshold(double factor, double minRatioIncrease, double busyLoadPerCpu) {

        public static Threshold load() {
            Properties p = new Properties();
            try (InputStream in = PerformanceComparison.class.getResourceAsStream(
                PerformanceReference.CALIBRATION_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException(PerformanceReference.CALIBRATION_RESOURCE + " is missing");
                }
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + PerformanceReference.CALIBRATION_RESOURCE, e);
            }
            return new Threshold(Double.parseDouble(p.getProperty("history.regressionFactor")),
                Double.parseDouble(p.getProperty("history.regressionMinRatioIncrease")),
                Double.parseDouble(p.getProperty("history.busyLoadPerCpu")));
        }

        /** True when {@code now} is clearly worse (larger) than {@code baseline}. */
        public boolean isRegression(double baseline, double now) {
            return now > baseline * factor && now - baseline > minRatioIncrease;
        }

        /** True when {@code now} is clearly better than {@code baseline}, by the same margins. */
        public boolean isImprovement(double baseline, double now) {
            return isRegression(now, baseline);
        }

        /** True when a run with this load average on this many processors was too busy to fail on its ratios. */
        public boolean isBusy(double loadAverage, int cpus) {
            return !Double.isNaN(loadAverage) && cpus > 0 && loadAverage > busyLoadPerCpu * cpus;
        }

        public String describe() {
            return String.format(Locale.ROOT, "more than %.1f x the baseline ratio and more than %.1f above it",
                factor, minRatioIncrease);
        }
    }

    /** How a subject's ratio compares with its baseline. */
    public enum Status {
        /** The baseline did not time the subject. */
        NEW("new"),
        /** The baseline timed it another way (first build against warm median) or over another number of files. */
        NOT_COMPARABLE("not comparable"),
        /** Faster or slower, but within the threshold. */
        WITHIN_NOISE(""),
        FASTER("faster"),
        REGRESSION("REGRESSION");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** One subject of the current run against its baseline row, if the baseline has one. */
    public record Line(Row now, Optional<Row> baseline, Status status) {

        /** The change of the ratio, as a signed percentage, or empty when not compared. */
        public String change() {
            if (baseline.isEmpty() || status == Status.NOT_COMPARABLE) {
                return "";
            }
            double base = baseline.get().ratio();
            if (base <= 0) {
                return "";
            }
            return String.format(Locale.ROOT, "%+.0f%%", 100.0 * (now.ratio() - base) / base);
        }
    }

    /** Compares each row of the current run with the same subject in the baseline run. */
    public static List<Line> compare(List<Row> current, Optional<Run> baseline, Threshold threshold) {
        List<Line> lines = new ArrayList<>();
        for (Row now : current) {
            Optional<Row> base = baseline.flatMap(run -> run.row(now.subject()));
            Status status;
            if (base.isEmpty()) {
                status = Status.NEW;
            } else if (!base.get().measure().equals(now.measure()) || base.get().files() != now.files()
                || Double.isNaN(base.get().ratio())) {
                status = Status.NOT_COMPARABLE;
            } else if (threshold.isRegression(base.get().ratio(), now.ratio())) {
                status = Status.REGRESSION;
            } else if (threshold.isImprovement(base.get().ratio(), now.ratio())) {
                status = Status.FASTER;
            } else {
                status = Status.WITHIN_NOISE;
            }
            lines.add(new Line(now, base, status));
        }
        return lines;
    }

    /**
     * Why this run's ratios cannot be trusted, either to fail on or to become a baseline, or empty when they can: the
     * machine was busy, or a Java agent (JaCoCo's coverage agent, a profiler) instrumented the validator's code,
     * which slows it but not the reference workload's JDK code.
     */
    public static Optional<String> untrusted(BuildInfo build, Threshold threshold) {
        if (threshold.isBusy(build.loadAverage(), build.cpus())) {
            return Optional.of(String.format(Locale.ROOT, "the machine is busy (load %.1f on %d CPUs, over %.1f per"
                + " CPU)", build.loadAverage(), build.cpus(), threshold.busyLoadPerCpu()));
        }
        if (!build.javaAgents().isEmpty()) {
            return Optional.of("a Java agent instruments the code (" + String.join(", ", build.javaAgents())
                + "; run with -Djacoco.skip=true)");
        }
        return Optional.empty();
    }

    /** The lines that are clear regressions. */
    public static List<Line> regressions(List<Line> lines) {
        return lines.stream().filter(l -> l.status() == Status.REGRESSION).toList();
    }

    /**
     * A plain-text table of the current run against the latest run of the previous version and against the last
     * recorded run, with a line describing each baseline.
     */
    public static String table(List<Row> current, String version, Optional<String> previousVersion,
                               Optional<Run> previousVersionRun, Optional<Run> lastRun, Threshold threshold) {
        List<Line> vsPrevious = compare(current, previousVersionRun, threshold);
        List<Line> vsLast = compare(current, lastRun, threshold);
        StringBuilder out = new StringBuilder();
        out.append("Performance history (ratio = timed ms / reference ms, lower is faster) for version ")
            .append(version).append('\n');
        out.append("  previous version: ").append(previousVersionRun.map(PerformanceComparison::describeRun)
            .orElse(previousVersion.map(v -> v + " (no run)").orElse("none recorded"))).append('\n');
        out.append("  last run:         ").append(lastRun.map(PerformanceComparison::describeRun)
            .orElse("none recorded")).append('\n');
        out.append("  regression:       ").append(threshold.describe()).append(", against the last run\n");

        List<String[]> cells = new ArrayList<>();
        cells.add(new String[] {"subject", "measure", "ms", "ratio", "prev version", "change", "", "last run",
            "change", ""});
        for (int i = 0; i < current.size(); i++) {
            Row now = current.get(i);
            Line p = vsPrevious.get(i);
            Line l = vsLast.get(i);
            cells.add(new String[] {now.subject(), now.measure(), String.format(Locale.ROOT, "%.1f", now.timedMs()),
                ratio(now.ratio()), p.baseline().map(r -> ratio(r.ratio())).orElse("-"), p.change(),
                p.status().label(), l.baseline().map(r -> ratio(r.ratio())).orElse("-"), l.change(),
                l.status().label()});
        }
        int[] widths = new int[cells.get(0).length];
        for (String[] row : cells) {
            for (int c = 0; c < row.length; c++) {
                widths[c] = Math.max(widths[c], row[c].length());
            }
        }
        for (String[] row : cells) {
            StringBuilder line = new StringBuilder("  ");
            for (int c = 0; c < row.length; c++) {
                boolean leftAligned = c < 2 || c == 6 || c == 9;
                String format = "%" + (leftAligned ? "-" : "") + Math.max(1, widths[c]) + "s";
                line.append(String.format(Locale.ROOT, format, row[c])).append(c < row.length - 1 ? "  " : "");
            }
            out.append(line.toString().stripTrailing()).append('\n');
        }
        return out.toString();
    }

    /** A ratio with three significant digits, the precision that is meaningful for run-to-run comparisons. */
    public static String ratio(double ratio) {
        if (Double.isNaN(ratio)) {
            return "-";
        }
        if (ratio >= 100) {
            return String.format(Locale.ROOT, "%.0f", ratio);
        }
        if (ratio >= 10) {
            return String.format(Locale.ROOT, "%.1f", ratio);
        }
        if (ratio >= 1) {
            return String.format(Locale.ROOT, "%.2f", ratio);
        }
        return String.format(Locale.ROOT, "%.3f", ratio);
    }

    /** One line about a recorded run: version, commit, when, where and how busy. */
    public static String describeRun(Run run) {
        Row r = run.first();
        return String.format(Locale.ROOT, "%s at %s, %s, JDK %s, %s %s, %d CPUs, load %s, reference %.1f ms%s",
            r.version(), r.commit(), r.recordedUtc(), r.jdk(), r.os(), r.arch(), r.cpus(),
            Double.isNaN(r.loadAverage()) ? "?" : String.format(Locale.ROOT, "%.1f", r.loadAverage()),
            r.referenceMs(), r.note().isEmpty() ? "" : " (" + r.note() + ")");
    }
}
