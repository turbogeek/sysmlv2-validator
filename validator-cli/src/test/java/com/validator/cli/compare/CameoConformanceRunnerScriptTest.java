package com.validator.cli.compare;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the Cameo macro conformance/cameo/CameoConformanceRunner.groovy outside Cameo, with a stand-in for Cameo,
 * and reads its output with {@link Results} and {@link ConformanceComparison}: the macro's run logic, its KPAR
 * handling and its file formats are checked on every build, although Cameo itself is not available here.
 */
@DisplayName("Cameo conformance macro")
class CameoConformanceRunnerScriptTest {

    /** A stand-in for Cameo with the methods the macro calls on its Cameo facade. */
    public static class FakeCameo {
        /** The root namespaces of the current scratch project. */
        List<Object> roots = new ArrayList<>();
        final List<String> calls = new ArrayList<>();
        final List<String> built = new ArrayList<>();
        boolean validateFails;
        boolean importAddsNothing;
        boolean importFailsAfterAdding;
        /** Models whose import label contains this cannot be deleted. */
        String undeletable;
        /** Only the first scratch project can be created. */
        boolean oneProjectOnly;
        int undoCleared;
        int closed;

        public Map<String, String> environment() {
            return Map.of("cameo.sysml2.plugin", "SysML v2 test", "cameo.openapi.version", "0");
        }

        public Object createProject(String name) {
            calls.add("create " + name);
            if (oneProjectOnly && calls.stream().filter(c -> c.startsWith("create ")).count() > 1) {
                throw new IllegalStateException("no more projects");
            }
            roots = new ArrayList<>();
            return new Object();
        }

        public List<Object> roots(Object project) {
            return new ArrayList<>(roots);
        }

        /** An error on every line that names the misspelled "Vehicel", as Cameo's text diagnostics would. */
        public List<Object> build(Object project, String text) {
            built.add(text);
            List<Object> diagnostics = new ArrayList<>();
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                int col = lines[i].indexOf("Vehicel");
                if (col >= 0) {
                    diagnostics.add(List.of("text", "ERROR", i + 1, col + 1, "", "Couldn't resolve reference to "
                        + "Type 'Vehicel'."));
                }
            }
            diagnostics.add(List.of("text-validation", "HINT", 1, 1, "", "a hint"));
            return List.of((long) diagnostics.size() - 1, 0L, diagnostics);
        }

        public void importText(Object project, String text, String label) {
            calls.add(label);
            if (importAddsNothing) {
                return;
            }
            roots.add(undeletable != null && label.contains(undeletable) ? "stuck" : new Object());
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
            return List.of(0L, 1L, List.of(List.of("validation", "WARNING", 0, 0, "rule, with comma", "P::x: \"m\""),
                List.of("validation", "DEBUG", 0, 0, "trace", "d")), 2);
        }

        public void dispose(Object project, List<Object> imported, String label) {
            roots.removeAll(imported.stream().filter(r -> !"stuck".equals(r)).toList());
        }

