package com.validator.cli.compare;

import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationError;
import com.validator.ValidationResult;
import com.validator.kpar.KparArchive;
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
import java.util.TreeSet;

/**
 * Runs this validator on the models of a manifest (conformance or performance), writing the {@link Results}
 * files that the pilot runner and the Cameo macro write too.
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
 * <p>Models with more lines than {@code largeLines} run once, as in the pilot runner. With {@code validateOnly}
 * (the conformance test), only {@code validate} runs. A KPAR is validated as one model; its diagnostics name the
 * member they are in.</p>
 */
public final class ValidatorRunner {

    /** The tool name in the results. */
    public static final String TOOL = "sysmlv2-validator";

    private final Path repoRoot;
    private final Path releaseRoot;
    private final int repetitions;
    private final int largeLines;
    private final Set<String> only;
    private boolean validateOnly;
    private String manifestSha256 = "";

    /**
     * @param repoRoot the repository root
     * @param releaseRoot the SysML-v2-Release checkout for release entries, or null
     * @param repetitions repetitions per model, at least 1
     * @param largeLines models with more lines run once
     * @param only ids to run (empty: all)
     */
    public ValidatorRunner(Path repoRoot, Path releaseRoot, int repetitions, int largeLines, Set<String> only) {
        this.repoRoot = repoRoot;
        this.releaseRoot = releaseRoot;
        this.repetitions = Math.max(1, repetitions);
        this.largeLines = largeLines;
        this.only = only;
    }

    /**
     * Time only {@code validate}, not {@code parse} and {@code index}: what the conformance test needs.
     *
     * @return this runner
     */
    public ValidatorRunner validateOnly() {
        this.validateOnly = true;
        return this;
    }

    /**
     * Record the SHA-256 of the manifest file in the environment, so that a comparison can tell runs of different
     * manifests apart.
     *
     * @param manifestFile the manifest that is run
     * @return this runner
     * @throws IOException if the file cannot be read
     */
    public ValidatorRunner manifest(Path manifestFile) throws IOException {
        this.manifestSha256 = Manifest.sha256(Files.readAllBytes(manifestFile));
        return this;
    }

