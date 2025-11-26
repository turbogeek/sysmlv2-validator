package com.validator.suggestions;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple spelling suggestion engine for keywords.
 * TODO: Enhance with better suggestion algorithms in future.
 */
public class SpellingSuggestionEngine {

    private static final List<String> SYSML_KEYWORDS = List.of(
        "package", "import", "part", "def", "attribute", "action", "state",
        "requirement", "constraint", "calc", "analysis", "verification",
        "view", "viewpoint", "rendering", "allocation", "connection",
        "binding", "flow", "interface", "use", "case", "concern"
    );

    /**
     * Get spelling suggestions for a token.
     */
    public List<String> suggestForError(String token) {
        if (token == null || token.length() < 2) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>();
        String lowerToken = token.toLowerCase();

        // Simple fuzzy matching - find keywords that start with similar letters
        for (String keyword : SYSML_KEYWORDS) {
            if (keyword.startsWith(lowerToken) || lowerToken.startsWith(keyword)) {
                suggestions.add(keyword);
            } else if (levenshteinDistance(lowerToken, keyword) <= 2) {
                suggestions.add(keyword);
            }
        }

        return suggestions;
    }

    /**
     * Simple Levenshtein distance for fuzzy matching.
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
}
