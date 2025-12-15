package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.semantic.ElementType;
import com.validator.semantic.ImportStatement;
import com.validator.semantic.ImportType;
import com.validator.semantic.Scope;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lint rule that detects unused elements in SysML v2 models.
 *
 * <p>Detects the following unused elements:
 * <ul>
 *   <li>Unused definitions (part def, action def, etc.) - never used as a type</li>
 *   <li>Unused imports - imported elements never referenced</li>
 *   <li>Unused attributes - attributes never referenced in constraints or expressions</li>
 *   <li>Unused parts - part usages never referenced</li>
 *   <li>Unused packages - packages with no external references</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>This rule respects the "unused" category in lint configuration.
 */
public class UnusedElementRule implements LintRule {

    private static final String RULE_ID = "unused-elements";
    private static final String CATEGORY = "unused";
    private static final String DESCRIPTION = "Detects unused definitions, imports, attributes, and other elements";

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
            ErrorCodes.LINT_UNUSED_DEFINITION,
            ErrorCodes.LINT_UNUSED_IMPORT,
            ErrorCodes.LINT_UNUSED_ATTRIBUTE,
            ErrorCodes.LINT_UNUSED_PART
        };
    }

    @Override
    public List<ValidationWarning> analyze(LintContext context) {
        List<ValidationWarning> warnings = new ArrayList<>();
        SymbolTable symbolTable = context.getSymbolTable();

        if (symbolTable == null) {
            return warnings;
        }

        // Check for unused definitions
        warnings.addAll(checkUnusedDefinitions(symbolTable));

        // Check for unused imports
        warnings.addAll(checkUnusedImports(symbolTable, context));

        // Check for unused attributes
        warnings.addAll(checkUnusedAttributes(symbolTable));

        // Check for unused parts
        warnings.addAll(checkUnusedParts(symbolTable));

        return warnings;
    }

    /**
     * Check for definitions that are never used as types.
     */
    private List<ValidationWarning> checkUnusedDefinitions(SymbolTable symbolTable) {
        List<ValidationWarning> warnings = new ArrayList<>();

        for (Symbol symbol : symbolTable.getAllSymbols()) {
            // Only check definitions
            if (!symbol.isDefinition()) {
                continue;
            }

            // Skip packages - they have different usage patterns
            if (symbol.getType() == ElementType.PACKAGE) {
                continue;
            }

            // Skip standard library elements (usually no location)
            if (symbol.getLocation() == null) {
                continue;
            }

            // Check if the definition is used
            if (symbol.isUnused()) {
                String elementKind = getReadableElementKind(symbol.getType());
                warnings.add(createWarning(
                    ErrorCodes.LINT_UNUSED_DEFINITION,
                    String.format("Unused %s '%s' is never referenced",
                        elementKind, symbol.getName()),
                    symbol.getLocation(),
                    String.format("Consider removing the unused %s or add a usage of type '%s'",
                        elementKind, symbol.getName())
                ));
            }
        }

        return warnings;
    }

    /**
     * Check for imports that are never used.
     */
    private List<ValidationWarning> checkUnusedImports(SymbolTable symbolTable, LintContext context) {
        List<ValidationWarning> warnings = new ArrayList<>();

        // Get all imports from global scope
        Scope globalScope = symbolTable.getGlobalScope();
        Collection<ImportStatement> imports = globalScope.getImports();

        // Track which imports are actually used
        Set<String> usedImportPaths = new HashSet<>();

        // Check each symbol to see what imports they might use
        for (Symbol symbol : symbolTable.getAllSymbols()) {
            if (symbol.isUsedAsType() || symbol.isUsedInExpression() || symbol.isUsedInImport()) {
                // This symbol was used - mark its import as used
                String qualifiedName = symbol.getQualifiedName();
                for (ImportStatement imp : imports) {
                    String importPath = imp.getImportPath();
                    if (imp.getImportType() == ImportType.WILDCARD) {
                        // Wildcard: check if symbol comes from that package
                        String packagePath = importPath.replace("::*", "").replace("*", "");
                        if (qualifiedName.startsWith(packagePath + "::")) {
                            usedImportPaths.add(importPath);
                        }
                    } else {
                        // Specific: check exact match
                        if (qualifiedName.equals(importPath)) {
                            usedImportPaths.add(importPath);
                        }
                    }
                }
            }
        }

        // Report unused imports
        // Note: We cannot easily get import locations from ImportStatement
        // This would require modifying ImportStatement to track location
        // For now, report as file-level warning
        for (ImportStatement imp : imports) {
            if (!usedImportPaths.contains(imp.getImportPath())) {
                // Skip standard library imports - they're often intentional
                String path = imp.getImportPath();
                if (isStandardLibraryImport(path)) {
                    continue;
                }

                warnings.add(createWarning(
                    ErrorCodes.LINT_UNUSED_IMPORT,
                    String.format("Unused import '%s'", imp.getImportPath()),
                    null, // No location available for imports currently
                    "Remove the unused import or use the imported element"
                ));
            }
        }

        return warnings;
    }

    /**
     * Check for attributes that are never referenced in constraints or expressions.
     */
    private List<ValidationWarning> checkUnusedAttributes(SymbolTable symbolTable) {
        List<ValidationWarning> warnings = new ArrayList<>();

        for (Symbol symbol : symbolTable.getAllSymbols()) {
            // Only check attribute usages
            if (symbol.getType() != ElementType.ATTRIBUTE_USAGE) {
                continue;
            }

            // Skip if no location (generated symbol)
            if (symbol.getLocation() == null) {
                continue;
            }

            // Check if attribute is used in expressions or as type
            if (symbol.isUnused() && !symbol.isUsedInExpression()) {
                warnings.add(createWarning(
                    ErrorCodes.LINT_UNUSED_ATTRIBUTE,
                    String.format("Unused attribute '%s' is never referenced in constraints or expressions",
                        symbol.getName()),
                    symbol.getLocation(),
                    "Consider removing the unused attribute or reference it in a constraint"
                ));
            }
        }

        return warnings;
    }

    /**
     * Check for part usages that are never referenced.
     */
    private List<ValidationWarning> checkUnusedParts(SymbolTable symbolTable) {
        List<ValidationWarning> warnings = new ArrayList<>();

        for (Symbol symbol : symbolTable.getAllSymbols()) {
            // Only check part usages
            if (symbol.getType() != ElementType.PART_USAGE) {
                continue;
            }

            // Skip if no location (generated symbol)
            if (symbol.getLocation() == null) {
                continue;
            }

            // Check if part is used
            if (symbol.isUnused()) {
                warnings.add(createWarning(
                    ErrorCodes.LINT_UNUSED_PART,
                    String.format("Unused part '%s' is never referenced",
                        symbol.getName()),
                    symbol.getLocation(),
                    "Consider removing the unused part or reference it in connections/constraints"
                ));
            }
        }

        return warnings;
    }

    /**
     * Check if an import path refers to standard library.
     */
    private boolean isStandardLibraryImport(String path) {
        return path != null && (
            path.startsWith("ISQ::") ||
            path.startsWith("SI::") ||
            path.startsWith("USCustomary::") ||
            path.startsWith("Base::") ||
            path.startsWith("Quantities::") ||
            path.startsWith("ScalarValues::") ||
            path.startsWith("Collections::") ||
            path.startsWith("ControlFunctions::") ||
            path.startsWith("DataFunctions::") ||
            path.startsWith("BaseFunctions::") ||
            path.startsWith("Performances::") ||
            path.startsWith("Analysis::") ||
            path.startsWith("Connections::") ||
            path.startsWith("Interfaces::") ||
            path.startsWith("Items::") ||
            path.startsWith("Parts::") ||
            path.startsWith("Ports::") ||
            path.startsWith("Actions::") ||
            path.startsWith("States::") ||
            path.startsWith("Requirements::") ||
            path.startsWith("Constraints::") ||
            path.startsWith("Calculations::") ||
            path.startsWith("Cases::") ||
            path.startsWith("Views::") ||
            path.startsWith("KerML::")
        );
    }

    /**
     * Get a human-readable element kind name.
     */
    private String getReadableElementKind(ElementType type) {
        if (type == null) {
            return "element";
        }
        String name = type.name().toLowerCase().replace("_", " ");
        // Remove "definition" suffix for cleaner messages
        return name.replace(" definition", "");
    }

    private ValidationWarning createWarning(String code, String message, Location location, String suggestion) {
        ValidationWarning.Builder builder = new ValidationWarning.Builder();
        builder.errorCode(code);
        builder.message(message);

        if (location != null) {
            builder.filePath(location.getFileName());
            builder.line(location.getLine());
            builder.column(location.getColumn());
        }

        if (suggestion != null) {
            builder.addSuggestion(suggestion);
        }

        return builder.build();
    }
}
