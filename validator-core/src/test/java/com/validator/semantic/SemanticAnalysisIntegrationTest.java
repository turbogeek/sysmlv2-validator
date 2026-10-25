package com.validator.semantic;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.testutil.BuildInfo;
import com.validator.testutil.PerformanceComparison;
import com.validator.testutil.PerformanceHistory;
import com.validator.testutil.PerformanceHistory.RecordMode;
import com.validator.testutil.PerformanceHistory.Row;
import com.validator.testutil.PerformanceHistory.Run;
import com.validator.testutil.PerformanceReadme;
import com.validator.testutil.PerformanceReference;
import com.validator.testutil.TestPaths;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for semantic analysis with real SysML v2 files.
 * Tests symbol table building, import resolution, and standard library integration.
 */
@DisplayName("Semantic Analysis Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SemanticAnalysisIntegrationTest {

    /** Warm builds of each budgeted model in testPerformance; their median is the model's time. */
    private static final int WARM_RUNS = 5;

    private static Path testSuiteRoot;
    private static StandardLibraryManager standardLibrary;
    private static List<IntegrationTestResult> allResults;
    private static PerformanceReference.Measurement referenceBefore;

    @BeforeAll
    public static void setUpAll() {
        // Find test suite directory
        testSuiteRoot = TestPaths.testSuiteRoot();

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
        // model is built WARM_RUNS more times on the now warm JVM and the median is what the budget applies to and
        // what the history records. Known slow models keep their first time: their limits are ratios with a wide
        // margin. Every model is timed and reported before anything is asserted.
        List<PerformanceHistory.Timing> timings = new ArrayList<>();
        List<String> budgetFailures = new ArrayList<>();
        for (IntegrationTestResult result : largeModels) {
            boolean knownSlow = calibration.isKnownSlow(result.fileName);
            List<Double> warm = new ArrayList<>();
            if (!knownSlow) {
                for (int i = 0; i < WARM_RUNS; i++) {
                    warm.add(testFile(result.file, result.category).buildMs());
                }
                Collections.sort(warm);
            }
            double ms = knownSlow ? result.buildMs() : warm.get(WARM_RUNS / 2);
            double limitMs = knownSlow ? calibration.knownSlowLimitMs(result.fileName, reference) : budgetMs;
            double ratio = ms / reference.medianMs();
            String warmText = warm.stream().map(w -> String.format(Locale.ROOT, "%.1f", w))
                .collect(Collectors.joining(", ", "[", "]"));
            System.out.println(String.format(Locale.ROOT,
                "  %s: %d symbols in %.1f ms (first build %d ms, warm %s; %.2f x reference%s)",
                result.fileName, result.symbolCount, ms, result.buildTimeMs, knownSlow ? "-" : warmText, ratio,
                knownSlow ? ", known slow" : ""));
            timings.add(new PerformanceHistory.Timing(result.fileName,
                knownSlow ? PerformanceHistory.FIRST_BUILD : PerformanceHistory.WARM_MEDIAN_PREFIX + WARM_RUNS, 1,
                result.symbolCount, ms));
            if (knownSlow && ms > limitMs) {
                budgetFailures.add(String.format("%s (known slow) took %.0f ms, more than %.0f x the reference %.1f ms"
                        + " = %.0f ms: it got slower", result.fileName, ms,
                    calibration.knownSlowRatioMax().get(result.fileName), reference.medianMs(), limitMs));
            } else if (!knownSlow && ms >= budgetMs) {
                budgetFailures.add(String.format("%s took %.0f ms, budget %.0f ms (%.0f ms nominal x slowdown %.2f x"
                        + " spread %.2f; reference %.1f ms, nominal %.1f ms)", result.fileName, ms, budgetMs,
                    calibration.budgetNominalMs(), slowdown, spread, reference.medianMs(),
                    calibration.nominalReferenceMs()));
            }
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
        // Each test suite's total: the sum of its files' first builds in this JVM, from tests 1 and 2
        for (String suite : List.of("stockticker", "positive")) {
            List<IntegrationTestResult> files = allResults.stream().filter(r -> r.category.equals(suite)).toList();
            if (!files.isEmpty()) {
                timings.add(new PerformanceHistory.Timing(PerformanceHistory.SUITE_PREFIX + suite,
                    PerformanceHistory.FIRST_BUILDS_TOTAL, files.size(),
                    files.stream().mapToInt(r -> r.symbolCount).sum(),
                    files.stream().mapToDouble(IntegrationTestResult::buildMs).sum()));
            }
        }

        HistoryOutcome history = largeModels.isEmpty() ? null
            : compareWithHistory(timings, reference.medianMs(), slowdown, !budgetFailures.isEmpty());

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
        if (history != null) {
            report.put("historyRecorded", history.decision().append());
            report.put("historyDecision", history.decision().reason());
            report.put("historyUntrusted", history.untrusted().orElse(null));
            report.put("historyRegressions", history.regressions().stream()
                .map(l -> l.now().subject()).collect(Collectors.toList()));
        }
        PerformanceReference.writeReport(Paths.get("target", "perf", "semantic-analysis-perf.json"), report);

        assertTrue(budgetFailures.isEmpty(), String.join("\n", budgetFailures));
        if (history != null && history.fails()) {
            fail(history.regressions().stream().map(l -> String.format(Locale.ROOT,
                    "%s (%s): %s x the reference, %s in the last run recorded on this machine (%s at %s), %s",
                    l.now().subject(), l.now().measure(), PerformanceComparison.ratio(l.now().ratio()),
                    PerformanceComparison.ratio(l.baseline().orElseThrow().ratio()), history.baseline().version(),
                    history.baseline().commit(), l.change()))
                .collect(Collectors.joining("\n", "Clearly slower than the last run recorded on this machine ("
                    + history.threshold().describe() + "):\n", "\nIf the slowdown is intended, record this run as the"
                    + " new baseline: mvn verify -D" + RecordMode.PROPERTY + "=true (see "
                    + PerformanceReadme.README_FILE + ")")));
        }
    }

    /** What the history check of one run found and decided; fails says whether its regressions fail the test. */
    private record HistoryOutcome(Optional<String> untrusted, PerformanceHistory.Decision decision, Run baseline,
                                  PerformanceComparison.Threshold threshold,
                                  List<PerformanceComparison.Line> regressions, boolean fails) {
    }

    /**
     * Compares this run with the performance history in docs/performance: with the latest run of the previous
     * version, with the last recorded run and with the last run recorded on this machine, which is the baseline a
     * clear regression fails against (usually all the same run). Writes the would-be rows to
     * target/perf/history-entry.csv, and appends them to the history and regenerates its README when perf.record says
     * so: always for true, never for false, and by default only for the first run of a project version that passed
     * its checks and whose ratios can be trusted (PerformanceComparison.untrusted).
     */
    private HistoryOutcome compareWithHistory(List<PerformanceHistory.Timing> timings, double referenceMs,
                                              double slowdown, boolean overBudget) {
        Path root = TestPaths.repositoryRoot();
        BuildInfo build = BuildInfo.capture(root);
        // A Java agent goes into the note, since a row recorded with one has ratios that do not compare
        String note = Stream.concat(Stream.ofNullable(System.getProperty("perf.note")),
                build.javaAgents().stream().map(agent -> "java agent " + agent))
            .filter(part -> !part.isBlank())
            .collect(Collectors.joining("; "));
        List<Row> rows = PerformanceHistory.newRun(build, Instant.now(), referenceMs, slowdown, timings, note);
        Path entry = Paths.get("target", "perf", "history-entry.csv");
        try {
            Files.createDirectories(entry.getParent());
            Files.writeString(entry, PerformanceHistory.header() + PerformanceHistory.formatRows(rows));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + entry, e);
        }

        Path historyFile = root.resolve(PerformanceReadme.HISTORY_FILE);
        PerformanceHistory history = PerformanceHistory.read(historyFile);
        PerformanceComparison.Threshold threshold = PerformanceComparison.Threshold.load();
        Optional<String> previousVersion = history.previousVersion(build.version());
        Optional<Run> previousRun = previousVersion.flatMap(history::latestRun);
        Optional<Run> lastRun = history.lastRun();
        Optional<Run> baseline = PerformanceComparison.baseline(history, rows);
        System.out.println("\n" + PerformanceComparison.table(rows, build.version(), previousVersion, previousRun,
            lastRun, baseline, threshold));
        List<PerformanceComparison.Line> regressions =
            PerformanceComparison.regressions(PerformanceComparison.compare(rows, baseline, threshold));

        RecordMode mode = RecordMode.fromProperty(System.getProperty(RecordMode.PROPERTY));
        Optional<String> untrusted = PerformanceComparison.untrusted(build, threshold);
        PerformanceHistory.Decision decision =
            PerformanceHistory.decide(mode, build, history, overBudget || !regressions.isEmpty(), untrusted);
        if (decision.append()) {
            PerformanceHistory.append(historyFile, rows);
            PerformanceReadme.regenerate(historyFile, root.resolve(PerformanceReadme.README_FILE));
        }
        System.out.println((decision.append() ? "Recorded in " : "Not recorded in ") + historyFile + ": "
            + decision.reason() + "; this run's rows are in " + entry.toAbsolutePath());
        if (!regressions.isEmpty() && decision.append()) {
            System.out.println("WARNING: " + regressions.size() + " clear regression(s) accepted as the new baseline");
        } else if (!regressions.isEmpty() && untrusted.isPresent()) {
            System.out.println("WARNING: " + regressions.size() + " clear regression(s), not failing because "
                + untrusted.get() + "; run the test again without that");
        }
        return new HistoryOutcome(untrusted, decision, baseline.orElse(null), threshold, regressions,
            PerformanceComparison.fails(regressions, decision, untrusted));
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
        // nanoTime: a warm build takes a few milliseconds, too short for currentTimeMillis to time usefully
        long startTime = System.nanoTime();
        IntegrationTestResult result = new IntegrationTestResult(file.getName(), category);
        result.file = file;

        try {
            // Parse the file
            SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
            ParseTree parseTree = parser.parseFile(file).getParseTree();

            if (parseTree == null) {
                result.success = false;
                result.errorMessage = "Parse tree is null";
                result.setBuildTime(System.nanoTime() - startTime);
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
            result.setBuildTime(System.nanoTime() - startTime);

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            result.setBuildTime(System.nanoTime() - startTime);
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
        long buildTimeNanos;
        File file;

        IntegrationTestResult(String fileName, String category) {
            this.fileName = fileName;
            this.category = category;
        }

        void setBuildTime(long nanos) {
            buildTimeNanos = nanos;
            buildTimeMs = Math.round(nanos / 1_000_000.0);
        }

        double buildMs() {
            return buildTimeNanos / 1_000_000.0;
        }
    }
}
