package com.validator.cli.perf;

import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationError;
import com.validator.ValidationResult;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.index.ModelIndexBuilder;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Times this validator on the models of a performance manifest, writing the {@link PerfResults} files that the
 * Cameo macro writes too.
 *
 * <p>Phases, in this order for every repetition of a model, after a garbage collection:</p>
 * <ul>
 *   <li>{@code startup} (once): constructing the validator, which loads the standard library (from its cache);</li>
 *   <li>{@code validate}: the whole validation of the file as the CLI does it, parse included (errors and
 *       warnings). It comes first, so that its first repetition is the model's first run;</li>
 *   <li>{@code parse}: parsing the text (syntax errors);</li>
 *   <li>{@code index}: building the name-resolution index from the parse tree (elements).</li>
 * </ul>
 *
 * <p>Models with more lines than {@code largeLines} run once, as in the Cameo macro.</p>
 */
public final class ValidatorPerfRunner {

    /** The tool name in the results. */
    public static final String TOOL = "sysmlv2-validator";

    private final Path repoRoot;
    private final Path releaseRoot;
    private final int repetitions;
    private final int largeLines;
    private final Set<String> only;

    /**
     * @param repoRoot the repository root
     * @param releaseRoot the SysML-v2-Release checkout for release entries, or null
     * @param repetitions repetitions per model, at least 1
     * @param largeLines models with more lines run once
     * @param only ids to run (empty: all)
     */
    public ValidatorPerfRunner(Path repoRoot, Path releaseRoot, int repetitions, int largeLines, Set<String> only) {
        this.repoRoot = repoRoot;
        this.releaseRoot = releaseRoot;
        this.repetitions = Math.max(1, repetitions);
        this.largeLines = largeLines;
        this.only = only;
    }

    /**
     * Run the manifest and write results.csv, diagnostics.csv, environment.properties and report.md.
     *
     * @param manifest the manifest
     * @param outDir the result directory (created)
     * @return the rows written
     * @throws IOException if a file cannot be read or written
     */
    public List<PerfResults.Row> run(List<PerfManifest.Entry> manifest, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        String loadAtStart = loadAverage();
        List<PerfResults.Row> rows = new ArrayList<>();
        List<PerfResults.Diagnostic> diagnostics = new ArrayList<>();

        long t0 = System.nanoTime();
        SysMLv2ValidatorImpl validator = new SysMLv2ValidatorImpl();
        rows.add(new PerfResults.Row(PerfResults.RUN, 1, "startup", ms(t0), -1, -1, -1, "ok", heap()));
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();

        for (PerfManifest.Entry entry : manifest) {
            if (!only.isEmpty() && !only.contains(entry.id())) {
                continue;
            }
            Path file = entry.resolve(repoRoot, releaseRoot);
            if (file == null || !Files.isRegularFile(file)) {
                rows.add(new PerfResults.Row(entry.id(), 1, "all", -1, -1, -1, -1, "skipped",
                    "file not found: " + (file == null ? entry.path() : file)));
                continue;
            }
            byte[] content = Files.readAllBytes(file);
            String note = PerfManifest.sha256(content).equals(entry.sha256()) ? ""
                : "the file differs from the manifest (sha256)";
            String text = new String(content, StandardCharsets.UTF_8);
            int reps = entry.lines() > largeLines ? 1 : repetitions;
            for (int rep = 1; rep <= reps; rep++) {
                System.gc();
                timeModel(validator, parser, entry.id(), file, text, rep, note, rows, rep == 1 ? diagnostics : null);
            }
        }

        String host = host();
        String runId = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
        PerfResults.writeResults(outDir.resolve("results.csv"), TOOL, validator.getVersion(), host, runId, rows);
        PerfResults.writeDiagnostics(outDir.resolve("diagnostics.csv"), TOOL, diagnostics);
        Map<String, String> env = environment(validator.getVersion(), host, runId);
        env.put("load.average.start", loadAtStart);
        env.put("load.average.end", loadAverage());
        env.put("repetitions", repetitions + " (1 above " + largeLines + " lines)");
        env.put("library", String.valueOf(new com.validator.library.LibraryConfig().getLibraryPaths()));
        PerfResults.writeEnvironment(outDir.resolve("environment.properties"), env);
        Files.writeString(outDir.resolve("report.md"), report(manifest, PerfResults.readResults(
            outDir.resolve("results.csv"))), StandardCharsets.UTF_8);
        return rows;
    }

