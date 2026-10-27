package com.validator.cli.compare;

import com.validator.ErrorCodes;
import com.validator.library.LibraryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Tests for the conformance comparison: corpus, KPARs, verdicts, report and command line. */
@DisplayName("Conformance comparison tool")
class ConformanceToolTest {

    private static final String RUN_ID = "20260101-000000";

    @TempDir
    Path temp;

    @Test
    @DisplayName("The committed manifest and KPARs are what the generator writes")
    void committedCorpusIsCurrent() throws IOException {
        Path repo = CameoConformanceRunnerScriptTest.repoRoot();
        Path conformance = repo.resolve(CompareMain.CONFORMANCE);
        List<Manifest.Entry> committed = Manifest.read(conformance.resolve("manifest.csv"));
        Path out = temp.resolve("conformance");
        List<Manifest.Entry> generated = ConformanceCorpus.generate(repo, out);
        assertEquals(committed.stream().map(Manifest.Entry::id).toList(),
            generated.stream().map(Manifest.Entry::id).toList(), "regenerate: CompareMain conformance corpus");
        for (int i = 0; i < generated.size(); i++) {
            Manifest.Entry g = generated.get(i);
            Manifest.Entry c = committed.get(i);
            String hint = "regenerate the corpus: CompareMain conformance corpus (" + g.id() + ")";
            assertEquals(c, g, hint);
            if (g.family().equals("kpar")) {
                assertArrayEquals(Files.readAllBytes(out.resolve("kpar").resolve(Path.of(c.path()).getFileName())),
                    Files.readAllBytes(repo.resolve(c.path())), hint);
            }
        }
        assertEquals(6, committed.stream().filter(e -> e.family().equals("kpar")).count());
        assertTrue(committed.size() > 200, "the repository's test files");
    }

    @Test
    @DisplayName("A KPAR: project and meta files first, an index of top packages, checksums, same bytes every time")
    void kpar() throws IOException {
        Map<String, String> members = new LinkedHashMap<>();
        members.put("Vehicle.sysml", ConformanceCorpus.VEHICLE);
        members.put("Quoted.sysml", "package 'My Package' { part p; }\nlibrary package Lib;\n");
        byte[] archive = ConformanceCorpus.kpar("A \"quoted\" title", members);
        assertArrayEquals(archive, ConformanceCorpus.kpar("A \"quoted\" title", members), "deterministic");
        TimeZone zone = TimeZone.getDefault();
        try {
            for (String other : List.of("UTC", "Asia/Tokyo", "America/Los_Angeles")) {
                TimeZone.setDefault(TimeZone.getTimeZone(other));
                assertArrayEquals(archive, ConformanceCorpus.kpar("A \"quoted\" title", members),
                    "the same bytes in " + other);
            }
        } finally {
            TimeZone.setDefault(zone);
        }

        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                assertEquals(ZipEntry.STORED, e.getMethod(), e.getName());
                assertEquals(null, e.getExtra(), "no time stamps in extra fields: " + e.getName());
                files.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertEquals(List.of(".project.json", ".meta.json", "Vehicle.sysml", "Quoted.sysml"),
            new ArrayList<>(files.keySet()));
        assertEquals("{\"name\":\"A \\\"quoted\\\" title\",\"version\":\"1.0.0\",\"usage\":[]}",
            files.get(".project.json"));
        String meta = files.get(".meta.json");
        assertTrue(meta.startsWith("{\"index\":{\"VehicleDefinitions\":\"Vehicle.sysml\",\"My Package\":"
            + "\"Quoted.sysml\",\"Lib\":\"Quoted.sysml\"}"), meta);
        assertTrue(meta.contains("\"Vehicle.sysml\":{\"value\":\""
            + Manifest.sha256(ConformanceCorpus.VEHICLE.getBytes(StandardCharsets.UTF_8))
            + "\",\"algorithm\":\"SHA256\"}"), meta);
        assertEquals(ConformanceCorpus.VEHICLE, files.get("Vehicle.sysml"));
    }

