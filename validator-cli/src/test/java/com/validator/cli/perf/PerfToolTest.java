package com.validator.cli.perf;

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

/** Tests for the performance-comparison tool: corpus, manifest, result files, validator run, comparison. */
@DisplayName("Performance comparison tool")
class PerfToolTest {

    @TempDir
    Path temp;

    @Test
    @DisplayName("The committed corpus is what the generator writes")
    void committedCorpusIsCurrent() throws IOException {
        Path repo = CameoPerfRunnerScriptTest.macro().getParent().getParent().getParent();
        List<PerfManifest.Entry> committed = PerfManifest.read(repo.resolve(PerfMain.CORPUS).resolve("manifest.csv"));
        List<PerfManifest.Entry> generated = PerfCorpusGenerator.generate(temp, temp.resolve(PerfMain.CORPUS), null);
        assertEquals(committed.stream().map(PerfManifest.Entry::id).toList(),
            generated.stream().map(PerfManifest.Entry::id).toList());
        for (int i = 0; i < generated.size(); i++) {
            PerfManifest.Entry g = generated.get(i);
            PerfManifest.Entry c = committed.get(i);
            if (!g.family().equals("real")) {
                assertEquals(c, g, "regenerate the corpus: PerfMain generate (" + g.id() + ")");
            }
            if (!c.isRelease()) {
                // The committed file itself: generated, or an existing model the manifest describes
                byte[] content = Files.readAllBytes(repo.resolve(c.path()));
                assertEquals(c.sha256(), PerfManifest.sha256(content), "manifest.csv is stale for " + c.id()
                    + ": run PerfMain generate");
                assertEquals(c.bytes(), content.length, c.id());
            }
        }
    }

    @Test
    @DisplayName("Generated models: sizes, errors only where asked, deterministic text")
    void generator() {
        String xs = PerfCorpusGenerator.system("S", 2, false);
        assertTrue(xs.contains("package Subsystem_0002 {"));
        assertFalse(xs.contains("Subsystem_0003"));
        assertTrue(xs.contains("interface link_0001_0002 : Common::PowerLink connect"));
        assertTrue(xs.contains("Common::TotalMass((controller_0001.mass, controller_0002.mass))"));
        assertFalse(xs.contains("DataPrt"));
        assertEquals(xs, PerfCorpusGenerator.system("S", 2, false));

        String errors = PerfCorpusGenerator.system("E", 20, true);
        assertEquals(2, count(errors, "Common::DataPrt"), "every tenth subsystem");
        assertEquals(2, count(errors, ":>> costt"));

        String chain = PerfCorpusGenerator.inheritance(3);
        assertTrue(chain.contains("part def Level_0002 :> Level_0001 {"));
        assertTrue(chain.contains("part deepest : Level_0002 {"));
        assertTrue(chain.contains("attribute :>> value_0001 = 7.0;"));

        String imports = PerfCorpusGenerator.imports(12);
        assertTrue(imports.contains("public import Layer_0010::*;"));
        assertTrue(imports.contains("attribute distant : Quantity_0001_09;"));
        assertFalse(imports.contains("public import Layer_-"));
    }

