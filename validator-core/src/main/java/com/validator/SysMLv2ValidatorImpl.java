package com.validator;

import com.validator.kpar.KparArchive;
import com.validator.library.LibraryConfig;
import com.validator.library.LibraryLoader;
import com.validator.library.LibraryCache;
import com.validator.library.LibraryCacheData;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import com.validator.semantic.SemanticValidator;
import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.index.InheritanceChecker;
import com.validator.semantic.index.NameCollisionChecker;
import com.validator.semantic.index.ModelIndex;
import com.validator.semantic.index.ModelIndexBuilder;
import com.validator.semantic.index.ReferenceChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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

        if (SysMLv2ParserFacade.isKparFile(file)) {
            return validateArchive(file);
        }

        if (!SysMLv2ParserFacade.isSysMLFile(file) && !SysMLv2ParserFacade.isKerMLFile(file)) {
            LOGGER.warn("File does not have .sysml or .kerml extension: {}", file.getName());
        }

        long startTime = System.currentTimeMillis();
        ParseResult parseResult = parserFacade.parseFile(file);
        ModelIndex index = new ModelIndex(libraryIndex);
        ModelIndexBuilder.Result built = indexFile(file.getAbsolutePath(), parseResult, index);
        return validateParsed(file.getAbsolutePath(), parseResult, index, built, startTime);
    }

    /**
     * Validate the members of a KPAR together, as {@link #validateAll(List)} validates several files, into one
     * result for the archive. An invalid archive gives one {@link ErrorCodes#IO_INVALID_KPAR} error.
     */
    private ValidationResult validateArchive(File file) throws IOException {
        Batch batch = new Batch();
        Supplier<ValidationResult> result;
        try {
            result = batch.addArchive(file);
        } catch (KparArchive.InvalidKparException e) {
            return invalidKpar(file, e);
        }
        batch.check();
        return result.get();
    }

    /**
     * Add a parsed file to an index and return its elements and the names in it that must resolve
     * (null if the file has no parse tree).
     *
     * @param path the file's path, or {@code <archive>!<member>} for a member of a KPAR
     */
    private ModelIndexBuilder.Result indexFile(String path, ParseResult parseResult, ModelIndex index) {
        if (parseResult.getParseTree() == null) {
            return null;
        }
        ModelIndexBuilder.Result built = ModelIndexBuilder.build(parseResult.getParseTree(), path, true);
        index.addAll(built.getElements());
        return built;
    }

    private ValidationResult validateParsed(String path, ParseResult parseResult, ModelIndex index,
            ModelIndexBuilder.Result built, long startTime) {
        LOGGER.info("Validating file: {}", path);

        // Convert syntax errors to validation errors using streams (with suggestions)
        List<ValidationError> errors = new ArrayList<>(parseResult.getSyntaxErrors().stream()
            .map(syntaxError -> {
                ValidationError.Builder builder = new ValidationError.Builder()
                    .filePath(path)
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
                    .filePath(path)
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
                new SemanticValidator(path, standardLibraryManager);
            List<ValidationError> semanticErrors =
                semanticValidator.validate(parseResult.getParseTree());
            errors.addAll(semanticErrors);

            // Names that must resolve: imports, typings, definition supertypes, metadata;
            // multiple inheritance that leaves conflicting features; names that make a new element where an
            // existing one was likely meant
            if (index != null && built != null) {
                errors.addAll(ReferenceChecker.check(built.getSites(), index, path));
                InheritanceChecker inheritance = new InheritanceChecker(index);
                warnings.addAll(inheritance.check(built.getElements(), path));
                NameCollisionChecker collisions = new NameCollisionChecker(index, inheritance);
                addSplit(collisions.check(built.getElements(), path), errors, warnings);
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
            path,
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
     *
     * <p>A KPAR ({@code .kpar}) gives one result, for the archive. Its members are validated together with
     * each other and with the other files, and their diagnostics carry the file path
     * {@code <archive>!<member>}. An invalid archive gives one {@link ErrorCodes#IO_INVALID_KPAR} error.</p>
     */
    @Override
    public List<ValidationResult> validateAll(List<File> files) throws IOException {
        LOGGER.info("Validating {} files", files.size());

        Batch batch = new Batch();
        List<Supplier<ValidationResult>> pending = new ArrayList<>();
        for (File file : files) {
            Supplier<ValidationResult> result;
            try {
                if (!file.exists()) {
                    throw new IOException("File not found: " + file.getAbsolutePath());
                }
                result = SysMLv2ParserFacade.isKparFile(file) ? batch.addArchive(file) : batch.addFile(file);
            } catch (KparArchive.InvalidKparException e) {
                ValidationResult invalid = invalidKpar(file, e);
                result = () -> invalid;
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
                ValidationResult failed = new ValidationResult(file.getAbsolutePath(), List.of(error), List.of(), 0);
                result = () -> failed;
            }
            pending.add(result);
        }
        batch.check();

        List<ValidationResult> results = new ArrayList<>();
        for (Supplier<ValidationResult> result : pending) {
            results.add(result.get());
        }
        LOGGER.info("Validation complete: {} files processed", results.size());
        return results;
    }

    /**
     * Files and KPAR members validated together. Each is parsed, indexed and validated as it is added;
     * {@link #check()} then checks the names in each against the index of all of them, which completes
     * their results.
     */
    private final class Batch {
        private final ModelIndex index = new ModelIndex(libraryIndex);
        /** One result per file or member, in the order added. */
        private final List<ValidationResult> units = new ArrayList<>();
        /** The units whose names are checked, by their position in {@link #units}. */
        private final Map<Integer, ModelIndexBuilder.Result> checked = new LinkedHashMap<>();

        /** Add a file; the result it supplies is complete once the batch is checked. */
        Supplier<ValidationResult> addFile(File file) throws IOException {
            long start = System.currentTimeMillis();
            int unit = add(file.getAbsolutePath(), parserFacade.parseFile(file), start);
            return () -> units.get(unit);
        }

        /** Add the members of a KPAR; the one result it supplies is complete once the batch is checked. */
        Supplier<ValidationResult> addArchive(File file) throws IOException {
            long start = System.currentTimeMillis();
            KparArchive archive = KparArchive.read(file.toPath());
            String archivePath = file.getAbsolutePath();
            LOGGER.info("Validating KPAR {} ({} model files)", archivePath, archive.getMembers().size());
            int first = units.size();
            for (KparArchive.Member member : archive.getMembers()) {
                long memberStart = System.currentTimeMillis();
                ParseResult parsed = parserFacade.parseString(member.text(), member.fileName());
                add(KparArchive.memberPath(archivePath, member.path()), parsed, memberStart);
            }
            int end = units.size();
            long validationTime = System.currentTimeMillis() - start;
            return () -> archiveResult(archivePath, units.subList(first, end), validationTime);
        }

        private int add(String path, ParseResult parsed, long start) {
            ModelIndexBuilder.Result built = indexFile(path, parsed, index);
            units.add(validateParsed(path, parsed, null, null, start));
            if (semanticValidationEnabled && parsed.getSyntaxErrors().isEmpty() && built != null) {
                checked.put(units.size() - 1, built);
            }
            return units.size() - 1;
        }

        void check() {
            InheritanceChecker inheritance = new InheritanceChecker(index);
            NameCollisionChecker collisions = new NameCollisionChecker(index, inheritance);
            for (Map.Entry<Integer, ModelIndexBuilder.Result> entry : checked.entrySet()) {
                ValidationResult result = units.get(entry.getKey());
                List<ValidationError> unresolved =
                    ReferenceChecker.check(entry.getValue().getSites(), index, result.getFilePath());
                List<ValidationWarning> conflicts =
                    inheritance.check(entry.getValue().getElements(), result.getFilePath());
                List<ValidationError> names = collisions.check(entry.getValue().getElements(), result.getFilePath());
                if (!unresolved.isEmpty() || !conflicts.isEmpty() || !names.isEmpty()) {
                    List<ValidationError> errors = new ArrayList<>(result.getErrors());
                    errors.addAll(unresolved);
                    List<ValidationWarning> warnings = new ArrayList<>(result.getWarnings());
                    warnings.addAll(conflicts);
                    addSplit(names, errors, warnings);
                    units.set(entry.getKey(), new ValidationResult(result.getFilePath(), errors,
                        warnings, result.getValidationTimeMs()));
                }
            }
        }
    }

    /**
     * The result of a KPAR: the diagnostics of its members in archive order. A diagnostic without a file path
     * gets its member's, so that every diagnostic names the member it is in.
     */
    private static ValidationResult archiveResult(String archivePath, List<ValidationResult> members,
            long validationTime) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        for (ValidationResult member : members) {
            for (ValidationError error : member.getErrors()) {
                errors.add(locate(error, member.getFilePath()));
            }
            for (ValidationWarning warning : member.getWarnings()) {
                warnings.add((ValidationWarning) locate(warning, member.getFilePath()));
            }
        }
        LOGGER.info("KPAR validation completed: {} errors, {} warnings in {}ms",
            errors.size(), warnings.size(), validationTime);
        return new ValidationResult(archivePath, errors, warnings, validationTime);
    }

    /** The diagnostic itself if it has a file path, else a copy of it with the given one. */
    static ValidationError locate(ValidationError diagnostic, String path) {
        if (diagnostic.getFilePath() != null) {
            return diagnostic;
        }
        ValidationError.Builder builder = diagnostic instanceof ValidationWarning
            ? new ValidationWarning.Builder() : new ValidationError.Builder();
        return builder.filePath(path)
            .line(diagnostic.getLine())
            .column(diagnostic.getColumn())
            .message(diagnostic.getMessage())
            .errorCode(diagnostic.getErrorCode())
            .severity(diagnostic.getSeverity())
            .suggestions(diagnostic.getSuggestions())
            .context(diagnostic.getContext())
            .specReference(diagnostic.getSpecReference())
            .build();
    }

    private static ValidationResult invalidKpar(File file, KparArchive.InvalidKparException e) {
        LOGGER.error("Invalid KPAR {}: {}", file.getAbsolutePath(), e.getMessage());
        ValidationError error = new ValidationError.Builder()
            .filePath(file.getAbsolutePath())
            .line(0)
            .column(0)
            .message("Invalid KPAR (KerML Project Archive): " + e.getMessage())
            .errorCode(ErrorCodes.IO_INVALID_KPAR)
            .severity(ValidationError.Severity.ERROR)
            .build();
        return new ValidationResult(file.getAbsolutePath(), List.of(error), List.of(), 0);
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

    /**
     * Plan the replacement of private wildcard imports by element imports of the names used
     * (see {@link com.validator.semantic.index.ImportMinimizer}). Nothing is written.
     *
     * @param files the model files, resolved together
     * @return the rewritten texts, the changes and any resolution mismatch
     * @throws IOException if a file cannot be read
     */
    public com.validator.semantic.index.ImportMinimizer.Result minimizeImports(List<File> files) throws IOException {
        return minimizeImports(files, false);
    }

    /**
     * As {@link #minimizeImports(List)}; with {@code owningNamespace}, each element is imported from the
     * namespace that owns it ({@code ISQBase::MassValue} rather than {@code ISQ::MassValue}).
     *
     * @param files the model files, resolved together
     * @param owningNamespace import from the owning namespace instead of through re-exports
     * @return the rewritten texts, the changes and any resolution mismatch
     * @throws IOException if a file cannot be read
     */
    public com.validator.semantic.index.ImportMinimizer.Result minimizeImports(List<File> files, boolean owningNamespace)
            throws IOException {
        return com.validator.semantic.index.ImportMinimizer.minimize(files, libraryIndex, parserFacade, owningNamespace);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /** Add checker results to the errors or the warnings by their severity. */
    private static void addSplit(List<ValidationError> found, List<ValidationError> errors,
            List<ValidationWarning> warnings) {
        for (ValidationError e : found) {
            if (e instanceof ValidationWarning w) {
                warnings.add(w);
            } else {
                errors.add(e);
            }
        }
    }
}
