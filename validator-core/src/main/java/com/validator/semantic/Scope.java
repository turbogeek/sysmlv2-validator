package com.validator.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a lexical scope in the SysML v2 model.
 * Scopes form a tree structure with parent-child relationships.
 */
public class Scope {
    private final String name;
    private final ScopeType type;
    private final Scope parent;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private final List<Scope> children = new ArrayList<>();
    private final List<ImportStatement> imports = new ArrayList<>();

    /**
     * Create a new scope.
     *
     * @param name the scope name
     * @param type the scope type
     * @param parent the parent scope (may be null for global scope)
     */
    public Scope(String name, ScopeType type, Scope parent) {
        this.name = Objects.requireNonNull(name, "Scope name cannot be null");
        this.type = Objects.requireNonNull(type, "Scope type cannot be null");
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    /**
     * Get the scope name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the scope type.
     */
    public ScopeType getType() {
        return type;
    }

    /**
     * Get the parent scope.
     *
     * @return the parent scope, or null for global scope
     */
    public Scope getParent() {
        return parent;
    }

    /**
     * Get the fully qualified name of this scope.
     */
    public String getQualifiedName() {
        if (parent == null || parent.getType() == ScopeType.GLOBAL) {
            return name.isEmpty() ? "" : name;
        }
        String parentQualified = parent.getQualifiedName();
        if (parentQualified.isEmpty()) {
            return name;
        }
        return parentQualified + "::" + name;
    }

    /**
     * Define a symbol in this scope.
     *
     * @param symbol the symbol to define
     * @throws IllegalStateException if a symbol with the same name already exists
     */
    public void define(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Symbol existing = symbols.get(symbol.getName());
        if (existing != null) {
            throw new IllegalStateException(String.format(
                "Duplicate symbol '%s' in scope '%s'", symbol.getName(), getQualifiedName()));
        }
        symbols.put(symbol.getName(), symbol);
    }

    /**
     * Lookup a symbol by name in this scope only (no parent lookup).
     *
     * @param symbolName the symbol name
     * @return the symbol if found, null otherwise
     */
    public Symbol lookup(String symbolName) {
        return symbols.get(symbolName);
    }

    /**
     * Resolve a symbol by name, checking this scope, imports, and parent scopes.
     *
     * @param symbolName the symbol name
     * @return the symbol if found, null otherwise
     */
    public Symbol resolve(String symbolName) {
        // 1. Check local symbols first
        Symbol symbol = symbols.get(symbolName);
        if (symbol != null) {
            return symbol;
        }

        // 2. Check imports
        for (ImportStatement imp : imports) {
            Symbol imported = imp.resolveImportedSymbol(symbolName);
            if (imported != null) {
                return imported;
            }
        }

        // 3. Check parent scope
        if (parent != null) {
            return parent.resolve(symbolName);
        }

        return null;
    }

    /**
     * Get all symbols defined in this scope.
     *
     * @return unmodifiable map of symbols
     */
    public Map<String, Symbol> getSymbols() {
        return Collections.unmodifiableMap(symbols);
    }

    /**
     * Get all child scopes.
     *
     * @return unmodifiable list of child scopes
     */
    public List<Scope> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Add an import statement to this scope.
     *
     * @param importStatement the import statement
     */
    public void addImport(ImportStatement importStatement) {
        Objects.requireNonNull(importStatement, "Import statement cannot be null");
        imports.add(importStatement);
    }

    /**
     * Get all import statements in this scope.
     *
     * @return unmodifiable list of imports
     */
    public List<ImportStatement> getImports() {
        return Collections.unmodifiableList(imports);
    }

    @Override
    public String toString() {
        return String.format("Scope{name='%s', type=%s, symbols=%d, children=%d}",
            getQualifiedName(), type, symbols.size(), children.size());
    }
}
