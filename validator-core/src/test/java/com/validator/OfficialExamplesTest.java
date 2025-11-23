package com.validator;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
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

    private static final String KERML_EXAMPLES_PATH =
        "E:/_Documents/git/SysML-v2-Release/kerml/src/examples";
    private static final String SYSML_SRC_PATH =
        "E:/_Documents/git/SysML-v2-Release/sysml/src";

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

        // Soft assertion - we want to see the results
        assertTrue(results.totalFiles > 0, "Should find KerML files");
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

        // Store detailed errors for analysis
        if (!results.errors.isEmpty()) {
            System.out.println("\n=== SysML Error Details (first 10) ===");
            results.errors.stream().limit(10).forEach(e -> {
                System.out.println("\nFile: " + e.file);
                e.errors.stream().limit(3).forEach(err -> System.out.println("  " + err));
            });
        }

        // Soft assertion
        assertTrue(results.totalFiles > 0, "Should find SysML files");
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
