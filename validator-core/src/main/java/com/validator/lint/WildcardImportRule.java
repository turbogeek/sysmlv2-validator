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
 * Lint rule that detects wildcard imports in SysML v2 models.
 *
 * <p>Wildcard imports (import Package::*) bring all elements from a package
 * into scope. This causes several issues:
 * <ul>
 *   <li>Scope pollution - many unused symbols in scope</li>
 *   <li>Unclear dependencies - not clear which elements are actually used</li>
 *   <li>Name collision risks - imported names may conflict</li>
 *   <li>Reduced readability - harder to understand model dependencies</li>
 * </ul>
 *
 * <p>This rule produces warnings for:
 * <ul>
 *   <li>Single-level wildcards: import Package::*</li>
 *   <li>Recursive wildcards: import Package::*::* or Package::**</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>This rule respects the "imports" category in lint configuration.
 */
public class WildcardImportRule implements LintRule {

    private static final String RULE_ID = "wildcard-imports";
    private static final String CATEGORY = "imports";
    private static final String DESCRIPTION =
        "Detects wildcard imports (::*) that cause scope pollution";

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
            ErrorCodes.LINT_WILDCARD_IMPORT,
            ErrorCodes.LINT_RECURSIVE_WILDCARD
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
            if (imp.isWildcard()) {
                ValidationWarning warning = createWildcardWarning(imp, context.getFilePath());
                warnings.add(warning);
            }
        }

        return warnings;
    }

    /**
     * Creates a warning for a wildcard import.
     */
    private ValidationWarning createWildcardWarning(ImportStatement imp, String filePath) {
        boolean isRecursive = imp.isRecursiveWildcard();
        String errorCode = isRecursive
            ? ErrorCodes.LINT_RECURSIVE_WILDCARD
            : ErrorCodes.LINT_WILDCARD_IMPORT;

        String importPath = imp.getImportPath();
        int importedCount = imp.getImportedSymbols().size();

        String message;
        if (isRecursive) {
            message = String.format(
                "Recursive wildcard import '%s' imports elements from all nested packages",
                importPath);
        } else {
            if (importedCount > 0) {
                message = String.format(
                    "Wildcard import '%s' imports %d elements into scope",
                    importPath, importedCount);
            } else {
                message = String.format(
                    "Wildcard import '%s' may import many elements into scope",
                    importPath);
            }
        }

        ValidationWarning.Builder builder = new ValidationWarning.Builder();
        builder.errorCode(errorCode);
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

        // Add suggestion
        String packageName = imp.getPackageName();
        String suggestion = String.format(
            "Replace with specific imports: import %s::ElementName; (for each element used)",
            packageName);
        builder.addSuggestion(suggestion);

        if (isRecursive) {
            builder.addSuggestion(
                "Recursive wildcards import from nested packages too - consider restructuring imports");
        }

        return builder.build();
    }
}
