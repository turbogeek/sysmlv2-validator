package com.validator.cli;

import com.validator.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorCLITest {

    @Test
    @DisplayName("A KPAR argument is one file, and an error in a member prints as archive!member:line:column")
    void kparArgument(@TempDir Path dir) throws IOException {
        Path kpar = kpar(dir.resolve("Model.kpar"), "First.sysml", "package First { part def A; }",
            "sub/Second.sysml", "package Second {\n    part def B :> ;\n}\n");

        Run run = run(kpar.toString());

        assertEquals(1, run.code(), run.out());
        assertTrue(run.out().contains("Validating: Model.kpar"), run.out());
        assertTrue(run.out().contains("[ERROR] Model.kpar!sub/Second.sysml:2:"), run.out());
        assertTrue(run.out().contains("Files validated: 1"), run.out());
        assertTrue(run.out().contains("VALIDATION FAILED"), run.out());
    }

    @Test
    @DisplayName("-r validates the KPARs in a directory as well as the .sysml files")
    void recursiveFindsKpars(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("models"));
        kpar(dir.resolve("models/Good.kpar"), "Good.sysml", "package Good { part def A; }");
        Files.writeString(dir.resolve("Plain.sysml"), "package Plain { part def B; }");
        Files.writeString(dir.resolve("notes.txt"), "not a model");

        Run run = run("-r", dir.toString());

        assertEquals(0, run.code(), run.out());
        assertTrue(run.out().contains("Files validated: 2"), run.out());
        assertTrue(run.out().contains("VALIDATION PASSED"), run.out());
    }

    @Test
    @DisplayName("An archive that is not a KPAR fails with IO_INVALID_KPAR")
    void invalidKpar(@TempDir Path dir) throws IOException {
        Path bad = Files.writeString(dir.resolve("Bad.kpar"), "package NotAnArchive;");

        Run run = run(bad.toString());

        assertEquals(1, run.code(), run.out());
        assertTrue(run.out().contains("[ERROR] Bad.kpar:0:0"), run.out());
        assertTrue(run.out().contains("IO_INVALID_KPAR: Invalid KPAR"), run.out());
    }

    @Test
    @DisplayName("The printed file of a diagnostic is the file name, or archive name!member path")
    void printedFileName() {
        assertEquals("M.kpar!sub/A.sysml", ValidatorCLI.fileName(issue("/models/M.kpar!sub/A.sysml")));
        assertEquals("A.sysml", ValidatorCLI.fileName(issue("/models/A.sysml")));
        assertEquals("<unknown>", ValidatorCLI.fileName(issue(null)));
    }

    private record Run(int code, String out) {
    }

    private static Run run(String... args) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            int code = new CommandLine(new ValidatorCLI()).execute(args);
            return new Run(code, buffer.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(original);
        }
    }

    /** Write a KPAR with a .project.json and the given members (path, text, path, text, ...). */
    private static Path kpar(Path file, String... pathsAndTexts) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry(".project.json"));
            zip.write("{\"name\":\"Test\",\"version\":\"1.0.0\",\"usage\":[]}".getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < pathsAndTexts.length; i += 2) {
                zip.putNextEntry(new ZipEntry(pathsAndTexts[i]));
                zip.write(pathsAndTexts[i + 1].getBytes(StandardCharsets.UTF_8));
            }
        }
        return file;
    }

    private static ValidationError issue(String path) {
        return new ValidationError.Builder().filePath(path).message("m").build();
    }

    @Test
    void testSuggestionsEnabledByDefault() {
        ValidatorCLI cli = new ValidatorCLI();
        CommandLine cmd = new CommandLine(cli);
        // By default suggestions should be true
        // Let's pass a file so it parses correctly without throwing missing required param
        cmd.parseArgs("dummy.sysml");
        
        // Unfortunately picocli populates private fields. We can check via reflection
        try {
            java.lang.reflect.Field f = ValidatorCLI.class.getDeclaredField("suggestions");
            f.setAccessible(true);
            boolean suggestions = (boolean) f.get(cli);
            assertTrue(suggestions, "Smart Compiler (suggestions) should be enabled by default");
        } catch (Exception e) {
            fail("Failed to read suggestions field: " + e.getMessage());
        }
    }

    @Test
    void testNoSuggestionsFlag() {
        ValidatorCLI cli = new ValidatorCLI();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs("--no-suggestions", "dummy.sysml");
        
        try {
            java.lang.reflect.Field f = ValidatorCLI.class.getDeclaredField("suggestions");
            f.setAccessible(true);
            boolean suggestions = (boolean) f.get(cli);
            assertFalse(suggestions, "--no-suggestions should disable the Smart Compiler (suggestions)");
        } catch (Exception e) {
            fail("Failed to read suggestions field: " + e.getMessage());
        }
    }
    
    @Test
    void testExplicitSuggestionsFlag() {
        ValidatorCLI cli = new ValidatorCLI();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs("--suggestions", "dummy.sysml");
        
        try {
            java.lang.reflect.Field f = ValidatorCLI.class.getDeclaredField("suggestions");
            f.setAccessible(true);
            boolean suggestions = (boolean) f.get(cli);
            assertTrue(suggestions, "--suggestions should explicitly enable the Smart Compiler (suggestions)");
        } catch (Exception e) {
            fail("Failed to read suggestions field: " + e.getMessage());
        }
    }
}
