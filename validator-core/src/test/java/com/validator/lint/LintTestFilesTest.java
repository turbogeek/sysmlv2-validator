package com.validator.lint;

import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationResult;
import com.validator.ValidationWarning;
import com.validator.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for lint test files in test-suite/lint/.
 * Validates that the example files parse correctly and demonstrate
 * the expected lint warnings.
 */
@DisplayName("Lint Test Files Integration Tests")
public class LintTestFilesTest {

    private static Validator validator;
    private static Path lintTestDir;

    @BeforeAll
    public static void setUp() {
        validator = new SysMLv2ValidatorImpl();

        // Find the lint test directory
        Path projectRoot = Paths.get("").toAbsolutePath();
        lintTestDir = projectRoot.resolve("test-suite/lint");

        // If running from validator-core, go up one level
        if (!Files.exists(lintTestDir)) {
            lintTestDir = projectRoot.getParent().resolve("test-suite/lint");
        }
    }

    @Test
    @DisplayName("wildcard_imports.sysml parses without syntax errors")
    public void testWildcardImportsParses() throws IOException {
        Path file = lintTestDir.resolve("wildcard_imports.sysml");
        if (!Files.exists(file)) {
            System.out.println("Skipping test - file not found: " + file);
            return;
        }

        ValidationResult result = validator.validate(file.toFile());

        // Should parse without syntax errors
        // (May have semantic warnings, but no parse failures)
        assertTrue(result.getErrors().stream()
                        .noneMatch(e -> e.getErrorCode().startsWith("SYNTAX")),
                "File should parse without syntax errors");

        System.out.println("wildcard_imports.sysml validation result:");
        System.out.println("  Errors: " + result.getErrorCount());
        System.out.println("  Warnings: " + result.getWarningCount());
    }

    @Test
    @DisplayName("public_imports.sysml parses without syntax errors")
    public void testPublicImportsParses() throws IOException {
        Path file = lintTestDir.resolve("public_imports.sysml");
        if (!Files.exists(file)) {
            System.out.println("Skipping test - file not found: " + file);
            return;
        }

        ValidationResult result = validator.validate(file.toFile());

        assertTrue(result.getErrors().stream()
                        .noneMatch(e -> e.getErrorCode().startsWith("SYNTAX")),
                "File should parse without syntax errors");

        System.out.println("public_imports.sysml validation result:");
        System.out.println("  Errors: " + result.getErrorCount());
        System.out.println("  Warnings: " + result.getWarningCount());
    }

    @Test
    @DisplayName("naming_conventions.sysml parses without syntax errors")
    public void testNamingConventionsParses() throws IOException {
        Path file = lintTestDir.resolve("naming_conventions.sysml");
        if (!Files.exists(file)) {
            System.out.println("Skipping test - file not found: " + file);
            return;
        }

        ValidationResult result = validator.validate(file.toFile());

        assertTrue(result.getErrors().stream()
                        .noneMatch(e -> e.getErrorCode().startsWith("SYNTAX")),
                "File should parse without syntax errors");

        System.out.println("naming_conventions.sysml validation result:");
        System.out.println("  Errors: " + result.getErrorCount());
        System.out.println("  Warnings: " + result.getWarningCount());
    }

    @Test
    @DisplayName("unused_elements.sysml parses without syntax errors")
    public void testUnusedElementsParses() throws IOException {
        Path file = lintTestDir.resolve("unused_elements.sysml");
        if (!Files.exists(file)) {
            System.out.println("Skipping test - file not found: " + file);
            return;
        }

        ValidationResult result = validator.validate(file.toFile());

        assertTrue(result.getErrors().stream()
                        .noneMatch(e -> e.getErrorCode().startsWith("SYNTAX")),
                "File should parse without syntax errors");

        System.out.println("unused_elements.sysml validation result:");
        System.out.println("  Errors: " + result.getErrorCount());
        System.out.println("  Warnings: " + result.getWarningCount());
    }

