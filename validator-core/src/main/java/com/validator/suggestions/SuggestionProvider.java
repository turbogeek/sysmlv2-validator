package com.validator.suggestions;

import java.util.Set;

/**
 * Interface for components that provide candidate symbols for spelling suggestions.
 * Implementations can provide keywords, standard library symbols, or user model symbols.
 */
public interface SuggestionProvider {

    /**
     * Returns the set of candidate strings that can be suggested.
     * The returned set should be efficient for iteration.
     *
     * @return set of candidate symbol names
     */
    Set<String> getCandidates();

    /**
     * Returns the name of this provider's source for display purposes.
     *
     * @return source name (e.g., "keyword", "stdlib", "model")
     */
    String getSourceName();

    /**
     * Returns the priority of this provider for ranking equal-distance suggestions.
     * Higher priority = more relevant suggestions.
     * Default: model (3) > stdlib (2) > keyword (1)
     *
     * @return priority value
     */
    default int getPriority() {
        return switch (getSourceName()) {
            case "model" -> 3;
            case "stdlib" -> 2;
            case "keyword" -> 1;
            default -> 0;
        };
    }
}