    @Test
    @DisplayName("Manifest: round trip, release paths, malformed rows")
    void manifest() throws IOException {
        Path file = temp.resolve("m.sysml");
        Files.writeString(file, "a\nb", StandardCharsets.UTF_8);
        PerfManifest.Entry entry = PerfManifest.describe(file, "m", "m.sysml", "real", "-", "clean", "One, two");
        assertEquals(2, entry.lines());
        assertEquals(3, entry.bytes());
        assertEquals("One; two", entry.description());
        PerfManifest.Entry release = new PerfManifest.Entry("r", "release:x/y.sysml", "real", "-", "clean", 1, 1,
            "-", "d, with comma");
        Path manifest = temp.resolve("manifest.csv");
        Files.writeString(manifest, PerfManifest.HEADER + "\n# comment\n\n", StandardCharsets.UTF_8);
        assertEquals(List.of(), PerfManifest.read(manifest));
        PerfManifest.write(manifest, List.of(entry, release));
        assertEquals(List.of(entry, release), PerfManifest.read(manifest));
        assertEquals(temp.resolve("m.sysml"), entry.resolve(temp, null));
        assertNull(release.resolve(temp, null));
        assertEquals(temp.resolve("x/y.sysml"), release.resolve(temp, temp));
        assertEquals(0, PerfManifest.countLines(new byte[0]));
        assertEquals(1, PerfManifest.countLines("a\n".getBytes(StandardCharsets.UTF_8)));

        Files.writeString(manifest, PerfManifest.HEADER + "\nonly,three,columns\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> PerfManifest.read(manifest));
    }

    @Test
    @DisplayName("Result files: CSV quoting, timing summary, environment")
    void results() throws IOException {
        assertEquals("a,\"b,c\",\"d\"\"e\",\"f\ng\",", PerfResults.csv("a", "b,c", "d\"e", "f\ng", null));
        assertEquals(List.of(List.of("a", "b,c", "d\"e", "f\ng", ""), List.of("x")),
            PerfResults.parseCsv("a,\"b,c\",\"d\"\"e\",\"f\ng\",\r\nx"));

        Path results = temp.resolve("results.csv");
        PerfResults.writeResults(results, "t", "1", "h", "r", List.of(
            new PerfResults.Row("m", 2, "p", 30, -1, 0, 0, "ok", "x, y"),
            new PerfResults.Row("m", 1, "p", 100, 5, 1, 2, "ok", ""),
            new PerfResults.Row("m", 3, "p", 10, -1, 0, 0, "ok", ""),
            new PerfResults.Row("m", 4, "p", -1, -1, -1, -1, "failed", "boom")));
        PerfResults.Run run = PerfResults.readResults(results);
        assertEquals("t", run.tool());
        assertEquals(3, run.ok("m", "p").size());
        assertEquals(1, run.first("m", "p").rep());
        assertNull(run.first("m", "q"));
        PerfResults.Timing timing = PerfResults.Timing.of(run.ok("m", "p"));
        assertEquals(100, timing.first());
        assertEquals(20, timing.warmMedian(), "median of 30 and 10");
        assertEquals(3, timing.reps());
        PerfResults.Timing noFirst = PerfResults.Timing.of(run.ok("m", "p").subList(2, 3));
        assertTrue(Double.isNaN(noFirst.first()), "repetition 1 is not among the rows");
        assertEquals(10, noFirst.warmMedian());
        assertEquals("– / 10.0", noFirst.cell());
        assertEquals("100", PerfResults.Timing.of(run.ok("m", "p").subList(0, 1)).cell());
        assertNull(PerfResults.Timing.of(List.of()));

        Files.writeString(results, "not,a,results,file\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> PerfResults.readResults(results));
        Files.writeString(results, PerfResults.RESULTS_HEADER + "\na,b\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> PerfResults.readResults(results));

        Path env = temp.resolve("environment.properties");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("z", "last");
        values.put("a", "C:\\path\nnext");
        PerfResults.writeEnvironment(env, values);
        assertEquals(Map.of("a", "C:\\path next", "z", "last"), PerfResults.readEnvironment(env));
        assertEquals(Map.of(), PerfResults.readEnvironment(temp.resolve("none")));
    }

    @Test
    @DisplayName("generate, run and compare through the command line")
    void commandLine() throws IOException {
        assumeTrue(new LibraryConfig().hasLibraryPaths(), "needs the SysML standard library (SYSML_LIBRARY_PATH)");
        Path repo = temp.resolve("repo");
        CommandLine cli = new CommandLine(new PerfMain());
        String noRelease = temp.resolve("no-release").toString();
        assertEquals(0, cli.execute("generate", "--repo", repo.toString(), "--release", noRelease));
        List<PerfManifest.Entry> manifest = PerfManifest.read(repo.resolve(PerfMain.CORPUS).resolve("manifest.csv"));
        assertEquals(13, manifest.size());
        // Without a checkout of SysML-v2-Release, its entries have no checksum
        assertEquals("-", manifest.get(manifest.size() - 1).sha256());

        assertEquals(2, cli.execute("compare", "--repo", repo.toString()), "nothing to compare yet");

        // Two models of the generated corpus, and one whose file is missing
        Path validatorDir = repo.resolve(PerfMain.RESULTS).resolve("validator_test_20260101-000000");
        assertEquals(0, cli.execute("run", "--repo", repo.toString(), "--release", noRelease, "--reps", "2",
            "--large-lines", "1000", "--only", "system-xs,errors-m,release-annex-a-vehicle",
            "--out", validatorDir.toString()));
        PerfResults.Run run = PerfResults.readResults(validatorDir.resolve("results.csv"));
        assertEquals(ValidatorPerfRunner.TOOL, run.tool());
        assertEquals(1, run.ok(PerfResults.RUN, "startup").size());
        assertEquals(2, run.ok("system-xs", "validate").size());
        assertEquals(0, run.ok("system-xs", "validate").get(0).errors());
        assertTrue(run.ok("system-xs", "index").get(0).elements() > 50);
        assertTrue(run.ok("errors-m", "validate").get(0).errors() > 0, "errors-m has errors");
        assertEquals(1, run.ok("errors-m", "validate").size(), "above --large-lines: one run");
        assertTrue(PerfResults.readEnvironment(validatorDir.resolve("environment.properties")).get("repetitions")
            .startsWith("2 (1 above 1000 lines)"));
        assertEquals("skipped", run.first("release-annex-a-vehicle", "all").status());
        assertFalse(PerfComparison.readDiagnostics(validatorDir.resolve("diagnostics.csv")).isEmpty());
        String report = Files.readString(validatorDir.resolve("report.md"));
        assertTrue(report.contains("| system-xs | "), report);
        assertFalse(report.contains("release-annex-a-vehicle"), report);

        // A Cameo run with one model only partly timed, then the comparison of the newest runs
        Path cameoDir = repo.resolve(PerfMain.RESULTS).resolve("cameo_test_20260101-000000");
        Files.createDirectories(cameoDir);
        PerfResults.writeResults(cameoDir.resolve("results.csv"), "cameo", "SysML v2 test", "cameo-host", "r",
            List.of(new PerfResults.Row(PerfResults.RUN, 1, "create_project", 900, -1, -1, -1, "ok", ""),
                new PerfResults.Row("system-xs", 1, "build", 40, -1, 0, 0, "ok", ""),
                new PerfResults.Row("system-xs", 1, "import", 300, -1, -1, -1, "ok", ""),
                new PerfResults.Row("system-xs", 2, "import", 100, -1, -1, -1, "ok", ""),
                new PerfResults.Row("system-xs", 1, "validate", 200, -1, 0, 2, "ok", ""),
                new PerfResults.Row("system-xs", 2, "validate", 50, -1, 0, 2, "ok", ""),
                new PerfResults.Row("errors-m", 1, "build", -1, -1, -1, -1, "unavailable", "x"),
                new PerfResults.Row("errors-m", 1, "import", 2000, -1, -1, -1, "ok", "")));
        PerfResults.writeDiagnostics(cameoDir.resolve("diagnostics.csv"), "cameo", List.of(
            new PerfResults.Diagnostic("errors-m", "text", "ERROR", 263, 25, "", "Couldn't resolve"),
            new PerfResults.Diagnostic("errors-m", "validation", "WARNING", 0, 0, "rule", "w")));
        PerfResults.writeEnvironment(cameoDir.resolve("environment.properties"),
            Map.of("tool", "cameo", "cameo.install", "/opt/cameo"));
        assertEquals(0, cli.execute("compare", "--repo", repo.toString()));
        String comparison = Files.readString(cameoDir.resolve("comparison.md"));
        assertTrue(comparison.contains("| cameo.install | "), comparison);
        assertTrue(comparison.contains("| system-xs | 105 | "), comparison);
        assertTrue(comparison.contains("500 / 150"), "import plus validate: " + comparison);
        assertTrue(comparison.contains("| unavailable |"), comparison);
        assertTrue(comparison.contains("(import only)"), comparison);
        assertTrue(comparison.contains(", 1, 1 |"), "errors-m line 263 is flagged by both: " + comparison);
        assertTrue(comparison.contains("(1 run) (import only)"), "errors-m ran once: " + comparison);
        assertTrue(comparison.contains("different machines"), comparison);

        Path explicit = temp.resolve("comparison.md");
        assertEquals(0, cli.execute("compare", "--repo", repo.toString(), "--validator", validatorDir.toString(),
            "--cameo", cameoDir.toString(), "--out", explicit.toString()));
        assertTrue(Files.isRegularFile(explicit));
        assertEquals(0, cli.execute());
    }

    @Test
    @DisplayName("The newest result directory is chosen by the run id in its results, optionally on one host")
    void newest() throws IOException {
        Path results = temp.resolve("results");
        assertNull(PerfMain.newest(results, "cameo_", null));
        // name, host, run id (UTC); the names' local times disagree with the run ids on purpose
        String[][] runs = {{"cameo_a_20260102-000000", "a", "20260102-050000"},
            {"cameo_b_20260103-000000", "b", "20260101-235959"}, {"cameo_c_x", "a", ""},
            {"validator_a_20260109-000000", "a", "20260109-000000"}};
        for (String[] r : runs) {
            Path dir = Files.createDirectories(results.resolve(r[0]));
            if (!r[2].isEmpty()) {
                PerfResults.writeResults(dir.resolve("results.csv"), "t", "1", r[1], r[2], List.of(
                    new PerfResults.Row(PerfResults.RUN, 1, "startup", 1, -1, -1, -1, "ok", "")));
            } else {
                Files.writeString(dir.resolve("results.csv"), PerfResults.RESULTS_HEADER + "\n",
                    StandardCharsets.UTF_8);
            }
        }
        assertEquals("cameo_a_20260102-000000", PerfMain.newest(results, "cameo_", null).getFileName().toString());
        assertEquals("cameo_b_20260103-000000", PerfMain.newest(results, "cameo_", "b").getFileName().toString());
        assertNull(PerfMain.newest(results, "cameo_", "z"));
        assertNotNull(PerfMain.newest(results, "validator_", "a"));
    }

    @Test
    @DisplayName("A model that differs from its manifest entry is noted")
    void changedModel() throws IOException {
        Path repo = temp.resolve("repo");
        Path model = repo.resolve("m.sysml");
        Files.createDirectories(repo);
        Files.writeString(model, "package M { part p; }\n", StandardCharsets.UTF_8);
        List<PerfManifest.Entry> manifest = List.of(
            PerfManifest.describe(model, "m", "m.sysml", "real", "-", "clean", "m"),
            new PerfManifest.Entry("changed", "m.sysml", "real", "-", "clean", 1, 1, "0000", "checksum differs"));
        Path out = temp.resolve("out");
        new ValidatorPerfRunner(repo, null, 1, 10000, Set.of()).run(manifest, out);
        PerfResults.Run run = PerfResults.readResults(out.resolve("results.csv"));
        assertTrue(run.ok("m", "validate").get(0).detail().startsWith("heap "));
        assertEquals("the file differs from the manifest (sha256)", run.ok("changed", "validate").get(0).detail());
        assertEquals("–", ValidatorPerfRunner.cell(List.of()));
    }

    private static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) {
            n++;
        }
        return n;
    }
}