    @Test
    @DisplayName("good_practices.sysml parses without syntax errors")
    public void testGoodPracticesParses() throws IOException {
        Path file = lintTestDir.resolve("good_practices.sysml");
        if (!Files.exists(file)) {
            System.out.println("Skipping test - file not found: " + file);
            return;
        }

        ValidationResult result = validator.validate(file.toFile());

        assertTrue(result.getErrors().stream()
                        .noneMatch(e -> e.getErrorCode().startsWith("SYNTAX")),
                "File should parse without syntax errors");

        System.out.println("good_practices.sysml validation result:");
        System.out.println("  Errors: " + result.getErrorCount());
        System.out.println("  Warnings: " + result.getWarningCount());
    }

    @Test
    @DisplayName("refactoring_example.sysml parses without syntax errors")
    public void testRefactoringExampleParses() throws IOException {
        Path file = lintTestDir.resolve("refactoring_example.sysml");
        if (!Files.exists(file)) {
            System.out.println("Skipping test - file not found: " + file);
            return;
        }

        ValidationResult result = validator.validate(file.toFile());

        assertTrue(result.getErrors().stream()
                        .noneMatch(e -> e.getErrorCode().startsWith("SYNTAX")),
                "File should parse without syntax errors");

        System.out.println("refactoring_example.sysml validation result:");
        System.out.println("  Errors: " + result.getErrorCount());
        System.out.println("  Warnings: " + result.getWarningCount());
    }

    @Test
    @DisplayName("unit_mismatches.sysml parses and produces unit lint warnings")
    public void testUnitMismatchesParses() throws IOException {
        Path file = lintTestDir.resolve("unit_mismatches.sysml");
        if (!Files.exists(file)) {
            System.out.println("Skipping test - file not found: " + file);
            return;
        }

        ValidationResult result = validator.validate(file.toFile());

        assertTrue(result.getErrors().stream()
                        .noneMatch(e -> e.getErrorCode().startsWith("SYNTAX")),
                "File should parse without syntax errors");

        System.out.println("unit_mismatches.sysml validation result:");
        System.out.println("  Errors: " + result.getErrorCount());
        System.out.println("  Warnings: " + result.getWarningCount());
        
        for(ValidationWarning w : result.getWarnings()) {
            System.out.println("  Warning: " + w.getErrorCode() + " - " + w.getMessage());
        }

        // Ensure we see the specific unit lint warnings
        boolean hasUnitMismatch = result.getWarnings().stream()
                .anyMatch(w -> "LINT020".equals(w.getErrorCode()));
        boolean hasMissingUnit = result.getWarnings().stream()
                .anyMatch(w -> "LINT021".equals(w.getErrorCode()));
        boolean hasGenericUnit = result.getWarnings().stream()
                .anyMatch(w -> "LINT022".equals(w.getErrorCode()));

        assertTrue(hasUnitMismatch, "Should detect LINT020 (unit mismatch)");
        assertTrue(hasMissingUnit, "Should detect LINT021 (missing unit)");
        assertTrue(hasGenericUnit, "Should detect LINT022 (generic type with unit)");
    }

    @Test
    @DisplayName("All lint test files exist")
    public void testAllLintFilesExist() {
        if (!Files.exists(lintTestDir)) {
            System.out.println("Lint test directory not found: " + lintTestDir);
            return;
        }

        String[] expectedFiles = {
                "wildcard_imports.sysml",
                "public_imports.sysml",
                "naming_conventions.sysml",
                "unused_elements.sysml",
                "good_practices.sysml",
                "refactoring_example.sysml",
                "unit_mismatches.sysml"
        };

        for (String fileName : expectedFiles) {
            Path file = lintTestDir.resolve(fileName);
            assertTrue(Files.exists(file), "Expected file to exist: " + file);
        }
    }
}
