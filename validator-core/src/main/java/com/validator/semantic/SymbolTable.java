package com.validator.semantic;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Symbol table for SysML v2 semantic validation.
 * Manages scopes and symbol resolution.
 */
public class SymbolTable {
    private final Scope globalScope;
    private Scope currentScope;
    private final Map<String, Symbol> qualifiedSymbols = new LinkedHashMap<>();
    private final Map<String, Scope> qualifiedScopes = new LinkedHashMap<>();

    /**
     * Create a new symbol table with a global scope.
     */
    public SymbolTable() {
        this.globalScope = new Scope("", ScopeType.GLOBAL, null);
        this.currentScope = globalScope;
        qualifiedScopes.put("", globalScope);
    }

    /**
     * Enter a new scope.
     *
     * @param name the scope name
     * @param type the scope type
     */
    public void enterScope(String name, ScopeType type) {
        Objects.requireNonNull(name, "Scope name cannot be null");
        Objects.requireNonNull(type, "Scope type cannot be null");

        Scope newScope = new Scope(name, type, currentScope);
        currentScope = newScope;

        // Register scope by qualified name
        String qualifiedName = newScope.getQualifiedName();
        if (!qualifiedName.isEmpty()) {
            qualifiedScopes.put(qualifiedName, newScope);
        }
    }

    /**
     * Exit the current scope, returning to parent.
     *
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
     *
     * @param symbol the symbol to define
     * @throws IllegalStateException if duplicate symbol
     */
    public void define(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");

        // Define in current scope (may throw if duplicate)
        currentScope.define(symbol);

        // Add to global registry
        qualifiedSymbols.put(symbol.getQualifiedName(), symbol);
    }

    /**
     * Resolve a simple name starting from current scope.
     *
     * @param name the simple name
     * @return the symbol if found, null otherwise
     */
    public Symbol resolve(String name) {
        if (name == null) {
            return null;
        }
        return currentScope.resolve(name);
    }

    /**
     * Lookup a symbol by simple name in the current scope only (no parent lookup).
     *
     * @param name the simple name
     * @return the symbol if found, null otherwise
     */
    public Symbol lookup(String name) {
        if (name == null) {
            return null;
        }
        return currentScope.lookup(name);
    }

    /**
     * Resolve a qualified name (Package::Element::SubElement).
     *
     * @param qualifiedName the qualified name
     * @return the symbol if found, null otherwise
     */
    public Symbol resolveQualified(String qualifiedName) {
        if (qualifiedName == null) {
            return null;
        }
        return qualifiedSymbols.get(qualifiedName);
    }

    /**
     * Resolve a scope by qualified name.
     *
     * @param qualifiedName the qualified scope name
     * @return the scope if found, null otherwise
     */
    public Scope resolveScope(String qualifiedName) {
        if (qualifiedName == null) {
            return null;
        }
        return qualifiedScopes.get(qualifiedName);
    }

    /**
     * Add an import statement to the current scope.
     *
     * @param importStatement the import statement
     */
    public void addImport(ImportStatement importStatement) {
        Objects.requireNonNull(importStatement, "Import statement cannot be null");
        currentScope.addImport(importStatement);
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
     * Get all symbols defined in the table.
     *
     * @return unmodifiable collection of all symbols
     */
    public Collection<Symbol> getAllSymbols() {
        return Collections.unmodifiableCollection(qualifiedSymbols.values());
    }

    /**
     * Get symbols filtered by element type.
     *
     * @param type the element type to filter by
     * @return unmodifiable list of matching symbols
     */
    public List<Symbol> getSymbolsByType(ElementType type) {
        return qualifiedSymbols.values().stream()
            .filter(s -> s.getType() == type)
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Get symbol table statistics.
     *
     * @return the statistics
     */
    public SymbolTableStats getStats() {
        return new SymbolTableStats();
    }

    /**
     * Statistics about the symbol table.
     */
    public class SymbolTableStats {
        /**
         * Get the total number of symbols.
         */
        public int getSymbolCount() {
            return qualifiedSymbols.size();
        }

        /**
         * Get the total number of scopes.
         */
        public int getScopeCount() {
            return qualifiedScopes.size();
        }

        /**
         * Get symbols grouped by element type.
         */
        public Map<ElementType, Integer> getSymbolsByType() {
            Map<ElementType, Integer> result = new EnumMap<>(ElementType.class);
            for (Symbol symbol : qualifiedSymbols.values()) {
                result.merge(symbol.getType(), 1, Integer::sum);
            }
            return result;
        }

        /**
         * Get symbols grouped by scope type.
         */
        public Map<ScopeType, Integer> getSymbolsByScope() {
            Map<ScopeType, Integer> result = new EnumMap<>(ScopeType.class);
            for (Scope scope : qualifiedScopes.values()) {
                int count = scope.getSymbols().size();
                result.merge(scope.getType(), count, Integer::sum);
            }
            // Ensure all referenced scope types are present
            for (Scope scope : qualifiedScopes.values()) {
                result.putIfAbsent(scope.getType(), 0);
            }
            return result;
        }
    }
}
