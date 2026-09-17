package com.validator.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dependencies. SysML v2 8.2.2.3: Dependency = PrefixMetadataAnnotation* 'dependency' DependencyDeclaration
 * RelationshipBody, with DependencyDeclaration = ( Identification 'from' )? client ( ',' client )* 'to'
 * supplier ( ',' supplier )* and RelationshipBody = ';' | '{' RelationshipOwnedElement* '}'.
 * A body lets a dependency own its documentation.
 */
@DisplayName("Dependency Parser Tests")
class DependencyParserTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    private SysMLv2ParserFacade.ParseResult parse(String text) {
        return parserFacade.parseString(text, "dependency.sysml");
    }

    @Test
    @DisplayName("Named dependency with a semicolon")
    void testNamedDependencySemicolon() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part def A;
                part def B;
                dependency d from A to B;
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Dependency with a body owning documentation")
    void testDependencyBodyWithDoc() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part def A;
                part def B;
                dependency usesB from A to B {
                    doc /* A needs B to work. */
                }
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Dependency with an empty body")
    void testDependencyEmptyBody() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part def A;
                part def B;
                dependency from A to B { }
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Dependency with several clients and suppliers and a keyword prefix")
    void testDependencyLists() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                metadata def M;
                part def A;
                part def B;
                part def C;
                part def D;
                #M dependency from A, B to C, D {
                    doc /* Several clients and suppliers. */
                }
            }
            """);
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Dependency without a supplier is a syntax error")
    void testMissingSupplierRejected() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                part def A;
                dependency from A to { doc /* no supplier */ }
            }
            """);
        assertTrue(result.hasErrors(), "A dependency without a supplier must be rejected");
    }
}
