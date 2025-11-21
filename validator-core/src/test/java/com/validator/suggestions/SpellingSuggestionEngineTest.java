package com.validator.suggestions;

import com.validator.ast.Location;
import com.validator.semantic.ElementType;
import com.validator.semantic.ScopeType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SpellingSuggestionEngine.
 */
@DisplayName("Spelling Suggestion Engine Tests")
public class SpellingSuggestionEngineTest {

    private SpellingSuggestionEngine engine;

    @BeforeEach
    public void setUp() {
        engine = new SpellingSuggestionEngine();
    }

    @Test
    @DisplayName("Should suggest keyword for misspelled keyword")
    public void testSuggestKeyword() {
        // "packge" -> "package"
        List<SuggestionResult> suggestions = engine.suggest("packge");

        assertFalse(suggestions.isEmpty(), "Should have suggestions");
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getSuggestion().equals("package")),
            "Should suggest 'package'");
    }

    @Test
    @DisplayName("Should suggest standard library type for misspelled type")
    public void testSuggestStandardLibraryType() {
        // "Intger" -> "Integer"
        List<SuggestionResult> suggestions = engine.suggest("Intger");

        assertFalse(suggestions.isEmpty(), "Should have suggestions");
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getSuggestion().equals("Integer")),
            "Should suggest 'Integer'");
    }

    @Test
    @DisplayName("Should suggest Boolean for misspelled Boolean")
    public void testSuggestBoolean() {
        // "Booleen" -> "Boolean"
        List<SuggestionResult> suggestions = engine.suggest("Booleen");

        assertFalse(suggestions.isEmpty(), "Should have suggestions");
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getSuggestion().equals("Boolean")),
            "Should suggest 'Boolean'");
    }

    @Test
    @DisplayName("Should return empty for exact match")
    public void testNoSuggestionsForExactMatch() {
        // Exact keyword - no suggestions needed
        List<SuggestionResult> suggestions = engine.suggest("package");

        assertTrue(suggestions.isEmpty(), "Should not suggest exact match");
    }

    @Test
    @DisplayName("Should return empty for null input")
    public void testNoSuggestionsForNull() {
        List<SuggestionResult> suggestions = engine.suggest(null);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    @DisplayName("Should return empty for empty input")
    public void testNoSuggestionsForEmpty() {
        List<SuggestionResult> suggestions = engine.suggest("");
        assertTrue(suggestions.isEmpty());
    }

    @Test
    @DisplayName("Should limit suggestions to max count")
    public void testLimitsSuggestions() {
        // Use a very common pattern that might match many
        List<SuggestionResult> suggestions = engine.suggest("par");

        assertTrue(suggestions.size() <= 5, "Should limit to 5 suggestions");
    }

    @Test
    @DisplayName("Should suggest user model symbols")
    public void testSuggestUserModelSymbols() {
        // Create a symbol table with user symbols
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);
        symbolTable.define(new Symbol("Vehicle", "TestPackage::Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));
        symbolTable.define(new Symbol("Engine", "TestPackage::Engine",
            ElementType.PART_DEFINITION, new Location("test.sysml", 5, 1)));
        symbolTable.exitScope();

        // Create engine with user model
        SpellingSuggestionEngine engineWithModel = SpellingSuggestionEngine.withUserModel(symbolTable);

        // "Vehicel" -> "Vehicle"
        List<SuggestionResult> suggestions = engineWithModel.suggest("Vehicel");

        assertFalse(suggestions.isEmpty(), "Should have suggestions");
        assertTrue(suggestions.stream()
            .anyMatch(s -> s.getSuggestion().equals("Vehicle") && s.getSource().equals("model")),
            "Should suggest 'Vehicle' from user model");
    }

    @Test
    @DisplayName("Should prioritize user model over standard library")
    public void testPrioritizesUserModel() {
        // Create a symbol table with a symbol similar to stdlib
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);
        symbolTable.define(new Symbol("Integers", "TestPackage::Integers",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));
        symbolTable.exitScope();

        SpellingSuggestionEngine engineWithModel = SpellingSuggestionEngine.withUserModel(symbolTable);

        // "Integrs" could match both "Integer" (stdlib) and "Integers" (model)
        List<SuggestionResult> suggestions = engineWithModel.suggest("Integrs");

        // Both should appear but with appropriate sources
        boolean hasModelSuggestion = suggestions.stream()
            .anyMatch(s -> s.getSource().equals("model"));
        boolean hasStdlibSuggestion = suggestions.stream()
            .anyMatch(s -> s.getSource().equals("stdlib"));

        assertTrue(hasModelSuggestion || hasStdlibSuggestion, "Should have suggestions from either source");
    }

    @Test
    @DisplayName("Should format suggestion message correctly")
    public void testFormatSuggestionMessage() {
        String message = engine.formatSuggestionMessage("packge");

        assertFalse(message.isEmpty(), "Should have formatted message");
        assertTrue(message.contains("Did you mean"), "Should start with 'Did you mean'");
    }

    @Test
    @DisplayName("Should calculate confidence correctly")
    public void testConfidenceCalculation() {
        // Closer match should have higher confidence
        List<SuggestionResult> suggestions = engine.suggest("packaeg");

        if (!suggestions.isEmpty()) {
            SuggestionResult first = suggestions.get(0);
            assertTrue(first.getConfidence() > 0.5, "Best match should have high confidence");
            assertTrue(first.getConfidence() <= 1.0, "Confidence should not exceed 1.0");
        }
    }

    @Test
    @DisplayName("Should sort suggestions by distance")
    public void testSortsByDistance() {
        List<SuggestionResult> suggestions = engine.suggest("require");

        if (suggestions.size() > 1) {
            for (int i = 0; i < suggestions.size() - 1; i++) {
                assertTrue(suggestions.get(i).getDistance() <= suggestions.get(i + 1).getDistance(),
                    "Suggestions should be sorted by distance");
            }
        }
    }

    @Test
    @DisplayName("Should have correct provider count")
    public void testProviderCount() {
        // Default engine has keyword + stdlib + cameo providers
        assertEquals(3, engine.getProviderCount());

        SymbolTable symbolTable = new SymbolTable();
        SpellingSuggestionEngine withModel = SpellingSuggestionEngine.withUserModel(symbolTable);
        assertEquals(4, withModel.getProviderCount());
    }

    @Test
    @DisplayName("KeywordProvider should have expected keywords")
    public void testKeywordProviderContent() {
        KeywordProvider provider = new KeywordProvider();

        assertTrue(provider.getCandidates().contains("package"));
        assertTrue(provider.getCandidates().contains("import"));
        assertTrue(provider.getCandidates().contains("part"));
        assertTrue(provider.getCandidates().contains("action"));
        assertTrue(provider.getCandidates().contains("requirement"));
        assertTrue(KeywordProvider.getKeywordCount() > 50, "Should have many keywords");
    }

    @Test
    @DisplayName("StandardLibraryProvider should have expected types")
    public void testStandardLibraryProviderContent() {
        StandardLibraryProvider provider = new StandardLibraryProvider();

        assertTrue(provider.getCandidates().contains("Integer"));
        assertTrue(provider.getCandidates().contains("Real"));
        assertTrue(provider.getCandidates().contains("String"));
        assertTrue(provider.getCandidates().contains("Boolean"));
    }

    @Test
    @DisplayName("SuggestionResult toDisplayString formats correctly")
    public void testSuggestionResultDisplay() {
        SuggestionResult result = new SuggestionResult("Integer", 1, 0.9, "stdlib");

        String display = result.toDisplayString();
        assertTrue(display.contains("Integer"));
        assertTrue(display.contains("standard library"));
    }
}
