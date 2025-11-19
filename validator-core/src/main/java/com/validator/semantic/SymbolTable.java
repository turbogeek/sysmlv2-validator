package com.validator.semantic;

import java.util.*;

/**
 * The symbol table for a SysML v2 model.
 * Manages scopes, symbols, and name resolution.
 */
public class SymbolTable {
    private final Scope globalScope;
    private Scope currentScope;
    private final Map<String, Scope> scopesByQualifiedName;
    private final Map<String, Symbol> globalSymbols;

    public SymbolTable() {
        this.globalScope = new Scope("", ScopeType.GLOBAL, null);
        this.currentScope = globalScope;
        this.scopesByQualifiedName = new LinkedHashMap<>();
        this.globalSymbols = new LinkedHashMap<>();
        scopesByQualifiedName.put("", globalScope);
    }

    /**
     * Get the global scope.
     */
    public Scope getGlobalScope() {
        return globalScope;
    }

    /**
     * Get the current scope.
     */
    public Scope getCurrentScope() {
        return currentScope;
    }

    /**
     * Enter a new scope.
     */
    public void enterScope(String name, ScopeType type) {
        Scope newScope = new Scope(name, type, currentScope);
        currentScope = newScope;
        scopesByQualifiedName.put(newScope.getQualifiedName(), newScope);
    }

    /**
     * Exit the current scope and return to the parent scope.
     * @throws IllegalStateException if already at global scope
     */
    public void exitScope() {
        if (currentScope == globalScope) {
            throw new IllegalStateException("Cannot exit global scope");
        }
        currentScope = currentScope.getParent();
    }

    /**
     * Define a symbol in the current scope.
     */
    public void define(Symbol symbol) {
        currentScope.define(symbol);

        // Also add to global symbols map for quick lookup
        globalSymbols.put(symbol.getQualifiedName(), symbol);
    }

    /**
     * Look up a symbol by name in the current scope only.
     */
    public Symbol lookup(String name) {
        return currentScope.lookup(name);
    }

    /**
     * Resolve a symbol by name, searching current scope and parent scopes.
     */
    public Symbol resolve(String name) {
        return currentScope.resolve(name);
    }

    /**
     * Resolve a qualified name (e.g., "Package::PartDef::attribute").
     */
    public Symbol resolveQualified(String qualifiedName) {
        // First try direct lookup in global symbols
        Symbol symbol = globalSymbols.get(qualifiedName);
        if (symbol != null) {
            return symbol;
        }

        // Try resolving through current scope
        return currentScope.resolveQualified(qualifiedName);
    }

    /**
     * Add an import to the current scope.
     */
    public void addImport(ImportStatement importStmt) {
        currentScope.addImport(importStmt);
    }

    /**
     * Get a scope by its qualified name.
     */
    public Scope getScopeByQualifiedName(String qualifiedName) {
        return scopesByQualifiedName.get(qualifiedName);
    }

    /**
     * Get all scopes in the symbol table.
     */
    public Collection<Scope> getAllScopes() {
        return Collections.unmodifiableCollection(scopesByQualifiedName.values());
    }

    /**
     * Get all symbols in the symbol table.
     */
    public Collection<Symbol> getAllSymbols() {
        return Collections.unmodifiableCollection(globalSymbols.values());
    }

    /**
     * Get all symbols of a specific type.
     */
    public List<Symbol> getSymbolsByType(ElementType type) {
        List<Symbol> result = new ArrayList<>();
        for (Symbol symbol : globalSymbols.values()) {
            if (symbol.getType() == type) {
                result.add(symbol);
            }
        }
        return result;
    }

    /**
     * Check if a symbol exists with the given qualified name.
     */
    public boolean hasSymbol(String qualifiedName) {
        return globalSymbols.containsKey(qualifiedName);
    }

    /**
     * Get statistics about the symbol table.
     */
    public SymbolTableStats getStats() {
        Map<ElementType, Integer> typeCounts = new EnumMap<>(ElementType.class);
        for (Symbol symbol : globalSymbols.values()) {
            typeCounts.merge(symbol.getType(), 1, Integer::sum);
        }
        return new SymbolTableStats(
            scopesByQualifiedName.size(),
            globalSymbols.size(),
            typeCounts
        );
    }

    @Override
    public String toString() {
        return String.format("SymbolTable{scopes=%d, symbols=%d}",
            scopesByQualifiedName.size(), globalSymbols.size());
    }

    /**
     * Statistics about the symbol table.
     */
    public static class SymbolTableStats {
        private final int scopeCount;
        private final int symbolCount;
        private final Map<ElementType, Integer> symbolsByType;

        public SymbolTableStats(int scopeCount, int symbolCount, Map<ElementType, Integer> symbolsByType) {
            this.scopeCount = scopeCount;
            this.symbolCount = symbolCount;
            this.symbolsByType = new EnumMap<>(symbolsByType);
        }

        public int getScopeCount() {
            return scopeCount;
        }

        public int getSymbolCount() {
            return symbolCount;
        }

        public Map<ElementType, Integer> getSymbolsByType() {
            return Collections.unmodifiableMap(symbolsByType);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Symbol Table Statistics:\n"));
            sb.append(String.format("  Scopes: %d\n", scopeCount));
            sb.append(String.format("  Symbols: %d\n", symbolCount));
            sb.append("  By Type:\n");
            symbolsByType.entrySet().stream()
                .sorted(Map.Entry.<ElementType, Integer>comparingByValue().reversed())
                .forEach(entry -> sb.append(String.format("    %s: %d\n", entry.getKey(), entry.getValue())));
            return sb.toString();
        }
    }
}
