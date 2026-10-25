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

    /**
     * The machine a run was measured on, as far as its ratios compare with another run's: the operating system
     * (without its version), the JVM's architecture, the processors and the JDK's feature release. The threshold was
     * derived from the noise of one machine; on another, the validator and the reference workload need not speed up
     * equally (a first build keeps the JIT compiler and garbage collector threads busy while the reference runs on
     * one thread, and an x86_64 JDK under Rosetta translates both differently from a native one), so a regression
     * only fails against a run of the same machine.
     */
    public record Machine(String os, String arch, int cpus, String jdk) {

        /** The machine of a recorded row; BuildInfo records the OS as its name and version, the JDK as its version. */
        public static Machine of(Row row) {
            int version = row.os().lastIndexOf(' ');
            String osName = version < 0 ? row.os() : row.os().substring(0, version);
            String feature = row.jdk().replaceFirst("^(\\d+).*$", "$1");
            return new Machine(osName, row.arch(), row.cpus(), feature);
        }

        public String describe() {
            return String.format(Locale.ROOT, "%s %s, %d CPUs, JDK %s", os, arch, cpus, jdk);
        }
    }

    /**
     * The run a clear regression fails against: the last recorded run on the machine of {@code current} (see
     * {@link Machine}), or empty when that machine has none, and then nothing can fail.
     */
    public static Optional<Run> baseline(PerformanceHistory history, List<Row> current) {
        if (current.isEmpty()) {
            return Optional.empty();
        }
        Machine here = Machine.of(current.get(0));
        return history.lastRun(run -> Machine.of(run.first()).equals(here));
    }

    /**
     * True when this run's regressions against its baseline fail the test: there are some, the run was not just
     * recorded as the new baseline (perf.record=true accepts a slowdown), and its ratios can be trusted.
     */
    public static boolean fails(List<Line> regressions, PerformanceHistory.Decision decision,
                                Optional<String> untrusted) {
        return !regressions.isEmpty() && !decision.append() && untrusted.isEmpty();
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
     * A plain-text table of the current run against the latest run of the previous version, against the last
     * recorded run and, when that was measured on another machine, against {@code baseline}, the last run on this
     * one (see {@link #baseline}), with a line describing each and saying which one a regression fails against.
     */
    public static String table(List<Row> current, String version, Optional<String> previousVersion,
                               Optional<Run> previousVersionRun, Optional<Run> lastRun, Optional<Run> baseline,
                               Threshold threshold) {
        boolean baselineIsLast = baseline.isPresent() && baseline.equals(lastRun);
        // Column groups: title, the run compared with (if any) and the comparison of each subject
        List<String> titles = new ArrayList<>(List.of("prev version", "last run"));
        List<Optional<Run>> runs = new ArrayList<>(List.of(previousVersionRun, lastRun));
        if (baseline.isPresent() && !baselineIsLast) {
            titles.add("this machine");
            runs.add(baseline);
        }
        List<List<Line>> lines = runs.stream().map(run -> compare(current, run, threshold)).toList();

        StringBuilder out = new StringBuilder();
        out.append("Performance history (ratio = timed ms / reference ms, lower is faster) for version ")
            .append(version).append('\n');
        out.append("  previous version: ").append(previousVersionRun.map(PerformanceComparison::describeRun)
            .orElse(previousVersion.map(v -> v + " (no run)").orElse("none recorded"))).append('\n');
        out.append("  last run:         ").append(lastRun.map(PerformanceComparison::describeRun)
            .orElse("none recorded")).append('\n');
        if (baseline.isPresent() && !baselineIsLast) {
            out.append("  this machine:     ").append(describeRun(baseline.get())).append('\n');
        }
        String here = current.isEmpty() ? "this machine" : Machine.of(current.get(0)).describe();
        out.append("  regression:       ").append(baseline.isPresent()
            ? threshold.describe() + (baselineIsLast ? ", against the last run" : ", against the last run on this"
                + " machine (" + here + ")")
            : lastRun.isPresent() ? "none can fail: no run is recorded on this machine (" + here + ")"
            : "none can fail: no run is recorded").append('\n');

        List<String[]> cells = new ArrayList<>();
        List<String> header = new ArrayList<>(List.of("subject", "measure", "ms", "ratio"));
        titles.forEach(title -> header.addAll(List.of(title, "change", "")));
        cells.add(header.toArray(String[]::new));
        for (int i = 0; i < current.size(); i++) {
            Row now = current.get(i);
            List<String> row = new ArrayList<>(List.of(now.subject(), now.measure(),
                String.format(Locale.ROOT, "%.1f", now.timedMs()), ratio(now.ratio())));
            for (int g = 0; g < runs.size(); g++) {
                Line line = lines.get(g).get(i);
                // Without a run to compare with every subject would be "new": the status is left blank instead
                row.addAll(List.of(line.baseline().map(r -> ratio(r.ratio())).orElse("-"), line.change(),
                    runs.get(g).isPresent() ? line.status().label() : ""));
            }
            cells.add(row.toArray(String[]::new));
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
                // The subject, the measure and each group's status are text; the rest are numbers
                boolean leftAligned = c < 2 || c >= 4 && (c - 4) % 3 == 2;
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
