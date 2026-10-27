package com.validator.cli.compare;

import com.validator.library.LibraryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Tests for the performance comparison of this validator and the pilot: corpus, manifest, result files, runs. */
@DisplayName("Performance comparison tool")
class PerfToolTest {

    @TempDir
    Path temp;

    @Test
    @DisplayName("The committed corpus is what the generator writes")
    void committedCorpusIsCurrent() throws IOException {
        Path repo = CameoConformanceRunnerScriptTest.repoRoot();
        List<Manifest.Entry> committed = Manifest.read(repo.resolve(CompareMain.PERF_CORPUS).resolve("manifest.csv"));
        List<Manifest.Entry> generated = PerfCorpus.generate(temp, temp.resolve(CompareMain.PERF_CORPUS), null);
        assertEquals(committed.stream().map(Manifest.Entry::id).toList(),
            generated.stream().map(Manifest.Entry::id).toList());
        for (int i = 0; i < generated.size(); i++) {
            Manifest.Entry g = generated.get(i);
            Manifest.Entry c = committed.get(i);
            if (!g.family().equals("real")) {
                assertEquals(c, g, "regenerate the corpus: CompareMain perf corpus (" + g.id() + ")");
            }
            if (!c.isRelease()) {
                // The committed file itself: generated, or an existing model the manifest describes
                byte[] content = Files.readAllBytes(repo.resolve(c.path()));
                assertEquals(c.sha256(), Manifest.sha256(content), "manifest.csv is stale for " + c.id()
                    + ": run CompareMain perf corpus");
                assertEquals(c.bytes(), content.length, c.id());
            }
        }
    }

    @Test
    @DisplayName("Generated models: sizes, errors only where asked, deterministic text")
    void generator() {
        String xs = PerfCorpus.system("S", 2, false);
        assertTrue(xs.contains("package Subsystem_0002 {"));
        assertFalse(xs.contains("Subsystem_0003"));
        assertTrue(xs.contains("interface link_0001_0002 : Common::PowerLink connect"));
        assertTrue(xs.contains("Common::TotalMass((controller_0001.mass, controller_0002.mass))"));
        assertFalse(xs.contains("DataPrt"));
        assertEquals(xs, PerfCorpus.system("S", 2, false));

        String errors = PerfCorpus.system("E", 20, true);
        assertEquals(2, count(errors, "Common::DataPrt"), "every tenth subsystem");
        assertEquals(2, count(errors, ":>> costt"));

        String chain = PerfCorpus.inheritance(3);
        assertTrue(chain.contains("part def Level_0002 :> Level_0001 {"));
        assertTrue(chain.contains("part deepest : Level_0002 {"));
        assertTrue(chain.contains("attribute :>> value_0001 = 7.0;"));

        String imports = PerfCorpus.imports(12);
        assertTrue(imports.contains("public import Layer_0010::*;"));
        assertTrue(imports.contains("attribute distant : Quantity_0001_09;"));
        assertFalse(imports.contains("public import Layer_-"));
    }

