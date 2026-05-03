package com.validator.semantic;

import com.validator.ValidationError;
import com.validator.ValidationWarning;
import com.validator.library.ImportResolver;
import com.validator.lint.LintAnalyzer;
import com.validator.lint.LintConfig;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic validator for SysML v2 models.
 * Performs validation beyond syntax checking:
 * - Import resolution against standard library
 * - Symbol table building and reference tracking
 * - Lint analysis (unused elements, naming, etc.)
 * - Type checking (planned)
 */
public class SemanticValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticValidator.class);

    private final String filePath;
    private final StandardLibraryManager standardLibraryManager;
    private final ImportResolver importResolver;
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<ValidationWarning> lintWarnings = new ArrayList<>();
    private boolean lintEnabled = true;
    private LintConfig lintConfig;

    /**
     * Constructor for semantic validator.
     *
     * @param filePath the path of the file being validated
     * @param standardLibraryManager the standard library manager for semantic resolution
     */
    public SemanticValidator(String filePath, StandardLibraryManager standardLibraryManager) {
        this.filePath = filePath;
        this.standardLibraryManager = standardLibraryManager;
        this.importResolver = new ImportResolver(standardLibraryManager);
        // Load lint config for the file's directory
        try {
            Path path = Paths.get(filePath);
            this.lintConfig = LintConfig.loadForFile(path);
        } catch (Exception e) {
            // Use default config if path conversion fails
            this.lintConfig = new LintConfig();
        }
    }

    /**
     * Enable or disable lint analysis.
     *
     * @param enabled true to enable lint analysis
     */
    public void setLintEnabled(boolean enabled) {
        this.lintEnabled = enabled;
    }

    /**
     * Set custom lint configuration.
     *
     * @param config the lint configuration to use
     */
    public void setLintConfig(LintConfig config) {
        this.lintConfig = config;
    }

    /**
     * Validate the parse tree for semantic errors.
     *
     * @param tree The ANTLR parse tree from syntax analysis
     * @return List of semantic validation errors
     */
    public List<ValidationError> validate(ParseTree tree) {
        LOGGER.debug("Starting semantic validation for: {}", filePath);

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

        // Perform lint analysis if enabled
        if (lintEnabled) {
            performLintAnalysis(tree);
        }

        return errors;
    }

    /**
     * Perform lint analysis on the parse tree.
     *
     * @param tree the parse tree to analyze
     */
    private void performLintAnalysis(ParseTree tree) {
        LOGGER.debug("Starting lint analysis for: {}", filePath);

        try {
            // Build symbol table
            SymbolTable symbolTable = SymbolTableBuilder.build(tree, filePath, standardLibraryManager);

            // Track references (second pass)
            ReferenceTracker.track(tree, symbolTable, filePath);

            // Run lint analyzer
            LintAnalyzer analyzer = new LintAnalyzer(lintConfig);
            List<ValidationWarning> lintResults = analyzer.analyze(tree, symbolTable, filePath);

            lintWarnings.addAll(lintResults);

            LOGGER.debug("Lint analysis complete: {} warnings", lintWarnings.size());
        } catch (Exception e) {
            LOGGER.warn("Lint analysis failed: {}", e.getMessage());
            // Don't fail validation if lint analysis fails
        }
    }

    /**
     * Get string warnings (legacy format).
     *
     * @return list of warning messages
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Get lint warnings as ValidationWarning objects.
     *
     * @return list of lint warnings
     */
    public List<ValidationWarning> getLintWarnings() {
        return new ArrayList<>(lintWarnings);
    }
}
