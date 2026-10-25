package com.validator;

import com.validator.library.LibraryConfig;
import com.validator.testutil.TestPaths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Inheritance warnings (diamonds and name conflicts) as the validator reports them: for one file, across
 * files validated together, and for the example files in the positive test suite.
 */
@DisplayName("Inheritance warnings through the validator")
class InheritanceValidationTest {

    private static SysMLv2ValidatorImpl validator;

    @BeforeAll
    static void setUp() {
        assumeTrue(new LibraryConfig().hasLibraryPaths(), "semantic validation needs the SysML library");
        validator = new SysMLv2ValidatorImpl();
    }

    @Test
    @DisplayName("A file with an unresolved diamond validates with a warning and no errors")
    void singleFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("Diamond.sysml");
        Files.writeString(file, """
            package Diamond {
                part def A { attribute f; }
                part def B :> A { attribute :>> f = 1; }
                part def C :> A { attribute :>> f = 2; }
                part def D :> B, C;
            }
            """);

        ValidationResult result = validator.validate(file.toFile());

        assertEquals(List.of(), result.getErrors());
        List<ValidationWarning> conflicts = inheritanceWarnings(result);
        assertEquals(1, conflicts.size(), conflicts.toString());
        ValidationWarning w = conflicts.get(0);
        assertEquals(ErrorCodes.DIAMOND_INHERITANCE_CONFLICT, w.getErrorCode());
        assertEquals(file.toFile().getAbsolutePath(), w.getFilePath());
        assertEquals(5, w.getLine());
        assertTrue(w.getMessage().contains("'Diamond::D'"), w.getMessage());
    }

    @Test
    @DisplayName("Files validated together: the diamond is reported in the file that declares D only")
    void acrossFiles(@TempDir Path dir) throws IOException {
        Path base = dir.resolve("Base.sysml");
        Files.writeString(base, """
            package Base {
                part def A { attribute f; }
                part def B :> A { attribute :>> f default 1; }
                part def C :> A { attribute :>> f default 2; }
            }
            """);
        Path user = dir.resolve("User.sysml");
        Files.writeString(user, """
            package User {
                private import Base::*;
                part def Unresolved :> B, C;
                part def Resolved :> B, C { attribute f :>> B::f, C::f = 3; }
            }
            """);

        List<ValidationResult> results = validator.validateAll(List.of(base.toFile(), user.toFile()));

        assertEquals(List.of(), inheritanceWarnings(results.get(0)));
        List<ValidationWarning> conflicts = inheritanceWarnings(results.get(1));
        assertEquals(1, conflicts.size(), conflicts.toString());
        assertEquals(3, conflicts.get(0).getLine());
        assertTrue(conflicts.get(0).getMessage().contains("'User::Unresolved'"), conflicts.get(0).getMessage());
        assertEquals(List.of(), results.get(1).getErrors());
    }

    @Test
    @DisplayName("The resolved diamond example has no errors and no inheritance warnings")
    void resolvedExample() throws IOException {
        ValidationResult result = validator.validate(example("61_diamond_resolved.sysml"));

        assertEquals(List.of(), result.getErrors());
        assertEquals(List.of(), inheritanceWarnings(result));
    }

    @Test
    @DisplayName("The unresolved diamond example has no errors and the documented warnings")
    void unresolvedExample() throws IOException {
        ValidationResult result = validator.validate(example("62_diamond_unresolved_warnings.sysml"));

        assertEquals(List.of(), result.getErrors());
        Map<String, String> found = new TreeMap<>();
        for (ValidationWarning w : inheritanceWarnings(result)) {
            String element = w.getLine() == 40 ? "HybridVehicle"
                : w.getLine() == 43 ? "prototype" : "line " + w.getLine();
            String feature = w.getMessage().replaceAll("(?s).*?'DiamondUnresolved::Vehicle::(\\w+)'.*", "$1");
            if (w.getErrorCode().equals(ErrorCodes.MULTIPLE_INHERITANCE_NAME_CONFLICT)) {
                feature = w.getMessage().replaceAll("(?s).*feature named '(\\w+)'.*", "$1");
            }
            String verdict = w.getMessage().contains("duplicates") ? "duplicates"
                : w.getMessage().contains("contradict") ? "contradict" : "name";
            found.put(element + " " + feature, w.getErrorCode() + " " + verdict);
        }
        Map<String, String> expected = new TreeMap<>();
        for (String element : List.of("HybridVehicle", "prototype")) {
            expected.put(element + " range", ErrorCodes.DIAMOND_INHERITANCE_CONFLICT + " contradict");
            expected.put(element + " energySource", ErrorCodes.DIAMOND_INHERITANCE_CONFLICT + " contradict");
            expected.put(element + " wheelCount", ErrorCodes.DIAMOND_INHERITANCE_CONFLICT + " duplicates");
            expected.put(element + " serviceInterval", ErrorCodes.MULTIPLE_INHERITANCE_NAME_CONFLICT + " name");
        }
        assertEquals(expected, found);
        assertEquals(8, inheritanceWarnings(result).size());
    }

    // ------------------------------------------------------------------------------------------

    private static File example(String name) {
        return TestPaths.testSuiteRoot().resolve("positive").resolve(name).toFile();
    }

    private static List<ValidationWarning> inheritanceWarnings(ValidationResult result) {
        return result.getWarnings().stream()
            .filter(w -> ErrorCodes.DIAMOND_INHERITANCE_CONFLICT.equals(w.getErrorCode())
                || ErrorCodes.MULTIPLE_INHERITANCE_NAME_CONFLICT.equals(w.getErrorCode()))
            .toList();
    }
}