    @Test
    @DisplayName("Manifest: round trip, release paths, malformed rows")
    void manifest() throws IOException {
        Path file = temp.resolve("m.sysml");
        Files.writeString(file, "a\nb", StandardCharsets.UTF_8);
        Manifest.Entry entry = Manifest.describe(file, "m", "m.sysml", "real", "-", "clean", "One, two");
        assertEquals(2, entry.lines());
        assertEquals(3, entry.bytes());
        assertEquals("One; two", entry.description());
        Manifest.Entry release = new Manifest.Entry("r", "release:x/y.sysml", "real", "-", "clean", 1, 1,
            "-", "d, with comma");
        Path manifest = temp.resolve("manifest.csv");
        Files.writeString(manifest, Manifest.HEADER + "\n# comment\n\n", StandardCharsets.UTF_8);
        assertEquals(List.of(), Manifest.read(manifest));
        Manifest.write(manifest, List.of(entry, release));
        assertEquals(List.of(entry, release), Manifest.read(manifest));
        assertEquals(temp.resolve("m.sysml"), entry.resolve(temp, null));
        assertNull(release.resolve(temp, null));
        assertEquals(temp.resolve("x/y.sysml"), release.resolve(temp, temp));
        assertEquals(0, Manifest.countLines(new byte[0]));
        assertEquals(1, Manifest.countLines("a\n".getBytes(StandardCharsets.UTF_8)));

        Files.writeString(manifest, Manifest.HEADER + "\nonly,three,columns\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> Manifest.read(manifest));
    }

    @Test
    @DisplayName("Result files: CSV quoting, timing summary, environment")
    void results() throws IOException {
        assertEquals("a,\"b,c\",\"d\"\"e\",\"f\ng\",", Results.csv("a", "b,c", "d\"e", "f\ng", null));
        assertEquals(List.of(List.of("a", "b,c", "d\"e", "f\ng", ""), List.of("x")),
            Results.parseCsv("a,\"b,c\",\"d\"\"e\",\"f\ng\",\r\nx"));

        Path results = temp.resolve("results.csv");
        Results.writeResults(results, "t", "1", "h", "r", List.of(
            new Results.Row("m", 2, "p", 30, -1, 0, 0, "ok", "x, y"),
            new Results.Row("m", 1, "p", 100, 5, 1, 2, "ok", ""),
            new Results.Row("m", 3, "p", 10, -1, 0, 0, "ok", ""),
            new Results.Row("m", 4, "p", -1, -1, -1, -1, "failed", "boom")));
        Results.Run run = Results.readResults(results);
        assertEquals("t", run.tool());
        assertEquals(3, run.ok("m", "p").size());
        assertEquals(1, run.first("m", "p").rep());
        assertNull(run.first("m", "q"));
        Results.Timing timing = Results.Timing.of(run.ok("m", "p"));
        assertEquals(100, timing.first());
        assertEquals(20, timing.warmMedian(), "median of 30 and 10");
        assertEquals(3, timing.reps());
        Results.Timing noFirst = Results.Timing.of(run.ok("m", "p").subList(2, 3));
        assertTrue(Double.isNaN(noFirst.first()), "repetition 1 is not among the rows");
        assertEquals(10, noFirst.warmMedian());
        assertEquals("– / 10.0", noFirst.cell());
        assertEquals("100", Results.Timing.of(run.ok("m", "p").subList(0, 1)).cell());
        assertNull(Results.Timing.of(List.of()));

        Files.writeString(results, "not,a,results,file\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> Results.readResults(results));
        Files.writeString(results, Results.RESULTS_HEADER + "\na,b\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> Results.readResults(results));

        Path env = temp.resolve("environment.properties");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("z", "last");
        values.put("a", "C:\\path\nnext");
        Results.writeEnvironment(env, values);
        assertEquals(Map.of("a", "C:\\path next", "z", "last"), Results.readEnvironment(env));
        assertEquals(Map.of(), Results.readEnvironment(temp.resolve("none")));

        Path diagnostics = temp.resolve("diagnostics.csv");
        List<Results.Diagnostic> written = List.of(
            new Results.Diagnostic("m", "", "syntax", "error", 3, 7, "SYNTAX_ERROR", "a, \"quoted\"\nmessage"),
            new Results.Diagnostic("k", "Car.sysml", "validation", "warning", 5, 1, "LINT020", "w"),
            new Results.Diagnostic("k", "", "validation", "info", 0, 0, "", "no line"));
        Results.writeDiagnostics(diagnostics, "t", written);
        assertEquals(written, Results.readDiagnostics(diagnostics));
        assertEquals("3", written.get(0).place());
        assertEquals("Car.sysml:5", written.get(1).place());
        assertNull(written.get(2).place());
        Files.writeString(diagnostics, "tool,file_id,source,severity,line,column,code,message\n",
            StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> Results.readDiagnostics(diagnostics), "the old format");
    }

    @Test
    @DisplayName("corpus, run and compare through the command line")
    void commandLine() throws IOException {
        assumeTrue(new LibraryConfig().hasLibraryPaths(), "needs the SysML standard library (SYSML_LIBRARY_PATH)");
        Path repo = temp.resolve("repo");
        CommandLine cli = new CommandLine(new CompareMain());
        String noRelease = temp.resolve("no-release").toString();
        assertEquals(0, cli.execute("perf", "corpus", "--repo", repo.toString(), "--release", noRelease));
        List<Manifest.Entry> manifest = Manifest.read(repo.resolve(CompareMain.PERF_CORPUS).resolve("manifest.csv"));
        assertEquals(13, manifest.size());
        // Without a checkout of SysML-v2-Release, its entries have no checksum
        assertEquals("-", manifest.get(manifest.size() - 1).sha256());

        assertEquals(2, cli.execute("perf", "compare", "--repo", repo.toString()), "nothing to compare yet");

        // Two models of the generated corpus, and one whose file is missing
        Path results = repo.resolve(CompareMain.PERF_RESULTS);
        Path validatorDir = results.resolve("validator_test_20260101-000000");
        assertEquals(0, cli.execute("perf", "run", "--repo", repo.toString(), "--release", noRelease, "--reps", "2",
            "--large-lines", "1000", "--only", "system-xs,errors-m,release-annex-a-vehicle",
            "--out", validatorDir.toString()));
        Results.Run run = Results.readResults(validatorDir.resolve("results.csv"));
        assertEquals(ValidatorRunner.TOOL, run.tool());
        assertEquals(1, run.ok(Results.RUN, "startup").size());
        assertEquals(2, run.ok("system-xs", "validate").size());
        assertEquals(0, run.ok("system-xs", "validate").get(0).errors());
        assertTrue(run.ok("system-xs", "index").get(0).elements() > 50);
        assertTrue(run.ok("errors-m", "validate").get(0).errors() > 0, "errors-m has errors");
        assertEquals(1, run.ok("errors-m", "validate").size(), "above --large-lines: one run");
        Map<String, String> env = Results.readEnvironment(validatorDir.resolve("environment.properties"));
        assertTrue(env.get("repetitions").startsWith("2 (1 above 1000 lines)"));
        assertEquals("validate, parse, index", env.get("phases"));
        assertEquals(Manifest.sha256(Files.readAllBytes(repo.resolve(CompareMain.PERF_CORPUS).resolve("manifest.csv"))),
            env.get("manifest.sha256"));
        assertEquals("skipped", run.first("release-annex-a-vehicle", "all").status());
        assertFalse(Results.readDiagnostics(validatorDir.resolve("diagnostics.csv")).isEmpty());
        String report = Files.readString(validatorDir.resolve("report.md"));
        assertTrue(report.contains("| system-xs | "), report);
        assertFalse(report.contains("release-annex-a-vehicle"), report);

        assertEquals(2, cli.execute("perf", "compare", "--repo", repo.toString()), "no pilot run yet");

        // A pilot run on another machine, then the comparison of the newest runs
        Path pilotDir = results.resolve("pilot_test_20260101-000000");
        Files.createDirectories(pilotDir);
        Results.writeResults(pilotDir.resolve("results.csv"), ConformanceComparison.PILOT, "0.63.0", "pilot-host",
            "r", List.of(new Results.Row(Results.RUN, 1, "startup", 9000, -1, -1, -1, "ok", ""),
                new Results.Row("system-xs", 1, "process", 300, -1, 0, 0, "ok", ""),
                new Results.Row("system-xs", 2, "process", 100, -1, 0, 0, "ok", ""),
                new Results.Row("errors-m", 1, "process", 2000, -1, 4, 0, "ok", "")));
        Results.writeEnvironment(pilotDir.resolve("environment.properties"),
            Map.of("tool", ConformanceComparison.PILOT, "pilot.jar", "/opt/pilot.jar"));
        assertEquals(0, cli.execute("perf", "compare", "--repo", repo.toString()));
        String comparison = Files.readString(results.resolve("comparison.md"));
        assertTrue(comparison.contains("| pilot.jar | "), comparison);
        assertTrue(comparison.contains("| 9000 |"), "the pilot's startup: " + comparison);
        assertTrue(comparison.contains("| system-xs | 105 | "), comparison);
        assertTrue(comparison.contains("| 300 / 100 |"), "first run and warm median: " + comparison);
        assertTrue(comparison.contains("(1 run)"), "errors-m ran once: " + comparison);
        assertTrue(comparison.contains(" | 4 / 0 |"), "the pilot's counts: " + comparison);
        assertTrue(comparison.contains("different machines"), comparison);

        Path explicit = temp.resolve("elsewhere/perf/comparison.md");
        assertEquals(0, cli.execute("perf", "compare", "--repo", repo.toString(), "--validator",
            validatorDir.toString(), "--pilot", pilotDir.toString(), "--out", explicit.toString()));
        assertTrue(Files.isRegularFile(explicit));
        assertEquals(0, cli.execute());
        assertEquals(0, cli.execute("perf"));
    }

    @Test
    @DisplayName("The newest result directory of a tool is chosen by the run id in its results, optionally on one host")
    void newest() throws IOException {
        Path results = temp.resolve("results");
        assertNull(CompareMain.newest(results, "cameo", null));
        // name, tool, host, run id (UTC); the names' local times disagree with the run ids on purpose
        String[][] runs = {{"cameo_a_20260102-000000", "cameo", "a", "20260102-050000"},
            {"cameo_b_20260103-000000", "cameo", "b", "20260101-235959"}, {"cameo_c_x", "cameo", "a", ""},
            {"renamed", "cameo", "c", "20260101-000000"},
            {"validator_a_20260109-000000", ValidatorRunner.TOOL, "a", "20260109-000000"}};
        for (String[] r : runs) {
            Path dir = Files.createDirectories(results.resolve(r[0]));
            if (!r[3].isEmpty()) {
                Results.writeResults(dir.resolve("results.csv"), r[1], "1", r[2], r[3], List.of(
                    new Results.Row(Results.RUN, 1, "startup", 1, -1, -1, -1, "ok", "")));
            } else {
                Files.writeString(dir.resolve("results.csv"), Results.RESULTS_HEADER + "\n",
                    StandardCharsets.UTF_8);
            }
        }
        assertEquals("cameo_a_20260102-000000", CompareMain.newest(results, "cameo", null).getFileName().toString());
        assertEquals("cameo_b_20260103-000000", CompareMain.newest(results, "cameo", "b").getFileName().toString());
        assertEquals("renamed", CompareMain.newest(results, "cameo", "c").getFileName().toString(),
            "by the tool in results.csv, not the directory name");
        assertNull(CompareMain.newest(results, "cameo", "z"));
        assertNull(CompareMain.newest(results, "pilot", null));
        assertNotNull(CompareMain.newest(results, ValidatorRunner.TOOL, "a"));
    }

    @Test
    @DisplayName("A model that differs from its manifest entry is noted")
    void changedModel() throws IOException {
        Path repo = temp.resolve("repo");
        Path model = repo.resolve("m.sysml");
        Files.createDirectories(repo);
        Files.writeString(model, "package M { part p; }\n", StandardCharsets.UTF_8);
        List<Manifest.Entry> manifest = List.of(
            Manifest.describe(model, "m", "m.sysml", "real", "-", "clean", "m"),
            new Manifest.Entry("changed", "m.sysml", "real", "-", "clean", 1, 1, "0000", "checksum differs"));
        Path out = temp.resolve("out");
        new ValidatorRunner(repo, null, 1, 10000, Set.of()).run(manifest, out);
        Results.Run run = Results.readResults(out.resolve("results.csv"));
        assertTrue(run.ok("m", "validate").get(0).detail().startsWith("heap "));
        assertEquals("the file differs from the manifest (sha256)", run.ok("changed", "validate").get(0).detail());
        assertEquals("–", ValidatorRunner.cell(List.of()));
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) {
            n++;
        }
        return n;
    }
}
