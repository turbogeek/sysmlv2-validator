package com.validator.semantic;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.testutil.PerformanceReference;
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
    private static PerformanceReference.Measurement referenceBefore;

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

        // How fast the machine is before the models are built (see testPerformance)
        referenceBefore = PerformanceReference.measure();
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

        // Budgets scale with a reference workload measured in this run, before and after the builds: on a throttled
        // or busy machine (battery power, other programs) the reference is slower than its recorded nominal time and
        // the budget grows by the same factor, while a slower validator still fails because the workload does not
        // use the validator. Models known to be slow are held to their recorded ratio instead (perf-reference.properties).
        PerformanceReference.Measurement referenceAfter = PerformanceReference.measure();
        PerformanceReference.Measurement reference =
            referenceAfter.medianMs() > referenceBefore.medianMs() ? referenceAfter : referenceBefore;
        PerformanceReference.Calibration calibration = PerformanceReference.loadCalibration();
        double slowdown = calibration.slowdown(reference);
        double spread = Math.max(referenceBefore.spread(), referenceAfter.spread());
        double budgetMs = calibration.budgetMs(reference, spread);

        System.out.println(String.format("\nPerformance reference: before %.1f ms (spread %.2f), after %.1f ms"
                + " (spread %.2f), nominal %.1f ms; slowdown %.2f; budget %.0f ms",
            referenceBefore.medianMs(), referenceBefore.spread(), referenceAfter.medianMs(), referenceAfter.spread(),
            calibration.nominalReferenceMs(), slowdown, budgetMs));
        System.out.println("Performance test - Top 5 largest models:");
        List<Map<String, Object>> models = new ArrayList<>();
        // A model's first build in this JVM varies with JIT compilation and garbage collection, so each budgeted
        // model is built three more times on the now warm JVM and the median is what the budget applies to. Known
        // slow models keep their first time: their limits are ratios with a wide margin.
        Map<String, Long> timedMs = new LinkedHashMap<>();
        for (IntegrationTestResult result : largeModels) {
            boolean knownSlow = calibration.isKnownSlow(result.fileName);
            List<Long> warm = new ArrayList<>();
            if (!knownSlow) {
                for (int i = 0; i < 3; i++) {
                    warm.add(testFile(result.file, result.category).buildTimeMs);
                }
                Collections.sort(warm);
            }
            long ms = knownSlow ? result.buildTimeMs : warm.get(1);
            timedMs.put(result.fileName, ms);
            double limitMs = knownSlow ? calibration.knownSlowLimitMs(result.fileName, reference) : budgetMs;
            double ratio = ms / reference.medianMs();
            System.out.println(String.format("  %s: %d symbols in %d ms (first build %d ms, warm %s; %.1f x reference%s)",
                result.fileName, result.symbolCount, ms, result.buildTimeMs, knownSlow ? "-" : warm.toString(), ratio,
                knownSlow ? ", known slow" : ""));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("file", result.fileName);
            m.put("symbols", result.symbolCount);
            m.put("firstBuildMs", result.buildTimeMs);
            m.put("warmBuildMs", warm);
            m.put("timedMs", ms);
            m.put("ratioToReference", Math.round(ratio * 100) / 100.0);
            m.put("knownSlow", knownSlow);
            m.put("limitMs", Math.round(limitMs));
            m.put("withinLimit", ms <= limitMs);
            models.add(m);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("referenceBeforeMedianMs", referenceBefore.medianMs());
        report.put("referenceBeforeSpread", referenceBefore.spread());
        report.put("referenceAfterMedianMs", referenceAfter.medianMs());
        report.put("referenceAfterSpread", referenceAfter.spread());
        report.put("referenceNominalMs", calibration.nominalReferenceMs());
        report.put("slowdown", slowdown);
        report.put("budgetNominalMs", calibration.budgetNominalMs());
        report.put("spread", spread);
        report.put("budgetMs", budgetMs);
        report.put("knownSlowRatioMax", calibration.knownSlowRatioMax());
        report.put("models", models);
        PerformanceReference.writeReport(Paths.get("target", "perf", "semantic-analysis-perf.json"), report);

        for (IntegrationTestResult result : largeModels) {
            if (calibration.isKnownSlow(result.fileName)) {
                double limitMs = calibration.knownSlowLimitMs(result.fileName, reference);
                assertTrue(timedMs.get(result.fileName) <= limitMs,
                    String.format("%s (known slow) took %d ms, more than %.0f x the reference %.1f ms = %.0f ms: it got slower",
                        result.fileName, timedMs.get(result.fileName), calibration.knownSlowRatioMax().get(result.fileName),
                        reference.medianMs(), limitMs));
            } else {
                assertTrue(timedMs.get(result.fileName) < budgetMs,
                    String.format("%s took %d ms, budget %.0f ms (%.0f ms nominal x slowdown %.2f x spread %.2f;"
                            + " reference %.1f ms, nominal %.1f ms)", result.fileName, timedMs.get(result.fileName), budgetMs,
                        calibration.budgetNominalMs(), slowdown, spread, reference.medianMs(),
                        calibration.nominalReferenceMs()));
            }
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
        result.file = file;

        try {
            // Parse the file
            SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
            ParseTree parseTree = parser.parseFile(file).getParseTree();

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
        File file;

        IntegrationTestResult(String fileName, String category) {
            this.fileName = fileName;
            this.category = category;
        }
    }
}
