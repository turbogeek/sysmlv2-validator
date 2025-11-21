package com.validator.suggestions;

import com.validator.semantic.ElementType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides user model symbols for spelling suggestions.
 * Wraps SymbolTable to extract symbol names from the current model.
 */
public final class UserModelProvider implements SuggestionProvider {

    private final SymbolTable symbolTable;
    private final Set<ElementType> filterTypes;

    /**
     * Creates a provider for all symbols in the symbol table.
     *
     * @param symbolTable the symbol table containing user model symbols
     */
    public UserModelProvider(SymbolTable symbolTable) {
        this(symbolTable, null);
    }

    /**
     * Creates a provider filtered to specific element types.
     *
     * @param symbolTable the symbol table containing user model symbols
     * @param filterTypes set of element types to include (null for all)
     */
    public UserModelProvider(SymbolTable symbolTable, Set<ElementType> filterTypes) {
        this.symbolTable = symbolTable;
        this.filterTypes = filterTypes;
    }

    @Override
    public Set<String> getCandidates() {
        if (symbolTable == null) {
            return Set.of();
        }

        var symbols = symbolTable.getAllSymbols();

        if (filterTypes != null && !filterTypes.isEmpty()) {
            return symbols.stream()
                .filter(s -> filterTypes.contains(s.getType()))
                .map(Symbol::getName)
                .collect(Collectors.toUnmodifiableSet());
        }

        return symbols.stream()
            .map(Symbol::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getSourceName() {
        return "model";
    }

    /**
     * Creates a provider for type definitions only (part def, action def, etc.).
     *
     * @param symbolTable the symbol table
     * @return provider filtered to definition types
     */
    public static UserModelProvider forDefinitions(SymbolTable symbolTable) {
        return new UserModelProvider(symbolTable, EnumSet.of(
            ElementType.PART_DEFINITION,
            ElementType.ACTION_DEFINITION,
            ElementType.STATE_DEFINITION,
            ElementType.REQUIREMENT_DEFINITION,
            ElementType.ATTRIBUTE_DEFINITION,
            ElementType.PORT_DEFINITION,
            ElementType.CONNECTION_DEFINITION,
            ElementType.INTERFACE_DEFINITION,
            ElementType.ITEM_DEFINITION,
            ElementType.ENUMERATION_DEFINITION,
            ElementType.CALC_DEFINITION,
            ElementType.VIEW_DEFINITION,
            ElementType.VIEWPOINT_DEFINITION,
            ElementType.DATA_TYPE
        ));
    }

    /**
     * Creates a provider for usages only (part, action, state, etc.).
     *
     * @param symbolTable the symbol table
     * @return provider filtered to usage types
     */
    public static UserModelProvider forUsages(SymbolTable symbolTable) {
        return new UserModelProvider(symbolTable, EnumSet.of(
            ElementType.PART_USAGE,
            ElementType.ACTION_USAGE,
            ElementType.STATE_USAGE,
            ElementType.REQUIREMENT_USAGE,
            ElementType.CONSTRAINT_USAGE,
            ElementType.ATTRIBUTE_USAGE,
            ElementType.PORT_USAGE,
            ElementType.CONNECTION_USAGE,
            ElementType.INTERFACE_USAGE,
            ElementType.ITEM_USAGE,
            ElementType.CALC_USAGE,
            ElementType.VIEW_USAGE
        ));
    }

    /**
     * Creates a provider for packages only.
     *
     * @param symbolTable the symbol table
     * @return provider filtered to packages
     */
    public static UserModelProvider forPackages(SymbolTable symbolTable) {
        return new UserModelProvider(symbolTable, EnumSet.of(ElementType.PACKAGE));
    }

    /**
     * Gets qualified name candidates for reference suggestions.
     *
     * @return set of all qualified symbol names
     */
    public Set<String> getQualifiedCandidates() {
        if (symbolTable == null) {
            return Set.of();
        }

        return symbolTable.getAllSymbols().stream()
            .map(Symbol::getQualifiedName)
            .collect(Collectors.toUnmodifiableSet());
    }
}
