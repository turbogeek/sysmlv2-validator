package com.validator;

import com.validator.kpar.KparArchive;
import com.validator.library.LibraryConfig;
import com.validator.testutil.Kpars;
import com.validator.testutil.TestPaths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Validating a model packaged as a KPAR: its members are validated together into one result, each diagnostic
 * names its member, and an archive that is not a KPAR gives {@link ErrorCodes#IO_INVALID_KPAR}.
 */
@DisplayName("KPAR models through the validator")
class KparValidationTest {

    private static final String VEHICLES = """
        package Vehicles {
            part def Vehicle {
                part wheels : Parts::Wheel[4];
            }
        }
        """;

    private static final String PARTS = """
        package Parts {
            part def Wheel;
            part def Garage {
                part car : Vehicles::Vehicle;
            }
        }
        """;

    private static SysMLv2ValidatorImpl validator;
    private static boolean semantic;

    @BeforeAll
    static void setUp() {
        semantic = new LibraryConfig().hasLibraryPaths();
        validator = new SysMLv2ValidatorImpl();
    }

    @Test
    @DisplayName("Two members that refer to each other validate clean, as one result for the archive")
    void membersResolveEachOther(@TempDir Path dir) throws IOException {
        assumeTrue(semantic, "name resolution needs the SysML library");
        Path kpar = Kpars.model(dir.resolve("Vehicles.kpar"), members("Vehicles.sysml", VEHICLES,
            "parts/Parts.sysml", PARTS));

        ValidationResult result = validator.validate(kpar.toFile());

        assertEquals(kpar.toFile().getAbsolutePath(), result.getFilePath());
        assertEquals(List.of(), result.getErrors());
        assertEquals(List.of(), result.getWarnings());
        assertTrue(result.isSuccess());
        assertEquals(result.getErrors(), validator.validate(kpar).getErrors());
    }

    @Test
    @DisplayName("A member alone does not resolve the names of the member it refers to")
    void memberAloneIsUnresolved(@TempDir Path dir) throws IOException {
        assumeTrue(semantic, "name resolution needs the SysML library");
        Path kpar = Kpars.model(dir.resolve("Alone.kpar"), members("Vehicles.sysml", VEHICLES));

        ValidationResult result = validator.validate(kpar.toFile());

        assertEquals(1, result.getErrorCount(), result.getErrors().toString());
        ValidationError error = result.getErrors().get(0);
        assertEquals(ErrorCodes.SEMANTIC_UNRESOLVED_REFERENCE, error.getErrorCode());
        assertEquals(KparArchive.memberPath(kpar.toFile().getAbsolutePath(), "Vehicles.sysml"), error.getFilePath());
        assertEquals(3, error.getLine());
    }

    @Test
    @DisplayName("A syntax error in the second member is reported in that member, at its line and column")
    void syntaxErrorInSecondMember(@TempDir Path dir) throws IOException {
        String second = """
            package Second {
                // not persisted
                part def B :> ;
            }
            """;
        Path kpar = Kpars.model(dir.resolve("Model.kpar"), members("First.sysml", "package First { part def A; }",
            "sub/Second.sysml", second));
        Path plain = Files.writeString(dir.resolve("Second.sysml"), second);
        String member = KparArchive.memberPath(kpar.toFile().getAbsolutePath(), "sub/Second.sysml");

        ValidationResult result = validator.validate(kpar.toFile());
        ValidationResult expected = validator.validate(plain.toFile());

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorCount(), result.getErrors().toString());
        ValidationError error = result.getErrors().get(0);
        assertEquals(member, error.getFilePath());
        assertEquals("SYNTAX_ERROR", error.getErrorCode());
        assertEquals(3, error.getLine());
        ValidationError plainError = expected.getErrors().get(0);
        assertEquals(plainError.getColumn(), error.getColumn());
        assertEquals(plainError.getMessage(), error.getMessage());
        ValidationWarning comment = result.getWarnings().stream()
            .filter(w -> "LINE_COMMENT_NOT_PERSISTED".equals(w.getErrorCode())).findFirst().orElseThrow();
        assertEquals(member, comment.getFilePath());
        assertEquals(2, comment.getLine());
    }

    @Test
    @DisplayName("An unresolved name in the second member is reported in that member, at its line")
    void semanticErrorInSecondMember(@TempDir Path dir) throws IOException {
        assumeTrue(semantic, "name resolution needs the SysML library");
        Path kpar = Kpars.model(dir.resolve("Model.kpar"), members("First.sysml", "package First { part def A; }",
            "Second.sysml", """
                package Second {
                    part def B :> First::A;
                    part def C :> First::Missing;
                }
                """));

        ValidationResult result = validator.validate(kpar.toFile());

        assertEquals(1, result.getErrorCount(), result.getErrors().toString());
        ValidationError error = result.getErrors().get(0);
        assertEquals(ErrorCodes.SEMANTIC_UNRESOLVED_REFERENCE, error.getErrorCode());
        assertEquals(kpar.toFile().getAbsolutePath() + "!Second.sysml", error.getFilePath());
        assertEquals(3, error.getLine());
    }

    @Test
    @DisplayName("A file that is not a zip, a zip without .project.json and one without model files are invalid")
    void malformedArchives(@TempDir Path dir) throws IOException {
        Path notZip = Files.writeString(dir.resolve("NotZip.kpar"), "package NotAnArchive;");
        Path noProject = Kpars.write(dir.resolve("NoProject.kpar"), Map.of("A.sysml", "package A;"));
        Path noModel = Kpars.write(dir.resolve("NoModel.kpar"), Map.of(".project.json", Kpars.project("X", "1")));

        assertInvalid(notZip, "not a zip archive");
        assertInvalid(noProject, "no .project.json");
        assertInvalid(noModel, "no model file");
    }

    @Test
    @DisplayName("validateAll validates archives alongside files, and an invalid archive does not stop the others")
    void archivesAlongsideFiles(@TempDir Path dir) throws IOException {
        Path kpar = Kpars.model(dir.resolve("Parts.kpar"), members("Parts.sysml", PARTS));
        Path vehicles = Files.writeString(dir.resolve("Vehicles.sysml"), VEHICLES);
        Path invalid = Files.writeString(dir.resolve("Invalid.kpar"), "not a zip");
        Path missing = dir.resolve("Missing.kpar");

        List<ValidationResult> results = validator.validateAll(
            List.of(kpar.toFile(), vehicles.toFile(), invalid.toFile(), missing.toFile()));

        assertEquals(4, results.size());
        assertEquals(kpar.toFile().getAbsolutePath(), results.get(0).getFilePath());
        assertEquals(vehicles.toFile().getAbsolutePath(), results.get(1).getFilePath());
        if (semantic) {
            assertEquals(List.of(), results.get(0).getErrors());
            assertEquals(List.of(), results.get(1).getErrors());
        }
        assertEquals(ErrorCodes.IO_INVALID_KPAR, results.get(2).getErrors().get(0).getErrorCode());
        assertEquals("IO_ERROR", results.get(3).getErrors().get(0).getErrorCode());
        assertThrows(IOException.class, () -> validator.validate(missing.toFile()));
        assertThrows(IOException.class, () -> validator.validate(dir.toFile()));
    }

    @Test
    @DisplayName("The StockTicker model as a KPAR has the diagnostics of its 12 files validated together")
    void stockTickerAsKpar(@TempDir Path dir) throws IOException {
        List<Path> files;
        try (Stream<Path> listed = Files.list(TestPaths.testSuiteRoot().resolve("stockticker"))) {
            files = listed.filter(p -> p.getFileName().toString().endsWith(".sysml")).sorted().toList();
        }
        assertEquals(12, files.size(), files.toString());
        Map<String, String> members = new LinkedHashMap<>();
        for (Path file : files) {
            members.put(file.getFileName().toString(), Files.readString(file));
        }
        Path kpar = Kpars.model(dir.resolve("StockTicker.kpar"), members);

        List<ValidationResult> separate = validator.validateAll(files.stream().map(Path::toFile).toList());
        ValidationResult archive = validator.validate(kpar.toFile());

        List<String> expectedErrors = new ArrayList<>();
        List<String> expectedWarnings = new ArrayList<>();
        for (ValidationResult result : separate) {
            String name = new File(result.getFilePath()).getName();
            result.getErrors().forEach(e -> expectedErrors.add(describe(name, e)));
            result.getWarnings().forEach(w -> expectedWarnings.add(describe(name, w)));
        }
        List<String> errors = archive.getErrors().stream()
            .map(e -> describe(KparArchive.memberOf(e.getFilePath()), e)).toList();
        List<String> warnings = archive.getWarnings().stream()
            .map(w -> describe(KparArchive.memberOf(w.getFilePath()), w)).toList();
        assertEquals(expectedErrors, errors);
        assertEquals(expectedWarnings, warnings);
        assertEquals(kpar.toFile().getAbsolutePath(), archive.getFilePath());
    }

    @Test
    @DisplayName("A diagnostic without a file path gets its member's; one with a path is kept as it is")
    void locate() {
        ValidationWarning warning = (ValidationWarning) new ValidationWarning.Builder()
            .line(4).column(2).message("m").errorCode("C").addSuggestion("s").context("x").specReference("r")
            .build();
        ValidationError error = new ValidationError.Builder().message("e").build();

        ValidationError located = SysMLv2ValidatorImpl.locate(warning, "A.kpar!B.sysml");
        ValidationError locatedError = SysMLv2ValidatorImpl.locate(error, "A.kpar!B.sysml");

        assertTrue(located instanceof ValidationWarning);
        assertEquals("A.kpar!B.sysml", located.getFilePath());
        assertEquals(warning.toString().replace("null:", "A.kpar!B.sysml:"), located.toString());
        assertEquals(List.of("s"), located.getSuggestions());
        assertEquals("x", located.getContext());
        assertFalse(locatedError instanceof ValidationWarning);
        assertEquals("A.kpar!B.sysml", locatedError.getFilePath());
        assertSame(located, SysMLv2ValidatorImpl.locate(located, "other"));
    }

    private static void assertInvalid(Path kpar, String message) throws IOException {
        ValidationResult result = validator.validate(kpar.toFile());

        assertEquals(kpar.toFile().getAbsolutePath(), result.getFilePath());
        assertEquals(1, result.getErrorCount(), result.getErrors().toString());
        assertEquals(List.of(), result.getWarnings());
        ValidationError error = result.getErrors().get(0);
        assertEquals(ErrorCodes.IO_INVALID_KPAR, error.getErrorCode());
        assertEquals(kpar.toFile().getAbsolutePath(), error.getFilePath());
        assertEquals(0, error.getLine());
        assertTrue(error.getMessage().contains(message), error.getMessage());
        assertTrue(error.getMessage().startsWith("Invalid KPAR"), error.getMessage());
    }

    private static Map<String, String> members(String... pathsAndTexts) {
        Map<String, String> members = new LinkedHashMap<>();
        for (int i = 0; i < pathsAndTexts.length; i += 2) {
            members.put(pathsAndTexts[i], pathsAndTexts[i + 1]);
        }
        return members;
    }

    private static String describe(String file, ValidationError e) {
        return e.getSeverity() + " " + file + ":" + e.getLine() + ":" + e.getColumn() + " " + e.getErrorCode()
            + " " + e.getMessage();
    }
}
