package com.validator.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Words that SysML v2 does not reserve are names. The lexer has keyword tokens for some words that no parser rule
 * uses, so 'attribute value : String;' was a syntax error although it is valid SysML v2: the reserved words are the
 * list in SysML v2 8.2.2.1.2, and 'value' is not one of them. Found by the UML-3-Experiments domain metamodel, whose
 * FacetValue has an attribute named 'value'. The words tested are exactly the keyword tokens that no parser rule
 * refers to, so accepting them as names cannot change how any other construct parses.
 */
@DisplayName("Non-reserved words as names")
class NonReservedWordNameTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    @ParameterizedTest(name = "''{0}'' is a name")
    @ValueSource(strings = {"any", "chains", "featuring", "inverses", "multiplicity", "sequence", "typing", "value"})
    @DisplayName("A word that SysML v2 does not reserve names a feature, a usage and an enumeration literal")
    void testWordIsName(String word) {
        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString("""
            package P {
                private import ScalarValues::*;
                attribute def Holder {
                    attribute %1$s : String;
                }
                enum def Kinds {
                    enum %1$s;
                }
                part %1$s;
                attribute h : Holder;
                attribute v = h.%1$s;
                attribute k : Kinds = Kinds::%1$s;
            }
            """.formatted(word), "names.sysml");
        assertFalse(result.hasErrors(), "'" + word + "' should be usable as a name: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Reserved words still cannot be names")
    void testReservedWordIsNotName() {
        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString("""
            package P {
                attribute def Holder {
                    attribute attribute : Integer;
                }
            }
            """, "reserved.sysml");
        assertTrue(result.hasErrors(), "A reserved word must not be accepted as a name");
    }
}
