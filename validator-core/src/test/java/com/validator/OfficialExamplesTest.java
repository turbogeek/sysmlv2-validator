package com.validator;

import com.validator.parser.SysMLv2Lexer;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import com.validator.testutil.TestPaths;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the validator against official SysML v2 release examples.
 * This test validates our grammar coverage against the official language examples.
 */
@DisplayName("Official SysML v2 Release Examples")
class OfficialExamplesTest {

    private static final String KERML_EXAMPLES_PATH = TestPaths.sysmlReleaseRoot()
        .map(root -> root.resolve("kerml/src/examples").toString())
        .orElse("SysML-v2-Release/kerml/src/examples");
    private static final String SYSML_SRC_PATH = TestPaths.sysmlReleaseRoot()
        .map(root -> root.resolve("sysml/src").toString())
        .orElse("SysML-v2-Release/sysml/src");

    /*
     * Ratchets: the number of official files that parse without syntax errors. They may only go up. When a grammar
     * change makes more files parse, raise them to the new count; never lower them to make a change pass.
     */

    /** sysml/src: all 252 files of the 2026-10 release checkout. */
    private static final int MIN_CLEAN_SYSML_FILES = 252;

    /** sysml/src files that parse with SLL prediction alone, without the facade's slower full-LL re-parse. */
    private static final int MIN_SLL_CLEAN_SYSML_FILES = 252;

    /**
     * kerml/src/examples: 54 of 58. The four that fail need KerML relationship members the grammar does not have yet
     * ('featuring F of y by C;', 'inverse B::g of A::f;', 'conjugation c conjugate X conjugates Y;' and the 'chains'
     * feature part). Note that the three ProductSelection_*.kerml files count as clean only because the unsupported
     * 'member' keyword is read as an expression statement.
     */
    private static final int MIN_CLEAN_KERML_FILES = 54;

    private static SysMLv2ParserFacade parserFacade;

    @BeforeAll
    static void setup() {
        parserFacade = new SysMLv2ParserFacade();
    }

    @Test
    @DisplayName("Parse KerML example files")
    void testKerMLExamples() throws IOException {
        Path kermlPath = Paths.get(KERML_EXAMPLES_PATH);
        if (!Files.exists(kermlPath)) {
            System.out.println("SKIP: KerML examples path not found: " + KERML_EXAMPLES_PATH);
            return;
        }

        TestResults results = parseFilesInDirectory(kermlPath, ".kerml");
        printResults("KerML Examples", results);

        // Store detailed errors for analysis
        if (!results.errors.isEmpty()) {
            System.out.println("\n=== KerML Error Details (first 10) ===");
            results.errors.stream().limit(10).forEach(e -> {
                System.out.println("\nFile: " + e.file);
                e.errors.stream().limit(3).forEach(err -> System.out.println("  " + err));
            });
        }

        assertTrue(results.totalFiles > 0, "Should find KerML files");
        assertTrue(results.filesSuccessful >= MIN_CLEAN_KERML_FILES,
            "KerML examples parsing without syntax errors dropped to " + results.filesSuccessful + " (minimum "
                + MIN_CLEAN_KERML_FILES + "); failing: " + failingFiles(results));
    }

    @Test
    @DisplayName("Parse SysML source files")
    void testSysMLSources() throws IOException {
        Path sysmlPath = Paths.get(SYSML_SRC_PATH);
        if (!Files.exists(sysmlPath)) {
            System.out.println("SKIP: SysML source path not found: " + SYSML_SRC_PATH);
            return;
        }

        TestResults results = parseFilesInDirectory(sysmlPath, ".sysml");
        printResults("SysML Sources", results);
        System.out.printf("SLL-only parses:     %d%n", results.filesSllClean);

        // Store detailed errors for analysis
        if (!results.errors.isEmpty()) {
            System.out.println("\n=== SysML Error Details (first 10) ===");
            results.errors.stream().limit(10).forEach(e -> {
                System.out.println("\nFile: " + e.file);
                e.errors.stream().limit(3).forEach(err -> System.out.println("  " + err));
            });
        }

        assertTrue(results.totalFiles > 0, "Should find SysML files");
        assertTrue(results.filesSuccessful >= MIN_CLEAN_SYSML_FILES,
            "SysML sources parsing without syntax errors dropped to " + results.filesSuccessful + " (minimum "
                + MIN_CLEAN_SYSML_FILES + "); failing: " + failingFiles(results));
        assertTrue(results.filesSllClean >= MIN_SLL_CLEAN_SYSML_FILES,
            "SysML sources parsing with SLL prediction alone dropped to " + results.filesSllClean + " (minimum "
                + MIN_SLL_CLEAN_SYSML_FILES + "): a grammar ambiguity now forces a full LL re-parse of "
                + results.sllFallbackFiles);
    }

    private static String failingFiles(TestResults results) {
        return results.errors.stream()
            .map(e -> Paths.get(e.file).getFileName() + " " + e.errors.stream().limit(1).toList())
            .toList()
            .toString();
    }

