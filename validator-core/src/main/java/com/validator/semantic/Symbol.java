package com.validator.semantic;

import com.validator.ast.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a symbol in the symbol table.
 * A symbol is any named element in a SysML v2 model (part def, attribute, requirement, etc.)
 */
public class Symbol {
    private final String name;
    private final String qualifiedName;
    private final ElementType type;
    private final Location location;
    private final Visibility visibility;
    private final List<Symbol> redefinitions;
    private final List<Symbol> specializations;
    private final List<Symbol> subsettings;
    private Object astNode; // Reference to original AST node

    public Symbol(String name, String qualifiedName, ElementType type, Location location) {
        this(name, qualifiedName, type, location, Visibility.PUBLIC);
    }

    public Symbol(String name, String qualifiedName, ElementType type, Location location, Visibility visibility) {
        this.name = Objects.requireNonNull(name, "Symbol name cannot be null");
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "Qualified name cannot be null");
        this.type = Objects.requireNonNull(type, "Element type cannot be null");
        this.location = location;
        this.visibility = visibility != null ? visibility : Visibility.PRIVATE;
        this.redefinitions = new ArrayList<>();
        this.specializations = new ArrayList<>();
        this.subsettings = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public ElementType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public List<Symbol> getRedefinitions() {
        return new ArrayList<>(redefinitions);
    }

    public void addRedefinition(Symbol symbol) {
        Objects.requireNonNull(symbol, "Cannot add null redefinition");
        if (!redefinitions.contains(symbol)) {
            redefinitions.add(symbol);
        }
    }

    public List<Symbol> getSpecializations() {
        return new ArrayList<>(specializations);
    }

    public void addSpecialization(Symbol symbol) {
        Objects.requireNonNull(symbol, "Cannot add null specialization");
        if (!specializations.contains(symbol)) {
            specializations.add(symbol);
        }
    }

    public List<Symbol> getSubsettings() {
        return new ArrayList<>(subsettings);
    }

    public void addSubsetting(Symbol symbol) {
        Objects.requireNonNull(symbol, "Cannot add null subsetting");
        if (!subsettings.contains(symbol)) {
            subsettings.add(symbol);
        }
    }

    public Object getAstNode() {
        return astNode;
    }

    public void setAstNode(Object astNode) {
        this.astNode = astNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return qualifiedName.equals(symbol.qualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName);
    }

    @Override
    public String toString() {
        return String.format("Symbol{name='%s', qualifiedName='%s', type=%s, visibility=%s}",
            name, qualifiedName, type, visibility);
    }
}
