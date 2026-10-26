package com.validator.cli.perf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the Cameo macro perf/cameo/CameoPerfRunner.groovy outside Cameo, with a stand-in for Cameo, and reads its
 * output with {@link PerfResults} and {@link PerfComparison}: the macro's run logic and file formats are checked
 * on every build, although Cameo itself is not available here.
 */
@DisplayName("Cameo performance macro")
class CameoPerfRunnerScriptTest {

    /** A stand-in for Cameo with the methods the macro calls on its Cameo facade. */
    public static class FakeCameo {
        final List<Object> roots = new ArrayList<>();
        final List<String> calls = new ArrayList<>();
        boolean validateFails;
        boolean importAddsNothing;
        boolean importFailsAfterAdding;
        int undoCleared;
        boolean closed;

        public Map<String, String> environment() {
            return Map.of("cameo.sysml2.plugin", "SysML v2 test", "cameo.openapi.version", "0");
        }

        public Object createProject(String name) {
            calls.add("create " + name);
            return new Object();
        }

        public List<Object> roots(Object project) {
            return new ArrayList<>(roots);
        }

        public List<Object> build(Object project, String text) {
            return List.of(1L, 0L, List.of(List.of("text", "ERROR", 3, 7, "", "Couldn't resolve 'x'")));
        }

        public void importText(Object project, String text, String label) {
            calls.add(label);
            if (importAddsNothing) {
                return;
            }
            roots.add(new Object());
            if (importFailsAfterAdding) {
                throw new IllegalStateException("half imported");
            }
        }

        public void clearUndo(Object project) {
            undoCleared++;
        }

        public long count(List<Object> imported) {
            return imported.size() * 10L;
        }

        public List<Object> validate(Object project, List<Object> imported) {
            if (validateFails) {
                throw new IllegalStateException("no validation here");
            }
            return List.of(0L, 1L, List.of(List.of("validation", "WARNING", 0, 0, "rule, with comma", "P::x: \"m\"")),
                2);
        }

        public long export(List<Object> imported) {
            return 1234L;
        }

        public void dispose(Object project, List<Object> imported, String label) {
            roots.removeAll(imported);
        }

        public void close(Object project) {
            closed = true;
        }
    }

    @TempDir
    Path temp;

    @Test
    @DisplayName("Writes results, diagnostics and environment that the comparison reads")
    void runsWithAStandIn() throws IOException {
        Path repo = repoWithCorpus();
        FakeCameo cameo = new FakeCameo();
        Object result = runMacro(repo, cameo, Map.of("PERF_REPETITIONS", "2"));

        Path out = onlyResultDir(repo);
        assertTrue(String.valueOf(result).startsWith("Wrote " + out), String.valueOf(result));
        assertTrue(cameo.closed, "the scratch project is closed");
        assertEquals(2, cameo.undoCleared, "after each repetition");
        assertTrue(cameo.roots.isEmpty(), "every imported model is deleted again");
        assertEquals(List.of("create", "Import small", "Import small"),
            cameo.calls.stream().map(c -> c.startsWith("create ") ? "create" : c).toList());

        PerfResults.Run run = PerfResults.readResults(out.resolve("results.csv"));
        assertEquals("cameo", run.tool());
        assertEquals("SysML v2 test", run.version());
        for (String phase : List.of("build", "import", "count", "validate", "export", "dispose")) {
            assertEquals(2, run.ok("small", phase).size(), phase);
        }
        assertEquals(1, run.ok(PerfResults.RUN, "create_project").size());
        assertEquals(10, run.ok("small", "count").get(0).elements());
        assertEquals(1, run.ok("small", "build").get(0).errors());
        assertEquals(1, run.ok("small", "validate").get(0).warnings());
        assertEquals("2 suites", run.ok("small", "validate").get(0).detail());
        assertEquals("skipped", run.first("missing", "all").status());

        List<PerfResults.Diagnostic> diagnostics = PerfComparison.readDiagnostics(out.resolve("diagnostics.csv"));
        assertEquals(2, diagnostics.size(), "from the first repetition only");
        PerfResults.Diagnostic validation = diagnostics.stream().filter(d -> d.source().equals("validation"))
            .findFirst().orElseThrow();
        assertEquals("rule, with comma", validation.code());
        assertEquals("P::x: \"m\"", validation.message());
        assertEquals(3, diagnostics.stream().filter(d -> d.source().equals("text")).findFirst().orElseThrow().line());

        Map<String, String> env = PerfResults.readEnvironment(out.resolve("environment.properties"));
        assertEquals("cameo", env.get("tool"));
        assertEquals("2 (1 above 10000 lines)", env.get("repetitions"));
        assertTrue(Files.readString(out.resolve("report.md")).contains("| small | "));
        assertTrue(Files.readString(out.resolve("run-log.txt")).contains("small repetition 2 of 2"));

        // The comparison reads the macro's output next to a validator run
        Path validator = repo.resolve("perf/cameo/results/validator_test_20260101-000000");
        Files.createDirectories(validator);
        PerfResults.writeResults(validator.resolve("results.csv"), "sysmlv2-validator", "1", "other-host", "r",
            List.of(new PerfResults.Row(PerfResults.RUN, 1, "startup", 1000, -1, -1, -1, "ok", ""),
                new PerfResults.Row("small", 1, "validate", 50, -1, 0, 0, "ok", ""),
                new PerfResults.Row("small", 2, "validate", 10, -1, 0, 0, "ok", "")));
        String comparison = PerfComparison.compare(PerfManifest.read(manifest(repo)), validator, out);
        assertTrue(comparison.contains("| small | "), comparison);
        assertTrue(comparison.contains("different machines"), comparison);
    }