        public void close(Object project) {
            closed++;
        }
    }

    /** A Cameo with the import of several files that its Import action uses: the macro uses it for KPARs. */
    public static class FakeCameoMultiFile extends FakeCameo {
        final List<List<String>> imported = new ArrayList<>();
        final List<java.io.File> seen = new ArrayList<>();
        /** How a diagnostic names its file: "path" (absolute), "uri" (file: URI) or "elsewhere" (another folder). */
        String fileForm = "path";

        public boolean multiFileImport() {
            return true;
        }

        public void importFiles(Object project, List<java.io.File> files, String label) {
            calls.add("files " + label);
            imported.add(files.stream().map(java.io.File::getName).toList());
            roots.add(new Object());
        }

        /** An error on every line of a file that names "Vehicel", with the file, as Cameo's build would. */
        public List<Object> buildFiles(Object project, List<java.io.File> files) throws IOException {
            List<Object> diagnostics = new ArrayList<>();
            for (java.io.File f : files) {
                seen.add(f);
                String[] lines = Files.readString(f.toPath()).split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    int col = lines[i].indexOf("Vehicel");
                    if (col >= 0) {
                        String file = switch (fileForm) {
                            case "uri" -> f.toURI().toString();
                            case "elsewhere" -> "/elsewhere/" + f.getParentFile().getName() + "/" + f.getName();
                            default -> f.getAbsolutePath();
                        };
                        diagnostics.add(List.of("text", "ERROR", i + 1, col + 1, "", "Couldn't resolve 'Vehicel'.",
                            file));
                    }
                }
            }
            return List.of((long) diagnostics.size(), 0L, diagnostics);
        }
    }

    /** A Cameo whose KPAR import is released: the macro uses it for KPARs. */
    public static class FakeCameoWithKpar extends FakeCameo {
        final List<Path> kpars = new ArrayList<>();

        public void importKpar(Object project, Path kpar, String label) {
            calls.add("kpar " + label);
            kpars.add(kpar);
            roots.add(new Object());
        }
    }

    @TempDir
    Path temp;

    @Test
    @DisplayName("Writes results, diagnostics and environment that the comparison reads")
    void runsWithAStandIn() throws IOException {
        Path repo = repoWithCorpus(temp);
        FakeCameo cameo = new FakeCameo();
        Object result = runMacro(repo, cameo, Map.of());

        Path out = onlyResultDir(repo);
        assertTrue(String.valueOf(result).startsWith("Wrote " + out), String.valueOf(result));
        assertEquals(1, cameo.closed, "the scratch project is closed");
        assertTrue(cameo.roots.isEmpty(), "every imported model is deleted again");
        assertEquals(3, cameo.undoCleared, "after each model that imported");
        assertEquals(List.of("create", "Import small", "Import kpar/two", "Import kpar/two-error"),
            cameo.calls.stream().map(c -> c.startsWith("create ") ? "create" : c).toList());
        assertEquals(ConformanceCorpus.VEHICLE + ConformanceCorpus.CAR, cameo.built.get(1),
            "the KPAR's members, joined in archive order");

        Results.Run run = Results.readResults(out.resolve("results.csv"));
        assertEquals("cameo", run.tool());
        assertEquals("SysML v2 test", run.version());
        assertEquals(1, run.ok(Results.RUN, "create_project").size());
        for (String phase : List.of("import", "count", "validate", "dispose", "build")) {
            assertEquals(1, run.ok("small", phase).size(), phase);
        }
        assertEquals(10, run.ok("small", "count").get(0).elements());
        assertEquals(1, run.ok("small", "validate").get(0).warnings());
        assertEquals("2 suites", run.ok("small", "validate").get(0).detail());
        assertEquals("KPAR as joined text, 2 members", run.ok("kpar/two", "import").get(0).detail());
        assertEquals("as joined text", run.ok("kpar/two", "build").get(0).detail());
        assertEquals(1, run.ok("kpar/two-error", "build").get(0).errors());
        // The macro's own check, not Cameo's verdict
        assertEquals("skipped", run.first("kpar/not-a-zip", "all").status());
        assertTrue(run.first("kpar/not-a-zip", "all").detail().startsWith("invalid KPAR: not a zip archive"));
        assertEquals("invalid KPAR: no .project.json (checked by this macro; Cameo's KPAR import is not used)",
            run.first("kpar/no-project", "all").detail());
        assertNull(run.first("kpar/no-project", "import"));
        assertEquals("skipped", run.first("missing", "all").status());

        List<Results.Diagnostic> diagnostics = Results.readDiagnostics(out.resolve("diagnostics.csv"));
        Results.Diagnostic misspelled = diagnostics.stream().filter(d -> d.fileId().equals("kpar/two-error")
            && d.severity().equals("error")).findFirst().orElseThrow();
        assertEquals("Car.sysml", misspelled.member(), "the member of the joined text the line is in");
        assertEquals(3, misspelled.line(), "the line in that member");
        assertEquals(16, misspelled.column());
        assertEquals("Car.sysml:3", misspelled.place());
        Results.Diagnostic validation = diagnostics.stream().filter(d -> d.fileId().equals("small")
            && d.source().equals("validation") && d.severity().equals("warning")).findFirst().orElseThrow();
        assertEquals("rule, with comma", validation.code());
        assertEquals("P::x: \"m\"", validation.message());
        assertEquals(List.of("info"), diagnostics.stream().filter(d -> d.code().equals("trace"))
            .map(Results.Diagnostic::severity).distinct().toList(), "DEBUG is info");
        assertTrue(diagnostics.stream().noneMatch(d -> d.fileId().equals("kpar/not-a-zip")));

        Map<String, String> env = Results.readEnvironment(out.resolve("environment.properties"));
        assertEquals("cameo", env.get("tool"));
        assertTrue(env.get("kpar").startsWith("as joined text"), env.get("kpar"));
        assertEquals("", env.get("files.changed"));
        String report = Files.readString(out.resolve("report.md"));
        assertTrue(report.contains("| small | accepted | "), report);
        assertTrue(report.contains("| kpar/two-error | rejected | "), report);
        assertTrue(report.contains("| kpar/not-a-zip | skipped | "), report);
        assertTrue(Files.readString(out.resolve("run-log.txt")).contains("kpar/two"));

        // The comparison reads the macro's output next to a validator run that accepts the misspelled KPAR
        Path validator = repo.resolve("conformance/results/validator_test_20260101-000000");
        ConformanceToolTest.writeRun(validator, ValidatorRunner.TOOL, Map.of("small", "accepted",
            "kpar/two", "accepted", "kpar/two-error", "accepted", "kpar/not-a-zip", "failed",
            "kpar/no-project", "failed"), List.of());
        String comparison = ConformanceComparison.compare(Manifest.read(manifest(repo)), List.of(validator, out));
        String section = comparison.substring(comparison.indexOf("## Validator accepts, Cameo rejects"),
            comparison.indexOf("## Validator rejects, Cameo accepts"));
        assertTrue(section.contains("**kpar/two-error**"), comparison);
        assertTrue(section.contains("Car.sysml:3: text: Couldn't resolve"), comparison);
        assertFalse(section.contains("**small**"), comparison);
        assertTrue(comparison.contains("the same verdict on 2; Validator accepts and Cameo rejects 1; Validator "
            + "rejects and Cameo accepts 0; not compared 2."), comparison);
        assertTrue(comparison.contains("- Cameo has no verdict on skipped 2."), comparison);
    }

    @Test
    @DisplayName("An undocumented step that fails is recorded as unavailable and the run goes on")
    void unavailableStep() throws IOException {
        Path repo = repoWithCorpus(temp);
        FakeCameo cameo = new FakeCameo();
        cameo.validateFails = true;
        runMacro(repo, cameo, Map.of("CONF_ONLY", "small, kpar/two"));

        Results.Run run = Results.readResults(onlyResultDir(repo).resolve("results.csv"));
        Results.Row validate = run.first("small", "validate");
        assertEquals("unavailable", validate.status());
        assertTrue(validate.detail().contains("no validation here"), validate.detail());
        assertEquals(1, run.ok("small", "build").size(), "later steps still run");
        assertNull(run.first("missing", "all"), "CONF_ONLY leaves the other models out");
        assertNotNull(run.first("kpar/two", "import"));
    }

    @Test
    @DisplayName("CONF_UNDOCUMENTED=false runs only the documented steps, which give no verdict; CONF_SKIP")
    void documentedOnly() throws IOException {
        Path repo = repoWithCorpus(temp);
        runMacro(repo, new FakeCameo(), Map.of("CONF_UNDOCUMENTED", "false", "CONF_SKIP", "kpar/two-error"));

        Path out = onlyResultDir(repo);
        Results.Run run = Results.readResults(out.resolve("results.csv"));
        assertEquals("unavailable", run.first("small", "build").status(), "the text is not checked");
        assertNull(run.first("small", "validate"));
        assertEquals(1, run.ok("small", "import").size());
        assertNull(run.first("kpar/two-error", "import"));
        assertEquals(ConformanceComparison.Verdict.UNCHECKED, verdict(run, "small"), "imported, not checked");
        assertTrue(Files.readString(out.resolve("report.md")).contains("| small | unchecked | "));
    }

    @Test
    @DisplayName("A macro argument with Null ticked is bound to null and counts as not set")
    void nullArgument() throws IOException {
        Path repo = repoWithCorpus(temp);
        Map<String, Object> settings = new HashMap<>();
        settings.put("CONF_UNDOCUMENTED", null);
        settings.put("CONF_ONLY", "small");
        runMacro(repo, new FakeCameo(), settings);
        Results.Run run = Results.readResults(onlyResultDir(repo).resolve("results.csv"));
        assertEquals(1, run.ok("small", "build").size(), "the default, true");

        // An argument left blank is not set either
        Path again = repoWithCorpus(Files.createDirectories(temp.resolve("blank")));
        runMacro(again, new FakeCameo(), Map.of("CONF_UNDOCUMENTED", " ", "CONF_ONLY", "small"));
        assertEquals(1, Results.readResults(onlyResultDir(again).resolve("results.csv")).ok("small", "build").size());
    }

    @Test
    @DisplayName("KPARs are imported and built as files, as Cameo's Import of several files does")
    void multiFileImport() throws IOException {
        Path repo = repoWithCorpus(temp);
        FakeCameoMultiFile cameo = new FakeCameoMultiFile();
        runMacro(repo, cameo, Map.of("CONF_ONLY", "small,kpar/two-error"));

        assertEquals(List.of("Import small", "files Import kpar/two-error"),
            cameo.calls.subList(1, cameo.calls.size()));
        assertEquals(List.of(List.of("Vehicle.sysml", "Car.sysml")), cameo.imported);
        assertTrue(cameo.seen.stream().noneMatch(java.io.File::exists), "the extracted members are deleted");
        Path out = onlyResultDir(repo);
        Results.Run run = Results.readResults(out.resolve("results.csv"));
        assertEquals("KPAR as files, 2 members", run.ok("kpar/two-error", "import").get(0).detail());
        assertEquals("as files", run.ok("kpar/two-error", "build").get(0).detail());
        assertEquals("", run.ok("small", "build").get(0).detail(), "a plain file is built as text");
        assertEquals(List.of("Car.sysml:3"), Results.readDiagnostics(out.resolve("diagnostics.csv")).stream()
            .filter(d -> d.fileId().equals("kpar/two-error") && d.severity().equals("error"))
            .map(Results.Diagnostic::place).toList());
        assertTrue(Results.readEnvironment(out.resolve("environment.properties")).get("kpar").startsWith("as files"));
    }

    @Test
    @DisplayName("A member is found from the file Cameo names, as a file: URI or by the longest path it ends with")
    void membersOfFiles() throws IOException {
        for (String form : List.of("uri", "elsewhere")) {
            Path repo = repoWithCorpus(Files.createDirectories(temp.resolve(form)));
            Map<String, String> members = new LinkedHashMap<>();
            members.put("a.sysml", "package A;\n");
            members.put("sub/a.sysml", "package B {\n    part p : Vehicel;\n}\n");
            Path file = Files.write(repo.resolve("conformance/kpar/same.kpar"), ConformanceCorpus.kpar("S", members));
            List<Manifest.Entry> entries = new ArrayList<>(Manifest.read(manifest(repo)));
            entries.add(Manifest.describe(file, "kpar/same", "conformance/kpar/same.kpar", "kpar", "-", "errors", "x"));
            Manifest.write(manifest(repo), entries);
            FakeCameoMultiFile cameo = new FakeCameoMultiFile();
            cameo.fileForm = form;
            runMacro(repo, cameo, Map.of("CONF_ONLY", "kpar/same"));
            assertEquals(List.of("sub/a.sysml:2"), Results.readDiagnostics(onlyResultDir(repo)
                .resolve("diagnostics.csv")).stream().filter(d -> d.severity().equals("error"))
                .map(Results.Diagnostic::place).toList(), form);
        }
    }

    @Test
    @DisplayName("A member path outside the archive is refused, and nothing is left in the temporary folder")
    void memberOutsideTheArchive() throws IOException {
        Path repo = repoWithCorpus(temp);
        Map<String, String> members = new LinkedHashMap<>();
        members.put("ok.sysml", "package Ok;\n");
        members.put("../escape.sysml", "package Escape;\n");
        Path file = Files.write(repo.resolve("conformance/kpar/escape.kpar"), ConformanceCorpus.kpar("E", members));
        List<Manifest.Entry> entries = new ArrayList<>(Manifest.read(manifest(repo)));
        entries.add(Manifest.describe(file, "kpar/escape", "conformance/kpar/escape.kpar", "kpar", "-", "-", "x"));
        Manifest.write(manifest(repo), entries);
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"));
        long before = kparFolders(tmp);
        runMacro(repo, new FakeCameoMultiFile(), Map.of("CONF_ONLY", "kpar/escape"));
        Results.Row row = Results.readResults(onlyResultDir(repo).resolve("results.csv")).first("kpar/escape", "all");
        assertEquals("skipped", row.status());
        assertTrue(row.detail().contains("member path outside the archive: ../escape.sysml"), row.detail());
        assertEquals(before, kparFolders(tmp));
    }

    private static long kparFolders(Path tmp) throws IOException {
        try (Stream<Path> dirs = Files.list(tmp)) {
            return dirs.filter(p -> p.getFileName().toString().startsWith("conformance-kpar-")).count();
        }
    }

    @Test
    @DisplayName("A model that cannot be deleted does not affect the next: they run in a new scratch project")
    void scratchProjectKeptClean() throws IOException {
        Path repo = repoWithCorpus(temp);
        FakeCameo cameo = new FakeCameo();
        cameo.undeletable = "kpar/two";
        runMacro(repo, cameo, Map.of("CONF_ONLY", "small,kpar/two,kpar/two-error"));

        Path out = onlyResultDir(repo);
        Results.Run run = Results.readResults(out.resolve("results.csv"));
        assertEquals("ok", run.first("kpar/two", "cleanup").status());
        assertEquals(List.of("create", "Import small", "Import kpar/two", "create", "Import kpar/two-error",
            "create"), cameo.calls.stream().map(c -> c.startsWith("create ") ? "create" : c).toList(),
            "kpar/two-error leaves a model too: each time a new project");
        assertEquals(3, cameo.closed);
        assertNull(run.first("small", "cleanup"));

        // No new scratch project: the run stops, and says so for every model left
        Path again = repoWithCorpus(Files.createDirectories(temp.resolve("again")));
        FakeCameo stuck = new FakeCameo();
        stuck.undeletable = "kpar/two";
        stuck.oneProjectOnly = true;
        runMacro(again, stuck, Map.of("CONF_ONLY", "small,kpar/two,kpar/two-error"));
        Results.Run stopped = Results.readResults(onlyResultDir(again).resolve("results.csv"));
        assertEquals("unavailable", stopped.first("kpar/two", "cleanup").status());
        assertTrue(stopped.first("kpar/two-error", "all").detail().startsWith("no scratch project: "));
        assertEquals(ConformanceComparison.Verdict.SKIPPED, verdict(stopped, "kpar/two-error"));
        assertEquals(ConformanceComparison.Verdict.ACCEPTED, verdict(stopped, "kpar/two"), "it did run");
    }

    @Test
    @DisplayName("A file that differs from the manifest is noted in its import row and the environment")
    void changedFile() throws IOException {
        Path repo = repoWithCorpus(temp);
        Files.writeString(repo.resolve("conformance/small.sysml"), "package Small { part def B; }\n");
        runMacro(repo, new FakeCameo(), Map.of("CONF_ONLY", "small"));
        Path out = onlyResultDir(repo);
        Results.Run run = Results.readResults(out.resolve("results.csv"));
        assertEquals("the file differs from the manifest (sha256)", run.ok("small", "import").get(0).detail());
        assertEquals("small", Results.readEnvironment(out.resolve("environment.properties")).get("files.changed"));
    }

    @Test
    @DisplayName("KPARs the validator's reader rejects fail in the macro too")
    void invalidKpars() throws IOException {
        Path repo = repoWithCorpus(temp);
        Path kpars = repo.resolve("conformance/kpar");
        Map<String, String> dirProject = new LinkedHashMap<>();
        dirProject.put(".project.json/", "");
        dirProject.put("P.sysml", "package P;\n");
        Map<String, String> notJson = new LinkedHashMap<>();
        notJson.put(".project.json", "name: P");
        notJson.put("P.sysml", "package P;\n");
        Map<String, String> onlyKerml = new LinkedHashMap<>();
        onlyKerml.put(".project.json", "{}");
        onlyKerml.put("P.kerml", "package P;\n");
        List<Manifest.Entry> entries = new ArrayList<>(Manifest.read(manifest(repo)));
        for (Map.Entry<String, Map<String, String>> k : Map.of("dir-project", dirProject, "not-json", notJson,
                "only-kerml", onlyKerml).entrySet()) {
            Path file = Files.write(kpars.resolve(k.getKey() + ".kpar"), ConformanceCorpus.zip(k.getValue()));
            entries.add(Manifest.describe(file, "kpar/" + k.getKey(), "conformance/kpar/" + k.getKey() + ".kpar",
                "kpar", "-", "invalid", "x"));
        }
        Map<String, String> bom = new LinkedHashMap<>();
        bom.put(".project.json", "\ufeff{\"name\": \"P\"}");
        bom.put("P.sysml", "package P;\n");
        Path bomFile = Files.write(kpars.resolve("bom.kpar"), ConformanceCorpus.zip(bom));
        entries.add(Manifest.describe(bomFile, "kpar/bom", "conformance/kpar/bom.kpar", "kpar", "-", "-", "x"));
        // Deflated data that cannot be inflated: the zip's directory is intact, the member is not
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(buffer)) {
            zip.putNextEntry(new java.util.zip.ZipEntry(".project.json"));
            zip.write("{}".getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new java.util.zip.ZipEntry("P.sysml"));
            zip.write("package Corrupted { part p; }\n".repeat(50).getBytes(StandardCharsets.UTF_8));
        }
        byte[] bytes = buffer.toByteArray();
        int data = new String(bytes, StandardCharsets.ISO_8859_1).indexOf("P.sysml") + "P.sysml".length();
        java.util.Arrays.fill(bytes, data, data + 6, (byte) 0xFF);
        Path corruptFile = Files.write(kpars.resolve("corrupt.kpar"), bytes);
        entries.add(Manifest.describe(corruptFile, "kpar/corrupt", "conformance/kpar/corrupt.kpar", "kpar", "-",
            "invalid", "x"));
        Path latin1 = kpars.resolve("latin1.kpar");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(latin1))) {
            zip.putNextEntry(new java.util.zip.ZipEntry(".project.json"));
            zip.write("{}".getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new java.util.zip.ZipEntry("P.sysml"));
            zip.write("package P { doc /* caf\u00e9 */ }".getBytes(StandardCharsets.ISO_8859_1));
        }
        entries.add(Manifest.describe(latin1, "kpar/latin1", "conformance/kpar/latin1.kpar", "kpar", "-", "invalid",
            "x"));
        Manifest.write(manifest(repo), entries);

        runMacro(repo, new FakeCameo(), Map.of("CONF_ONLY", "kpar/dir-project,kpar/not-json,kpar/only-kerml,"
            + "kpar/latin1,kpar/bom,kpar/corrupt"));
        Results.Run run = Results.readResults(onlyResultDir(repo).resolve("results.csv"));
        assertTrue(run.first("kpar/dir-project", "all").detail().startsWith("invalid KPAR: no .project.json"));
        assertTrue(run.first("kpar/not-json", "all").detail().startsWith(
            "invalid KPAR: .project.json is not a JSON object"));
        assertTrue(run.first("kpar/latin1", "all").detail().startsWith("invalid KPAR: P.sysml is not UTF-8 text"));
        assertTrue(run.first("kpar/corrupt", "all").detail().startsWith("invalid KPAR: corrupted zip entry"),
            run.first("kpar/corrupt", "all").detail());
        assertTrue(run.first("kpar/only-kerml", "all").detail().startsWith("no .sysml member"));
        for (String id : List.of("kpar/dir-project", "kpar/not-json", "kpar/only-kerml", "kpar/latin1",
                "kpar/corrupt")) {
            assertEquals(ConformanceComparison.Verdict.SKIPPED, verdict(run, id), id);
        }
        assertEquals(1, run.ok("kpar/bom", "import").size(), "a byte order mark before the JSON is allowed");
    }

    @Test
    @DisplayName("An import that adds nothing fails; a failed import leaves nothing behind and is still built")
    void importProblems() throws IOException {
        Path repo = repoWithCorpus(temp);
        FakeCameo empty = new FakeCameo();
        empty.importAddsNothing = true;
        runMacro(repo, empty, Map.of("CONF_ONLY", "small"));
        Results.Run run = Results.readResults(onlyResultDir(repo).resolve("results.csv"));
        assertEquals("failed", run.first("small", "count").status());
        assertEquals("the import added no root namespace", run.first("small", "count").detail());
        assertNull(run.first("small", "validate"), "nothing is validated on nothing");
        assertEquals(ConformanceComparison.Verdict.FAILED, verdict(run, "small"));

        Path again = repoWithCorpus(Files.createDirectories(temp.resolve("second")));
        FakeCameo half = new FakeCameo();
        half.importFailsAfterAdding = true;
        runMacro(again, half, Map.of("CONF_ONLY", "kpar/two-error"));
        Path out = onlyResultDir(again);
        Results.Run failed = Results.readResults(out.resolve("results.csv"));
        assertEquals("failed", failed.first("kpar/two-error", "import").status());
        assertTrue(failed.first("kpar/two-error", "import").detail().contains("half imported"));
        assertTrue(half.roots.isEmpty(), "what the failed import left is removed");
        assertEquals(1, half.undoCleared);
        assertEquals(1, failed.ok("kpar/two-error", "build").get(0).errors(), "the text diagnostics are kept");
        assertTrue(Results.readDiagnostics(out.resolve("diagnostics.csv")).stream()
            .anyMatch(d -> "Car.sysml:3".equals(d.place())));
        String log = Files.readString(out.resolve("run-log.txt"));
        assertTrue(log.contains("removed 1 root namespaces left by the failed import"), log);
    }

    @Test
    @DisplayName("Once Cameo has a KPAR import, the macro uses it for KPARs and text for the rest")
    void nativeKparImport() throws IOException {
        Path repo = repoWithCorpus(temp);
        FakeCameoWithKpar cameo = new FakeCameoWithKpar();
        runMacro(repo, cameo, Map.of("CONF_ONLY", "small,kpar/two,kpar/not-a-zip"));

        assertEquals(List.of("Import small", "kpar Import kpar/two", "kpar Import kpar/not-a-zip"),
            cameo.calls.subList(1, cameo.calls.size()), "Cameo decides about an invalid KPAR");
        assertEquals(List.of(repo.resolve("conformance/kpar/two.kpar"),
            repo.resolve("conformance/kpar/not-a-zip.kpar")), cameo.kpars);
        Path out = onlyResultDir(repo);
        Results.Run run = Results.readResults(out.resolve("results.csv"));
        assertEquals("KPAR import, 2 members", run.ok("kpar/two", "import").get(0).detail());
        assertTrue(run.first("kpar/not-a-zip", "build").detail().startsWith("the members could not be read"));
        assertEquals("Cameo KPAR import", Results.readEnvironment(out.resolve("environment.properties"))
            .get("kpar"));
    }

    private static ConformanceComparison.Verdict verdict(Results.Run run, String id) {
        return new ConformanceComparison.ToolRun(Path.of("x"), run, Map.of(), List.of()).verdict(id);
    }

    /** Run the macro as Cameo's macro engine does: through JSR-223, settings and the stand-in as bindings. */
    private static Object runMacro(Path repo, FakeCameo cameo, Map<String, ?> settings) throws IOException {
        Map<String, Object> variables = new HashMap<>(settings);
        variables.put("CONF_REPO", repo.toString());
        variables.put("confApi", cameo);
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
        assertNotNull(engine, "the groovy JSR-223 engine");
        variables.forEach(engine::put);
        try (Reader script = Files.newBufferedReader(macro(), StandardCharsets.UTF_8)) {
            return engine.eval(script);
        } catch (ScriptException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * A repository with a manifest of a plain model, two valid KPARs (one with a misspelled type in its second
     * member), two invalid KPARs and a model whose file is missing.
     */
    private static Path repoWithCorpus(Path parent) throws IOException {
        Path repo = parent.resolve("repo");
        Path kpars = Files.createDirectories(repo.resolve("conformance/kpar"));
        Path small = repo.resolve("conformance/small.sysml");
        Files.writeString(small, "package Small { part def A; part a : A; }\n", StandardCharsets.UTF_8);
        Map<String, String> members = new LinkedHashMap<>();
        members.put("Vehicle.sysml", ConformanceCorpus.VEHICLE);
        members.put("Car.sysml", ConformanceCorpus.CAR);
        Path two = Files.write(kpars.resolve("two.kpar"), ConformanceCorpus.kpar("Two", members));
        members.put("Car.sysml", ConformanceCorpus.CAR.replace(": Vehicle {", ": Vehicel {"));
        Path twoError = Files.write(kpars.resolve("two-error.kpar"), ConformanceCorpus.kpar("Two", members));
        Path notZip = Files.writeString(kpars.resolve("not-a-zip.kpar"), "package P;\n", StandardCharsets.UTF_8);
        Path noProject = Files.write(kpars.resolve("no-project.kpar"),
            ConformanceCorpus.zip(Map.of("P.sysml", "package P;\n")));
        Manifest.write(manifest(repo), List.of(
            Manifest.describe(small, "small", "conformance/small.sysml", "positive", "-", "clean", "A small model"),
            Manifest.describe(two, "kpar/two", "conformance/kpar/two.kpar", "kpar", "-", "clean", "Two members"),
            Manifest.describe(twoError, "kpar/two-error", "conformance/kpar/two-error.kpar", "kpar", "-", "errors",
                "Car.sysml line 3"),
            Manifest.describe(notZip, "kpar/not-a-zip", "conformance/kpar/not-a-zip.kpar", "kpar", "-", "invalid",
                "Text"),
            Manifest.describe(noProject, "kpar/no-project", "conformance/kpar/no-project.kpar", "kpar", "-",
                "invalid", "No .project.json"),
            new Manifest.Entry("missing", "conformance/nowhere.sysml", "positive", "-", "clean", 1, 1, "-",
                "Not on this machine")));
        return repo;
    }

    private static Path manifest(Path repo) {
        return repo.resolve("conformance/manifest.csv");
    }

    private static Path onlyResultDir(Path repo) throws IOException {
        try (Stream<Path> dirs = Files.list(repo.resolve("conformance/results"))) {
            List<Path> cameo = dirs.filter(p -> p.getFileName().toString().startsWith("cameo_")).toList();
            assertEquals(1, cameo.size(), cameo.toString());
            return cameo.get(0);
        }
    }

    /** conformance/cameo/CameoConformanceRunner.groovy of this checkout. */
    static Path macro() {
        return repoRoot().resolve("conformance/cameo/CameoConformanceRunner.groovy");
    }

    /** The root of this checkout. */
    static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isRegularFile(dir.resolve("conformance/cameo/CameoConformanceRunner.groovy"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "conformance/cameo/CameoConformanceRunner.groovy not found above "
            + Path.of("").toAbsolutePath());
        return dir;
    }
}