    @Test
    @DisplayName("Verdicts and the report of three tools: disagreements both ways, error places, added checks")
    void comparison() throws IOException {
        List<Manifest.Entry> manifest = List.of(entry("a", "clean"), entry("b", "errors"), entry("c", "clean"),
            entry("d", "errors"), entry("e", "invalid"), entry("f", "clean"), entry("g", "warnings"),
            entry("h", "clean"), entry("i", "errors"));
        String lint = "LINT020";
        String collision = ErrorCodes.ALIAS_NAME_COLLISION;
        Path v = temp.resolve("validator");
        writeRun(v, ValidatorRunner.TOOL, Map.of("a", "accepted", "b", "rejected", "c", "accepted", "d", "rejected",
            "e", "failed", "f", "skipped", "g", "accepted", "h", "accepted", "i", "rejected"), List.of(
                new Results.Diagnostic("i", "", "validation", "error", 5, 1, "SEMANTIC_X", "i"),
                new Results.Diagnostic("b", "", "validation", "error", 3, 1, "SEMANTIC_X", "x"),
                new Results.Diagnostic("d", "", "validation", "error", 7, 1, "SEMANTIC_Y", "y"),
                new Results.Diagnostic("g", "", "validation", "warning", 2, 1, lint, "unit"),
                new Results.Diagnostic("g", "", "validation", "warning", 4, 1, collision, "alias"),
                new Results.Diagnostic("g", "", "validation", "warning", 6, 1, "SEMANTIC_Z", "plain")));
        Path p = temp.resolve("pilot");
        writeRun(p, ConformanceComparison.PILOT, Map.of("a", "accepted", "b", "rejected", "c", "rejected",
            "d", "accepted", "e", "failed", "g", "accepted", "h", "accepted", "i", "rejected"), List.of(
                new Results.Diagnostic("i", "", "linking", "error", 5, 1, "", "i"),
                new Results.Diagnostic("i", "", "linking", "error", 6, 1, "", "i"),
                new Results.Diagnostic("b", "", "validation", "error", 3, 1, "", "x"),
                new Results.Diagnostic("c", "", "validation", "error", 5, 1, "", "c")));
        Path c = temp.resolve("cameo");
        writeRun(c, ConformanceComparison.CAMEO, Map.of("a", "accepted", "b", "rejected", "c", "rejected",
            "d", "accepted", "e", "failed", "g", "accepted", "h", "unchecked", "i", "rejected"), List.of(
                new Results.Diagnostic("i", "", "validation", "error", 0, 0, "rule", "on an element"),
                new Results.Diagnostic("b", "", "text", "error", 4, 1, "", "x"),
                new Results.Diagnostic("c", "Car.sysml", "text", "error", 5, 9, "", "Couldn't resolve\n'Vehicel'"),
                new Results.Diagnostic("g", "", "validation", "warning", 0, 0, "rule", "w")));

        String report = ConformanceComparison.compare(manifest, List.of(c, v, p));
        assertTrue(report.startsWith("# Conformance: Validator, Pilot, Cameo"), "the tools in a fixed order");
        assertTrue(report.contains("8 models."), "f is skipped by all that ran it: " + report);
        assertTrue(report.contains("- All 3 tools give the same verdict on 5 of the 7 models all of them judged."),
            report);
        assertTrue(report.contains("- Validator and Cameo: the same verdict on 5; Validator accepts and Cameo rejects"
            + " 1; Validator rejects and Cameo accepts 1; not compared 1."), report);
        assertTrue(report.contains("- Validator and Pilot: the same verdict on 6; Validator accepts and Pilot rejects"
            + " 1; Validator rejects and Pilot accepts 1."), report);
        assertTrue(report.contains("- Cameo has no verdict on unchecked 1."), report);
        assertTrue(section(report, "### Some error lines in common").contains("- **i**: in common 5; only Validator "
            + "none; only Pilot 6; Cameo reports no lines"), report);
        String accepts = section(report, "## Validator accepts, Cameo rejects");
        assertTrue(accepts.contains("- **c** (`c.sysml`, expected clean): Cameo rejected"), accepts);
        assertTrue(accepts.contains("  - Car.sysml:5: text: Couldn't resolve 'Vehicel'"), accepts);
        String rejects = section(report, "## Validator rejects, Cameo accepts");
        assertTrue(rejects.contains("- **d** (`d.sysml`, expected errors): Validator rejected"), rejects);
        assertTrue(rejects.contains("  - 7: validation SEMANTIC_Y: y"), rejects);
        assertTrue(section(report, "## Pilot and Cameo disagree").contains("None."), report);
        assertTrue(section(report, "## Validator and Pilot disagree").contains("- c: Validator accepted, Pilot "
            + "rejected"), report);
        assertTrue(section(report, "### No error line in common")
            .contains("- **b**: none in common; only Validator 3; only Pilot 3; only Cameo 4"), report);
        assertTrue(report.contains("| e | invalid | failed | failed | failed |"), report);
        assertTrue(report.contains("| g | warnings | accepted | accepted | accepted | 0 / 1 (+2) | 0 / 0 | 0 / 1 |"),
            "added-check warnings apart: " + report);
        assertFalse(report.contains("| f |"), report);

        String csv = ConformanceComparison.verdicts(manifest, List.of(v, c));
        assertTrue(csv.startsWith("id,family,expect,sysmlv2-validator,sysmlv2-validator_errors,"
            + "sysmlv2-validator_warnings,cameo,cameo_errors,cameo_warnings\n"), csv);
        assertTrue(csv.contains("\nf,test,clean,skipped,0,0,missing,0,0\n"), csv);
        assertTrue(csv.contains("\ng,test,warnings,accepted,0,3,accepted,0,1\n"), csv);

        String two = ConformanceComparison.compare(manifest, List.of(v, p));
        assertFalse(two.contains("All 2 tools"), two);
        assertFalse(two.contains("## Validator accepts, Cameo rejects"), "no Cameo run: " + two);
    }