    @Test
    @DisplayName("An undocumented step that fails is recorded as unavailable and the run goes on")
    void unavailableStep() throws IOException {
        Path repo = repoWithCorpus();
        FakeCameo cameo = new FakeCameo();
        cameo.validateFails = true;
        runMacro(repo, cameo, Map.of("PERF_REPETITIONS", "1", "PERF_ONLY", "small"));

        PerfResults.Run run = PerfResults.readResults(onlyResultDir(repo).resolve("results.csv"));
        PerfResults.Row validate = run.first("small", "validate");
        assertEquals("unavailable", validate.status());
        assertTrue(validate.detail().contains("no validation here"), validate.detail());
        assertEquals(1, run.ok("small", "export").size(), "later steps still run");
        assertEquals(null, run.first("missing", "all"), "PERF_ONLY leaves the other models out");
    }

    @Test
    @DisplayName("PERF_UNDOCUMENTED=false times only the documented steps")
    void documentedOnly() throws IOException {
        Path repo = repoWithCorpus();
        runMacro(repo, new FakeCameo(), Map.of("PERF_REPETITIONS", "1", "PERF_UNDOCUMENTED", "false"));

        PerfResults.Run run = PerfResults.readResults(onlyResultDir(repo).resolve("results.csv"));
        assertEquals(null, run.first("small", "build"));
        assertEquals(null, run.first("small", "validate"));
        assertEquals(1, run.ok("small", "import").size());
    }

    @Test
    @DisplayName("An import that adds nothing is a failure, not zeros; a failed import leaves nothing behind")
    void importProblems() throws IOException {
        Path repo = repoWithCorpus();
        FakeCameo empty = new FakeCameo();
        empty.importAddsNothing = true;
        runMacro(repo, empty, Map.of("PERF_REPETITIONS", "1", "PERF_ONLY", "small"));
        PerfResults.Run run = PerfResults.readResults(onlyResultDir(repo).resolve("results.csv"));
        assertEquals("failed", run.first("small", "count").status());
        assertEquals("the import added no root namespace", run.first("small", "count").detail());
        assertEquals(null, run.first("small", "validate"), "nothing is timed on nothing");

        Path repo2 = temp.resolve("second");
        Files.createDirectories(repo2);
        Path again = repoWithCorpus(repo2);
        FakeCameo half = new FakeCameo();
        half.importFailsAfterAdding = true;
        runMacro(again, half, Map.of("PERF_REPETITIONS", "1", "PERF_ONLY", "small"));
        PerfResults.Run failed = PerfResults.readResults(onlyResultDir(again).resolve("results.csv"));
        assertEquals("failed", failed.first("small", "import").status());
        assertTrue(failed.first("small", "import").detail().contains("half imported"));
        assertTrue(half.roots.isEmpty(), "what the failed import left is removed");
        assertEquals(1, half.undoCleared);
        String log = Files.readString(onlyResultDir(again).resolve("run-log.txt"));
        assertTrue(log.contains("removed 1 root namespaces left by the failed import"), log);
    }

    /** Run the macro as Cameo's macro engine does: through JSR-223, settings and the stand-in as bindings. */
    private Object runMacro(Path repo, FakeCameo cameo, Map<String, String> settings) throws IOException {
        Map<String, Object> variables = new HashMap<>(settings);
        variables.put("PERF_REPO", repo.toString());
        variables.put("perfApi", cameo);
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
        assertNotNull(engine, "the groovy JSR-223 engine");
        variables.forEach(engine::put);
        try (Reader script = Files.newBufferedReader(macro(), StandardCharsets.UTF_8)) {
            return engine.eval(script);
        } catch (ScriptException e) {
            throw new AssertionError(e);
        }
    }

    /** A repository with a manifest of one small model and one model whose file is missing. */
    private Path repoWithCorpus() throws IOException {
        return repoWithCorpus(temp);
    }

    private Path repoWithCorpus(Path parent) throws IOException {
        Path repo = parent.resolve("repo");
        Path corpus = repo.resolve("perf/cameo/corpus");
        Files.createDirectories(corpus);
        Path small = corpus.resolve("small.sysml");
        Files.writeString(small, "package Small { part def A; part a : A; }\n", StandardCharsets.UTF_8);
        PerfManifest.write(manifest(repo), List.of(
            PerfManifest.describe(small, "small", "perf/cameo/corpus/small.sysml", "system", "xs", "clean",
                "A small model"),
            new PerfManifest.Entry("missing", "release:nowhere.sysml", "real", "-", "clean", 1, 1, "-",
                "Not on this machine")));
        return repo;
    }

    private static Path manifest(Path repo) {
        return repo.resolve("perf/cameo/corpus/manifest.csv");
    }

    private static Path onlyResultDir(Path repo) throws IOException {
        try (Stream<Path> dirs = Files.list(repo.resolve("perf/cameo/results"))) {
            List<Path> cameo = dirs.filter(p -> p.getFileName().toString().startsWith("cameo_")).toList();
            assertEquals(1, cameo.size(), cameo.toString());
            return cameo.get(0);
        }
    }

    /** perf/cameo/CameoPerfRunner.groovy of this checkout. */
    static Path macro() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isRegularFile(dir.resolve("perf/cameo/CameoPerfRunner.groovy"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "perf/cameo/CameoPerfRunner.groovy not found above " + Path.of("").toAbsolutePath());
        return dir.resolve("perf/cameo/CameoPerfRunner.groovy");
    }
}
