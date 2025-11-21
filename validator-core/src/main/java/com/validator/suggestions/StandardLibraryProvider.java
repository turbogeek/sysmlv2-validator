package com.validator.suggestions;

import com.validator.semantic.StandardLibraryManager;
import com.validator.semantic.Symbol;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides standard library symbols for spelling suggestions.
 * Wraps StandardLibraryManager to extract symbol names.
 */
public final class StandardLibraryProvider implements SuggestionProvider {

    private final StandardLibraryManager standardLibrary;
    private Set<String> cachedCandidates;

    /**
     * Creates a provider using the default StandardLibraryManager.
     */
    public StandardLibraryProvider() {
        this(new StandardLibraryManager());
    }

    /**
     * Creates a provider using a custom StandardLibraryManager.
     *
     * @param standardLibrary the standard library manager to use
     */
    public StandardLibraryProvider(StandardLibraryManager standardLibrary) {
        this.standardLibrary = standardLibrary;
    }

    @Override
    public Set<String> getCandidates() {
        if (cachedCandidates == null) {
            cachedCandidates = standardLibrary.getAllSymbols().stream()
                .map(Symbol::getName)
                .collect(Collectors.toUnmodifiableSet());
        }
        return cachedCandidates;
    }

    @Override
    public String getSourceName() {
        return "stdlib";
    }

    /**
     * Gets qualified name candidates for import suggestions.
     * Includes both simple names and qualified names.
     *
     * @return set of all symbol names including qualified names
     */
    public Set<String> getQualifiedCandidates() {
        return standardLibrary.getAllSymbols().stream()
            .flatMap(symbol -> {
                // Include both simple name and qualified name
                return java.util.stream.Stream.of(
                    symbol.getName(),
                    symbol.getQualifiedName()
                );
            })
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets package names for import suggestions.
     *
     * @return set of standard library package names
     */
    public Set<String> getPackageNames() {
        return Set.of("KerML", "SysML", "ISQ", "SI", "ScalarValues", "Collections",
            "SequenceFunctions", "ControlFunctions", "BaseFunctions");
    }

    /**
     * Invalidates the cached candidates to force refresh.
     * Call this if the standard library is modified.
     */
    public void invalidateCache() {
        cachedCandidates = null;
    }
}