    /**
     * Run the manifest and write results.csv, diagnostics.csv, environment.properties and report.md.
     *
     * @param manifest the manifest
     * @param outDir the result directory (created)
     * @return the rows written
     * @throws IOException if a file cannot be read or written
     */
    public List<Results.Row> run(List<Manifest.Entry> manifest, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        String loadAtStart = loadAverage();
        Set<String> unknown = new TreeSet<>(only);
        manifest.forEach(e -> unknown.remove(e.id()));
        if (!unknown.isEmpty()) {
            System.err.println("Not in the manifest, so not run: " + String.join(", ", unknown));
        }
        List<Results.Row> rows = new ArrayList<>();
        List<Results.Diagnostic> diagnostics = new ArrayList<>();

        long t0 = System.nanoTime();
        SysMLv2ValidatorImpl validator = new SysMLv2ValidatorImpl();
        rows.add(new Results.Row(Results.RUN, 1, "startup", ms(t0), -1, -1, -1, "ok", heap()));
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();

        for (Manifest.Entry entry : manifest) {
            if (!only.isEmpty() && !only.contains(entry.id())) {
                continue;
            }
            Path file = entry.resolve(repoRoot, releaseRoot);
            if (file == null || !Files.isRegularFile(file)) {
                rows.add(new Results.Row(entry.id(), 1, "all", -1, -1, -1, -1, "skipped",
                    "file not found: " + (file == null ? entry.path() : file)));
                continue;
            }
            byte[] content = Files.readAllBytes(file);
            String note = Manifest.sha256(content).equals(entry.sha256()) ? ""
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
        Results.writeResults(outDir.resolve("results.csv"), TOOL, validator.getVersion(), host, runId, rows);
        Results.writeDiagnostics(outDir.resolve("diagnostics.csv"), TOOL, diagnostics);
        Map<String, String> env = environment(validator.getVersion(), host, runId);
        env.put("load.average.start", loadAtStart);
        env.put("load.average.end", loadAverage());
        env.put("repetitions", repetitions + " (1 above " + largeLines + " lines)");
        com.validator.library.LibraryConfig libraries = new com.validator.library.LibraryConfig();
        env.put("library", String.valueOf(libraries.getLibraryPaths()));
        env.put("library.dassault", String.valueOf(libraries.getDSViewsPaths()));
        env.put("phases", validateOnly ? "validate" : "validate, parse, index");
        env.put("manifest.sha256", manifestSha256);
        env.put("only", String.join(",", only));
        Results.writeEnvironment(outDir.resolve("environment.properties"), env);
        Files.writeString(outDir.resolve("report.md"), report(manifest, Results.readResults(
            outDir.resolve("results.csv"))), StandardCharsets.UTF_8);
        return rows;
    }

    private void timeModel(SysMLv2ValidatorImpl validator, SysMLv2ParserFacade parser, String id, Path file,
            String text, int rep, String note, List<Results.Row> rows, List<Results.Diagnostic> diagnostics) {
        String name = file.getFileName().toString();
        try {
            long t = System.nanoTime();
            ValidationResult result = validator.validate(file.toFile());
            rows.add(new Results.Row(id, rep, "validate", ms(t), -1, result.getErrorCount(),
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
            rows.add(new Results.Row(id, rep, "validate", -1, -1, -1, -1, "failed",
                note.isEmpty() ? e.toString() : e + "; " + note));
            return;
        }
        if (validateOnly || name.toLowerCase(java.util.Locale.ROOT).endsWith(".kpar")) {
            return;
        }
        long t = System.nanoTime();
        SysMLv2ParserFacade.ParseResult parsed = parser.parseString(text, name);
        rows.add(new Results.Row(id, rep, "parse", ms(t), -1, parsed.getSyntaxErrors().size(), 0, "ok", ""));

        t = System.nanoTime();
        ModelIndexBuilder.Result index = ModelIndexBuilder.build(parsed.getParseTree(), name, true);
        rows.add(new Results.Row(id, rep, "index", ms(t), index.getElements().size(), -1, -1, "ok", ""));
    }

    static Results.Diagnostic diagnostic(String id, ValidationError e, String severity) {
        String code = e.getErrorCode() == null ? "" : e.getErrorCode();
        String source = code.startsWith("SYNTAX") ? "syntax" : code.startsWith("IO_") ? "io" : "validation";
        return new Results.Diagnostic(id, member(e.getFilePath()), source, severity, e.getLine(), e.getColumn(),
            code, e.getMessage());
    }

    /** The member of a KPAR a diagnostic is in: its path is "archive.kpar!member"; empty for a plain file. */
    static String member(String filePath) {
        String member = filePath == null ? null : KparArchive.memberOf(filePath);
        return member == null ? "" : member;
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
    static String report(List<Manifest.Entry> manifest, Results.Run run) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(run.tool()).append(' ').append(run.version()).append(" on ").append(run.host())
            .append("\n\nMilliseconds: first repetition / median of the others. ")
            .append("`validate` is the whole validation of the file, parse included.\n\n");
        Results.Timing startup = Results.Timing.of(run.ok(Results.RUN, "startup"));
        if (startup != null) {
            sb.append(String.format(Locale.ROOT, "Startup (standard library loaded): %.0f ms\n\n", startup.first()));
        }
        sb.append("| Model | Lines | validate | parse | index | Elements | Errors | Warnings |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Manifest.Entry e : manifest) {
            Results.Row skipped = run.first(e.id(), "all");
            List<Results.Row> validate = run.ok(e.id(), "validate");
            if (skipped != null || validate.isEmpty() && run.first(e.id(), "validate") == null) {
                continue;
            }
            List<Results.Row> index = run.ok(e.id(), "index");
            sb.append("| ").append(e.id()).append(" | ").append(e.lines()).append(" | ")
                .append(cell(validate)).append(" | ").append(cell(run.ok(e.id(), "parse"))).append(" | ")
                .append(cell(index)).append(" | ")
                .append(index.isEmpty() ? "" : Long.toString(index.get(0).elements())).append(" | ")
                .append(validate.isEmpty() ? "failed" : Long.toString(validate.get(0).errors())).append(" | ")
                .append(validate.isEmpty() ? "" : Long.toString(validate.get(0).warnings())).append(" |\n");
        }
        return sb.toString();
    }

    static String cell(List<Results.Row> rows) {
        Results.Timing t = Results.Timing.of(rows);
        if (t == null) {
            return "–";
        }
        return t.cell();
    }
}
