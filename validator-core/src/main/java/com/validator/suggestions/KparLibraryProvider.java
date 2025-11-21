package com.validator.suggestions;

import com.validator.library.KparReader;
import com.validator.library.KparReader.KparReadResult;
import com.validator.semantic.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides symbols from KPAR library archives for spelling suggestions.
 * Supports loading multiple KPAR files to build a comprehensive suggestion set.
 */
public class KparLibraryProvider implements SuggestionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(KparLibraryProvider.class);

    private final Set<String> candidates;
    private final String sourceName;

    /**
     * Creates an empty KPAR library provider.
     */
    public KparLibraryProvider() {
        this.candidates = new HashSet<>();
        this.sourceName = "kpar";
    }

    /**
     * Creates a provider from a single KPAR file.
     *
     * @param kparFile the KPAR file to load
     * @throws IOException if the file cannot be read
     */
    public KparLibraryProvider(File kparFile) throws IOException {
        this();
        loadKparFile(kparFile);
    }

    /**
     * Creates a provider from multiple KPAR files.
     *
     * @param kparFiles list of KPAR files to load
     * @throws IOException if any file cannot be read
     */
    public KparLibraryProvider(List<File> kparFiles) throws IOException {
        this();
        for (File kparFile : kparFiles) {
            loadKparFile(kparFile);
        }
    }

    /**
     * Loads symbols from a KPAR file.
     *
     * @param kparFile the KPAR file to load
     * @throws IOException if the file cannot be read
     */
    public void loadKparFile(File kparFile) throws IOException {
        KparReader reader = new KparReader();
        KparReadResult result = reader.read(kparFile);

        for (Symbol symbol : result.getSymbols()) {
            candidates.add(symbol.getName());
        }

        LOGGER.info("Loaded {} symbols from KPAR: {}",
            result.getSymbolCount(), kparFile.getName());
    }

    /**
     * Adds a symbol name directly to the candidate set.
     *
     * @param symbolName the symbol name to add
     */
    public void addCandidate(String symbolName) {
        candidates.add(symbolName);
    }

    /**
     * Adds multiple symbol names to the candidate set.
     *
     * @param symbolNames the symbol names to add
     */
    public void addCandidates(Set<String> symbolNames) {
        candidates.addAll(symbolNames);
    }

    @Override
    public Set<String> getCandidates() {
        return Set.copyOf(candidates);
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Gets the number of candidates loaded.
     *
     * @return candidate count
     */
    public int getCandidateCount() {
        return candidates.size();
    }

    /**
     * Clears all loaded candidates.
     */
    public void clear() {
        candidates.clear();
    }
}
