package com.validator.semantic;

import com.validator.suggestions.SpellingSuggestionEngine;
import com.validator.suggestions.StandardLibraryProvider;
import com.validator.suggestions.UserModelProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves import statements to actual symbols.
 * Handles wildcard imports, specific imports, filtered imports, and aliased imports.
 * Provides spelling suggestions when imports cannot be resolved.
 */
public class ImportResolver {
    private final SymbolTable symbolTable;
    private final StandardLibraryManager standardLibrary;
    private final SpellingSuggestionEngine suggestionEngine;
    private final List<ImportError> errors;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "StandardLibraryManager is intentionally shared")
    public ImportResolver(SymbolTable symbolTable, StandardLibraryManager standardLibrary) {
        this.symbolTable = Objects.requireNonNull(symbolTable, "Symbol table cannot be null");
        this.standardLibrary = standardLibrary; // Can be null if no standard library
        this.errors = new ArrayList<>();

        // Initialize suggestion engine with all available sources
        List<com.validator.suggestions.SuggestionProvider> providers = new ArrayList<>();
        if (standardLibrary != null) {
            providers.add(new StandardLibraryProvider(standardLibrary));
        }
        providers.add(new UserModelProvider(symbolTable));
        this.suggestionEngine = new SpellingSuggestionEngine(providers, 5);
    }

    /**
     * Gets errors as simple strings (for backwards compatibility).
     *
     * @return list of error messages
     */
    public List<String> getErrors() {
        return errors.stream()
            .map(ImportError::getMessage)
            .toList();
    }

    /**
     * Gets structured import errors with suggestions.
     *
     * @return list of import errors with suggestions
     */
    public List<ImportError> getStructuredErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Resolve an import statement and populate it with imported symbols.
     *
     * @param importStmt The import statement to resolve
     * @param currentScope The scope where the import appears
     */
    public void resolveImport(ImportStatement importStmt, Scope currentScope) {
        String importPath = importStmt.getImportPath();

        switch (importStmt.getImportType()) {
            case WILDCARD:
                resolveWildcardImport(importStmt, importPath, currentScope);
                break;
            case SPECIFIC:
                resolveSpecificImport(importStmt, importPath, currentScope);
                break;
            case FILTERED:
                resolveFilteredImport(importStmt, importPath, currentScope);
                break;
            case ALIAS:
                resolveAliasedImport(importStmt, importPath, currentScope);
                break;
            default:
                errors.add(new ImportError(
                    String.format("Unknown import type: %s", importStmt.getImportType()),
                    importPath,
                    List.of()
                ));
                break;
        }
    }

    /**
     * Resolve wildcard import: import Package::.
     */
    private void resolveWildcardImport(ImportStatement importStmt, String importPath, Scope currentScope) {
        // Remove "::*" suffix
        String packagePath = importPath.replaceAll("::?\\*$", "");

        // Try to find the package/scope
        Scope targetScope = findScope(packagePath);

        if (targetScope != null) {
            // Import all public symbols from the target scope using streams
            targetScope.getSymbols().values().stream()
                .filter(symbol -> symbol.getVisibility() == Visibility.PUBLIC
                    || symbol.getVisibility() == Visibility.PACKAGE)
                .forEach(importStmt::addImportedSymbol);
        } else {
            // Try standard library
            if (standardLibrary != null) {
                List<Symbol> stdLibSymbols = standardLibrary.getSymbolsInPackage(packagePath);
                if (!stdLibSymbols.isEmpty()) {
                    stdLibSymbols.forEach(importStmt::addImportedSymbol);
                    return;
                }
            }

            // Extract the last part of the package path for suggestions
            String lastPart = extractLastPart(packagePath);
            List<String> suggestions = suggestionEngine.suggestForError(lastPart);
            errors.add(new ImportError(
                String.format("Cannot resolve wildcard import '%s': package not found", packagePath),
                packagePath,
                suggestions
            ));
        }
    }

    /**
     * Resolve specific import: import Package::Element.
     */
    private void resolveSpecificImport(ImportStatement importStmt, String importPath, Scope currentScope) {
        // Try to resolve the qualified name
        Symbol symbol = symbolTable.resolveQualified(importPath);

        if (symbol != null) {
            // Check visibility
            if (isAccessible(symbol, currentScope)) {
                importStmt.addImportedSymbol(symbol);
            } else {
                errors.add(new ImportError(
                    String.format("Cannot import '%s': element is not accessible (visibility: %s)",
                        importPath, symbol.getVisibility()),
                    importPath,
                    List.of("Consider changing visibility to 'public' or importing from a different package")
                ));
            }
        } else {
            // Try standard library
            if (standardLibrary != null) {
                symbol = standardLibrary.resolveSymbol(importPath);
                if (symbol != null) {
                    importStmt.addImportedSymbol(symbol);
                    return;
                }
            }

            String lastPart = extractLastPart(importPath);
            List<String> suggestions = suggestionEngine.suggestForError(lastPart);
            errors.add(new ImportError(
                String.format("Cannot resolve import '%s': element not found", importPath),
                importPath,
                suggestions
            ));
        }
    }

    /**
     * Resolve filtered import: import Package::{Element1, Element2}.
     */
    private void resolveFilteredImport(ImportStatement importStmt, String importPath, Scope currentScope) {
        // Parse: "Package::{Element1, Element2}"
        int braceStart = importPath.indexOf('{');
        int braceEnd = importPath.indexOf('}');

        if (braceStart == -1 || braceEnd == -1) {
            errors.add(new ImportError(
                String.format("Invalid filtered import syntax: '%s'", importPath),
                importPath,
                List.of()
            ));
            return;
        }

        String packagePath = importPath.substring(0, braceStart).replaceAll("::$", "");
        String elementList = importPath.substring(braceStart + 1, braceEnd);

        // Use streams to process filtered elements
        java.util.Arrays.stream(elementList.split(","))
            .map(String::trim)
            .forEach(element -> {
                String qualifiedName = packagePath.isEmpty() ? element : packagePath + "::" + element;

                Symbol symbol = symbolTable.resolveQualified(qualifiedName);
                if (symbol != null && isAccessible(symbol, currentScope)) {
                    importStmt.addImportedSymbol(symbol);
                } else if (standardLibrary != null) {
                    Symbol stdSymbol = standardLibrary.resolveSymbol(qualifiedName);
                    if (stdSymbol != null) {
                        importStmt.addImportedSymbol(stdSymbol);
                    } else {
                        String elementName = extractLastPart(qualifiedName);
                        List<String> suggestions = suggestionEngine.suggestForError(elementName);
                        errors.add(new ImportError(
                            String.format("Cannot resolve filtered import element '%s'", qualifiedName),
                            qualifiedName,
                            suggestions
                        ));
                    }
                } else {
                    String elementName = extractLastPart(qualifiedName);
                    List<String> suggestions = suggestionEngine.suggestForError(elementName);
                    errors.add(new ImportError(
                        String.format("Cannot resolve filtered import element '%s'", qualifiedName),
                        qualifiedName,
                        suggestions
                    ));
                }
            });
    }

    /**
     * Resolve aliased import: import Package::Element as Alias.
     */
    private void resolveAliasedImport(ImportStatement importStmt, String importPath, Scope currentScope) {
        // Parse: "Package::Element as Alias"
        String[] parts = importPath.split("\\s+as\\s+");
        if (parts.length != 2) {
            errors.add(new ImportError(
                String.format("Invalid aliased import syntax: '%s'", importPath),
                importPath,
                List.of()
            ));
            return;
        }

        String elementPath = parts[0].trim();
        // Alias is already stored in ImportStatement

        Symbol symbol = symbolTable.resolveQualified(elementPath);
        if (symbol != null && isAccessible(symbol, currentScope)) {
            importStmt.addImportedSymbol(symbol);
        } else if (standardLibrary != null) {
            symbol = standardLibrary.resolveSymbol(elementPath);
            if (symbol != null) {
                importStmt.addImportedSymbol(symbol);
            } else {
                String lastPart = extractLastPart(elementPath);
                List<String> suggestions = suggestionEngine.suggestForError(lastPart);
                errors.add(new ImportError(
                    String.format("Cannot resolve aliased import '%s'", elementPath),
                    elementPath,
                    suggestions
                ));
            }
        } else {
            String lastPart = extractLastPart(elementPath);
            List<String> suggestions = suggestionEngine.suggestForError(lastPart);
            errors.add(new ImportError(
                String.format("Cannot resolve aliased import '%s'", elementPath),
                elementPath,
                suggestions
            ));
        }
    }

    /**
     * Find a scope by qualified name.
     */
    private Scope findScope(String qualifiedName) {
        return symbolTable.getScopeByQualifiedName(qualifiedName);
    }

    /**
     * Check if a symbol is accessible from the current scope.
     * Handles public, private, protected, and package visibility.
     */
    private boolean isAccessible(Symbol symbol, Scope currentScope) {
        Visibility visibility = symbol.getVisibility();

        switch (visibility) {
            case PUBLIC:
                return true;

            case PRIVATE:
                // Private symbols only accessible within their defining scope
                String symbolPackage = getPackage(symbol.getQualifiedName());
                String currentPackage = currentScope.getQualifiedName();
                return symbolPackage.equals(currentPackage);

            case PROTECTED:
                // Protected symbols accessible within package and subpackages
                String symPkg = getPackage(symbol.getQualifiedName());
                String curPkg = currentScope.getQualifiedName();
                return curPkg.startsWith(symPkg);

            case PACKAGE:
                // Package-level visibility: accessible within same package
                String symPackage = getPackage(symbol.getQualifiedName());
                String curPackage = getPackage(currentScope.getQualifiedName());
                return symPackage.equals(curPackage);

            default:
                return false;
        }
    }

    /**
     * Extract package name from qualified name.
     * Example: "Package::SubPackage::Element" -> "Package::SubPackage"
     */
    private String getPackage(String qualifiedName) {
        int lastSeparator = qualifiedName.lastIndexOf("::");
        if (lastSeparator == -1) {
            return "";
        }
        return qualifiedName.substring(0, lastSeparator);
    }

    /**
     * Extract the last part of a qualified name for suggestion matching.
     * Example: "Package::SubPackage::Element" -> "Element"
     */
    private String extractLastPart(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return "";
        }
        int lastSeparator = qualifiedName.lastIndexOf("::");
        if (lastSeparator == -1) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastSeparator + 2);
    }

    /**
     * Resolve all imports in the symbol table.
     */
    public void resolveAllImports() {
        for (Scope scope : symbolTable.getAllScopes()) {
            for (ImportStatement importStmt : scope.getImports()) {
                resolveImport(importStmt, scope);
            }
        }
    }

    /**
     * Get import resolution statistics.
     */
    public ImportResolutionStats getStats() {
        List<ImportStatement> allImports = symbolTable.getAllScopes().stream()
            .flatMap(scope -> scope.getImports().stream())
            .toList();

        int totalImports = allImports.size();
        long unresolvedImports = allImports.stream()
            .filter(importStmt -> importStmt.getImportedSymbols().isEmpty())
            .count();
        int resolvedImports = totalImports - (int) unresolvedImports;

        return new ImportResolutionStats(totalImports, resolvedImports, (int) unresolvedImports, errors.size());
    }

    /**
     * Import resolution statistics.
     */
    public static class ImportResolutionStats {
        private final int totalImports;
        private final int resolvedImports;
        private final int unresolvedImports;
        private final int errorCount;

        public ImportResolutionStats(int totalImports, int resolvedImports, int unresolvedImports, int errorCount) {
            this.totalImports = totalImports;
            this.resolvedImports = resolvedImports;
            this.unresolvedImports = unresolvedImports;
            this.errorCount = errorCount;
        }

        public int getTotalImports() {
            return totalImports;
        }
        public int getResolvedImports() {
            return resolvedImports;
        }
        public int getUnresolvedImports() {
            return unresolvedImports;
        }
        public int getErrorCount() {
            return errorCount;
        }

        @Override
        public String toString() {
            return String.format("Import Resolution: %d total, %d resolved, %d unresolved, %d errors",
                totalImports, resolvedImports, unresolvedImports, errorCount);
        }
    }

    /**
     * Represents an import resolution error with suggestions.
     */
    public static class ImportError {
        private final String message;
        private final String importPath;
        private final List<String> suggestions;

        public ImportError(String message, String importPath, List<String> suggestions) {
            this.message = message;
            this.importPath = importPath;
            this.suggestions = new ArrayList<>(suggestions);
        }

        public String getMessage() {
            return message;
        }

        public String getImportPath() {
            return importPath;
        }

        public List<String> getSuggestions() {
            return new ArrayList<>(suggestions);
        }

        public boolean hasSuggestions() {
            return !suggestions.isEmpty();
        }

        /**
         * Formats the error with suggestions for display.
         *
         * @return formatted error message
         */
        public String toDisplayString() {
            if (suggestions.isEmpty()) {
                return message;
            }

            StringBuilder sb = new StringBuilder(message);
            if (suggestions.size() == 1) {
                sb.append(". Did you mean ").append(suggestions.get(0)).append("?");
            } else {
                sb.append(". Did you mean one of:");
                for (String suggestion : suggestions) {
                    sb.append("\n  - ").append(suggestion);
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return toDisplayString();
        }
    }
}
