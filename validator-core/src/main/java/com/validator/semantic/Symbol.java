package com.validator.semantic;

import com.validator.ast.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a named symbol in the SysML v2 model.
 * Symbols include packages, definitions, usages, and other named elements.
 */
public class Symbol {
    private final String name;
    private final String qualifiedName;
    private final ElementType type;
    private final Location location;
    private final Visibility visibility;

    // Type relationships
    private final List<Symbol> specializations = new ArrayList<>();  // :>
    private final List<Symbol> redefinitions = new ArrayList<>();    // :>>
    private final List<Symbol> subsettings = new ArrayList<>();      // subsets

    // Reference tracking for lint analysis
    private final List<Location> references = new ArrayList<>();
    private boolean usedAsType = false;
    private boolean usedInExpression = false;
    private boolean usedInImport = false;

    // AST node attachment (for back-reference to parse tree)
    private Object astNode;

    /**
     * Create a new symbol with public visibility.
     *
     * @param name the simple name of the symbol
     * @param qualifiedName the fully qualified name
     * @param type the element type
     * @param location the source location (may be null for generated symbols)
     */
    public Symbol(String name, String qualifiedName, ElementType type, Location location) {
        this(name, qualifiedName, type, location, Visibility.PUBLIC);
    }

    /**
     * Create a new symbol.
     *
     * @param name the simple name of the symbol
     * @param qualifiedName the fully qualified name
     * @param type the element type
     * @param location the source location (may be null for generated symbols)
     * @param visibility the visibility (defaults to PRIVATE if null)
     */
    public Symbol(String name, String qualifiedName, ElementType type,
                  Location location, Visibility visibility) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "Qualified name cannot be null");
        this.type = Objects.requireNonNull(type, "Element type cannot be null");
        this.location = location; // Can be null for generated symbols
        this.visibility = visibility != null ? visibility : Visibility.PRIVATE;
    }

    /**
     * Get the simple name of this symbol.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the fully qualified name of this symbol.
     */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /**
     * Get the element type.
     */
    public ElementType getType() {
        return type;
    }

    /**
     * Get the source location.
     *
     * @return the location, or null for generated symbols
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the visibility.
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Add a specialization relationship (:>).
     *
     * @param parent the symbol being specialized
     */
    public void addSpecialization(Symbol parent) {
        Objects.requireNonNull(parent, "Specialization symbol cannot be null");
        specializations.add(parent);
    }

    /**
     * Get all specializations.
     *
     * @return unmodifiable list of specializations
     */
    public List<Symbol> getSpecializations() {
        return Collections.unmodifiableList(specializations);
    }

    /**
     * Add a redefinition relationship (:>>).
     *
     * @param redefined the symbol being redefined
     */
    public void addRedefinition(Symbol redefined) {
        Objects.requireNonNull(redefined, "Redefinition symbol cannot be null");
        redefinitions.add(redefined);
    }

    /**
     * Get all redefinitions.
     *
     * @return unmodifiable list of redefinitions
     */
    public List<Symbol> getRedefinitions() {
        return Collections.unmodifiableList(redefinitions);
    }

    /**
     * Add a subsetting relationship.
     *
     * @param subset the symbol being subsetted
     */
    public void addSubsetting(Symbol subset) {
        Objects.requireNonNull(subset, "Subsetting symbol cannot be null");
        subsettings.add(subset);
    }

    /**
     * Get all subsettings.
     *
     * @return unmodifiable list of subsettings
     */
    public List<Symbol> getSubsettings() {
        return Collections.unmodifiableList(subsettings);
    }

    /**
     * Get the attached AST node.
     *
     * @return the AST node, or null if not set
     */
    public Object getAstNode() {
        return astNode;
    }

    /**
     * Set the attached AST node.
     *
     * @param astNode the AST node (may be null)
     */
    public void setAstNode(Object astNode) {
        this.astNode = astNode;
    }

    // =========================================================================
    // Reference Tracking for Lint Analysis
    // =========================================================================

    /**
     * Records a reference to this symbol from another location.
     *
     * @param location the location where this symbol is referenced
     */
    public void addReference(Location refLocation) {
        if (refLocation != null) {
            references.add(refLocation);
        }
    }

    /**
     * Gets all references to this symbol.
     *
     * @return unmodifiable list of reference locations
     */
    public List<Location> getReferences() {
        return Collections.unmodifiableList(references);
    }

    /**
     * Gets the number of times this symbol is referenced.
     *
     * @return the reference count
     */
    public int getReferenceCount() {
        return references.size();
    }

    /**
     * Checks if this symbol is unused (no references).
     *
     * @return true if this symbol has no references
     */
    public boolean isUnused() {
        return references.isEmpty() && !usedAsType && !usedInExpression && !usedInImport;
    }

    /**
     * Checks if this symbol has any references.
     *
     * @return true if this symbol is referenced at least once
     */
    public boolean isReferenced() {
        return !isUnused();
    }

    /**
     * Marks this symbol as used as a type (e.g., part x : TypeName).
     */
    public void markUsedAsType() {
        this.usedAsType = true;
    }

    /**
     * Checks if this symbol was used as a type.
     *
     * @return true if used as a type
     */
    public boolean isUsedAsType() {
        return usedAsType;
    }

    /**
     * Marks this symbol as used in an expression.
     */
    public void markUsedInExpression() {
        this.usedInExpression = true;
    }

    /**
     * Checks if this symbol was used in an expression.
     *
     * @return true if used in expression
     */
    public boolean isUsedInExpression() {
        return usedInExpression;
    }

    /**
     * Marks this symbol as used (imported) by another file.
     */
    public void markUsedInImport() {
        this.usedInImport = true;
    }

    /**
     * Checks if this symbol was used in an import.
     *
     * @return true if used in import
     */
    public boolean isUsedInImport() {
        return usedInImport;
    }

    /**
     * Checks if this symbol is a definition type (part def, action def, etc.).
     *
     * @return true if this is a definition
     */
    public boolean isDefinition() {
        return type != null && type.name().contains("DEFINITION");
    }

    /**
     * Checks if this symbol is a usage type (part, action, attribute, etc.).
     *
     * @return true if this is a usage
     */
    public boolean isUsage() {
        return type != null && type.name().contains("USAGE");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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
