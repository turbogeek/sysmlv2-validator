package com.validator.robustness;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.ParseResult;
import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
import com.validator.suggestions.KeywordProvider;
import com.validator.suggestions.SpellingSuggestionEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Misspelled keywords, as the typo robustness harness ({@link TypoRobustnessTest}) makes them: one letter dropped,
 * doubled or swapped with the next at the start of a declaration. The grammar accepted some of them silently, and
 * where it did report an error the suggestions were for the wrong word.
 */
@DisplayName("Misspelled keywords")
class MisspelledKeywordTest {

    private final SysMLv2ParserFacade parser = new SysMLv2ParserFacade();

    private List<SyntaxError> errors(String source, String fileName) {
        return parser.parseString(source, fileName).getSyntaxErrors();
    }

    private List<String> allSuggestions(String source) {
        ParseResult result = parser.parseString(source, "typo.sysml");
        return result.getSyntaxErrors().stream().flatMap(e -> e.getSuggestions().stream()).toList();
    }

    @Test
    @DisplayName("Every keyword of the lexer is a spelling suggestion")
    void keywordProviderCoversTheLexer() {
        SpellingSuggestionEngine engine = SpellingSuggestionEngine.keywordsOnly();
        // 'actor' and 'occurrence' were missing from the hand-written list
        assertTrue(engine.suggest("actr").stream().anyMatch(s -> s.getSuggestion().equals("actor")));
        assertTrue(engine.suggest("occurence").stream().anyMatch(s -> s.getSuggestion().equals("occurrence")));
        for (String keyword : List.of("actor", "occurrence", "stakeholder", "objective", "variant", "rendering")) {
            assertTrue(new KeywordProvider().getCandidates().contains(keyword), keyword);
        }
    }

    @Test
    @DisplayName("A misspelled keyword the grammar accepts as a name is an error with the keyword suggested")
    void misspelledKeywordInBodyIsAnError() {
        String model = "package P {\n    part def V {\n        attriubte mass : Real;\n    }\n}\n";
        List<SyntaxError> errors = errors(model, "typo.sysml");
        // Before: no error at all; 'attriubte' parsed as an expression and 'mass : Real;' as a feature
        assertEquals(1, errors.size(), errors.toString());
        SyntaxError error = errors.get(0);
        assertEquals(3, error.getLine());
        assertEquals(18, error.getCharPositionInLine(), "at 'mass', like ANTLR's own missing-token errors");
        assertTrue(error.getMessage().startsWith("missing ';' at 'mass' after 'attriubte'"), error.getMessage());
        assertEquals("attribute", error.getSuggestions().get(0));

        String inIf = "action def A {\n    action b;\n    then if true {\n        acction c : B { }\n    }\n}\n";
        assertTrue(allSuggestions(inIf).contains("action"),
            "a body between braces in the middle of a rule (if, loop) is checked too");
    }

    @Test
    @DisplayName("A misspelled 'enum' in an enumeration body is an error, not a second value")
    void misspelledEnumIsAnError() {
        // Before: 'eunm Pending;' parsed as the two values 'eunm' and 'Pending'
        List<SyntaxError> errors = errors("enum def Status {\n    eunm Pending;\n    enum Active;\n}\n", "typo.sysml");
        assertEquals(1, errors.size(), errors.toString());
        assertEquals(2, errors.get(0).getLine());
        assertTrue(errors.get(0).getSuggestions().contains("enum"), errors.get(0).getSuggestions().toString());
        assertTrue(errors("enum def Color {\n    red;\n    green;\n    enum blue;\n}\n", "x.sysml").isEmpty());
    }

    @Test
    @DisplayName("A result expression at the end of a body is not mistaken for a misspelled keyword")
    void resultExpressionsStayValid() {
        assertTrue(errors("calc def Twice {\n    in x : Real;\n    x\n}\n", "x.sysml").isEmpty());
        assertTrue(errors("constraint def Positive {\n    in x : Real;\n    x\n}\n", "x.sysml").isEmpty());
        // A reserved word the lexer has no token for is a construct the grammar lacks, not a typo
        assertTrue(errors("assoc A {\n    const end a : B;\n    const end b : B;\n}\n", "x.kerml").stream()
            .noneMatch(e -> e.getMessage().contains("not a keyword")));
    }

    @Test
    @DisplayName("A syntax error after a misspelled keyword suggests the keyword")
    void syntaxErrorSuggestsMisspelledKeyword() {
        // The parser reports these at a later token, so the suggestions used to be for that token
        assertTrue(allSuggestions("packagge P {\n}\n").contains("package"));
        assertTrue(allSuggestions("package P {\n    prat def V;\n}\n").contains("part"));
        assertTrue(allSuggestions("package P {\n    folw f from a.b to c.d;\n}\n").contains("flow"));
    }
}
