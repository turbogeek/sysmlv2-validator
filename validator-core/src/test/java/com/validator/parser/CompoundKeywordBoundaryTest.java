package com.validator.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compound keywords end at a word boundary. The lexer has one token for each two-word keyword ('attribute def',
 * 'enum def', 'use case', 'typed by', ...), and ANTLR's longest match used to take the token even when the second
 * word was only the start of a name: 'attribute defaultForm' became 'attribute def' + 'aultForm', and an enum
 * literal named 'definition' became 'enum def' + 'inition'. Both are valid SysML v2 (a usage may be named
 * anything that is not a reserved word), so the compound token may only match when the character after it cannot
 * continue an identifier.
 */
@DisplayName("Compound keyword boundary tests")
class CompoundKeywordBoundaryTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    private SysMLv2ParserFacade.ParseResult parse(String text) {
        return parserFacade.parseString(text, "compound-keywords.sysml");
    }

    private static List<String> tokenNames(String text) {
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(text));
        List<String> names = new ArrayList<>();
        for (Token t = lexer.nextToken(); t.getType() != Token.EOF; t = lexer.nextToken()) {
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                names.add(SysMLv2Lexer.VOCABULARY.getSymbolicName(t.getType()));
            }
        }
        return names;
    }

    private static List<String> tokenTexts(String text) {
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(text));
        List<String> out = new ArrayList<>();
        for (Token t = lexer.nextToken(); t.getType() != Token.EOF; t = lexer.nextToken()) {
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                out.add(SysMLv2Lexer.VOCABULARY.getSymbolicName(t.getType()) + ":" + t.getText());
            }
        }
        return out;
    }

    @Test
    @DisplayName("A usage whose name starts with 'def' is a usage, not a definition with a clipped name")
    void testUsageNameIsNotClipped() {
        // Before the fix these lines PARSED WITHOUT ERROR as definitions named 'aultForm' and 'inition': a silent
        // misparse, so the check is on the tokens, not only on the absence of syntax errors.
        assertEquals(List.of("ATTRIBUTE:attribute", "ID:defaultForm"),
            tokenTexts("attribute defaultForm : Integer;").subList(0, 2));
        assertEquals(List.of("ITEM:item", "ID:definition"), tokenTexts("item definition;").subList(0, 2));
        assertEquals(List.of("PART:part", "ID:defaultPart"), tokenTexts("part defaultPart;").subList(0, 2));
    }

    @Test
    @DisplayName("Usages whose names start with 'def' parse")
    void testUsageNamesStartingWithDef() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                private import ScalarValues::*;
                part def Holder {
                    attribute defaultForm : Integer;
                    part defaultPart;
                    item definition;
                    port defaultPort;
                    action deferred;
                    ref item defined;
                }
            }
            """);
        assertFalse(result.hasErrors(), "Names starting with 'def' should parse: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Enumeration literals named definition and usage")
    void testEnumLiteralNamedDefinition() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                enum def CreationForm {
                    enum definition;
                    enum usage;
                }
                attribute def Choice {
                    attribute form : CreationForm = CreationForm::definition;
                }
            }
            """);
        assertFalse(result.hasErrors(), "An enum literal named 'definition' should parse: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Compound keywords still lex as one token before a name, a short name, a body or a semicolon")
    void testCompoundKeywordsStillMatch() {
        assertEquals(List.of("ATTRIBUTE_DEF", "ID", "SEMICOLON"), tokenNames("attribute def A;"));
        assertEquals("PART_DEF", tokenNames("part def<P> Q;").get(0));
        assertEquals("ITEM_DEF", tokenNames("item def{}").get(0));
        assertEquals("ENUM_DEF", tokenNames("enum def;").get(0));
        assertEquals("METADATA_DEF", tokenNames("metadata def\n\tM;").get(0));
        assertEquals("USE_CASE_DEF", tokenNames("use  case\tdef U;").get(0));
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                attribute def A;
                part def <P> Q;
                item def {}
                enum def E { enum a; }
            }
            """);
        assertFalse(result.hasErrors(), "Compound keywords should still parse: " + result.getSyntaxErrors());
    }

    @ParameterizedTest(name = "''{0}X'' is not the compound keyword ''{0}''")
    @ValueSource(strings = {"part def", "action def", "state def", "requirement def", "use case def", "view def",
            "viewpoint def", "constraint def", "attribute def", "enum def", "connection def", "interface def",
            "allocation def", "port def", "item def", "calc def", "analysis def", "case def", "verification def",
            "concern def", "rendering def", "occurrence def", "flow def", "metadata def", "individual def",
            "assoc struct", "use case", "defined by", "typed by"})
    @DisplayName("A compound keyword followed by identifier characters is not the compound keyword")
    void testNoCompoundTokenInsideAName(String compound) {
        String compoundToken = tokenNames(compound + " X").get(0);
        for (String tail : List.of("ault", "inition", "X", "_1", "9")) {
            List<String> names = tokenNames(compound + tail + " ;");
            assertNotEquals(compoundToken, names.get(0),
                "'" + compound + tail + "' lexed as " + names + "; the compound keyword must end at a word boundary");
        }
    }
}
