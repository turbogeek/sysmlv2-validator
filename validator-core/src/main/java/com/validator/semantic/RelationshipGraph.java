package com.validator.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An in-memory graph tracking relationships between SysML v2 symbols.
 * Enables deep namespace resolution, traceability queries, and impact analysis.
 */
public class RelationshipGraph {

    public static class Relationship {
        private final String source;
        private final String target;
        private final RelationshipType type;

        public Relationship(String source, String target, RelationshipType type) {
            this.source = Objects.requireNonNull(source);
            this.target = Objects.requireNonNull(target);
            this.type = Objects.requireNonNull(type);
        }

        public String getSource() {
            return source;
        }

        public String getTarget() {
            return target;
        }

        public RelationshipType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Relationship that = (Relationship) o;
            return source.equals(that.source) && target.equals(that.target) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target, type);
        }
    }

    private final Map<String, Set<Relationship>> outgoingEdges = new HashMap<>();
    private final Map<String, Set<Relationship>> incomingEdges = new HashMap<>();
    private final SymbolTable symbolTable;

    public RelationshipGraph(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * Add a directed relationship between two symbol qualified names.
     */
    public void addEdge(String sourceQualifiedName, String targetQualifiedName, RelationshipType type) {
        if (sourceQualifiedName == null || targetQualifiedName == null || type == null) {
            return;
        }

        Relationship rel = new Relationship(sourceQualifiedName, targetQualifiedName, type);
        
        outgoingEdges.computeIfAbsent(sourceQualifiedName, k -> new HashSet<>()).add(rel);
        incomingEdges.computeIfAbsent(targetQualifiedName, k -> new HashSet<>()).add(rel);
    }

    /**
     * Build the graph from the current SymbolTable.
     */
    public void buildFromSymbolTable() {
        outgoingEdges.clear();
        incomingEdges.clear();

        for (Symbol symbol : symbolTable.getAllSymbols()) {
            String qn = symbol.getQualifiedName();

            // Index specializations
            for (Symbol spec : symbol.getSpecializations()) {
                addEdge(qn, spec.getQualifiedName(), RelationshipType.SPECIALIZES);
            }

            // Index redefinitions
            for (Symbol redef : symbol.getRedefinitions()) {
                addEdge(qn, redef.getQualifiedName(), RelationshipType.REDEFINES);
            }

            // Index subsettings
            for (Symbol subset : symbol.getSubsettings()) {
                addEdge(qn, subset.getQualifiedName(), RelationshipType.SUBSETS);
            }

            // Implicit owns relationship (parent package -> child)
            int lastColonIndex = qn.lastIndexOf("::");
            if (lastColonIndex > 0) {
                String parentQn = qn.substring(0, lastColonIndex);
                if (symbolTable.resolveQualifiedDirect(parentQn) != null) {
                    addEdge(parentQn, qn, RelationshipType.OWNS);
                }
            }
        }
    }

    /**
     * Get related elements for a given symbol and relationship type.
     */
    public List<Symbol> getRelatedElements(String qualifiedName, RelationshipType type) {
        Set<Relationship> edges = outgoingEdges.get(qualifiedName);
        if (edges == null) {
            return Collections.emptyList();
        }

        return edges.stream()
                .filter(e -> e.getType() == type)
                .map(e -> symbolTable.resolveQualifiedDirect(e.getTarget()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get incoming related elements.
     */
    public List<Symbol> getIncomingElements(String qualifiedName, RelationshipType type) {
        Set<Relationship> edges = incomingEdges.get(qualifiedName);
        if (edges == null) {
            return Collections.emptyList();
        }

        return edges.stream()
                .filter(e -> e.getType() == type)
                .map(e -> symbolTable.resolveQualifiedDirect(e.getSource()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Resolve a deep namespace reference like `SysML::Actions::Action::start`
     * by traversing the OWNS relationships and specializations.
     * 
     * @param rootQualifiedName the root symbol to start from (e.g. SysML::Actions::Action)
     * @param path the path of member names to resolve (e.g. ["start"])
     * @return the resolved Symbol, or null if not found
     */
    public Symbol resolveDeepNamespace(String rootQualifiedName, List<String> path) {
        Symbol current = symbolTable.resolveQualifiedDirect(rootQualifiedName);
        if (current == null) {
            return null;
        }

        for (String step : path) {
            Symbol next = findOwnedMember(current, step);
            if (next == null) {
                return null;
            }
            current = next;
        }

        return current;
    }

    private Symbol findOwnedMember(Symbol parent, String memberName) {
        // Direct child
        String expectedQn = parent.getQualifiedName() + "::" + memberName;
        Symbol directChild = symbolTable.resolveQualifiedDirect(expectedQn);
        if (directChild != null) {
            return directChild;
        }

        // If not found, we should check inherited members
        Queue<Symbol> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.add(parent);
        visited.add(parent.getQualifiedName());

        while (!queue.isEmpty()) {
            Symbol current = queue.poll();
            
            // Check direct children of current
            List<Symbol> owned = getRelatedElements(current.getQualifiedName(), RelationshipType.OWNS);
            for (Symbol child : owned) {
                if (child.getName().equals(memberName)) {
                    return child;
                }
            }

            // Add specializations to check inherited members
            List<Symbol> specs = getRelatedElements(current.getQualifiedName(), RelationshipType.SPECIALIZES);
            for (Symbol spec : specs) {
                if (visited.add(spec.getQualifiedName())) {
                    queue.add(spec);
                }
            }
        }

        return null;
    }
}
