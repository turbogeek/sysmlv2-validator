package com.validator.cli.perf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares a run of this validator ({@link ValidatorPerfRunner}) with a run of the Cameo macro
 * (perf/cameo/CameoPerfRunner.groovy) on the same manifest, as Markdown.
 *
 * <p>Cameo phases: {@code create_project} (once), {@code build} (parse and name resolution into a transient
 * model, with the text diagnostics), {@code import} (the documented textual import into the project, which
 * builds again and copies the model), {@code count}, {@code validate} (Cameo's SysML v2 validation of the
 * imported model), {@code export} and {@code dispose}. The end-to-end time compared with this validator's
 * {@code validate} is Cameo's {@code import} plus {@code validate}: text in, model checked.</p>
 */
public final class PerfComparison {

    private PerfComparison() {
    }

    /**
     * Compare two result directories.
     *
     * @param manifest the manifest both ran
     * @param validatorDir this validator's result directory
     * @param cameoDir the Cameo result directory
     * @return the Markdown report
     * @throws IOException if a result file cannot be read
     */
    public static String compare(List<PerfManifest.Entry> manifest, Path validatorDir, Path cameoDir)
            throws IOException {
        PerfResults.Run v = PerfResults.readResults(validatorDir.resolve("results.csv"));
        PerfResults.Run c = PerfResults.readResults(cameoDir.resolve("results.csv"));
        Map<String, String> vEnv = PerfResults.readEnvironment(validatorDir.resolve("environment.properties"));
        Map<String, String> cEnv = PerfResults.readEnvironment(cameoDir.resolve("environment.properties"));
        List<PerfResults.Diagnostic> vDiag = readDiagnostics(validatorDir.resolve("diagnostics.csv"));
        List<PerfResults.Diagnostic> cDiag = readDiagnostics(cameoDir.resolve("diagnostics.csv"));

        StringBuilder sb = new StringBuilder("# Performance: this validator and Cameo\n\n");
        environment(sb, vEnv, cEnv, v, c);
        startup(sb, v, c);
        times(sb, manifest, v, c);
        diagnostics(sb, manifest, v, c, vDiag, cDiag);
        sb.append("""

            ## What the phases are

            - **validator `validate`**: the whole validation of one file, as the CLI does it: parse, name resolution
              and every semantic check, on a JVM where the standard library is already loaded.
            - **Cameo `build`**: parse and name resolution into a transient model (`SysMLTransientModelBuilder`), with
              the text diagnostics. Undocumented API.
            - **Cameo `import`**: `SysMLTextualNotationService.importTextual`, the documented import, which builds
              again and copies the model into the project.
            - **Cameo `validate`**: Cameo's SysML v2 validation of the imported model, active and passive suites
              (`ValidationService`, `ValidationUtils.validateRecursively`). Undocumented API.
            - Times are milliseconds: the first repetition of each model, then the median of the others. Each
              repetition runs its phases in order on the same text, so a later phase can profit from an earlier one
              (both tools parse the text twice: validator `parse` then `validate`, Cameo `build` then `import`).
            - `load.average.*` is the machine's load during the run: other work on the machine skews the times.
            - **Ratio** is Cameo's `import` plus `validate` over the validator's `validate`, warm medians: above 1,
              the validator is faster. Where either side ran a model once (models above 10,000 lines run once),
              the first runs are compared, marked "(1 run)"; "(import only)" marks a ratio without Cameo's
              `validate`.
            """);
        return sb.toString();
    }

    private static void environment(StringBuilder sb, Map<String, String> vEnv, Map<String, String> cEnv,
            PerfResults.Run v, PerfResults.Run c) {
        sb.append("| | Validator | Cameo |\n|---|---|---|\n");
        Set<String> keys = new TreeSet<>(vEnv.keySet());
        keys.addAll(cEnv.keySet());
        sb.append("| tool | ").append(v.tool()).append(' ').append(v.version()).append(" | ").append(c.tool())
            .append(' ').append(c.version()).append(" |\n");
        for (String k : keys) {
            if (!k.equals("tool") && !k.equals("tool.version")) {
                sb.append("| ").append(k).append(" | ").append(vEnv.getOrDefault(k, "")).append(" | ")
                    .append(cEnv.getOrDefault(k, "")).append(" |\n");
            }
        }
        if (!v.host().equals(c.host())) {
            sb.append("\n**The runs are from different machines (").append(v.host()).append(", ").append(c.host())
                .append("), so the times and ratios compare machines as well as tools.** Run both on one machine for")
                .append(" a fair comparison.\n");
        }
        sb.append('\n');
    }

    private static void startup(StringBuilder sb, PerfResults.Run v, PerfResults.Run c) {
        PerfResults.Timing vs = PerfResults.Timing.of(v.ok(PerfResults.RUN, "startup"));
        PerfResults.Timing cs = PerfResults.Timing.of(c.ok(PerfResults.RUN, "create_project"));
        sb.append("## Startup\n\n| Validator: load the standard library | Cameo: create a SysML v2 project |\n")
            .append("|---:|---:|\n| ").append(vs == null ? "–" : PerfResults.Timing.format(vs.first())).append(" | ")
            .append(cs == null ? "–" : PerfResults.Timing.format(cs.first())).append(" |\n\n");
    }

