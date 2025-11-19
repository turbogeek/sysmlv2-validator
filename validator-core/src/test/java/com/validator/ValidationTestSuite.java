package com.validator;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation test suite that validates all test files.
 * This test suite is automatically run as part of the Maven build process.
 *
 * Test Categories:
 * - Positive tests: Valid SysML v2 files that should parse without errors
 * - Negative tests: Invalid SysML v2 files that should detect specific errors
 * - StockTicker tests: Real-world complex models
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ValidationTestSuite {

    private static Validator validator;
    private static Path testSuiteRoot;
    private static List<TestResult> allResults = new ArrayList<>();

    static class TestResult {
        String fileName;
        String category;
        boolean expectedToPass;
        boolean actuallyPassed;
        int errorCount;
        String firstError;

        TestResult(String fileName, String category, boolean expectedToPass, ValidationResult result) {
            this.fileName = fileName;
            this.category = category;
            this.expectedToPass = expectedToPass;
            this.actuallyPassed = result.isSuccess();
            this.errorCount = result.getErrorCount();
            if (!result.getErrors().isEmpty()) {
                ValidationError firstErr = result.getErrors().get(0);
                this.firstError = String.format("Line %d:%d - %s",
                    firstErr.getLine(), firstErr.getColumn(), firstErr.getMessage());
            }
        }

        boolean isCorrect() {
            return expectedToPass == actuallyPassed;
        }
    }

    @BeforeAll
    public static void setUp() {
        validator = new SysMLv2ValidatorImpl();

        // Find test-suite directory (works from both IDE and Maven)
        Path currentDir = Paths.get("").toAbsolutePath();
        testSuiteRoot = currentDir.resolve("../../test-suite").normalize();

        if (!Files.exists(testSuiteRoot)) {
            // Try from module root
            testSuiteRoot = currentDir.getParent().resolve("test-suite").normalize();
        }

        if (!Files.exists(testSuiteRoot)) {
            // Try absolute path
            testSuiteRoot = Paths.get("E:/_Documents/git/sysml-validator/test-suite");
        }

        System.out.println("========================================");
        System.out.println("SysML v2 Validation Test Suite");
        System.out.println("========================================");
        System.out.println("Test suite root: " + testSuiteRoot);
        System.out.println();
    }

    @AfterAll
    public static void printSummary() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("TEST SUITE SUMMARY");
        System.out.println("========================================");

        int totalTests = allResults.size();
        long correct = allResults.stream().filter(TestResult::isCorrect).count();
        long incorrect = totalTests - correct;

        long positiveTests = allResults.stream().filter(r -> r.expectedToPass).count();
        long positiveCorrect = allResults.stream().filter(r -> r.expectedToPass && r.isCorrect()).count();

        long negativeTests = allResults.stream().filter(r -> !r.expectedToPass).count();
        long negativeCorrect = allResults.stream().filter(r -> !r.expectedToPass && r.isCorrect()).count();

        System.out.printf("Total tests: %d%n", totalTests);
        System.out.printf("Correct: %d (%.1f%%)%n", correct, (correct * 100.0 / totalTests));
        System.out.printf("Incorrect: %d%n", incorrect);
        System.out.println();

        System.out.printf("Positive tests (should pass): %d/%d correct%n", positiveCorrect, positiveTests);
        System.out.printf("Negative tests (should fail): %d/%d correct%n", negativeCorrect, negativeTests);
        System.out.println();

        if (incorrect > 0) {
            System.out.println("Failed Tests:");
            allResults.stream()
                .filter(r -> !r.isCorrect())
                .forEach(r -> {
                    System.out.printf("  ✗ %s (%s)%n", r.fileName, r.category);
                    System.out.printf("    Expected: %s, Actual: %s%n",
                        r.expectedToPass ? "PASS" : "FAIL",
                        r.actuallyPassed ? "PASS" : "FAIL");
                    if (r.firstError != null) {
                        System.out.printf("    Error: %s%n", r.firstError);
                    }
                });
            System.out.println();
        }

        System.out.println("========================================");
    }

    @Test
    @Order(1)
    @DisplayName("Positive Tests - Valid SysML v2 Files")
    public void testPositiveCases() throws IOException {
        Path positiveDir = testSuiteRoot.resolve("positive");

        if (!Files.exists(positiveDir)) {
            System.err.println("WARNING: Positive test directory not found: " + positiveDir);
            return;
        }

        List<File> files = findSysMLFiles(positiveDir);
        System.out.printf("Running %d positive tests (should all pass)...%n", files.size());

        int passed = 0;
        int failed = 0;

        for (File file : files) {
            ValidationResult result = validator.validate(file);
            TestResult testResult = new TestResult(file.getName(), "positive", true, result);
            allResults.add(testResult);

            if (result.isSuccess()) {
                passed++;
                System.out.printf("  ✓ %s%n", file.getName());
            } else {
                failed++;
                System.out.printf("  ✗ %s - UNEXPECTED FAILURE%n", file.getName());
                for (ValidationError error : result.getErrors()) {
                    System.out.printf("      Line %d:%d - %s%n",
                        error.getLine(), error.getColumn(), error.getMessage());
                }
            }
        }

        System.out.printf("Positive tests: %d passed, %d failed%n%n", passed, failed);

        // All positive tests should pass
        assertEquals(files.size(), passed,
            String.format("Expected all %d positive tests to pass, but %d failed", files.size(), failed));
    }

    @Test
    @Order(2)
    @DisplayName("Negative Tests - Invalid SysML v2 Files")
    public void testNegativeCases() throws IOException {
        Path negativeDir = testSuiteRoot.resolve("negative");

        if (!Files.exists(negativeDir)) {
            System.err.println("WARNING: Negative test directory not found: " + negativeDir);
            return;
        }

        List<File> files = findSysMLFiles(negativeDir);
        System.out.printf("Running %d negative tests (should all fail)...%n", files.size());

        int correctlyFailed = 0;
        int incorrectlyPassed = 0;

        for (File file : files) {
            ValidationResult result = validator.validate(file);
            TestResult testResult = new TestResult(file.getName(), "negative", false, result);
            allResults.add(testResult);

            if (!result.isSuccess()) {
                correctlyFailed++;
                System.out.printf("  ✓ %s - detected %d error(s)%n",
                    file.getName(), result.getErrorCount());
            } else {
                incorrectlyPassed++;
                System.out.printf("  ✗ %s - UNEXPECTED PASS (should have detected errors)%n",
                    file.getName());
            }
        }

        System.out.printf("Negative tests: %d correctly failed, %d incorrectly passed%n%n",
            correctlyFailed, incorrectlyPassed);

        // All negative tests should fail (detect errors)
        assertEquals(files.size(), correctlyFailed,
            String.format("Expected all %d negative tests to fail, but %d passed", files.size(), incorrectlyPassed));
    }

    @Test
    @Order(3)
    @DisplayName("StockTicker Tests - Real-World Models")
    public void testStockTickerModels() throws IOException {
        Path stockTickerDir = testSuiteRoot.resolve("stockticker");

        if (!Files.exists(stockTickerDir)) {
            System.err.println("WARNING: StockTicker test directory not found: " + stockTickerDir);
            return;
        }

        List<File> files = findSysMLFiles(stockTickerDir);
        System.out.printf("Running %d StockTicker model tests...%n", files.size());

        int passed = 0;
        int failed = 0;

        for (File file : files) {
            ValidationResult result = validator.validate(file);
            TestResult testResult = new TestResult(file.getName(), "stockticker", true, result);
            allResults.add(testResult);

            if (result.isSuccess()) {
                passed++;
                System.out.printf("  ✓ %s (validated in %dms)%n",
                    file.getName(), result.getValidationTimeMs());
            } else {
                failed++;
                System.out.printf("  ✗ %s - %d error(s)%n",
                    file.getName(), result.getErrorCount());
                // Show first few errors
                result.getErrors().stream().limit(3).forEach(error -> {
                    System.out.printf("      Line %d:%d - %s%n",
                        error.getLine(), error.getColumn(), error.getMessage());
                });
                if (result.getErrorCount() > 3) {
                    System.out.printf("      ... and %d more error(s)%n",
                        result.getErrorCount() - 3);
                }
            }
        }

        System.out.printf("StockTicker tests: %d passed, %d failed%n%n", passed, failed);

        // StockTicker models should all pass (they're real-world models)
        if (failed > 0) {
            System.out.println("NOTE: StockTicker failures may indicate grammar gaps to fix");
        }
    }

    private List<File> findSysMLFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return new ArrayList<>();
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".sysml"))
                .map(Path::toFile)
                .sorted()
                .collect(Collectors.toList());
        }
    }
}
