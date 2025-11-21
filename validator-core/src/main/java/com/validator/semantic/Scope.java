package com.validator.semantic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a scope in the symbol table.
 * Scopes are hierarchical and contain named symbols.
 */
public class Scope {
    private final String name;
    private final ScopeType type;
    private final Scope parent;
    private final Map<String, Symbol> symbols;
    private final List<ImportStatement> imports;
    private final List<Scope> children;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Parent scope reference required for scope hierarchy")
    public Scope(String name, ScopeType type, Scope parent) {
        this.name = Objects.requireNonNull(name, "Scope name cannot be null");
        this.type = Objects.requireNonNull(type, "Scope type cannot be null");
        this.parent = parent;
        this.symbols = new LinkedHashMap<>(); // Preserve insertion order
        this.imports = new ArrayList<>();
        this.children = new ArrayList<>();

        if (parent != null) {
            parent.addChild(this);
        }
    }

    public String getName() {
        return name;
    }

    public ScopeType getType() {
        return type;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Parent scope must be mutable for hierarchical navigation")
    public Scope getParent() {
        return parent;
    }

    /**
     * Get the qualified name of this scope by walking up the parent chain.
     */
    public String getQualifiedName() {
        if (parent == null || parent.getType() == ScopeType.GLOBAL) {
            return name;
        }
        return parent.getQualifiedName() + "::" + name;
    }

    /**
     * Define a new symbol in this scope.
     * @throws IllegalStateException if a symbol with the same name already exists
     */
    public void define(Symbol symbol) {
        String symbolName = symbol.getName();
        if (symbols.containsKey(symbolName)) {
            Symbol existing = symbols.get(symbolName);
            throw new IllegalStateException(
                String.format("Symbol '%s' already defined in scope '%s' at %s (previous definition at %s)",
                    symbolName, getQualifiedName(), symbol.getLocation(), existing.getLocation())
            );
        }
        symbols.put(symbolName, symbol);
    }

    /**
     * Look up a symbol by name in this scope only (not parent scopes).
     */
    public Symbol lookup(String symbolName) {
        return symbols.get(symbolName);
    }

    /**
     * Resolve a symbol by name, searching this scope and parent scopes.
     */
    public Symbol resolve(String symbolName) {
        // Check this scope
        Symbol symbol = lookup(symbolName);
        if (symbol != null) {
            return symbol;
        }

        // Check imports using streams
        symbol = imports.stream()
            .map(importStmt -> importStmt.resolve(symbolName))
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (symbol != null) {
            return symbol;
        }

        // Check parent scope
        if (parent != null) {
            return parent.resolve(symbolName);
        }

        return null;
    }

    /**
     * Resolve a qualified name (e.g., "Package::PartDef::attribute").
     */
    public Symbol resolveQualified(String qualifiedName) {
        String[] parts = qualifiedName.split("::");
        if (parts.length == 1) {
            return resolve(parts[0]);
        }

        // Try to resolve the first part
        Symbol current = resolve(parts[0]);
        if (current == null) {
            return null;
        }

        // Walk through remaining parts
        for (int i = 1; i < parts.length; i++) {
            // This requires getting the scope for the current symbol
            // For now, we'll implement a simplified version
            // Full implementation would need to traverse the AST
            return null; // TODO: Implement nested qualified name resolution
        }

        return current;
    }

    public void addImport(ImportStatement importStmt) {
        Objects.requireNonNull(importStmt, "Cannot add null import");
        imports.add(importStmt);
    }

    public List<ImportStatement> getImports() {
        return Collections.unmodifiableList(imports);
    }

    public Map<String, Symbol> getSymbols() {
        return Collections.unmodifiableMap(symbols);
    }

    public List<Scope> getChildren() {
        return new ArrayList<>(children);
    }

    private void addChild(Scope child) {
        children.add(child);
    }

    /**
     * Get all symbols visible in this scope (including parent scopes).
     */
    public Map<String, Symbol> getAllVisibleSymbols() {
        Map<String, Symbol> allSymbols = new LinkedHashMap<>();

        // Add parent symbols first (so local symbols can override)
        if (parent != null) {
            allSymbols.putAll(parent.getAllVisibleSymbols());
        }

        // Add imported symbols using streams
        imports.stream()
            .map(ImportStatement::getImportedSymbols)
            .forEach(allSymbols::putAll);

        // Add local symbols (these take precedence)
        allSymbols.putAll(symbols);

        return allSymbols;
    }

    @Override
    public String toString() {
        return String.format("Scope{name='%s', type=%s, symbols=%d, imports=%d, children=%d}",
            name, type, symbols.size(), imports.size(), children.size());
    }
}