    private static void times(StringBuilder sb, List<PerfManifest.Entry> manifest, PerfResults.Run v,
            PerfResults.Run c) {
        sb.append("## Time per model (ms)\n\n")
            .append("| Model | Lines | Validator `validate` | Cameo `build` | Cameo `import` | Cameo `validate` |")
            .append(" Cameo `import` + `validate` | Ratio |\n|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (PerfManifest.Entry e : manifest) {
            if (!ran(v, e.id()) && !ran(c, e.id())) {
                continue;
            }
            PerfResults.Timing tv = PerfResults.Timing.of(v.ok(e.id(), "validate"));
            PerfResults.Timing tImport = PerfResults.Timing.of(c.ok(e.id(), "import"));
            PerfResults.Timing tValidate = PerfResults.Timing.of(c.ok(e.id(), "validate"));
            String sum = "–";
            String ratio = "–";
            if (tImport != null) {
                String only = tValidate == null ? " (import only)" : "";
                double first = tImport.first() + (tValidate == null ? 0 : tValidate.first());
                double warm = tImport.warmMedian() + (tValidate == null ? 0 : tValidate.warmMedian());
                boolean single = tImport.single() || tValidate != null && tValidate.single();
                sum = (single ? PerfResults.Timing.format(first) : PerfResults.Timing.format(first) + " / "
                    + PerfResults.Timing.format(warm)) + only;
                if (tv != null) {
                    // One run on either side: compare the first runs, not a first run with a warm median
                    boolean firsts = single || tv.single();
                    double mine = firsts ? tv.first() : tv.warmMedian();
                    double theirs = firsts ? first : warm;
                    if (mine > 0 && !Double.isNaN(theirs)) {
                        ratio = String.format(Locale.ROOT, "%.1f", theirs / mine) + (firsts ? " (1 run)" : "")
                            + only;
                    }
                }
            }
            sb.append("| ").append(e.id()).append(" | ").append(e.lines()).append(" | ")
                .append(cell(tv, v, e.id(), "validate")).append(" | ")
                .append(cell(PerfResults.Timing.of(c.ok(e.id(), "build")), c, e.id(), "build")).append(" | ")
                .append(cell(tImport, c, e.id(), "import")).append(" | ")
                .append(cell(tValidate, c, e.id(), "validate")).append(" | ")
                .append(sum).append(" | ").append(ratio).append(" |\n");
        }
        sb.append('\n');
    }

    private static void diagnostics(StringBuilder sb, List<PerfManifest.Entry> manifest, PerfResults.Run v,
            PerfResults.Run c, List<PerfResults.Diagnostic> vDiag, List<PerfResults.Diagnostic> cDiag) {
        sb.append("## Diagnostics (first repetition)\n\n")
            .append("Lines with an error: the lines where each tool reports at least one error, and how many lines")
            .append(" both flag.\n\n")
            .append("| Model | Expected | Validator errors / warnings | Cameo text errors / warnings |")
            .append(" Cameo validation errors / warnings | Error lines: validator, Cameo, both |\n")
            .append("|---|---|---:|---:|---:|---:|\n");
        for (PerfManifest.Entry e : manifest) {
            if (!ran(v, e.id()) && !ran(c, e.id())) {
                continue;
            }
            PerfResults.Row vv = v.first(e.id(), "validate");
            PerfResults.Row cb = c.first(e.id(), "build");
            PerfResults.Row cv = c.first(e.id(), "validate");
            Set<Integer> vLines = errorLines(vDiag, e.id());
            Set<Integer> cLines = errorLines(cDiag, e.id());
            Set<Integer> both = new TreeSet<>(vLines);
            both.retainAll(cLines);
            sb.append("| ").append(e.id()).append(" | ").append(e.expect()).append(" | ").append(counts(vv))
                .append(" | ").append(counts(cb)).append(" | ").append(counts(cv)).append(" | ")
                .append(vLines.size()).append(", ").append(cLines.size()).append(", ").append(both.size())
                .append(" |\n");
        }
    }

    private static Set<Integer> errorLines(List<PerfResults.Diagnostic> diagnostics, String id) {
        Set<Integer> lines = new LinkedHashSet<>();
        for (PerfResults.Diagnostic d : diagnostics) {
            String severity = d.severity().toLowerCase(Locale.ROOT);
            if (d.fileId().equals(id) && d.line() > 0 && (severity.equals("error") || severity.equals("fatal"))) {
                lines.add(d.line());
            }
        }
        return lines;
    }

    private static String counts(PerfResults.Row r) {
        if (r == null) {
            return "–";
        }
        if (!"ok".equals(r.status())) {
            return r.status();
        }
        return r.errors() + " / " + r.warnings();
    }

    /** The timing, else the status of the phase's first row, else of the model's skipped row. */
    private static String cell(PerfResults.Timing t, PerfResults.Run run, String id, String phase) {
        if (t != null) {
            return t.cell();
        }
        PerfResults.Row first = run.first(id, phase);
        PerfResults.Row all = run.first(id, "all");
        return first != null ? first.status() : all != null ? all.status() : "–";
    }

    /** True when a run has any row for the model, timed or not. */
    private static boolean ran(PerfResults.Run run, String id) {
        for (PerfResults.Row r : run.rows()) {
            if (r.fileId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Read diagnostics.csv.
     *
     * @param file the CSV file; a missing file reads as empty
     * @return the diagnostics
     * @throws IOException if it cannot be read or is malformed
     */
    static List<PerfResults.Diagnostic> readDiagnostics(Path file) throws IOException {
        List<PerfResults.Diagnostic> result = new ArrayList<>();
        if (!Files.isRegularFile(file)) {
            return result;
        }
        List<List<String>> records = PerfResults.parseCsv(Files.readString(file, StandardCharsets.UTF_8));
        for (List<String> f : records.subList(Math.min(1, records.size()), records.size())) {
            if (f.size() != 8) {
                throw new IOException(file + ": expected 8 columns, found " + f.size() + ": " + f);
            }
            result.add(new PerfResults.Diagnostic(f.get(1), f.get(2), f.get(3), parseInt(f.get(4)),
                parseInt(f.get(5)), f.get(6), f.get(7)));
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
}
