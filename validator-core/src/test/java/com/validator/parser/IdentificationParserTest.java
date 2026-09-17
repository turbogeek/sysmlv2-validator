package com.validator.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Identification is optional. SysML v2 8.2.2.1: Identification = ( '&lt;' declaredShortName '&gt;' )?
 * declaredName?, and DefinitionDeclaration = Identification SubclassificationPart?, so a definition may have a
 * short name, a name, both or neither. PackageDeclaration and NamespaceDeclaration use the same Identification,
 * so 'package &lt;P&gt; LongName' and an unnamed package are legal too.
 * Unnamed definitions are how element templates are written: a palette or creation-dialog button copies the one
 * element a template package owns, and an unnamed element lets the modeler name the copy (CATIA Magic's own
 * DS_Views::ViewPalettes::ElementTemplates are written this way).
 */
@DisplayName("Identification Parser Tests")
class IdentificationParserTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    private SysMLv2ParserFacade.ParseResult parse(String text) {
        return parserFacade.parseString(text, "identification.sysml");
    }

    @Test
    @DisplayName("Unnamed definitions of every kind")
    void testUnnamedDefinitions() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                item def;
                part def;
                attribute def;
                port def;
                action def;
                calc def;
                connection def;
                view def;
            }
            """);
        assertFalse(result.hasErrors(), "Unnamed definitions should parse: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Unnamed definition with a keyword prefix and a body")
    void testUnnamedDefinitionWithKeywordAndBody() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                metadata def M;
                #M connection def {
                    end [0..*] ref;
                    end [0..*] ref;
                }
            }
            """);
        assertFalse(result.hasErrors(), "A keyworded unnamed definition with ends should parse: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Named definitions still parse, with and without a short name")
    void testNamedDefinitionsStillParse() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                item def Customer;
                view def <cd> ClassDiagram;
                part def <pd> 'Part Definition';
            }
            """);
        assertFalse(result.hasErrors(), "Named definitions should parse: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("Package and namespace with a short name, and an unnamed package")
    void testPackageIdentification() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            library package <DS_Views> DassaultSystemesViews {
                package <cv> CoreViews {
                    package;
                }
            }
            namespace <N> Configurations {
                package ProjectViewCreationConfig;
            }
            """);
        assertFalse(result.hasErrors(), "Package identification should parse: " + result.getSyntaxErrors());
    }

    @Test
    @DisplayName("A definition body without braces or a semicolon is still a syntax error")
    void testMissingBodyRejected() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                item def
            }
            """);
        assertTrue(result.hasErrors(), "A definition without a body or semicolon must be rejected");
    }
}
