package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.semantic.ImportStatement;
import com.validator.semantic.Scope;
import com.validator.semantic.SymbolTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lint rule that detects public imports in SysML v2 models.
 *
 * <p>Public imports (public import Package::Element) re-export the imported
 * elements to any package that imports the current package. This creates
 * transitive dependencies that can cause several issues:
 * <ul>
 *   <li>Scope leakage - imported elements become visible outside this package</li>
 *   <li>Transitive dependencies - importers get unexpected symbols</li>
 *   <li>Harder to refactor - changes to imports affect other packages</li>
 *   <li>Unclear API surface - not clear what a package actually exports</li>
 * </ul>
 *
 * <p>This rule produces warnings for:
 * <ul>
 *   <li>Public wildcard imports: public import Package::* (ERROR severity)</li>
 *   <li>Public specific imports: public import Package::Element (INFO severity)</li>
 * </ul>
 *
 * <p>Note: Public specific imports may be intentional (facade pattern) so they
 * are reported at INFO level by default.
 *
 * <h2>Configuration</h2>
 * <p>This rule respects the "imports" category in lint configuration.
 */
public class PublicImportRule implements LintRule {

    private static final String RULE_ID = "public-imports";
    private static final String CATEGORY = "imports";
    private static final String DESCRIPTION =
        "Detects public imports that re-export elements to importers";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String[] getErrorCodes() {
        return new String[] {
            ErrorCodes.LINT_PUBLIC_IMPORT
        };
    }

    @Override
    public List<ValidationWarning> analyze(LintContext context) {
        List<ValidationWarning> warnings = new ArrayList<>();
        SymbolTable symbolTable = context.getSymbolTable();

        if (symbolTable == null) {
            return warnings;
        }

        // Get all imports from the global scope and all child scopes
        Scope globalScope = symbolTable.getGlobalScope();
        List<ImportStatement> allImports = globalScope.getAllImports();

        for (ImportStatement imp : allImports) {
            if (imp.isPublic()) {
                ValidationWarning warning = createPublicImportWarning(imp, context.getFilePath());
                warnings.add(warning);
            }
        }

        return warnings;
    }

    /**
     * Creates a warning for a public import.
     */
    private ValidationWarning createPublicImportWarning(ImportStatement imp, String filePath) {
        String importPath = imp.getImportPath();
        boolean isWildcard = imp.isWildcard();

        String message;
        if (isWildcard) {
            message = String.format(
                "Public wildcard import 'public import %s' re-exports all elements to importers",
                importPath);
        } else {
            message = String.format(
                "Public import 'public import %s' re-exports element to importers",
                importPath);
        }

        ValidationWarning.Builder builder = new ValidationWarning.Builder();
        builder.errorCode(ErrorCodes.LINT_PUBLIC_IMPORT);
        builder.message(message);

        // Use import's location if available
        Location location = imp.getLocation();
        if (location != null) {
            builder.filePath(location.getFileName());
            builder.line(location.getLine());
            builder.column(location.getColumn());
        } else if (filePath != null) {
            builder.filePath(filePath);
        }

        // Add suggestions
        if (isWildcard) {
            builder.addSuggestion(
                "Use private import (remove 'public' keyword) to avoid scope leakage");
            builder.addSuggestion(
                "If intentional, consider using specific public imports for only the elements to re-export");
        } else {
            builder.addSuggestion(
                "Use private import (remove 'public' keyword) unless re-exporting is intentional");
            builder.addSuggestion(
                "Public imports are appropriate for facade patterns where you want to re-export elements");
        }

        return builder.build();
    }
}
