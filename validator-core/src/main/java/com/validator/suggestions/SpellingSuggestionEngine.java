package com.validator.suggestions;

import com.validator.semantic.ElementType;
import com.validator.semantic.SymbolTable;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Main engine for generating spelling suggestions using Levenshtein distance.
 * Combines multiple providers (keywords, standard library, user model) to find
 * the best matches for misspelled identifiers.
 */
public final class SpellingSuggestionEngine {

    private static final int DEFAULT_MAX_DISTANCE = 3;
    private static final int DEFAULT_MAX_SUGGESTIONS = 5;

    private final List<SuggestionProvider> providers;
    private final LevenshteinDistance levenshtein;
    private final int maxSuggestions;

    /**
     * Creates an engine with default settings (keywords and standard library).
     */
    public SpellingSuggestionEngine() {
        this(List.of(new KeywordProvider(), new StandardLibraryProvider()), DEFAULT_MAX_SUGGESTIONS);
    }

    /**
     * Creates an engine with custom providers.
     *
     * @param providers list of suggestion providers
     * @param maxSuggestions maximum number of suggestions to return
     */
    public SpellingSuggestionEngine(List<SuggestionProvider> providers, int maxSuggestions) {
        this.providers = new ArrayList<>(providers);
        this.levenshtein = LevenshteinDistance.getDefaultInstance();
        this.maxSuggestions = maxSuggestions;
    }

    /**
     * Creates an engine with all providers including user model.
     *
     * @param symbolTable the user's symbol table
     * @return configured engine
     */
    public static SpellingSuggestionEngine withUserModel(SymbolTable symbolTable) {
        return new SpellingSuggestionEngine(
            List.of(
                new KeywordProvider(),
                new StandardLibraryProvider(),
                new UserModelProvider(symbolTable)
            ),
            DEFAULT_MAX_SUGGESTIONS
        );
    }

    /**
     * Creates an engine optimized for type suggestions.
     *
     * @param symbolTable the user's symbol table
     * @return engine with definition-focused providers
     */
    public static SpellingSuggestionEngine forTypes(SymbolTable symbolTable) {
        return new SpellingSuggestionEngine(
            List.of(
                new StandardLibraryProvider(),
                UserModelProvider.forDefinitions(symbolTable)
            ),
            DEFAULT_MAX_SUGGESTIONS
        );
    }

    /**
     * Adds a provider to this engine.
     *
     * @param provider the provider to add
     */
    public void addProvider(SuggestionProvider provider) {
        providers.add(provider);
    }

    /**
     * Generates spelling suggestions for a misspelled identifier.
     *
     * @param misspelled the misspelled identifier
     * @return list of suggestions sorted by relevance
     */
    public List<SuggestionResult> suggest(String misspelled) {
        if (misspelled == null || misspelled.isEmpty()) {
            return Collections.emptyList();
        }

        int maxDistance = calculateMaxDistance(misspelled.length());
        List<SuggestionResult> results = new ArrayList<>();

        for (SuggestionProvider provider : providers) {
            Set<String> candidates = provider.getCandidates();
            String sourceName = provider.getSourceName();

            for (String candidate : candidates) {
                int distance = levenshtein.apply(misspelled.toLowerCase(), candidate.toLowerCase());

                if (distance <= maxDistance && distance > 0) {
                    double confidence = calculateConfidence(misspelled, candidate, distance);
                    results.add(new SuggestionResult(candidate, distance, confidence, sourceName));
                }
            }
        }

        // Sort by relevance and limit results
        Collections.sort(results);
        return results.stream()
            .limit(maxSuggestions)
            .toList();
    }

    /**
     * Generates suggestions with context filtering for specific element types.
     *
     * @param misspelled the misspelled identifier
     * @param contextType the expected element type context (for filtering)
     * @return list of context-aware suggestions
     */
    public List<SuggestionResult> suggestForContext(String misspelled, ElementType contextType) {
        // For now, use the same logic. Future: filter by context type.
        return suggest(misspelled);
    }

    /**
     * Generates a formatted list of suggestion strings for error messages.
     *
     * @param misspelled the misspelled identifier
     * @return list of formatted suggestions like "'Integer' (standard library)"
     */
    public List<String> suggestForError(String misspelled) {
        return suggest(misspelled).stream()
            .map(SuggestionResult::toDisplayString)
            .toList();
    }

    /**
     * Formats suggestions as a "Did you mean..." message.
     *
     * @param misspelled the misspelled identifier
     * @return formatted message or empty string if no suggestions
     */
    public String formatSuggestionMessage(String misspelled) {
        List<SuggestionResult> suggestions = suggest(misspelled);

        if (suggestions.isEmpty()) {
            return "";
        }

        if (suggestions.size() == 1) {
            return "Did you mean " + suggestions.get(0).toDisplayString() + "?";
        }

        StringBuilder sb = new StringBuilder("Did you mean one of:\n");
        for (SuggestionResult suggestion : suggestions) {
            sb.append("  - ").append(suggestion.toDisplayString()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Calculates the maximum allowed Levenshtein distance based on token length.
     * Shorter tokens require closer matches to avoid noise.
     */
    private int calculateMaxDistance(int length) {
        if (length <= 3) {
            return 1;
        } else if (length <= 6) {
            return 2;
        } else {
            return DEFAULT_MAX_DISTANCE;
        }
    }

    /**
     * Calculates a confidence score for a suggestion.
     * Higher is better (0.0 to 1.0).
     */
    private double calculateConfidence(String misspelled, String candidate, int distance) {
        int maxLength = Math.max(misspelled.length(), candidate.length());
        double baseConfidence = 1.0 - ((double) distance / maxLength);

        // Bonus for case-insensitive prefix match
        String lowerMisspelled = misspelled.toLowerCase();
        String lowerCandidate = candidate.toLowerCase();

        if (lowerCandidate.startsWith(lowerMisspelled) || lowerMisspelled.startsWith(lowerCandidate)) {
            baseConfidence = Math.min(1.0, baseConfidence + 0.1);
        }

        // Bonus for same length
        if (misspelled.length() == candidate.length()) {
            baseConfidence = Math.min(1.0, baseConfidence + 0.05);
        }

        return baseConfidence;
    }

    /**
     * Returns the number of providers in this engine.
     *
     * @return provider count
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Returns the total number of candidates across all providers.
     *
     * @return total candidate count
     */
    public int getTotalCandidateCount() {
        return providers.stream()
            .mapToInt(p -> p.getCandidates().size())
            .sum();
    }
}
