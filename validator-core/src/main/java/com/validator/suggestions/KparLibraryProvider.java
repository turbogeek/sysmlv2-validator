package com.validator.suggestions;

import com.validator.library.KparReader;
import com.validator.semantic.Symbol;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides symbols from KPAR (KerML/SysML Package Archive) files for spelling suggestions.
 * Can be loaded from one or more KPAR files or populated programmatically.
 */
public final class KparLibraryProvider implements SuggestionProvider {

    private final Set<String> candidates = new HashSet<>();
    private final KparReader reader = new KparReader();

    /**
     * Create an empty KPAR library provider.
     */
    public KparLibraryProvider() {
    }

    /**
     * Create a provider and load symbols from a KPAR file.
     *
     * @param kparFile the KPAR file to load
     */
    public KparLibraryProvider(File kparFile) {
        loadKparFile(kparFile);
    }

    /**
     * Create a provider and load symbols from multiple KPAR files.
     *
     * @param kparFiles the KPAR files to load
     */
    public KparLibraryProvider(List<File> kparFiles) {
        for (File file : kparFiles) {
            loadKparFile(file);
        }
    }

    /**
     * Load symbols from a KPAR file.
     *
     * @param kparFile the KPAR file to load
     */
    public void loadKparFile(File kparFile) {
        try {
            KparReader.KparReadResult result = reader.read(kparFile);
            for (Symbol symbol : result.getSymbols()) {
                candidates.add(symbol.getName());
            }
        } catch (IOException e) {
            // Log and continue - don't fail on individual file errors
            System.err.println("Warning: Could not load KPAR file: "
                + kparFile.getName() + " - " + e.getMessage());
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
