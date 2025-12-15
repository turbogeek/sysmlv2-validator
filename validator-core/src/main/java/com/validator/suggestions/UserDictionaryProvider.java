package com.validator.suggestions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Suggestion provider that uses user-defined dictionaries.
 *
 * <p>This provider integrates user dictionaries with the spelling suggestion
 * system. It can:
 * <ul>
 *   <li>Provide suggestions from user-defined terms</li>
 *   <li>Skip warnings for words in the user dictionary</li>
 *   <li>Combine project and user-level dictionaries</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * UserDictionaryProvider provider = new UserDictionaryProvider();
 * provider.loadDictionaries(projectDir);
 *
 * // Check if a word should be skipped
 * if (provider.isInDictionary("VehicleECU")) {
 *     // Don't generate spelling warning
 * }
 *
 * // Get suggestions for a misspelled word
 * List&lt;String&gt; suggestions = provider.getSuggestions("vehicl");
 * </pre>
 */
public class UserDictionaryProvider implements SuggestionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDictionaryProvider.class);

    private UserDictionary projectDictionary;
    private UserDictionary userDictionary;
    private boolean caseSensitive;

    /**
     * Creates a new user dictionary provider.
     */
    public UserDictionaryProvider() {
        this.projectDictionary = new UserDictionary();
        this.userDictionary = new UserDictionary();
        this.caseSensitive = false;
    }

    /**
     * Load dictionaries from default locations.
     *
     * @param projectDir the project directory (for project dictionary)
     */
    public void loadDictionaries(Path projectDir) {
        // Load project dictionary
        if (projectDir != null) {
            projectDictionary = UserDictionary.loadProjectDictionary(projectDir);
        }

        // Load user dictionary
        userDictionary = UserDictionary.loadUserDictionary();

        LOGGER.info("Loaded dictionaries: {} project words, {} user words",
            projectDictionary.size(), userDictionary.size());
    }

    /**
     * Load dictionaries from specific paths.
     *
     * @param projectDictPath path to project dictionary (may be null)
     * @param userDictPath path to user dictionary (may be null)
     */
    public void loadDictionaries(Path projectDictPath, Path userDictPath) {
        if (projectDictPath != null) {
            try {
                projectDictionary = UserDictionary.load(projectDictPath);
            } catch (Exception e) {
                LOGGER.warn("Failed to load project dictionary: {}", e.getMessage());
                projectDictionary = new UserDictionary();
            }
        }

        if (userDictPath != null) {
            try {
                userDictionary = UserDictionary.load(userDictPath);
            } catch (Exception e) {
                LOGGER.warn("Failed to load user dictionary: {}", e.getMessage());
                userDictionary = new UserDictionary();
            }
        }
    }

    /**
     * Check if a word is in either dictionary.
     *
     * @param word the word to check
     * @return true if the word is in a dictionary
     */
    public boolean isInDictionary(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }

        if (caseSensitive) {
            return projectDictionary.contains(word)
                || userDictionary.contains(word);
        } else {
            return projectDictionary.containsIgnoreCase(word)
                || userDictionary.containsIgnoreCase(word);
        }
    }

    @Override
    public Set<String> getCandidates() {
        return getAllWords();
    }

    @Override
    public String getSourceName() {
        return "user-dictionary";
    }

    @Override
    public int getPriority() {
        // User dictionary has highest priority (user knows best)
        return 4;
    }

    /**
     * Get suggestions for a misspelled word.
     *
     * @param word the misspelled word
     * @return list of suggestions sorted by similarity
     */
    public List<String> getSuggestions(String word) {
        if (word == null || word.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>();

        // Get all dictionary words
        Set<String> allWords = getAllWords();

        // Find similar words using Levenshtein distance
        for (String dictWord : allWords) {
            int distance = levenshteinDistance(word.toLowerCase(), dictWord.toLowerCase());
            // Allow words within edit distance of 2 or 30% of word length
            int threshold = Math.max(2, word.length() / 3);
            if (distance <= threshold && distance > 0) {
                suggestions.add(dictWord);
            }
        }

        // Sort by similarity (closest matches first)
        suggestions.sort((a, b) -> {
            int distA = levenshteinDistance(word.toLowerCase(), a.toLowerCase());
            int distB = levenshteinDistance(word.toLowerCase(), b.toLowerCase());
            return Integer.compare(distA, distB);
        });

        // Limit to top 5 suggestions
        if (suggestions.size() > 5) {
            suggestions = suggestions.subList(0, 5);
        }

        return suggestions;
    }

    /**
     * Add a word to the project dictionary.
     *
     * @param word the word to add
     * @return true if added successfully
     */
    public boolean addToProjectDictionary(String word) {
        return projectDictionary.addWord(word);
    }

    /**
     * Add a word to the user dictionary.
     *
     * @param word the word to add
     * @return true if added successfully
     */
    public boolean addToUserDictionary(String word) {
        return userDictionary.addWord(word);
    }

    /**
     * Save the project dictionary.
     *
     * @throws Exception if saving fails
     */
    public void saveProjectDictionary() throws Exception {
        if (projectDictionary.isModified()) {
            projectDictionary.save();
        }
    }

    /**
     * Save the user dictionary.
     *
     * @throws Exception if saving fails
     */
    public void saveUserDictionary() throws Exception {
        if (userDictionary.isModified()) {
            userDictionary.save();
        }
    }

    /**
     * Save both dictionaries if modified.
     *
     * @throws Exception if saving fails
     */
    public void saveDictionaries() throws Exception {
        saveProjectDictionary();
        saveUserDictionary();
    }

    /**
     * Get all words from both dictionaries.
     *
     * @return combined set of words
     */
    public Set<String> getAllWords() {
        Set<String> allWords = new java.util.HashSet<>();
        allWords.addAll(projectDictionary.getWords());
        allWords.addAll(userDictionary.getWords());
        return allWords;
    }

    /**
     * Get the project dictionary.
     *
     * @return project dictionary
     */
    public UserDictionary getProjectDictionary() {
        return projectDictionary;
    }

    /**
     * Get the user dictionary.
     *
     * @return user dictionary
     */
    public UserDictionary getUserDictionary() {
        return userDictionary;
    }

    /**
     * Set whether dictionary lookups are case-sensitive.
     *
     * @param caseSensitive true for case-sensitive matching
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Check if dictionary lookups are case-sensitive.
     *
     * @return true if case-sensitive
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Calculate Levenshtein distance between two strings.
     *
     * @param s1 first string
     * @param s2 second string
     * @return edit distance
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
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Get total word count from all dictionaries.
     *
     * @return total word count
     */
    public int getTotalWordCount() {
        return getAllWords().size();
    }
}
