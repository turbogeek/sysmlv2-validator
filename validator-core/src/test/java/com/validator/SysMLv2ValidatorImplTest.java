package com.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SysMLv2ValidatorImpl.
 */
public class SysMLv2ValidatorImplTest {

    private final Validator validator = new SysMLv2ValidatorImpl();

    @Test
    public void testValidateValidFile(@TempDir Path tempDir) throws IOException {
        // Create valid SysML file
        String content = """
            package TestPackage;

            part def Vehicle {
                part engine;
                part wheels[4];
            }
            """;

        Path testFile = tempDir.resolve("test.sysml");
        Files.writeString(testFile, content);

        ValidationResult result = validator.validate(testFile.toFile());

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Valid file should pass validation");
        assertEquals(0, result.getErrorCount());
        assertEquals(0, result.getWarningCount());
    }

    @Test
    public void testValidateInvalidFile(@TempDir Path tempDir) throws IOException {
        // Create invalid SysML file (syntax error)
        String content = "package ;"; // Missing package name

        Path testFile = tempDir.resolve("invalid.sysml");
        Files.writeString(testFile, content);

        ValidationResult result = validator.validate(testFile.toFile());

        assertNotNull(result);
        assertFalse(result.isSuccess(), "Invalid file should fail validation");
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    public void testValidateString() {
        String validSource = "package Test;";

        ValidationResult result = validator.validate(validSource, "test.sysml");

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testValidateInvalidString() {
        String invalidSource = "package ;";

        ValidationResult result = validator.validate(invalidSource, "test.sysml");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    public void testValidateMultipleFiles(@TempDir Path tempDir) throws IOException {
        // Create multiple files
        Path file1 = tempDir.resolve("file1.sysml");
        Files.writeString(file1, "package File1;");

        Path file2 = tempDir.resolve("file2.sysml");
        Files.writeString(file2, "package File2;");

        Path file3 = tempDir.resolve("file3.sysml");
        Files.writeString(file3, "package ;"); // Invalid

        List<File> files = Arrays.asList(
            file1.toFile(),
            file2.toFile(),
            file3.toFile()
        );

        List<ValidationResult> results = validator.validateAll(files);

        assertNotNull(results);
        assertEquals(3, results.size());

        // First two should pass
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());

        // Third should fail
        assertFalse(results.get(2).isSuccess());
    }

    @Test
    public void testValidateNonExistentFile() {
        File nonExistent = new File("does_not_exist.sysml");

        assertThrows(IOException.class, () -> {
            validator.validate(nonExistent);
        });
    }

    @Test
    public void testValidateComplexModel(@TempDir Path tempDir) throws IOException {
        String content = """
            package BreakfastModel;

            public import ScalarValues::*;

            part def Ingredient {
                attribute name : String;
                attribute quantity : Real;
            }

            part def Meal {
                part ingredients : Ingredient[*];
            }

            action def MakeBreakfast {
                action prepareIngredients;
                action cook;
                action serve;

                first prepareIngredients;
                then cook;
                then serve;
            }

            requirement def NutritionalRequirement {
                subject meal : Meal;
                require constraint meal.ingredients->size() >= 3;
            }

            view BreakfastFlow : Views::ActionView {
                expose MakeBreakfast::**;
                render asDefault;
            }
            """;

        Path testFile = tempDir.resolve("breakfast.sysml");
        Files.writeString(testFile, content);

        ValidationResult result = validator.validate(testFile.toFile());

        assertNotNull(result);
        if (!result.isSuccess()) {
            System.err.println("Validation errors:");
            for (ValidationError error : result.getErrors()) {
                System.err.printf("  Line %d:%d - %s%n",
                    error.getLine(), error.getColumn(), error.getMessage());
            }
        }
        assertTrue(result.isSuccess(), "Complex model should parse successfully");
    }

    @Test
    public void testGetVersion() {
        String version = validator.getVersion();
        assertNotNull(version);
        assertFalse(version.isEmpty());
    }

    @Test
    public void testGetName() {
        String name = validator.getName();
        assertNotNull(name);
        assertFalse(name.isEmpty());
    }
}