    @Test
    @DisplayName("Which warnings belong to the checks this validator adds")
    void addedChecks() {
        assertTrue(ConformanceComparison.addedCheck("LINT001"));
        assertTrue(ConformanceComparison.addedCheck(ErrorCodes.ALIAS_NAME_COLLISION));
        assertTrue(ConformanceComparison.addedCheck(ErrorCodes.DIAMOND_INHERITANCE_CONFLICT));
        assertTrue(ConformanceComparison.addedCheck("LINE_COMMENT_NOT_PERSISTED"));
        assertFalse(ConformanceComparison.addedCheck("SEMANTIC_UNRESOLVED_TYPE"));
        assertFalse(ConformanceComparison.addedCheck(null));
    }

    @Test
    @DisplayName("Verdicts: a failed or skipped row, errors of the deciding phases only, the first repetition only")
    void verdicts() {
        Results.Run run = new Results.Run("cameo", "1", "h", List.of(
            new Results.Row(Results.RUN, 1, "create_project", 1, -1, -1, -1, "ok", ""),
            new Results.Row("ok", 1, "import", 1, -1, 0, 0, "ok", ""),
            new Results.Row("ok", 1, "dispose", 1, -1, 5, 0, "ok", "not a deciding phase"),
            new Results.Row("ok", 1, "build", 1, -1, 0, 0, "ok", ""),
            new Results.Row("unchecked", 1, "import", 1, -1, 0, 0, "ok", ""),
            new Results.Row("unchecked", 1, "validate", 1, -1, 0, 0, "ok", "2 suites"),
            new Results.Row("unchecked", 1, "build", -1, -1, -1, -1, "unavailable", "renamed"),
            new Results.Row("validated", 1, "import", 1, -1, 0, 0, "ok", ""),
            new Results.Row("validated", 1, "validate", 1, -1, 1, 0, "ok", "2 suites"),
            new Results.Row("bad", 1, "import", 1, -1, 0, 0, "ok", ""),
            new Results.Row("bad", 1, "build", 1, -1, 2, 0, "ok", ""),
            new Results.Row("broken", 1, "count", -1, -1, -1, -1, "failed", "nothing imported"),
            new Results.Row("gone", 1, "all", -1, -1, -1, -1, "skipped", "file not found")));
        ConformanceComparison.ToolRun cameo = new ConformanceComparison.ToolRun(temp, run, Map.of(), List.of());
        assertEquals(ConformanceComparison.Verdict.ACCEPTED, cameo.verdict("ok"));
        assertEquals(ConformanceComparison.Verdict.UNCHECKED, cameo.verdict("unchecked"), "the build did not run");
        assertEquals(ConformanceComparison.Verdict.REJECTED, cameo.verdict("validated"), "validation errors decide");
        assertEquals(ConformanceComparison.Verdict.REJECTED, cameo.verdict("bad"));
        assertEquals(ConformanceComparison.Verdict.FAILED, cameo.verdict("broken"));
        assertEquals("count: nothing imported", cameo.failure("broken"));
        assertEquals(ConformanceComparison.Verdict.SKIPPED, cameo.verdict("gone"));
        assertEquals(ConformanceComparison.Verdict.MISSING, cameo.verdict("other"));

        ConformanceComparison.ToolRun validator = new ConformanceComparison.ToolRun(temp, new Results.Run(
            ValidatorRunner.TOOL, "1", "h", List.of(new Results.Row("m", 1, "validate", 1, -1, 0, 0, "ok", ""),
                new Results.Row("m", 2, "validate", 1, -1, 3, 0, "ok", ""),
                new Results.Row("m", 1, "parse", 1, -1, 4, 0, "ok", ""))), Map.of(), List.of());
        assertEquals(ConformanceComparison.Verdict.ACCEPTED, validator.verdict("m"));

        // A skip in any phase; a run that failed to start ran nothing
        ConformanceComparison.ToolRun pilot = new ConformanceComparison.ToolRun(temp, new Results.Run(
            ConformanceComparison.PILOT, "1", "h", List.of(
                new Results.Row("p", 1, "process", -1, -1, -1, -1, "skipped", "the pilot is not loaded"))),
            Map.of(), List.of());
        assertEquals(ConformanceComparison.Verdict.SKIPPED, pilot.verdict("p"));
        ConformanceComparison.ToolRun broken = new ConformanceComparison.ToolRun(temp, new Results.Run(
            ConformanceComparison.PILOT, "1", "h", List.of(
                new Results.Row(Results.RUN, 1, "startup", -1, -1, -1, -1, "failed", "no library"),
                new Results.Row("p", 1, "process", 1, -1, 0, 0, "ok", ""))), Map.of(), List.of());
        assertTrue(broken.broken());
        assertEquals(ConformanceComparison.Verdict.SKIPPED, broken.verdict("p"));
    }