    private void timeModel(SysMLv2ValidatorImpl validator, SysMLv2ParserFacade parser, String id, Path file,
            String text, int rep, String note, List<PerfResults.Row> rows, List<PerfResults.Diagnostic> diagnostics) {
        String name = file.getFileName().toString();
        try {
            long t = System.nanoTime();
            ValidationResult result = validator.validate(file.toFile());
            rows.add(new PerfResults.Row(id, rep, "validate", ms(t), -1, result.getErrorCount(),
                result.getWarningCount(), "ok", note.isEmpty() ? heap() : note));
            if (diagnostics != null) {
                for (ValidationError e : result.getErrors()) {
                    diagnostics.add(diagnostic(id, e, "error"));
                }
                for (ValidationError w : result.getWarnings()) {
                    diagnostics.add(diagnostic(id, w, "warning"));
                }
            }
        } catch (IOException | RuntimeException | StackOverflowError e) {
            rows.add(new PerfResults.Row(id, rep, "validate", -1, -1, -1, -1, "failed", e.toString()));
            return;
        }
        long t = System.nanoTime();
        SysMLv2ParserFacade.ParseResult parsed = parser.parseString(text, name);
        rows.add(new PerfResults.Row(id, rep, "parse", ms(t), -1, parsed.getSyntaxErrors().size(), 0, "ok", ""));

        t = System.nanoTime();
        ModelIndexBuilder.Result index = ModelIndexBuilder.build(parsed.getParseTree(), name, true);
        rows.add(new PerfResults.Row(id, rep, "index", ms(t), index.getElements().size(), -1, -1, "ok", ""));
    }

    private static PerfResults.Diagnostic diagnostic(String id, ValidationError e, String severity) {
        String code = e.getErrorCode() == null ? "" : e.getErrorCode();
        String source = code.startsWith("SYNTAX") ? "syntax" : "validation";
        return new PerfResults.Diagnostic(id, source, severity, e.getLine(), e.getColumn(), code, e.getMessage());
    }

    private static double ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private static String heap() {
        Runtime rt = Runtime.getRuntime();
        return "heap " + (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024) + " MB";
    }

    static String host() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            return "unknown";
        }
    }

    private static Map<String, String> environment(String version, String host, String runId) {
        Map<String, String> env = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        env.put("tool", TOOL);
        env.put("tool.version", version);
        env.put("host", host);
        env.put("run.id", runId);
        env.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
            + System.getProperty("os.arch"));
        env.put("cpus", Integer.toString(rt.availableProcessors()));
        env.put("java", System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
        env.put("heap.max.mb", Long.toString(rt.maxMemory() / (1024 * 1024)));
        return env;
    }

    /** The system load average of the last minute (-1 where the platform has none): other work skews times. */
    static String loadAverage() {
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        return String.format(Locale.ROOT, "%.2f", load);
    }

    /** A per-model summary of one run. */
    static String report(List<PerfManifest.Entry> manifest, PerfResults.Run run) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(run.tool()).append(' ').append(run.version()).append(" on ").append(run.host())
            .append("\n\nMilliseconds: first repetition / median of the others. ")
            .append("`validate` is the whole validation of the file, parse included.\n\n");
        PerfResults.Timing startup = PerfResults.Timing.of(run.ok(PerfResults.RUN, "startup"));
        if (startup != null) {
            sb.append(String.format(Locale.ROOT, "Startup (standard library loaded): %.0f ms\n\n", startup.first()));
        }
        sb.append("| Model | Lines | validate | parse | index | Elements | Errors | Warnings |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (PerfManifest.Entry e : manifest) {
            PerfResults.Row skipped = run.first(e.id(), "all");
            List<PerfResults.Row> validate = run.ok(e.id(), "validate");
            if (skipped != null || validate.isEmpty() && run.first(e.id(), "validate") == null) {
                continue;
            }
            List<PerfResults.Row> index = run.ok(e.id(), "index");
            sb.append("| ").append(e.id()).append(" | ").append(e.lines()).append(" | ")
                .append(cell(validate)).append(" | ").append(cell(run.ok(e.id(), "parse"))).append(" | ")
                .append(cell(index)).append(" | ")
                .append(index.isEmpty() ? "" : Long.toString(index.get(0).elements())).append(" | ")
                .append(validate.isEmpty() ? "failed" : Long.toString(validate.get(0).errors())).append(" | ")
                .append(validate.isEmpty() ? "" : Long.toString(validate.get(0).warnings())).append(" |\n");
        }
        return sb.toString();
    }

    static String cell(List<PerfResults.Row> rows) {
        PerfResults.Timing t = PerfResults.Timing.of(rows);
        if (t == null) {
            return "–";
        }
        return t.cell();
    }
}
