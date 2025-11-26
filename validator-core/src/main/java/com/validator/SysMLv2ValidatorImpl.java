package com.validator;

import com.validator.library.LibraryConfig;
import com.validator.library.LibraryIndex;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import com.validator.semantic.SemanticValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main implementation of the SysML v2 validator.
 * Performs both syntax and semantic validation.
 * - Phase 1: Syntax validation using ANTLR parser
 * - Phase 2: Semantic validation including import resolution
 */
public class SysMLv2ValidatorImpl implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SysMLv2ValidatorImpl.class);
    private static final String VERSION = "0.2.0-SNAPSHOT";
    private static final String NAME = "SysML v2 Semantic Validator";

    private final SysMLv2ParserFacade parserFacade;
    private final LibraryIndex libraryIndex;
    private final boolean semanticValidationEnabled;

    public SysMLv2ValidatorImpl() {
        this.parserFacade = new SysMLv2ParserFacade();

        // Initialize library index
        LibraryConfig libraryConfig = new LibraryConfig();
        this.libraryIndex = new LibraryIndex();

        // Index libraries if available
        if (libraryConfig.hasLibraryPaths()) {
            libraryIndex.indexLibraries(libraryConfig);
            this.semanticValidationEnabled = true;
            LOGGER.info("Semantic validation enabled with {} indexed packages",
                libraryIndex.getIndexedPackages().size());
        } else {
            this.semanticValidationEnabled = false;
            LOGGER.warn("Semantic validation disabled - no library paths configured");
        }
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
        List<ValidationError> errors = new ArrayList<>(parseResult.getSyntaxErrors().stream()
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
            .toList());

        List<ValidationWarning> warnings = new ArrayList<>();

        // Perform semantic validation if enabled and syntax is valid
        if (semanticValidationEnabled && errors.isEmpty() && parseResult.getParseTree() != null) {
            LOGGER.debug("Performing semantic validation");
            SemanticValidator semanticValidator =
                new SemanticValidator(file.getAbsolutePath(), libraryIndex);
            List<ValidationError> semanticErrors =
                semanticValidator.validate(parseResult.getParseTree());
            errors.addAll(semanticErrors);

            // Add semantic warnings
            for (String warning : semanticValidator.getWarnings()) {
                warnings.add((ValidationWarning) new ValidationWarning.Builder()
                    .message(warning)
                    .build());
            }

            LOGGER.debug("Semantic validation found {} errors, {} warnings",
                semanticErrors.size(), semanticValidator.getWarnings().size());
        } else if (!semanticValidationEnabled) {
            warnings.add((ValidationWarning) new ValidationWarning.Builder()
                .message("Semantic validation disabled - "
                    + "configure library path to enable import resolution")
                .build());
        }

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