    @Test
    @DisplayName("The report warns of runs of different manifests and of files that differ from the manifest")
    void consistency() throws IOException {
        List<Manifest.Entry> manifest = List.of(entry("a", "clean"), entry("b", "clean"));
        Path v = temp.resolve("v");
        writeRun(v, ValidatorRunner.TOOL, Map.of("a", "accepted", "b", "accepted"), List.of());
        Path c = temp.resolve("c");
        writeRun(c, ConformanceComparison.CAMEO, Map.of("a", "accepted", "b", "accepted"), List.of());
        assertFalse(ConformanceComparison.compare(manifest, List.of(v, c)).contains("different versions"));

        Results.writeEnvironment(v.resolve("environment.properties"), Map.of("tool", ValidatorRunner.TOOL,
            "manifest.sha256", "aaaaaaaaaaaaaaaa", "library", "/lib"));
        Results.writeEnvironment(c.resolve("environment.properties"), Map.of("tool", "cameo",
            "manifest.sha256", "bbbbbbbbbbbbbbbb", "conf.undocumented", "true"));
        Results.writeResults(c.resolve("results.csv"), "cameo", "1", "h", RUN_ID, List.of(
            new Results.Row("a", 1, "import", 1, -1, 0, 0, "ok", "the file differs from the manifest (sha256)"),
            new Results.Row("a", 1, "build", 1, -1, 0, 0, "ok", ""),
            new Results.Row("b", 1, "build", 1, -1, 0, 0, "ok", "")));
        String report = ConformanceComparison.compare(manifest, "aaaaaaaaaaaaaaaa", List.of(v, c));
        assertTrue(report.contains("**The runs used different versions of the manifest (Validator aaaaaaaaaaaa; "
            + "Cameo bbbbbbbbbbbb)"), report);
        assertTrue(report.contains("**Cameo ran another version of the manifest than the one compared"), report);
        assertFalse(report.contains("**Validator ran another version"), report);
        assertTrue(report.contains("**Cameo ran files that differ from the manifest: a.**"), report);
        assertTrue(report.contains("- Validator: library `/lib`"), report);
        assertTrue(report.contains("- Cameo: conf.undocumented `true`"), report);
    }

