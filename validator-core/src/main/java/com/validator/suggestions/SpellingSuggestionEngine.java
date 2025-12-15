package com.validator.suggestions;

import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhanced spelling suggestion engine that uses multiple providers
 * to suggest corrections for misspelled identifiers.
 */
public final class SpellingSuggestionEngine {

    private static final int MAX_SUGGESTIONS = 5;
    private static final int MAX_EDIT_DISTANCE = 3;

    private final List<SuggestionProvider> providers;

    /**
     * Create a default spelling suggestion engine with standard providers.
     */
    public SpellingSuggestionEngine() {
        this.providers = new ArrayList<>();
        providers.add(new KeywordProvider());
        providers.add(new StandardLibraryProvider());
        providers.add(new CameoLibraryProvider());
    }

    /**
     * Create a spelling suggestion engine with custom providers.
     *
     * @param providers the suggestion providers to use
     */
    private SpellingSuggestionEngine(List<SuggestionProvider> providers) {
        this.providers = new ArrayList<>(providers);
    }

    /**
     * Create an engine that includes symbols from a user model.
     *
     * @param symbolTable the user's symbol table
     * @return a new engine with user model support
     */
    public static SpellingSuggestionEngine withUserModel(SymbolTable symbolTable) {
        List<SuggestionProvider> providers = new ArrayList<>();
        providers.add(new KeywordProvider());
        providers.add(new StandardLibraryProvider());
        providers.add(new CameoLibraryProvider());
        providers.add(new UserModelProvider(symbolTable));
        return new SpellingSuggestionEngine(providers);
    }

    /**
     * Get spelling suggestions for a potentially misspelled identifier.
     *
     * @param input the input string to find suggestions for
     * @return list of suggestions sorted by relevance
     */
    public List<SuggestionResult> suggest(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        Set<SuggestionResult> allSuggestions = new HashSet<>();

        for (SuggestionProvider provider : providers) {
            for (String candidate : provider.getCandidates()) {
                // Skip exact matches - no suggestion needed
                if (candidate.equals(input)) {
                    return Collections.emptyList();
                }

                int distance = levenshteinDistance(input.toLowerCase(), candidate.toLowerCase());
                if (distance > 0 && distance <= MAX_EDIT_DISTANCE) {
                    double confidence = calculateConfidence(input, candidate, distance);
                    allSuggestions.add(new SuggestionResult(
                        candidate, distance, confidence, provider.getSourceName()));
                }
            }
        }

        // Sort and limit
        return allSuggestions.stream()
            .sorted()
            .limit(MAX_SUGGESTIONS)
            .collect(Collectors.toList());
    }

    /**
     * Get suggestions for error recovery (legacy method).
     *
     * @param token the misspelled token
     * @return list of suggested strings
     */
    public List<String> suggestForError(String token) {
        return suggest(token).stream()
            .map(SuggestionResult::getSuggestion)
            .collect(Collectors.toList());
    }

    /**
     * Format a suggestion message for display in error output.
     *
     * @param input the misspelled input
     * @return formatted suggestion message
     */
    public String formatSuggestionMessage(String input) {
        List<SuggestionResult> suggestions = suggest(input);
        if (suggestions.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("Did you mean: ");
        for (int i = 0; i < suggestions.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(suggestions.get(i).toDisplayString());
        }
        sb.append("?");
        return sb.toString();
    }

    /**
     * Get the number of suggestion providers.
     *
     * @return provider count
     */
    public int getProviderCount() {
        return providers.size();
    }

    private double calculateConfidence(String input, String candidate, int distance) {
        // Base confidence inversely proportional to distance
        double baseConfidence = 1.0 - (distance / (double) (Math.max(input.length(),
            candidate.length()) + 1));

        // Boost confidence for prefix matches
        if (candidate.toLowerCase().startsWith(input.toLowerCase())
                || input.toLowerCase().startsWith(candidate.toLowerCase())) {
            baseConfidence = Math.min(1.0, baseConfidence + 0.1);
        }

        // Boost confidence for same length
        if (input.length() == candidate.length()) {
            baseConfidence = Math.min(1.0, baseConfidence + 0.05);
        }

        return baseConfidence;
    }

    /**
     * Calculate Levenshtein edit distance between two strings.
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Provider that supplies symbols from the user's model.
     */
    private static class UserModelProvider implements SuggestionProvider {
        private final Set<String> candidates;

        UserModelProvider(SymbolTable symbolTable) {
            this.candidates = new HashSet<>();
            if (symbolTable != null) {
                for (Symbol symbol : symbolTable.getAllSymbols()) {
                    candidates.add(symbol.getName());
                }
            }
        }

        @Override
        public Set<String> getCandidates() {
            return candidates;
        }

        @Override
        public String getSourceName() {
            return "model";
        }

        @Override
        public int getPriority() {
            return 4; // Higher than stdlib
        }
    }
}
