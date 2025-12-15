package com.validator.suggestions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages a user-defined dictionary for project-specific terms.
 *
 * <p>The user dictionary allows users to add custom terms that should not
 * trigger spelling warnings. This is useful for:
 * <ul>
 *   <li>Domain-specific terminology (e.g., "ECU", "CAN", "LIN")</li>
 *   <li>Project-specific names (e.g., "VehicleECU", "BrakeActuator")</li>
 *   <li>Acronyms and abbreviations</li>
 * </ul>
 *
 * <h2>Dictionary File Format</h2>
 * <p>The dictionary file is a plain text file with one word per line:
 * <pre>
 * # Project-specific terms
 * VehicleECU
 * BrakeActuator
 * ThrottleController
 * # Acronyms
 * ECU
 * CAN
 * LIN
 * </pre>
 * <p>Lines starting with # are treated as comments.
 *
 * <h2>Dictionary Locations</h2>
 * <p>Dictionaries can be stored at:
 * <ul>
 *   <li>Project level: {@code .sysml-dictionary} in project root</li>
 *   <li>User level: {@code ~/.sysml-validator/dictionary}</li>
 * </ul>
 */
public class UserDictionary {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDictionary.class);

    /**
     * Default dictionary file name for project-level dictionaries.
     */
    public static final String PROJECT_DICTIONARY_NAME = ".sysml-dictionary";

    /**
     * Default dictionary file name for user-level dictionaries.
     */
    public static final String USER_DICTIONARY_NAME = "dictionary";

    /**
     * User config directory name.
     */
    public static final String USER_CONFIG_DIR = ".sysml-validator";

    private final Set<String> words;
    private final Path dictionaryPath;
    private boolean modified;

    /**
     * Creates an empty user dictionary.
     */
    public UserDictionary() {
        this.words = new LinkedHashSet<>();
        this.dictionaryPath = null;
        this.modified = false;
    }

    /**
     * Creates a user dictionary associated with a file path.
     *
     * @param dictionaryPath the path to the dictionary file
     */
    public UserDictionary(Path dictionaryPath) {
        this.words = new LinkedHashSet<>();
        this.dictionaryPath = dictionaryPath;
        this.modified = false;
    }

    /**
     * Load a dictionary from a file.
     *
     * @param path the path to the dictionary file
     * @return the loaded dictionary
     * @throws IOException if reading fails
     */
    public static UserDictionary load(Path path) throws IOException {
        UserDictionary dictionary = new UserDictionary(path);

        if (!Files.exists(path)) {
            LOGGER.debug("Dictionary file does not exist: {}", path);
            return dictionary;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                dictionary.words.add(line);
            }
        }

        LOGGER.info("Loaded {} words from dictionary: {}", dictionary.words.size(), path);
        return dictionary;
    }

    /**
     * Load the project dictionary from the current directory.
     *
     * @return the project dictionary, or an empty dictionary if not found
     */
    public static UserDictionary loadProjectDictionary() {
        return loadProjectDictionary(Paths.get("."));
    }

    /**
     * Load the project dictionary from a specified directory.
     *
     * @param projectDir the project directory
     * @return the project dictionary, or an empty dictionary if not found
     */
    public static UserDictionary loadProjectDictionary(Path projectDir) {
        Path dictPath = projectDir.resolve(PROJECT_DICTIONARY_NAME);
        try {
            return load(dictPath);
        } catch (IOException e) {
            LOGGER.warn("Failed to load project dictionary: {}", e.getMessage());
            return new UserDictionary(dictPath);
        }
    }

    /**
     * Load the user-level dictionary.
     *
     * @return the user dictionary, or an empty dictionary if not found
     */
    public static UserDictionary loadUserDictionary() {
        Path userConfigDir = getUserConfigDir();
        Path dictPath = userConfigDir.resolve(USER_DICTIONARY_NAME);
        try {
            return load(dictPath);
        } catch (IOException e) {
            LOGGER.warn("Failed to load user dictionary: {}", e.getMessage());
            return new UserDictionary(dictPath);
        }
    }

    /**
     * Get the user configuration directory.
     *
     * @return the path to the user config directory
     */
    public static Path getUserConfigDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, USER_CONFIG_DIR);
    }

    /**
     * Save the dictionary to its associated file.
     *
     * @throws IOException if writing fails
     * @throws IllegalStateException if no file path is associated
     */
    public void save() throws IOException {
        if (dictionaryPath == null) {
            throw new IllegalStateException("No dictionary path associated");
        }
        save(dictionaryPath);
    }

    /**
     * Save the dictionary to a specified file.
     *
     * @param path the path to save to
     * @throws IOException if writing fails
     */
    public void save(Path path) throws IOException {
        // Ensure parent directory exists
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("# SysML v2 User Dictionary");
            writer.newLine();
            writer.write("# Add project-specific terms, one per line");
            writer.newLine();
            writer.newLine();

            for (String word : words) {
                writer.write(word);
                writer.newLine();
            }
        }

        modified = false;
        LOGGER.info("Saved {} words to dictionary: {}", words.size(), path);
    }

    /**
     * Add a word to the dictionary.
     *
     * @param word the word to add
     * @return true if the word was added (not already present)
     */
    public boolean addWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        String trimmed = word.trim();
        boolean added = words.add(trimmed);
        if (added) {
            modified = true;
            LOGGER.debug("Added word to dictionary: {}", trimmed);
        }
        return added;
    }

    /**
     * Remove a word from the dictionary.
     *
     * @param word the word to remove
     * @return true if the word was removed
     */
    public boolean removeWord(String word) {
        if (word == null) {
            return false;
        }
        boolean removed = words.remove(word.trim());
        if (removed) {
            modified = true;
            LOGGER.debug("Removed word from dictionary: {}", word);
        }
        return removed;
    }

    /**
     * Check if the dictionary contains a word.
     *
     * @param word the word to check
     * @return true if the dictionary contains the word
     */
    public boolean contains(String word) {
        if (word == null) {
            return false;
        }
        return words.contains(word.trim());
    }

    /**
     * Check if the dictionary contains a word (case-insensitive).
     *
     * @param word the word to check
     * @return true if the dictionary contains the word
     */
    public boolean containsIgnoreCase(String word) {
        if (word == null) {
            return false;
        }
        String lowerWord = word.trim().toLowerCase();
        return words.stream()
            .anyMatch(w -> w.toLowerCase().equals(lowerWord));
    }

    /**
     * Get all words in the dictionary.
     *
     * @return unmodifiable set of words
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(words);
    }

    /**
     * Get the number of words in the dictionary.
     *
     * @return word count
     */
    public int size() {
        return words.size();
    }

    /**
     * Check if the dictionary is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return words.isEmpty();
    }

    /**
     * Check if the dictionary has been modified since loading.
     *
     * @return true if modified
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Get the associated dictionary file path.
     *
     * @return the path, or null if not associated with a file
     */
    public Path getDictionaryPath() {
        return dictionaryPath;
    }

    /**
     * Clear all words from the dictionary.
     */
    public void clear() {
        if (!words.isEmpty()) {
            words.clear();
            modified = true;
        }
    }

    /**
     * Merge another dictionary into this one.
     *
     * @param other the dictionary to merge
     * @return number of new words added
     */
    public int merge(UserDictionary other) {
        if (other == null) {
            return 0;
        }
        int added = 0;
        for (String word : other.words) {
            if (words.add(word)) {
                added++;
            }
        }
        if (added > 0) {
            modified = true;
        }
        return added;
    }
}