    @Test
    @DisplayName("Disagreements caused by Dassault library names the pilot cannot resolve are marked")
    void dassaultNames() throws IOException {
        List<Manifest.Entry> manifest = List.of(entry("views", "clean"), entry("other", "clean"));
        Path v = temp.resolve("v");
        writeRun(v, ValidatorRunner.TOOL, Map.of("views", "accepted", "other", "accepted"), List.of());
        Path p = temp.resolve("p");
        writeRun(p, ConformanceComparison.PILOT, Map.of("views", "rejected", "other", "rejected"), List.of(
            new Results.Diagnostic("views", "", "linking", "error", 3, 1, "", "Couldn't resolve reference to Type "
                + "'DS_Views::SymbolicViews::gv'."),
            new Results.Diagnostic("other", "", "linking", "error", 3, 1, "", "Couldn't resolve reference to Type "
                + "'Vehicel'.")));
        String section = section(ConformanceComparison.compare(manifest, List.of(v, p)),
            "## Validator and Pilot disagree");
        assertTrue(section.contains("1 of these 2 are models in which the pilot cannot resolve"), section);
        assertTrue(section.contains("- views: Validator accepted, Pilot rejected (the pilot: unresolved Dassault "
            + "library names)"), section);
        assertTrue(section.contains("- other: Validator accepted, Pilot rejected\n"), section);
    }

    @Test
    @DisplayName("run and compare through the command line")
    void commandLine() throws IOException {
        assumeTrue(new LibraryConfig().hasLibraryPaths(), "needs the SysML standard library (SYSML_LIBRARY_PATH)");
        Path real = CameoConformanceRunnerScriptTest.repoRoot();
        Path repo = temp.resolve("repo");
        Path results = Files.createDirectories(repo.resolve(CompareMain.CONFORMANCE).resolve("results"));
        Files.copy(real.resolve(CompareMain.CONFORMANCE).resolve("manifest.csv"),
            repo.resolve(CompareMain.CONFORMANCE).resolve("manifest.csv"));
        CommandLine cli = new CommandLine(new CompareMain());

        Path validatorDir = results.resolve("validator_test_" + RUN_ID);
        assertEquals(0, cli.execute("conformance", "run", "--repo", real.toString(), "--only",
            "positive/01_simple_part_def,negative/01_action_parallel_invalid,lint/unit_mismatches,"
                + "kpar/two-members,kpar/two-members-error,kpar/invalid-not-a-zip",
            "--out", validatorDir.toString()));
        Results.Run run = Results.readResults(validatorDir.resolve("results.csv"));
        assertEquals(ValidatorRunner.TOOL, run.tool());
        assertEquals(Manifest.sha256(Files.readAllBytes(real.resolve(CompareMain.CONFORMANCE).resolve("manifest.csv"))),
            Results.readEnvironment(validatorDir.resolve("environment.properties")).get("manifest.sha256"));
        assertEquals(1, run.ok("positive/01_simple_part_def", "validate").size(), "one run per model");
        assertEquals(0, run.ok("positive/01_simple_part_def", "validate").get(0).errors());
        assertEquals(null, run.first("positive/01_simple_part_def", "parse"), "validate only");
        assertTrue(run.ok("negative/01_action_parallel_invalid", "validate").get(0).errors() > 0);
        List<Results.Diagnostic> diagnostics = Results.readDiagnostics(validatorDir.resolve("diagnostics.csv"));
        assertTrue(diagnostics.stream().anyMatch(d -> d.fileId().equals("negative/01_action_parallel_invalid")
            && d.severity().equals("error") && d.line() > 0), diagnostics.toString());
        // KPARs: the members are validated together; an error is placed in its member
        assertEquals(0, run.ok("kpar/two-members", "validate").get(0).errors(), "Car.sysml uses Vehicle.sysml");
        assertEquals(List.of("Car.sysml:3"), diagnostics.stream().filter(d -> d.fileId().equals(
            "kpar/two-members-error") && d.severity().equals("error")).map(Results.Diagnostic::place).toList());
        assertEquals(List.of("IO_INVALID_KPAR"), diagnostics.stream().filter(d -> d.fileId().equals(
            "kpar/invalid-not-a-zip")).map(Results.Diagnostic::code).toList());

        assertEquals(2, cli.execute("conformance", "compare", "--repo", repo.toString()), "one tool only");

        writeRun(results.resolve("pilot_test_" + RUN_ID), ConformanceComparison.PILOT, Map.of(
            "positive/01_simple_part_def", "accepted", "negative/01_action_parallel_invalid", "rejected",
            "lint/unit_mismatches", "accepted"), List.of());
        assertEquals(0, cli.execute("conformance", "compare", "--repo", repo.toString()));
        String report = Files.readString(results.resolve("comparison.md"));
        assertTrue(report.startsWith("# Conformance: Validator, Pilot"), report);
        assertTrue(report.contains("| positive/01_simple_part_def | clean | accepted | accepted |"), report);
        assertTrue(Files.readString(results.resolve("verdicts.csv"))
            .contains("negative/01_action_parallel_invalid,negative,errors,rejected,"));

        Path explicit = temp.resolve("elsewhere/report.md");
        assertEquals(0, cli.execute("conformance", "compare", "--repo", repo.toString(), "--validator",
            validatorDir.toString(), "--pilot", results.resolve("pilot_test_" + RUN_ID).toString(), "--out",
            explicit.toString()));
        assertTrue(Files.isRegularFile(explicit.resolveSibling("verdicts.csv")));
        assertEquals(0, cli.execute("conformance"));
    }

