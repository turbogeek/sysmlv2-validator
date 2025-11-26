package com.validator.semantic;

import com.validator.ValidationError;
import com.validator.library.ImportResolver;
import com.validator.library.LibraryIndex;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic validator for SysML v2 models.
 * Performs validation beyond syntax checking:
 * - Import resolution against standard library
 * - Name resolution (planned)
 * - Type checking (planned)
 */
public class SemanticValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticValidator.class);

    private final String filePath;
    private final LibraryIndex libraryIndex;
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public SemanticValidator(String filePath, LibraryIndex libraryIndex) {
        this.filePath = filePath;
        this.libraryIndex = libraryIndex;
    }

    /**
     * Validate the parse tree for semantic errors.
     *
     * @param tree The ANTLR parse tree from syntax analysis
     * @return List of semantic validation errors
     */
    public List<ValidationError> validate(ParseTree tree) {
        LOGGER.debug("Starting semantic validation for: {}", filePath);

        // Create import resolver
        ImportResolver importResolver = new ImportResolver(libraryIndex);

        // Create listener for semantic validation
        SemanticValidationListener listener =
            new SemanticValidationListener(filePath, importResolver, errors, warnings);

        // Walk the parse tree
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        // Collect errors from import resolver
        errors.addAll(importResolver.getErrors());
        warnings.addAll(importResolver.getWarnings());

        LOGGER.debug("Semantic validation complete: {} errors, {} warnings",
            errors.size(), warnings.size());

        return errors;
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
}
