package com.validator.suggestions;

import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.Symbol;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides symbols from parsed libraries for spelling suggestions.
 * Can be loaded from the StandardLibraryManager or populated programmatically.
 */
public final class KparLibraryProvider implements SuggestionProvider {

    private final Set<String> candidates = new HashSet<>();

    /**
     * Create an empty library provider.
     */
    public KparLibraryProvider() {
    }

    /**
     * Create a provider and load symbols from a StandardLibraryManager.
     *
     * @param manager the manager containing parsed symbols
     */
    public KparLibraryProvider(StandardLibraryManager manager) {
        loadFromManager(manager);
    }

    /**
     * Load symbols from a StandardLibraryManager.
     *
     * @param manager the manager to load symbols from
     */
    public void loadFromManager(StandardLibraryManager manager) {
        if (manager == null) return;
        for (Symbol symbol : manager.getAllSymbols()) {
            candidates.add(symbol.getName());
        }
    }

    /**
     * Add a single candidate symbol.
     *
     * @param candidate the candidate to add
     */
    public void addCandidate(String candidate) {
        if (candidate != null && !candidate.isEmpty()) {
            candidates.add(candidate);
        }
    }

    /**
     * Add multiple candidate symbols.
     *
     * @param newCandidates the candidates to add
     */
    public void addCandidates(Collection<String> newCandidates) {
        for (String candidate : newCandidates) {
            addCandidate(candidate);
        }
    }

    /**
     * Clear all loaded candidates.
     */
    public void clear() {
        candidates.clear();
    }

    /**
     * Get the number of loaded candidates.
     *
     * @return candidate count
     */
    public int getCandidateCount() {
        return candidates.size();
    }

    @Override
    public Set<String> getCandidates() {
        return Collections.unmodifiableSet(candidates);
    }

    @Override
    public String getSourceName() {
        return "kpar";
    }

    @Override
    public int getPriority() {
        return 4; // Higher than stdlib, lower than model
    }
}
