package com.validator.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comments and metadata annotating one or more elements with 'about'.
 * SysML v2 8.2.2.4.2: Comment = ( 'comment' Identification ( 'about' Annotation ( ',' Annotation )* )? )?
 * ( 'locale' STRING )? REGULAR_COMMENT ; there is no ';' after the comment body.
 * The OMG standard library uses the list form (Kernel Semantic Library, Objects.kerml).
 */
@DisplayName("Comment about Parser Tests")
class CommentAboutParserTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    private SysMLv2ParserFacade.ParseResult parse(String text) {
        return parserFacade.parseString(text, "comment_about.sysml");
    }

    @Test
    @DisplayName("Unnamed comment about one qualified element, no semicolon")
    void testUnnamedCommentAboutQualifiedName() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part part1 { attribute attribute1; }
                comment about
                part1::attribute1
                /* The annotated element
                 * is attribute1. */
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Named comment about a list of elements")
    void testNamedCommentAboutList() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part def A;
                part def B;
                part def C;
                comment Design about A, B,
                    C
                    /* Explains how A, B and C work together. */
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Comment about a list with locale")
    void testCommentAboutListWithLocale() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part def A;
                part def B;
                comment Note about A, B locale "en_US" /* Text. */
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Metadata annotation about a list of elements")
    void testMetadataAboutList() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                metadata def M;
                part def A;
                part def B;
                @M about A, B;
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Dangling comma after about is a syntax error")
    void testDanglingCommaRejected() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part def A;
                comment Bad about A, /* missing element */
            }
            """);
        assertTrue(result.hasErrors(), "A trailing comma in the about list must be rejected");
    }
}
