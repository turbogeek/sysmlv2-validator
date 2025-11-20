package com.validator.semantic;

import com.validator.parser.SysMLv2ParserFacade;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for semantic analysis with real SysML v2 files.
 * Tests symbol table building, import resolution, and standard library integration.
 */
@DisplayName("Semantic Analysis Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SemanticAnalysisIntegrationTest {

    private static Path testSuiteRoot;
    private static StandardLibraryManager standardLibrary;
    private static List<IntegrationTestResult> allResults;

    @BeforeAll
    public static void setUpAll() {
        // Find test suite directory
        testSuiteRoot = Paths.get("E:/_Documents/git/sysml-validator/test-suite");
        if (!Files.exists(testSuiteRoot)) {
            testSuiteRoot = Paths.get("test-suite");
        }

        // Initialize standard library
        standardLibrary = new StandardLibraryManager();
        standardLibrary.initializeBuiltins();

        allResults = new ArrayList<>();
    }

    @AfterAll
    public static void tearDownAll() {
        // Print summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SEMANTIC ANALYSIS INTEGRATION TEST SUMMARY");
        System.out.println("=".repeat(80));

        int totalFiles = allResults.size();
        int successful = (int) allResults.stream().filter(r -> r.success).count();
        int failed = totalFiles - successful;

        System.out.println(String.format("Total files tested: %d", totalFiles));
        System.out.println(String.format("Successful: %d (%.1f%%)", successful, 100.0 * successful / totalFiles));
        System.out.println(String.format("Failed: %d (%.1f%%)", failed, 100.0 * failed / totalFiles));

        // Symbol statistics
        int totalSymbols = allResults.stream().mapToInt(r -> r.symbolCount).sum();
        int totalImports = allResults.stream().mapToInt(r -> r.importCount).sum();
        long totalTime = allResults.stream().mapToLong(r -> r.buildTimeMs).sum();

        System.out.println(String.format("\nTotal symbols extracted: %d", totalSymbols));
        System.out.println(String.format("Total imports resolved: %d", totalImports));
        System.out.println(String.format("Total build time: %d ms (avg: %.1f ms/file)",
            totalTime, (double) totalTime / totalFiles));

        // Top 10 files by symbol count
        System.out.println("\nTop 10 files by symbol count:");
        allResults.stream()
            .sorted(Comparator.comparingInt((IntegrationTestResult r) -> r.symbolCount).reversed())
            .limit(10)
            .forEach(r -> System.out.println(String.format("  %s: %d symbols", r.fileName, r.symbolCount)));

        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(1)
    @DisplayName("Test StockTicker Models (12 files)")
    public void testStockTickerModels() throws IOException {
        Path stockTickerDir = testSuiteRoot.resolve("stockticker");
        if (!Files.exists(stockTickerDir)) {
            System.out.println("StockTicker directory not found, skipping test");
            return;
        }

        List<File> files = findSysMLFiles(stockTickerDir);
        System.out.println(String.format("\nTesting %d StockTicker models...", files.size()));

        int passed = 0;
        for (File file : files) {
            IntegrationTestResult result = testFile(file, "stockticker");
            allResults.add(result);

            if (result.success) {
                passed++;
                System.out.println(String.format("  ✓ %s (%d symbols, %d imports, %d ms)",
                    file.getName(), result.symbolCount, result.importCount, result.buildTimeMs));
            } else {
                System.out.println(String.format("  ✗ %s: %s",
                    file.getName(), result.errorMessage));
            }
        }

        System.out.println(String.format("StockTicker: %d/%d passed\n", passed, files.size()));
        assertTrue(passed > 0, "At least some StockTicker models should parse successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Test Positive Test Cases (49 files)")
    public void testPositiveTestCases() throws IOException {
        Path positiveDir = testSuiteRoot.resolve("positive");
        if (!Files.exists(positiveDir)) {
            System.out.println("Positive test directory not found, skipping test");
            return;
        }

        List<File> files = findSysMLFiles(positiveDir);
        System.out.println(String.format("\nTesting %d positive test cases...", files.size()));

        int passed = 0;
        for (File file : files) {
            IntegrationTestResult result = testFile(file, "positive");
            allResults.add(result);

            if (result.success) {
                passed++;
                if (result.symbolCount > 5 || result.importCount > 0) {
                    System.out.println(String.format("  ✓ %s (%d symbols, %d imports)",
                        file.getName(), result.symbolCount, result.importCount));
                }
            } else {
                System.out.println(String.format("  ✗ %s: %s",
                    file.getName(), result.errorMessage));
            }
        }

        System.out.println(String.format("Positive tests: %d/%d passed (%.1f%%)\n",
            passed, files.size(), 100.0 * passed / files.size()));

        // For positive tests, we expect high success rate
        double successRate = 100.0 * passed / files.size();
        assertTrue(successRate >= 70.0,
            String.format("Expected at least 70%% success rate, got %.1f%%", successRate));
    }

    @Test
    @Order(3)
    @DisplayName("Test Performance with Large Models")
    public void testPerformance() {
        // Find the largest models tested
        List<IntegrationTestResult> largeModels = allResults.stream()
            .filter(r -> r.symbolCount >= 10)
            .sorted(Comparator.comparingInt((IntegrationTestResult r) -> r.symbolCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        System.out.println("\nPerformance test - Top 5 largest models:");
        for (IntegrationTestResult result : largeModels) {
            System.out.println(String.format("  %s: %d symbols in %d ms (%.2f symbols/ms)",
                result.fileName, result.symbolCount, result.buildTimeMs,
                result.symbolCount / (double) Math.max(1, result.buildTimeMs)));

            // Assert performance: should build <1s even for large models
            assertTrue(result.buildTimeMs < 1000,
                String.format("%s took %d ms, expected <1000ms", result.fileName, result.buildTimeMs));
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test Standard Library Usage")
    public void testStandardLibraryUsage() {
        int filesWithStdLib = 0;
        Map<String, Integer> stdLibUsage = new LinkedHashMap<>();

        for (IntegrationTestResult result : allResults) {
            if (result.standardLibraryTypes > 0) {
                filesWithStdLib++;
            }

            // Track which standard library packages are used
            for (String pkgName : result.stdLibPackagesUsed) {
                stdLibUsage.merge(pkgName, 1, Integer::sum);
            }
        }

        System.out.println(String.format("\nStandard Library Usage:"));
        System.out.println(String.format("  Files using standard library: %d/%d",
            filesWithStdLib, allResults.size()));

        if (!stdLibUsage.isEmpty()) {
            System.out.println("  Packages used:");
            stdLibUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.println(String.format("    %s: %d files", e.getKey(), e.getValue())));
        }
    }

    /**
     * Test a single SysML file.
     */
    private IntegrationTestResult testFile(File file, String category) {
        long startTime = System.currentTimeMillis();
        IntegrationTestResult result = new IntegrationTestResult(file.getName(), category);

        try {
            // Parse the file
            SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
            ParseTree parseTree = parser.parseFile(file.getAbsolutePath());

            if (parseTree == null) {
                result.success = false;
                result.errorMessage = "Parse tree is null";
                result.buildTimeMs = System.currentTimeMillis() - startTime;
                return result;
            }

            // Build symbol table
            SymbolTable symbolTable = SymbolTableBuilder.build(parseTree, file.getAbsolutePath());
            result.symbolCount = symbolTable.getAllSymbols().size();

            // Resolve imports
            ImportResolver importResolver = new ImportResolver(symbolTable, standardLibrary);
            importResolver.resolveAllImports();

            ImportResolver.ImportResolutionStats importStats = importResolver.getStats();
            result.importCount = importStats.getResolvedImports();
            result.unresolvedImports = importStats.getUnresolvedImports();

            // Check for standard library usage
            for (Symbol symbol : symbolTable.getAllSymbols()) {
                String qname = symbol.getQualifiedName();
                if (qname.startsWith("ISQ::") || qname.startsWith("SI::") ||
                    qname.startsWith("KerML::") || qname.startsWith("SysML::")) {
                    result.standardLibraryTypes++;

                    String pkg = qname.split("::")[0];
                    if (!result.stdLibPackagesUsed.contains(pkg)) {
                        result.stdLibPackagesUsed.add(pkg);
                    }
                }
            }

            result.success = true;
            result.buildTimeMs = System.currentTimeMillis() - startTime;

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            result.buildTimeMs = System.currentTimeMillis() - startTime;
        }

        return result;
    }

    /**
     * Find all .sysml files in a directory.
     */
    private List<File> findSysMLFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return Collections.emptyList();
        }

        return Files.walk(directory)
            .filter(p -> p.toString().endsWith(".sysml"))
            .map(Path::toFile)
            .sorted(Comparator.comparing(File::getName))
            .collect(Collectors.toList());
    }

    /**
     * Result of testing a single file.
     */
    static class IntegrationTestResult {
        String fileName;
        String category;
        boolean success;
        String errorMessage;
        int symbolCount;
        int importCount;
        int unresolvedImports;
        int standardLibraryTypes;
        List<String> stdLibPackagesUsed = new ArrayList<>();
        long buildTimeMs;

        IntegrationTestResult(String fileName, String category) {
            this.fileName = fileName;
            this.category = category;
        }
    }
}
