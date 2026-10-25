package com.validator.suggestions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserDictionaryProvider combines a project and a user dictionary: words in either are known, close words are
 * suggested, and added words are saved to the dictionary they were added to.
 */
@DisplayName("User dictionary provider")
class UserDictionaryProviderTest {

    @TempDir
    Path dir;

    private UserDictionaryProvider provider;
    private Path projectFile;
    private Path userFile;

    @BeforeEach
    void setUp() throws IOException {
        projectFile = dir.resolve("project.dict");
        userFile = dir.resolve("user.dict");
        Files.writeString(projectFile, "# project terms\nVehicleECU\n\nVelocity\n");
        Files.writeString(userFile, "Torquemeter\n  Vehicle  \n");
        provider = new UserDictionaryProvider();
        provider.loadDictionaries(projectFile, userFile);
    }

    @Test
    @DisplayName("Words of either dictionary are known, ignoring case unless asked not to")
    void testIsInDictionary() {
        assertTrue(provider.isInDictionary("VehicleECU"));
        assertTrue(provider.isInDictionary("torquemeter"), "Lookups ignore case by default");
        assertTrue(provider.isInDictionary("Vehicle"), "Lines are trimmed");
        assertFalse(provider.isInDictionary("# project terms"), "Comment lines are not words");
        assertFalse(provider.isInDictionary(null));
        assertFalse(provider.isInDictionary(""));

        assertFalse(provider.isCaseSensitive());
        provider.setCaseSensitive(true);
        assertTrue(provider.isCaseSensitive());
        assertFalse(provider.isInDictionary("torquemeter"));
        assertTrue(provider.isInDictionary("Torquemeter"));
    }

    @Test
    @DisplayName("The provider offers the words of both dictionaries as candidates")
    void testCandidates() {
        Set<String> expected = Set.of("VehicleECU", "Velocity", "Torquemeter", "Vehicle");
        assertEquals(expected, provider.getAllWords());
        assertEquals(expected, provider.getCandidates());
        assertEquals(4, provider.getTotalWordCount());
        assertEquals(2, provider.getProjectDictionary().size());
        assertEquals(2, provider.getUserDictionary().size());
        assertEquals("user-dictionary", provider.getSourceName());
        assertEquals(4, provider.getPriority());
    }

    @Test
    @DisplayName("Suggestions are close words, closest first, without the word itself, at most five")
    void testSuggestions() {
        assertEquals(List.of("Vehicle"), provider.getSuggestions("Vehicel"));
        assertEquals("Vehicle", provider.getSuggestions("Vehicl").get(0), "The closest word comes first");
        assertTrue(provider.getSuggestions("Vehicle").stream().noneMatch("Vehicle"::equals),
            "A word is not its own suggestion");
        assertTrue(provider.getSuggestions("Xylophone").isEmpty());
        assertTrue(provider.getSuggestions(null).isEmpty());
        assertTrue(provider.getSuggestions("").isEmpty());

        for (String word : List.of("abcd", "abce", "abcf", "abcg", "abch", "abci", "abcj")) {
            provider.addToUserDictionary(word);
        }
        List<String> many = provider.getSuggestions("abcz");
        assertEquals(5, many.size(), "At most five suggestions: " + many);
    }

    @Test
    @DisplayName("Added words are saved to their own dictionary file")
    void testAddAndSave() throws Exception {
        assertTrue(provider.addToProjectDictionary("Gearbox"));
        assertFalse(provider.addToProjectDictionary("Gearbox"), "A word is added once");
        assertTrue(provider.addToUserDictionary("Flywheel"));
        provider.saveDictionaries();

        UserDictionaryProvider reloaded = new UserDictionaryProvider();
        reloaded.loadDictionaries(projectFile, userFile);
        assertTrue(reloaded.getProjectDictionary().contains("Gearbox"));
        assertFalse(reloaded.getUserDictionary().contains("Gearbox"));
        assertTrue(reloaded.getUserDictionary().contains("Flywheel"));
        assertTrue(reloaded.getProjectDictionary().contains("VehicleECU"), "Existing words are kept");
    }

    @Test
    @DisplayName("An unchanged dictionary is not written")
    void testSaveUnchanged() throws Exception {
        Files.delete(projectFile);
        Files.delete(userFile);
        provider.saveDictionaries();
        assertFalse(Files.exists(projectFile));
        assertFalse(Files.exists(userFile));
    }

    @Test
    @DisplayName("A dictionary without a file cannot be saved once changed")
    void testSaveWithoutFile() {
        UserDictionaryProvider fresh = new UserDictionaryProvider();
        assertTrue(fresh.addToProjectDictionary("Word"));
        assertThrows(IllegalStateException.class, fresh::saveProjectDictionary);
        assertTrue(fresh.addToUserDictionary("Other"));
        assertThrows(IllegalStateException.class, fresh::saveUserDictionary);
    }

    @Test
    @DisplayName("Missing and unreadable dictionary files give empty dictionaries")
    void testMissingAndUnreadableFiles() throws IOException {
        UserDictionaryProvider missing = new UserDictionaryProvider();
        missing.loadDictionaries(dir.resolve("none.dict"), dir.resolve("absent.dict"));
        assertEquals(0, missing.getTotalWordCount());

        Path directory = Files.createDirectory(dir.resolve("not-a-file"));
        UserDictionaryProvider unreadable = new UserDictionaryProvider();
        unreadable.loadDictionaries(directory, directory);
        assertEquals(0, unreadable.getTotalWordCount());

        UserDictionaryProvider unchanged = new UserDictionaryProvider();
        unchanged.addToUserDictionary("Kept");
        unchanged.loadDictionaries(null, null);
        assertTrue(unchanged.isInDictionary("Kept"), "Null paths leave the dictionaries as they are");
    }

    @Test
    @DisplayName("The default locations are the project directory and the user's configuration directory")
    void testDefaultLocations() throws IOException {
        Path project = Files.createDirectory(dir.resolve("project"));
        Files.writeString(project.resolve(UserDictionary.PROJECT_DICTIONARY_NAME), "ProjectWord\n");
        Path home = Files.createDirectory(dir.resolve("home"));
        Path userConfig = Files.createDirectories(home.resolve(UserDictionary.USER_CONFIG_DIR));
        Files.writeString(userConfig.resolve(UserDictionary.USER_DICTIONARY_NAME), "HomeWord\n");

        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
        try {
            UserDictionaryProvider defaults = new UserDictionaryProvider();
            defaults.loadDictionaries(project);
            assertEquals(Set.of("ProjectWord"), defaults.getProjectDictionary().getWords());
            assertEquals(Set.of("HomeWord"), defaults.getUserDictionary().getWords());

            UserDictionaryProvider userOnly = new UserDictionaryProvider();
            userOnly.loadDictionaries((Path) null);
            assertTrue(userOnly.getProjectDictionary().isEmpty());
            assertTrue(userOnly.isInDictionary("homeword"));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
