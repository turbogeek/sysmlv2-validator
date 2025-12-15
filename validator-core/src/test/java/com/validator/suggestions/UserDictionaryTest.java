package com.validator.suggestions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserDictionary.
 */
@DisplayName("User Dictionary Tests")
public class UserDictionaryTest {

    private UserDictionary dictionary;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        dictionary = new UserDictionary();
    }

    @Test
    @DisplayName("New dictionary is empty")
    public void testNewDictionaryEmpty() {
        assertTrue(dictionary.getWords().isEmpty());
        assertEquals(0, dictionary.size());
    }

    @Test
    @DisplayName("Can add word to dictionary")
    public void testAddWord() {
        dictionary.addWord("Vehicle");

        assertTrue(dictionary.contains("Vehicle"));
        assertEquals(1, dictionary.size());
    }

    @Test
    @DisplayName("Contains is case-sensitive by default")
    public void testContainsCaseSensitive() {
        dictionary.addWord("Vehicle");

        assertTrue(dictionary.contains("Vehicle"));
        assertFalse(dictionary.contains("vehicle"));
    }

    @Test
    @DisplayName("containsIgnoreCase is case-insensitive")
    public void testContainsIgnoreCase() {
        dictionary.addWord("Vehicle");

        assertTrue(dictionary.containsIgnoreCase("vehicle"));
        assertTrue(dictionary.containsIgnoreCase("VEHICLE"));
        assertTrue(dictionary.containsIgnoreCase("VeHiClE"));
    }

    @Test
    @DisplayName("Duplicate words are not added")
    public void testNoDuplicates() {
        dictionary.addWord("Vehicle");
        dictionary.addWord("Vehicle");

        assertEquals(1, dictionary.size());
    }

    @Test
    @DisplayName("Can remove word from dictionary")
    public void testRemoveWord() {
        dictionary.addWord("Vehicle");
        assertTrue(dictionary.contains("Vehicle"));

        dictionary.removeWord("Vehicle");

        assertFalse(dictionary.contains("Vehicle"));
        assertEquals(0, dictionary.size());
    }

    @Test
    @DisplayName("Adding null word returns false")
    public void testAddNullWord() {
        boolean result = dictionary.addWord(null);

        assertFalse(result);
        assertEquals(0, dictionary.size());
    }

    @Test
    @DisplayName("Adding empty word returns false")
    public void testAddEmptyWord() {
        boolean result1 = dictionary.addWord("");
        boolean result2 = dictionary.addWord("   ");

        assertFalse(result1);
        assertFalse(result2);
        assertEquals(0, dictionary.size());
    }

    @Test
    @DisplayName("Words are trimmed when added")
    public void testWordsTrimmed() {
        dictionary.addWord("  Vehicle  ");

        assertTrue(dictionary.contains("Vehicle"));
    }

    @Test
    @DisplayName("getWords returns unmodifiable copy")
    public void testGetWordsUnmodifiable() {
        dictionary.addWord("Vehicle");
        Set<String> words = dictionary.getWords();

        assertThrows(UnsupportedOperationException.class, () -> words.add("Engine"));
    }

    @Test
    @DisplayName("Can save and load dictionary")
    public void testSaveAndLoad() throws IOException {
        Path dictFile = tempDir.resolve("test.dict");

        dictionary.addWord("Vehicle");
        dictionary.addWord("Engine");
        dictionary.addWord("Wheel");
        dictionary.save(dictFile);

        UserDictionary loaded = UserDictionary.load(dictFile);

        assertTrue(loaded.contains("Vehicle"));
        assertTrue(loaded.contains("Engine"));
        assertTrue(loaded.contains("Wheel"));
        assertEquals(3, loaded.size());
    }

    @Test
    @DisplayName("Load ignores comment lines")
    public void testLoadIgnoresComments() throws IOException {
        Path dictFile = tempDir.resolve("test.dict");
        Files.writeString(dictFile, """
            # This is a comment
            Vehicle
            # Another comment
            Engine
            """);

        UserDictionary loaded = UserDictionary.load(dictFile);

        assertTrue(loaded.contains("Vehicle"));
        assertTrue(loaded.contains("Engine"));
        assertFalse(loaded.contains("# This is a comment"));
        assertEquals(2, loaded.size());
    }

    @Test
    @DisplayName("Load ignores empty lines")
    public void testLoadIgnoresEmptyLines() throws IOException {
        Path dictFile = tempDir.resolve("test.dict");
        Files.writeString(dictFile, """
            Vehicle

            Engine

            Wheel
            """);

        UserDictionary loaded = UserDictionary.load(dictFile);

        assertEquals(3, loaded.size());
    }

    @Test
    @DisplayName("Load handles non-existent file gracefully")
    public void testLoadNonExistent() throws IOException {
        Path nonExistent = tempDir.resolve("nonexistent.dict");

        UserDictionary loaded = UserDictionary.load(nonExistent);

        assertEquals(0, loaded.size());
    }

    @Test
    @DisplayName("Save creates parent directories")
    public void testSaveCreatesDirectories() throws IOException {
        Path nestedPath = tempDir.resolve("sub/dir/test.dict");

        dictionary.addWord("Vehicle");
        dictionary.save(nestedPath);

        assertTrue(Files.exists(nestedPath));
    }

    @Test
    @DisplayName("Clear removes all words")
    public void testClear() {
        dictionary.addWord("Vehicle");
        dictionary.addWord("Engine");
        assertEquals(2, dictionary.size());

        dictionary.clear();

        assertEquals(0, dictionary.size());
        assertTrue(dictionary.getWords().isEmpty());
    }

    @Test
    @DisplayName("Dictionary with path saves to that path")
    public void testDefaultSavePath() throws IOException {
        Path dictFile = tempDir.resolve("default.dict");
        UserDictionary dictWithPath = new UserDictionary(dictFile);

        dictWithPath.addWord("Vehicle");
        dictWithPath.save();

        assertTrue(Files.exists(dictFile));

        UserDictionary loaded = UserDictionary.load(dictFile);
        assertTrue(loaded.contains("Vehicle"));
    }

    @Test
    @DisplayName("Save without path throws exception")
    public void testSaveWithoutPathThrows() {
        assertThrows(IllegalStateException.class, () -> dictionary.save());
    }

    @Test
    @DisplayName("Contains returns false for null")
    public void testContainsNull() {
        assertFalse(dictionary.contains(null));
    }

    @Test
    @DisplayName("Contains returns false for empty string")
    public void testContainsEmpty() {
        assertFalse(dictionary.contains(""));
    }

    @Test
    @DisplayName("Special characters are preserved")
    public void testSpecialCharacters() {
        dictionary.addWord("ECU-Controller");
        dictionary.addWord("CAN_Bus");
        dictionary.addWord("ISO26262");

        assertTrue(dictionary.contains("ECU-Controller"));
        assertTrue(dictionary.contains("CAN_Bus"));
        assertTrue(dictionary.contains("ISO26262"));
    }

    @Test
    @DisplayName("Unicode words are supported")
    public void testUnicodeWords() {
        dictionary.addWord("Véhicule");
        dictionary.addWord("車両");
        dictionary.addWord("Fahrzeug");

        assertTrue(dictionary.contains("Véhicule"));
        assertTrue(dictionary.contains("車両"));
        assertTrue(dictionary.contains("Fahrzeug"));
    }

    @Test
    @DisplayName("Merge adds words from another dictionary")
    public void testMerge() {
        dictionary.addWord("Vehicle");

        UserDictionary other = new UserDictionary();
        other.addWord("Engine");
        other.addWord("Wheel");

        int added = dictionary.merge(other);

        assertEquals(2, added);
        assertEquals(3, dictionary.size());
        assertTrue(dictionary.contains("Vehicle"));
        assertTrue(dictionary.contains("Engine"));
        assertTrue(dictionary.contains("Wheel"));
    }

    @Test
    @DisplayName("Merge with null returns 0")
    public void testMergeNull() {
        dictionary.addWord("Vehicle");

        int added = dictionary.merge(null);

        assertEquals(0, added);
        assertEquals(1, dictionary.size());
    }

    @Test
    @DisplayName("Dictionary tracks modified state")
    public void testModifiedState() {
        assertFalse(dictionary.isModified());

        dictionary.addWord("Vehicle");
        assertTrue(dictionary.isModified());
    }

    @Test
    @DisplayName("Save resets modified state")
    public void testSaveResetsModified() throws IOException {
        Path dictFile = tempDir.resolve("test.dict");
        dictionary = new UserDictionary(dictFile);

        dictionary.addWord("Vehicle");
        assertTrue(dictionary.isModified());

        dictionary.save();
        assertFalse(dictionary.isModified());
    }

    @Test
    @DisplayName("isEmpty returns correct value")
    public void testIsEmpty() {
        assertTrue(dictionary.isEmpty());

        dictionary.addWord("Vehicle");
        assertFalse(dictionary.isEmpty());

        dictionary.clear();
        assertTrue(dictionary.isEmpty());
    }

    @Test
    @DisplayName("getDictionaryPath returns associated path")
    public void testGetDictionaryPath() {
        assertNull(dictionary.getDictionaryPath());

        Path path = Path.of("test.dict");
        UserDictionary dictWithPath = new UserDictionary(path);
        assertEquals(path, dictWithPath.getDictionaryPath());
    }

    @Test
    @DisplayName("addWord returns true for new word")
    public void testAddWordReturnValue() {
        assertTrue(dictionary.addWord("Vehicle"));
        assertFalse(dictionary.addWord("Vehicle"));  // Duplicate
    }

    @Test
    @DisplayName("removeWord returns true when word exists")
    public void testRemoveWordReturnValue() {
        dictionary.addWord("Vehicle");

        assertTrue(dictionary.removeWord("Vehicle"));
        assertFalse(dictionary.removeWord("Vehicle"));  // Already removed
    }

    @Test
    @DisplayName("loadProjectDictionary works with path")
    public void testLoadProjectDictionary() throws IOException {
        Path dictFile = tempDir.resolve(UserDictionary.PROJECT_DICTIONARY_NAME);
        Files.writeString(dictFile, "ProjectTerm\n");

        UserDictionary loaded = UserDictionary.loadProjectDictionary(tempDir);

        assertTrue(loaded.contains("ProjectTerm"));
    }
}