    /**
     * Parses with SLL prediction alone and fails on the first conflict, as the facade's fast first stage does. A
     * file that parses without errors but fails here costs a second, full LL parse (that re-parse made a warm parse
     * of the test suite's 12_simple_vehicle_model.sysml take about 7 s instead of 0.1 s).
     */
    private static boolean parsesWithSllAlone(String content) {
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(content));
        lexer.removeErrorListeners();
        SysMLv2Parser parser = new SysMLv2Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new BailErrorStrategy());
        try {
            parser.compilationUnit();
            return true;
        } catch (ParseCancellationException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Analyze error patterns across all files")
    void testErrorPatternAnalysis() throws IOException {
        Map<String, Integer> errorPatterns = new HashMap<>();

        // Analyze KerML
        Path kermlPath = Paths.get(KERML_EXAMPLES_PATH);
        if (Files.exists(kermlPath)) {
            analyzeErrorPatterns(kermlPath, ".kerml", errorPatterns);
        }

        // Analyze SysML
        Path sysmlPath = Paths.get(SYSML_SRC_PATH);
        if (Files.exists(sysmlPath)) {
            analyzeErrorPatterns(sysmlPath, ".sysml", errorPatterns);
        }

        // Print pattern analysis
        System.out.println("\n=== Error Pattern Analysis ===");
        errorPatterns.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(20)
            .forEach(e -> System.out.printf("  %4d: %s%n", e.getValue(), e.getKey()));
    }

    private TestResults parseFilesInDirectory(Path directory, String extension) throws IOException {
        TestResults results = new TestResults();

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> files = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(extension))
                .toList();

            results.totalFiles = files.size();

            for (Path file : files) {
                try {
                    String content = Files.readString(file);
                    SysMLv2ParserFacade.ParseResult parseResult =
                        parserFacade.parseString(content, file.getFileName().toString());

                    if (parseResult.hasErrors()) {
                        results.filesWithErrors++;
                        FileError fileError = new FileError();
                        fileError.file = file.toString();
                        fileError.errors = parseResult.getSyntaxErrors().stream()
                            .map(e -> e.getMessage())
                            .toList();
                        results.errors.add(fileError);
                    } else {
                        results.filesSuccessful++;
                        // The SLL check needs the facade's KerML keyword handling, so it covers .sysml files only
                        if (extension.equals(".sysml")) {
                            if (parsesWithSllAlone(content)) {
                                results.filesSllClean++;
                            } else {
                                results.sllFallbackFiles.add(file.getFileName().toString());
                            }
                        }

                        // Try to build symbol table
                        try {
                            SymbolTable symbolTable = SymbolTableBuilder.build(
                                parseResult.getParseTree(), file.getFileName().toString());
                            results.totalSymbols += symbolTable.getAllSymbols().size();
                        } catch (Exception e) {
                            // Symbol table building error
                            results.symbolTableErrors++;
                        }
                    }
                } catch (Exception e) {
                    results.readErrors++;
                }
            }
        }

        return results;
    }

    private void analyzeErrorPatterns(Path directory, String extension,
                                      Map<String, Integer> patterns) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> files = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(extension))
                .toList();

            for (Path file : files) {
                try {
                    String content = Files.readString(file);
                    SysMLv2ParserFacade.ParseResult parseResult =
                        parserFacade.parseString(content, file.getFileName().toString());

                    if (parseResult.hasErrors()) {
                        for (var error : parseResult.getSyntaxErrors()) {
                            String pattern = extractErrorPattern(error.getMessage());
                            patterns.merge(pattern, 1, Integer::sum);
                        }
                    }
                } catch (Exception e) {
                    // Ignore read errors in pattern analysis
                }
            }
        }
    }

    private String extractErrorPattern(String errorMessage) {
        // Extract the key pattern from error message
        if (errorMessage.contains("mismatched input")) {
            int idx = errorMessage.indexOf("mismatched input");
            int end = errorMessage.indexOf("expecting", idx);
            if (end > idx) {
                return "mismatched: " + errorMessage.substring(idx + 17, end).trim();
            }
            return "mismatched input";
        }
        if (errorMessage.contains("extraneous input")) {
            int idx = errorMessage.indexOf("extraneous input");
            int end = errorMessage.indexOf("expecting", idx);
            if (end > idx) {
                return "extraneous: " + errorMessage.substring(idx + 17, end).trim();
            }
            return "extraneous input";
        }
        if (errorMessage.contains("no viable alternative")) {
            return "no viable alternative";
        }
        if (errorMessage.contains("missing")) {
            int idx = errorMessage.indexOf("missing");
            int end = errorMessage.indexOf(" at ", idx);
            if (end > idx) {
                return "missing: " + errorMessage.substring(idx + 8, end).trim();
            }
            return "missing token";
        }
        // Generic
        if (errorMessage.length() > 50) {
            return errorMessage.substring(0, 50) + "...";
        }
        return errorMessage;
    }

    private void printResults(String category, TestResults results) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println(category + " Validation Results");
        System.out.println("=".repeat(70));
        System.out.printf("Total files:         %d%n", results.totalFiles);
        System.out.printf("Successful parses:   %d (%.1f%%)%n",
            results.filesSuccessful,
            results.totalFiles > 0 ? 100.0 * results.filesSuccessful / results.totalFiles : 0);
        System.out.printf("Files with errors:   %d (%.1f%%)%n",
            results.filesWithErrors,
            results.totalFiles > 0 ? 100.0 * results.filesWithErrors / results.totalFiles : 0);
        System.out.printf("Read errors:         %d%n", results.readErrors);
        System.out.printf("Symbol table errors: %d%n", results.symbolTableErrors);
        System.out.printf("Total symbols found: %d%n", results.totalSymbols);
        System.out.println("=".repeat(70));
    }

    static class TestResults {
        int totalFiles = 0;
        int filesSuccessful = 0;
        int filesSllClean = 0;
        List<String> sllFallbackFiles = new ArrayList<>();
        int filesWithErrors = 0;
        int readErrors = 0;
        int symbolTableErrors = 0;
        int totalSymbols = 0;
        List<FileError> errors = new ArrayList<>();
    }

    static class FileError {
        String file;
        List<String> errors;
    }
}
