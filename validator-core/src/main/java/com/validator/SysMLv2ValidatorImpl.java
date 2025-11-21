package com.validator;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main implementation of the SysML v2 validator.
 * Currently performs syntax validation using ANTLR parser.
 * Semantic validation will be added in Phase 2.
 */
public class SysMLv2ValidatorImpl implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SysMLv2ValidatorImpl.class);
    private static final String VERSION = "0.1.0-SNAPSHOT";
    private static final String NAME = "SysML v2 Semantic Validator";

    private final SysMLv2ParserFacade parserFacade;

    public SysMLv2ValidatorImpl() {
        this.parserFacade = new SysMLv2ParserFacade();
    }

    @Override
    public ValidationResult validate(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }

        if (!file.isFile()) {
            throw new IOException("Not a file: " + file.getAbsolutePath());
        }

        if (!SysMLv2ParserFacade.isSysMLFile(file) && !SysMLv2ParserFacade.isKerMLFile(file)) {
            LOGGER.warn("File does not have .sysml or .kerml extension: {}", file.getName());
        }

        LOGGER.info("Validating file: {}", file.getAbsolutePath());
        long startTime = System.currentTimeMillis();

        // Parse the file
        ParseResult parseResult = parserFacade.parseFile(file);

        // Convert syntax errors to validation errors using streams (with suggestions)
        List<ValidationError> errors = parseResult.getSyntaxErrors().stream()
            .map(syntaxError -> {
                ValidationError.Builder builder = new ValidationError.Builder()
                    .filePath(file.getAbsolutePath())
                    .line(syntaxError.getLine())
                    .column(syntaxError.getCharPositionInLine())
                    .message(syntaxError.getMessage())
                    .errorCode("SYNTAX_ERROR")
                    .severity(ValidationError.Severity.ERROR);

                // Add spelling suggestions if available
                if (syntaxError.hasSuggestions()) {
                    builder.suggestions(syntaxError.getSuggestions());
                }
                return builder.build();
            })
            .toList();

        List<ValidationWarning> warnings = new ArrayList<>();

        long validationTime = System.currentTimeMillis() - startTime;

        ValidationResult result = new ValidationResult(
            file.getAbsolutePath(),
            errors,
            warnings,
            validationTime
        );

        LOGGER.info("Validation completed: {} errors, {} warnings in {}ms",
            errors.size(), warnings.size(), validationTime);

        return result;
    }

    @Override
    public ValidationResult validate(Path filePath) throws IOException {
        return validate(filePath.toFile());
    }

    @Override
    public ValidationResult validate(String sourceCode, String fileName) {
        LOGGER.info("Validating source: {}", fileName);
        long startTime = System.currentTimeMillis();

        // Parse the source code
        ParseResult parseResult = parserFacade.parseString(sourceCode, fileName);

        // Convert syntax errors to validation errors using streams (with suggestions)
        List<ValidationError> errors = parseResult.getSyntaxErrors().stream()
            .map(syntaxError -> {
                ValidationError.Builder builder = new ValidationError.Builder()
                    .filePath(fileName)
                    .line(syntaxError.getLine())
                    .column(syntaxError.getCharPositionInLine())
                    .message(syntaxError.getMessage())
                    .errorCode("SYNTAX_ERROR")
                    .severity(ValidationError.Severity.ERROR);

                // Add spelling suggestions if available
                if (syntaxError.hasSuggestions()) {
                    builder.suggestions(syntaxError.getSuggestions());
                }
                return builder.build();
            })
            .toList();

        List<ValidationWarning> warnings = new ArrayList<>();

        long validationTime = System.currentTimeMillis() - startTime;

        ValidationResult result = new ValidationResult(
            fileName,
            errors,
            warnings,
            validationTime
        );

        LOGGER.info("Validation completed: {} errors, {} warnings in {}ms",
            errors.size(), warnings.size(), validationTime);

        return result;
    }

    @Override
    public List<ValidationResult> validateAll(List<File> files) throws IOException {
        LOGGER.info("Validating {} files", files.size());

        List<ValidationResult> results = files.stream()
            .map(file -> {
                try {
                    return validate(file);
                } catch (IOException e) {
                    LOGGER.error("Error validating file: {}", file.getAbsolutePath(), e);
                    // Create error result
                    ValidationError error = new ValidationError.Builder()
                        .filePath(file.getAbsolutePath())
                        .line(0)
                        .column(0)
                        .message("Failed to read file: " + e.getMessage())
                        .errorCode("IO_ERROR")
                        .severity(ValidationError.Severity.ERROR)
                        .build();

                    return new ValidationResult(
                        file.getAbsolutePath(),
                        List.of(error),
                        List.of(),
                        0
                    );
                }
            })
            .toList();

        LOGGER.info("Validation complete: {} files processed", results.size());
        return results;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
