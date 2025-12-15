package com.validator.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves import statements against the symbol table and standard library.
 */
public final class ImportResolver {

    private static final Pattern FILTERED_PATTERN = Pattern.compile(
        "(.+)::\\{([^}]+)\\}");

    private final SymbolTable symbolTable;
    private final StandardLibraryManager standardLibrary;
    private final List<String> errors = new ArrayList<>();

    /**
     * Create an import resolver.
     *
     * @param symbolTable the symbol table
     * @param standardLibrary the standard library manager
     */
    public ImportResolver(SymbolTable symbolTable, StandardLibraryManager standardLibrary) {
        this.symbolTable = symbolTable;
        this.standardLibrary = standardLibrary;
    }

    /**
     * Resolve an import statement.
     *
     * @param importStatement the import to resolve
     * @param currentScope the scope containing the import
     */
    public void resolveImport(ImportStatement importStatement, Scope currentScope) {
        String path = importStatement.getImportPath();
        ImportType type = importStatement.getImportType();

        switch (type) {
            case WILDCARD:
                resolveWildcard(importStatement, path);
                break;
            case SPECIFIC:
                resolveSpecific(importStatement, path);
                break;
            case FILTERED:
                resolveFiltered(importStatement, path);
                break;
            case ALIAS:
                resolveAlias(importStatement, path);
                break;
            default:
                errors.add("Unknown import type: " + type);
        }
    }

    private void resolveWildcard(ImportStatement importStatement, String path) {
        // Remove the ::* suffix
        String packagePath = path.replace("::*", "");

        // Try standard library first
        List<Symbol> stdLibSymbols = standardLibrary.getSymbolsInPackage(packagePath);
        for (Symbol symbol : stdLibSymbols) {
            importStatement.addImportedSymbol(symbol);
        }

        // Try symbol table
        Scope packageScope = symbolTable.resolveScope(packagePath);
        if (packageScope != null) {
            for (Symbol symbol : packageScope.getSymbols().values()) {
                if (symbol.getVisibility() == Visibility.PUBLIC) {
                    importStatement.addImportedSymbol(symbol);
                }
            }
        }

        if (importStatement.getImportedSymbols().isEmpty()) {
            errors.add("Cannot resolve wildcard import: " + path);
        }
    }

    private void resolveSpecific(ImportStatement importStatement, String path) {
        // Try standard library first
        Symbol stdSymbol = standardLibrary.resolveSymbol(path);
        if (stdSymbol != null) {
            importStatement.addImportedSymbol(stdSymbol);
            return;
        }

        // Try symbol table
        Symbol symbol = symbolTable.resolveQualified(path);
        if (symbol != null) {
            if (symbol.getVisibility() == Visibility.PUBLIC) {
                importStatement.addImportedSymbol(symbol);
            } else {
                errors.add("Cannot import private symbol: " + path);
            }
            return;
        }

        errors.add("Cannot resolve import: " + path);
    }

    private void resolveFiltered(ImportStatement importStatement, String path) {
        Matcher matcher = FILTERED_PATTERN.matcher(path);
        if (!matcher.matches()) {
            errors.add("Invalid filtered import syntax: " + path);
            return;
        }

        String packagePath = matcher.group(1);
        String elementList = matcher.group(2);
        String[] elements = elementList.split("\\s*,\\s*");

        for (String element : elements) {
            String qualifiedName = packagePath + "::" + element.trim();

            // Try standard library first
            Symbol stdSymbol = standardLibrary.resolveSymbol(qualifiedName);
            if (stdSymbol != null) {
                importStatement.addImportedSymbol(stdSymbol);
                continue;
            }

            // Try symbol table
            Symbol symbol = symbolTable.resolveQualified(qualifiedName);
            if (symbol != null && symbol.getVisibility() == Visibility.PUBLIC) {
                importStatement.addImportedSymbol(symbol);
            } else if (symbol == null) {
                errors.add("Cannot resolve filtered import element: " + qualifiedName);
            }
        }
    }

    private void resolveAlias(ImportStatement importStatement, String path) {
        // Remove "as <alias>" suffix from path
        String actualPath = path;
        int asIndex = path.indexOf(" as ");
        if (asIndex > 0) {
            actualPath = path.substring(0, asIndex);
        }

        // Try standard library first
        Symbol stdSymbol = standardLibrary.resolveSymbol(actualPath);
        if (stdSymbol != null) {
            // Use alias as the key
            String alias = importStatement.getAlias();
            if (alias != null) {
                importStatement.addImportedSymbol(new Symbol(alias, stdSymbol.getQualifiedName(),
                    stdSymbol.getType(), stdSymbol.getLocation(), stdSymbol.getVisibility()));
            } else {
                importStatement.addImportedSymbol(stdSymbol);
            }
            return;
        }

        // Try symbol table
        Symbol symbol = symbolTable.resolveQualified(actualPath);
        if (symbol != null) {
            String alias = importStatement.getAlias();
            if (alias != null) {
                importStatement.addImportedSymbol(new Symbol(alias, symbol.getQualifiedName(),
                    symbol.getType(), symbol.getLocation(), symbol.getVisibility()));
            } else {
                importStatement.addImportedSymbol(symbol);
            }
            return;
        }

        errors.add("Cannot resolve aliased import: " + actualPath);
    }

    /**
     * Resolve all imports in all scopes of the symbol table.
     */
    public void resolveAllImports() {
        resolveImportsInScope(symbolTable.getGlobalScope());
    }

    private void resolveImportsInScope(Scope scope) {
        for (ImportStatement importStmt : scope.getImports()) {
            resolveImport(importStmt, scope);
        }
        // Note: Would need to iterate child scopes if the Scope class exposed them
    }

    /**
     * Get all resolution errors.
     *
     * @return list of error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Clear all errors.
     */
    public void clearErrors() {
        errors.clear();
    }

    /**
     * Get import resolution statistics.
     *
     * @return the statistics
     */
    public ImportResolutionStats getStats() {
        return new ImportResolutionStats();
    }

    /**
     * Statistics about import resolution.
     */
    public class ImportResolutionStats {
        private int totalImports = 0;
        private int resolvedImports = 0;
        private int unresolvedImports = 0;

        ImportResolutionStats() {
            countImportsInScope(symbolTable.getGlobalScope());
        }

        private void countImportsInScope(Scope scope) {
            for (ImportStatement importStmt : scope.getImports()) {
                totalImports++;
                if (!importStmt.getImportedSymbols().isEmpty()) {
                    resolvedImports++;
                } else {
                    unresolvedImports++;
                }
            }
        }

        /**
         * Get total imports count.
         */
        public int getTotalImports() {
            return totalImports;
        }

        /**
         * Get resolved imports count.
         */
        public int getResolvedImports() {
            return resolvedImports;
        }

        /**
         * Get unresolved imports count.
         */
        public int getUnresolvedImports() {
            return unresolvedImports;
        }
    }
}
