package com.validator.cli;

import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationError;
import com.validator.ValidationResult;
import com.validator.Validator;
import com.validator.refactor.RefactoringEngine;
import com.validator.refactor.RefactoringEngine.RefactoringPlan;
import com.validator.refactor.RefactoringEngine.ApplyResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command-line interface for SysML v2 semantic validator.
 */
@Command(
    name = "sysml-validator",
    mixinStandardHelpOptions = true,
    version = "0.1.0-SNAPSHOT",
    description = "ANTLR-based semantic validator for SysML v2 and KerML with intelligent error reporting"
)
public class ValidatorCLI implements Callable<Integer> {

    @Parameters(
        index = "0..*",
        description = "Files to validate (*.sysml, *.kerml)"
    )
    private List<File> files;

    @Option(
        names = {"-r", "--recursive"},
        description = "Recursively validate all files in directories"
    )
    private boolean recursive = false;

    @Option(
        names = {"-s", "--suggestions"},
        description = "Enable intelligent error suggestions (spelling, imports, types)"
    )
    private boolean suggestions = true;

    @Option(
        names = {"-f", "--format"},
        description = "Output format: text, json, xml (default: text)"
    )
    private String format = "text";

    @Option(
        names = {"-o", "--output"},
        description = "Output file (default: stdout)"
    )
    private File outputFile;

    @Option(
        names = {"--no-color"},
        description = "Disable colored output"
    )
    private boolean noColor = false;

    @Option(
        names = {"-v", "--verbose"},
        description = "Verbose output"
    )
    private boolean verbose = false;

    @Option(
        names = {"--refactor-imports"},
        description = "Analyze and suggest import refactorings (wildcard to specific)"
    )
    private boolean refactorImports = false;

    @Option(
        names = {"--apply-refactoring", "-y"},
        description = "Automatically apply import refactorings without prompting"
    )
    private boolean applyRefactoring = false;

    @Option(
        names = {"--refactor-format"},
        description = "Output format for refactoring suggestions: human, json, lsp, monaco (default: human)"
    )
    private String refactorFormat = "human";

    @Option(
        names = {"--error-free-only"},
        description = "Only refactor files without parse errors (default: true)"
    )
    private boolean errorFreeOnly = true;

    @Override
    public Integer call() throws Exception {
        System.out.println("SysML v2 Semantic Validator v0.1.0-SNAPSHOT");
        System.out.println("===========================================");
        System.out.println();

        if (files == null || files.isEmpty()) {
            System.err.println("Error: No files specified");
            System.err.println();
            System.err.println("Usage: sysml-validator [OPTIONS] <files...>");
            System.err.println("       sysml-validator --help for more information");
            return 1;
        }

        // Expand files list if recursive option is enabled
        List<File> filesToValidate = expandFileList(files, recursive);

        if (verbose) {
            System.out.println("Validator: SysML v2 Semantic Validator");
            System.out.println("Version: 0.1.0-SNAPSHOT");
            System.out.println("Current Phase: Phase 1 - Syntax Validation");
            System.out.println("Files to validate: " + filesToValidate.size());
            System.out.println();
        }

        // Create validator
        Validator validator = new SysMLv2ValidatorImpl();

        // Validate files
        List<ValidationResult> results;
        try {
            results = validator.validateAll(filesToValidate);
        } catch (IOException e) {
            System.err.println("Error during validation: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 2;
        }

        // Display results
        int totalErrors = 0;
        int totalWarnings = 0;
        int filesWithErrors = 0;

        for (ValidationResult result : results) {
            if (result.getErrorCount() > 0 || result.getWarningCount() > 0 || verbose) {
                printResult(result);
            }

            totalErrors += result.getErrorCount();
            totalWarnings += result.getWarningCount();
            if (result.getErrorCount() > 0) {
                filesWithErrors++;
            }
        }

        // Summary
        System.out.println();
        System.out.println("========================================");
        System.out.println("VALIDATION SUMMARY");
        System.out.println("========================================");
        System.out.println("Files validated: " + results.size());
        System.out.println("Files with errors: " + filesWithErrors);
        System.out.println("Total errors: " + totalErrors);
        System.out.println("Total warnings: " + totalWarnings);
        System.out.println();

        // Handle import refactoring if requested
        if (refactorImports) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("IMPORT REFACTORING ANALYSIS");
            System.out.println("========================================");
            System.out.println();

            int refactoredFiles = performImportRefactoring(filesToValidate);
            if (refactoredFiles > 0) {
                System.out.println();
                System.out.println("Refactored " + refactoredFiles + " file(s)");
            }
        }

        if (totalErrors == 0) {
            System.out.println("✓ VALIDATION PASSED");
            return 0;
        } else {
            System.out.println("✗ VALIDATION FAILED");
            return 1;
        }
    }

