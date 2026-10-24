package com.validator;

import com.validator.library.LibraryConfig;
import com.validator.library.LibraryLoader;
import com.validator.library.LibraryCache;
import com.validator.library.LibraryCacheData;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import com.validator.semantic.SemanticValidator;
import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.index.ModelIndex;
import com.validator.semantic.index.ModelIndexBuilder;
import com.validator.semantic.index.ReferenceChecker;
import com.validator.semantic.index.ReferenceSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final LibraryCache libraryCache;
    private final LibraryLoader libraryLoader;
    private final StandardLibraryManager standardLibraryManager;
    private final boolean semanticValidationEnabled;
    private final ModelIndex libraryIndex = new ModelIndex();

    public SysMLv2ValidatorImpl() {
        this.parserFacade = new SysMLv2ParserFacade();

        // Initialize standard library manager and library loader
        LibraryConfig libraryConfig = new LibraryConfig();
        this.libraryCache = new LibraryCache();
        this.standardLibraryManager = new StandardLibraryManager();
        this.standardLibraryManager.initializeBuiltins();
        this.libraryLoader = new LibraryLoader(this.libraryCache, this.standardLibraryManager);

        // Load libraries if paths are configured
        if (libraryConfig.hasLibraryPaths()) {
            this.libraryLoader.loadLibraries(libraryConfig);
            for (LibraryCacheData data : this.libraryLoader.getLoaded()) {
                this.libraryIndex.addAll(data.getElements());
            }
            this.semanticValidationEnabled = true;
            LOGGER.info("Semantic validation enabled with {} library symbols loaded",
                this.standardLibraryManager.getStats().getSymbolCount());
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

        long startTime = System.currentTimeMillis();
        ParseResult parseResult = parserFacade.parseFile(file);
        ModelIndex index = new ModelIndex(libraryIndex);
        List<ReferenceSite> sites = indexFile(file, parseResult, index);
        return validateParsed(file, parseResult, index, sites, startTime);
    }

    /** Add a parsed file to an index and return the names in it that must resolve. */
    private List<ReferenceSite> indexFile(File file, ParseResult parseResult, ModelIndex index) {
        if (parseResult.getParseTree() == null) {
            return List.of();
        }
        ModelIndexBuilder.Result built =
            ModelIndexBuilder.build(parseResult.getParseTree(), file.getAbsolutePath(), true);
        index.addAll(built.getElements());
        return built.getSites();
    }

    private ValidationResult validateParsed(File file, ParseResult parseResult, ModelIndex index,
            List<ReferenceSite> sites, long startTime) {
        LOGGER.info("Validating file: {}", file.getAbsolutePath());

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

        // Add warnings for line comments (they are not persisted to the model)
        if (parseResult.hasLineComments()) {
            for (SysMLv2ParserFacade.LineComment comment : parseResult.getLineComments()) {
                warnings.add((ValidationWarning) new ValidationWarning.Builder()
                    .filePath(file.getAbsolutePath())
                    .line(comment.getLine())
                    .column(comment.getColumn())
                    .message("Single-line comment '//' is not persisted to the model. "
                        + "Use block comment /* */ or doc keyword for persistent documentation.")
                    .errorCode("LINE_COMMENT_NOT_PERSISTED")
                    .build());
            }
        }

        // Perform semantic validation if enabled and syntax is valid
        if (semanticValidationEnabled && errors.isEmpty() && parseResult.getParseTree() != null) {
            LOGGER.debug("Performing semantic validation");
            SemanticValidator semanticValidator =
                new SemanticValidator(file.getAbsolutePath(), standardLibraryManager);
            List<ValidationError> semanticErrors =
                semanticValidator.validate(parseResult.getParseTree());
            errors.addAll(semanticErrors);

            // Names that must resolve: imports, typings, definition supertypes, metadata
            if (index != null) {
                errors.addAll(ReferenceChecker.check(sites, index, file.getAbsolutePath()));
            }

            // Add semantic warnings
            for (String warning : semanticValidator.getWarnings()) {
                warnings.add((ValidationWarning) new ValidationWarning.Builder()
                    .message(warning)
                    .build());
            }

            // Add lint warnings
            warnings.addAll(semanticValidator.getLintWarnings());

            LOGGER.debug("Semantic validation found {} errors, {} warnings (including {} lint)",
                semanticErrors.size(),
                semanticValidator.getWarnings().size() + semanticValidator.getLintWarnings().size(),
                semanticValidator.getLintWarnings().size());
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

        // Add warnings for line comments (they are not persisted to the model)
        if (parseResult.hasLineComments()) {
            for (SysMLv2ParserFacade.LineComment comment : parseResult.getLineComments()) {
                warnings.add((ValidationWarning) new ValidationWarning.Builder()
                    .filePath(fileName)
                    .line(comment.getLine())
                    .column(comment.getColumn())
                    .message("Single-line comment '//' is not persisted to the model. "
                        + "Use block comment /* */ or doc keyword for persistent documentation.")
                    .errorCode("LINE_COMMENT_NOT_PERSISTED")
                    .build());
            }
        }

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

    /**
     * Validate several files together. Every file is parsed and indexed first, so a name in one file
     * resolves against the others as well as against the libraries; names are checked once all files
     * are indexed. Parse trees are not kept between the two passes.
     */
    @Override
    public List<ValidationResult> validateAll(List<File> files) throws IOException {
        LOGGER.info("Validating {} files", files.size());

        ModelIndex index = new ModelIndex(libraryIndex);
        List<ValidationResult> results = new ArrayList<>();
        Map<Integer, List<ReferenceSite>> pending = new LinkedHashMap<>();
        for (File file : files) {
            ValidationResult result;
            try {
                if (!file.exists()) {
                    throw new IOException("File not found: " + file.getAbsolutePath());
                }
                long start = System.currentTimeMillis();
                ParseResult parsed = parserFacade.parseFile(file);
                List<ReferenceSite> sites = indexFile(file, parsed, index);
                result = validateParsed(file, parsed, null, List.of(), start);
                if (semanticValidationEnabled && parsed.getSyntaxErrors().isEmpty() && !sites.isEmpty()) {
                    pending.put(results.size(), sites);
                }
            } catch (IOException e) {
                LOGGER.error("Error validating file: {}", file.getAbsolutePath(), e);
                ValidationError error = new ValidationError.Builder()
                    .filePath(file.getAbsolutePath())
                    .line(0)
                    .column(0)
                    .message("Failed to read file: " + e.getMessage())
                    .errorCode("IO_ERROR")
                    .severity(ValidationError.Severity.ERROR)
                    .build();
                result = new ValidationResult(file.getAbsolutePath(), List.of(error), List.of(), 0);
            }
            results.add(result);
        }

        for (Map.Entry<Integer, List<ReferenceSite>> entry : pending.entrySet()) {
            ValidationResult result = results.get(entry.getKey());
            List<ValidationError> unresolved =
                ReferenceChecker.check(entry.getValue(), index, result.getFilePath());
            if (!unresolved.isEmpty()) {
                List<ValidationError> errors = new ArrayList<>(result.getErrors());
                errors.addAll(unresolved);
                results.set(entry.getKey(), new ValidationResult(result.getFilePath(), errors,
                    result.getWarnings(), result.getValidationTimeMs()));
            }
        }

        LOGGER.info("Validation complete: {} files processed", results.size());
        return results;
    }

    /**
     * Syntax errors found in the configured library files. Reported on every run, including runs
     * that load those files from the cache.
     */
    public List<LibraryLoader.LibraryDiagnostic> getLibraryDiagnostics() {
        return libraryLoader.getDiagnostics();
    }

    /** The library cache used by this validator. */
    public LibraryCache getLibraryCache() {
        return libraryCache;
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
