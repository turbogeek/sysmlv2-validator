package com.validator.cli.compare;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares the time this validator and the OMG reference implementation (the SysML v2 Pilot Implementation, run
 * with conformance/pilot/PilotRunner.java) take on the models of perf/corpus, as Markdown.
 *
 * <p>This validator's {@code validate} (parse, name resolution and every check of one file) is compared with the
 * pilot's {@code process} (parse, linking and validation of one model by {@code SysMLInteractive}).</p>
 */
public final class PerfComparison {

    private PerfComparison() {
    }

    /**
     * Compare two result directories.
     *
     * @param manifest the manifest both ran
     * @param validatorDir this validator's result directory
     * @param pilotDir the pilot's result directory
     * @return the Markdown report
     * @throws IOException if a result file cannot be read
     */
    public static String compare(List<Manifest.Entry> manifest, Path validatorDir, Path pilotDir) throws IOException {
        Results.Run v = Results.readResults(validatorDir.resolve("results.csv"));
        Results.Run p = Results.readResults(pilotDir.resolve("results.csv"));
        Map<String, String> vEnv = Results.readEnvironment(validatorDir.resolve("environment.properties"));
        Map<String, String> pEnv = Results.readEnvironment(pilotDir.resolve("environment.properties"));

        StringBuilder sb = new StringBuilder("# Performance: this validator and the pilot implementation\n\n");
        sb.append("Compared: `").append(validatorDir.getFileName()).append("` and `").append(pilotDir.getFileName())
            .append("`.\n\n");
        environment(sb, vEnv, pEnv, v, p);

        Results.Timing vs = Results.Timing.of(v.ok(Results.RUN, "startup"));
        Results.Timing ps = Results.Timing.of(p.ok(Results.RUN, "startup"));
        sb.append("## Startup: loading the standard library\n\n| Validator | Pilot |\n|---:|---:|\n| ")
            .append(vs == null ? "–" : Results.Timing.format(vs.first())).append(" | ")
            .append(ps == null ? "–" : Results.Timing.format(ps.first())).append(" |\n\n");

        sb.append("## Time per model (ms)\n\n")
            .append("| Model | Lines | Validator `validate` | Pilot `process` | Ratio | Validator errors / warnings |")
            .append(" Pilot errors / warnings |\n|---|---:|---:|---:|---:|---:|---:|\n");
        for (Manifest.Entry e : manifest) {
            if (!ran(v, e.id()) && !ran(p, e.id())) {
                continue;
            }
            Results.Timing tv = Results.Timing.of(v.ok(e.id(), "validate"));
            Results.Timing tp = Results.Timing.of(p.ok(e.id(), "process"));
            sb.append("| ").append(e.id()).append(" | ").append(e.lines()).append(" | ")
                .append(cell(tv, v, e.id(), "validate")).append(" | ").append(cell(tp, p, e.id(), "process"))
                .append(" | ").append(ratio(tv, tp)).append(" | ").append(counts(v.first(e.id(), "validate")))
                .append(" | ").append(counts(p.first(e.id(), "process"))).append(" |\n");
        }
        sb.append("""

            ## Notes

            - Times are milliseconds: the first repetition of each model, then the median of the others. Models
              above 10,000 lines run once on both sides; their ratio compares the first runs and is marked "(1 run)".
            - **Ratio** is the pilot's time over this validator's: above 1, the validator is faster.
            - The counts differ by design where this validator adds checks the pilot does not have (lint, name
              collisions, diamonds); see conformance/ for the comparison of what each tool reports.
            - `load.average.*` is the machine's load during the run: other work on the machine skews the times.
            """);
        return sb.toString();
    }

    private static void environment(StringBuilder sb, Map<String, String> vEnv, Map<String, String> pEnv,
            Results.Run v, Results.Run p) {
        sb.append("| | Validator | Pilot |\n|---|---|---|\n");
        sb.append("| tool | ").append(v.tool()).append(' ').append(v.version()).append(" | ").append(p.tool())
            .append(' ').append(p.version()).append(" |\n");
        Set<String> keys = new TreeSet<>(vEnv.keySet());
        keys.addAll(pEnv.keySet());
        for (String k : keys) {
            if (!k.equals("tool") && !k.equals("tool.version")) {
                sb.append("| ").append(k).append(" | ").append(vEnv.getOrDefault(k, "")).append(" | ")
                    .append(pEnv.getOrDefault(k, "")).append(" |\n");
            }
        }
        if (!v.host().equals(p.host())) {
            sb.append("\n**The runs are from different machines (").append(v.host()).append(", ").append(p.host())
                .append("), so the ratios compare machines as well as tools.**\n");
        }
        sb.append('\n');
    }

    private static String ratio(Results.Timing tv, Results.Timing tp) {
        if (tv == null || tp == null) {
            return "–";
        }
        // One run on either side: compare the first runs, not a first run with a warm median
        boolean firsts = tv.single() || tp.single();
        double mine = firsts ? tv.first() : tv.warmMedian();
        double theirs = firsts ? tp.first() : tp.warmMedian();
        if (!(mine > 0) || Double.isNaN(theirs)) {
            return "–";
        }
        return String.format(Locale.ROOT, "%.1f", theirs / mine) + (firsts ? " (1 run)" : "");
    }

    /** The timing, else the status of the phase's first row, else of the model's skipped row. */
    static String cell(Results.Timing t, Results.Run run, String id, String phase) {
        if (t != null) {
            return t.cell();
        }
        Results.Row first = run.first(id, phase);
        Results.Row all = run.first(id, "all");
        return first != null ? first.status() : all != null ? all.status() : "–";
    }

    static String counts(Results.Row r) {
        if (r == null) {
            return "–";
        }
        if (!"ok".equals(r.status())) {
            return r.status();
        }
        return r.errors() + " / " + r.warnings();
    }

    /** True when a run has any row for the model, timed or not. */
    static boolean ran(Results.Run run, String id) {
        for (Results.Row r : run.rows()) {
            if (r.fileId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