    private int performImportRefactoring(List<File> filesToRefactor) {
        RefactoringEngine engine = new RefactoringEngine();
        engine.setErrorFreeOnly(errorFreeOnly);

        // Set output format
        switch (refactorFormat.toLowerCase()) {
            case "json":
                engine.setOutputFormat(RefactoringEngine.OutputFormat.JSON);
                break;
            case "lsp":
                engine.setOutputFormat(RefactoringEngine.OutputFormat.LSP);
                break;
            case "monaco":
                engine.setOutputFormat(RefactoringEngine.OutputFormat.MONACO);
                break;
            default:
                engine.setOutputFormat(RefactoringEngine.OutputFormat.HUMAN);
        }

        int refactoredCount = 0;
        Scanner scanner = applyRefactoring ? null : new Scanner(System.in);

        for (File file : filesToRefactor) {
            if (!file.getName().endsWith(".sysml") && !file.getName().endsWith(".kerml")) {
                continue;
            }

            RefactoringPlan plan = engine.analyze(file.getAbsolutePath());

            // Output the refactoring plan
            String output = engine.formatOutput(plan);
            System.out.println(output);

            if (plan.canApply()) {
                boolean shouldApply = applyRefactoring;

                if (!applyRefactoring && scanner != null) {
                    System.out.print("Apply these refactorings to " + file.getName() + "? [y/N] ");
                    String response = scanner.nextLine().trim().toLowerCase();
                    shouldApply = response.equals("y") || response.equals("yes");
                }

                if (shouldApply) {
                    ApplyResult result = engine.apply(plan);
                    System.out.println(result);
                    if (result.isSuccess() && result.getAppliedCount() > 0) {
                        refactoredCount++;
                    }
                }
            }
        }

        if (scanner != null) {
            scanner.close();
        }

        return refactoredCount;
    }

    private void printResult(ValidationResult result) {
        System.out.println("Validating: " + new File(result.getFilePath()).getName());
        System.out.println("========================================");

        if (result.getErrorCount() == 0 && result.getWarningCount() == 0) {
            System.out.println("✓ No errors or warnings found");
        } else {
            for (ValidationError error : result.getErrors()) {
                System.out.printf("[%s] %s:%d:%d%n",
                    error.getSeverity(),
                    new File(error.getFilePath()).getName(),
                    error.getLine(),
                    error.getColumn());
                System.out.printf("  %s: %s%n", error.getErrorCode(), error.getMessage());
                
                if (error.getSpecReference() != null && !error.getSpecReference().isEmpty()) {
                    System.out.printf("  Ref: %s%n", error.getSpecReference());
                }

                if (!error.getSuggestions().isEmpty() && suggestions) {
                    System.out.println("  Suggestions:");
                    for (String suggestion : error.getSuggestions()) {
                        System.out.println("    - " + suggestion);
                    }
                }
                System.out.println();
            }

            for (ValidationError warning : result.getWarnings()) {
                System.out.printf("[%s] %s:%d:%d%n",
                    warning.getSeverity(),
                    new File(warning.getFilePath()).getName(),
                    warning.getLine(),
                    warning.getColumn());
                System.out.printf("  %s: %s%n", warning.getErrorCode(), warning.getMessage());
                
                if (warning.getSpecReference() != null && !warning.getSpecReference().isEmpty()) {
                    System.out.printf("  Ref: %s%n", warning.getSpecReference());
                }
                System.out.println();
            }
        }

        if (verbose) {
            System.out.printf("Validation time: %dms%n", result.getValidationTimeMs());
        }
        System.out.println();
    }

    private List<File> expandFileList(List<File> fileList, boolean recurse) {
        List<File> result = new ArrayList<>();

        for (File file : fileList) {
            if (file.isDirectory() && recurse) {
                expandDirectory(file, result);
            } else if (file.isFile()) {
                result.add(file);
            }
        }

        return result;
    }

    private void expandDirectory(File dir, List<File> result) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File file : dirFiles) {
                if (file.isDirectory()) {
                    expandDirectory(file, result);
                } else if (file.getName().endsWith(".sysml") || file.getName().endsWith(".kerml")) {
                    result.add(file);
                }
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ValidatorCLI()).execute(args);
        System.exit(exitCode);
    }
}