    /**
     * A result directory of a tool with the given verdict per model: "accepted", "rejected", "failed", "skipped" or
     * (Cameo) "unchecked".
     */
    static void writeRun(Path dir, String tool, Map<String, String> verdicts, List<Results.Diagnostic> diagnostics)
            throws IOException {
        Files.createDirectories(dir);
        String phase = switch (tool) {
            case ConformanceComparison.PILOT -> "process";
            // Cameo's text errors come from its build step; without it there is no verdict
            case ConformanceComparison.CAMEO -> "build";
            default -> "validate";
        };
        List<Results.Row> rows = new ArrayList<>();
        rows.add(new Results.Row(Results.RUN, 1, "startup", 1, -1, -1, -1, "ok", ""));
        for (Map.Entry<String, String> v : new TreeMap<>(verdicts).entrySet()) {
            rows.add(switch (v.getValue()) {
                case "accepted" -> new Results.Row(v.getKey(), 1, phase, 1, -1, 0, 0, "ok", "");
                case "rejected" -> new Results.Row(v.getKey(), 1, phase, 1, -1, 1, 0, "ok", "");
                case "failed" -> new Results.Row(v.getKey(), 1, phase, -1, -1, -1, -1, "failed", "could not load");
                case "unchecked" -> new Results.Row(v.getKey(), 1, "import", 1, -1, 0, 0, "ok", "");
                default -> new Results.Row(v.getKey(), 1, "all", -1, -1, -1, -1, "skipped", "file not found");
            });
        }
        Results.writeResults(dir.resolve("results.csv"), tool, "1.0", tool + "-host", RUN_ID, rows);
        Results.writeDiagnostics(dir.resolve("diagnostics.csv"), tool, diagnostics);
        Results.writeEnvironment(dir.resolve("environment.properties"), Map.of("tool", tool, "run.id", RUN_ID));
    }

    private static Manifest.Entry entry(String id, String expect) {
        return new Manifest.Entry(id, id + ".sysml", "test", "-", expect, 10, 100, "-", "A test model");
    }

    /** The text of a report section, up to the next one. */
    private static String section(String report, String heading) {
        int start = report.indexOf(heading);
        assertTrue(start >= 0, heading + " in " + report);
        int end = report.indexOf("\n## ", start + heading.length());
        return report.substring(start, end < 0 ? report.length() : end);
    }
}
