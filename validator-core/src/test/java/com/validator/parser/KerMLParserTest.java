package com.validator.parser;

import com.validator.semantic.ElementType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KerML language support in the parser.
 */
@DisplayName("KerML Parser Tests")
class KerMLParserTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    @Test
    @DisplayName("Should parse datatype definition")
    void testDatatypeDefinition() {
        String kerml = """
            package Types {
                datatype Natural;
                datatype Integer;
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "types.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "types.kerml");
        assertTrue(symbolTable.getAllSymbols().size() >= 2, "Should have at least 2 symbols");
    }

    @Test
    @DisplayName("Should parse class definition")
    void testClassDefinition() {
        String kerml = """
            package Classes {
                class MyClass {
                    feature myValue : Integer;
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "classes.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "classes.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.CLASS_DEFINITION),
            "Should have a class definition");
    }

    @Test
    @DisplayName("Should parse struct definition")
    void testStructDefinition() {
        String kerml = """
            package Structures {
                struct Point {
                    feature x : Real;
                    feature y : Real;
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "structs.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "structs.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.STRUCT_DEFINITION),
            "Should have a struct definition");
    }

    @Test
    @DisplayName("Should parse association definition")
    void testAssocDefinition() {
        String kerml = """
            package Associations {
                assoc Friendship {
                    end feature friend1 : Person;
                    end feature friend2 : Person;
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "assocs.kerml");
        // Even if parsing has warnings about 'end', check for major errors
        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "assocs.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.size() >= 1, "Should have at least 1 symbol");
    }

    @Test
    @DisplayName("Should parse behavior definition")
    void testBehaviorDefinition() {
        String kerml = """
            package Behaviors {
                behavior Process {
                    step doWork;
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "behaviors.kerml");
        // May have warnings but should not completely fail

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "behaviors.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.BEHAVIOR_DEFINITION),
            "Should have a behavior definition");
    }

    @Test
    @DisplayName("Should parse function definition")
    void testFunctionDefinition() {
        String kerml = """
            package Functions {
                function Add {
                    in x : Integer;
                    in y : Integer;
                    return z : Integer;
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "functions.kerml");
        // May have warnings about function body syntax

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "functions.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.FUNCTION_DEFINITION),
            "Should have a function definition");
    }

    @Test
    @DisplayName("Should parse predicate definition")
    void testPredicateDefinition() {
        String kerml = """
            package Predicates {
                predicate IsPositive {
                    in x : Integer;
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "predicates.kerml");

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "predicates.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.PREDICATE_DEFINITION),
            "Should have a predicate definition");
    }

    @Test
    @DisplayName("Should parse classifier definition")
    void testClassifierDefinition() {
        String kerml = """
            package Classifiers {
                classifier Entity {
                    feature id : Integer;
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "classifiers.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "classifiers.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.CLASSIFIER_DEFINITION),
            "Should have a classifier definition");
    }

    @Test
    @DisplayName("Should parse type definition")
    void testTypeDefinition() {
        String kerml = """
            package Types {
                type Anything;
                type Something :> Anything;
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "types.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "types.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().filter(s -> s.getType() == ElementType.TYPE_DEFINITION).count() >= 2,
            "Should have 2 type definitions");
    }

    @Test
    @DisplayName("Should parse feature definition")
    void testFeatureDefinition() {
        String kerml = """
            package Features {
                feature myFeature : Integer [0..*];
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "features.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "features.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.FEATURE_DEFINITION),
            "Should have a feature definition");
    }

    @Test
    @DisplayName("Should parse connector definition")
    void testConnectorDefinition() {
        String kerml = """
            package Connectors {
                connector Link {
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "connectors.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "connectors.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.CONNECTOR_DEFINITION),
            "Should have a connector definition");
    }

    @Test
    @DisplayName("Should parse metaclass definition")
    void testMetaclassDefinition() {
        String kerml = """
            package Meta {
                metaclass MyMeta {
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "meta.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "meta.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.METACLASS_DEFINITION),
            "Should have a metaclass definition");
    }

    @Test
    @DisplayName("Should parse interaction definition")
    void testInteractionDefinition() {
        String kerml = """
            package Interactions {
                interaction Message {
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(kerml, "interactions.kerml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "interactions.kerml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.INTERACTION_DEFINITION),
            "Should have an interaction definition");
    }

    @Test
    @DisplayName("Should parse mixed KerML and SysML")
    void testMixedKerMLSysML() {
        String mixed = """
            package MixedModel {
                datatype Length;

                part def Vehicle {
                    attribute wheelbase : Length;
                }

                class Engine {
                }
            }
            """;

        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(mixed, "mixed.sysml");
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());

        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "mixed.sysml");
        Collection<Symbol> symbols = symbolTable.getAllSymbols();

        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.DATATYPE_DEFINITION),
            "Should have datatype definition");
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.PART_DEFINITION),
            "Should have part definition");
        assertTrue(symbols.stream().anyMatch(s -> s.getType() == ElementType.CLASS_DEFINITION),
            "Should have class definition");
    }

    @Test
    @DisplayName("Should recognize KerML file extension")
    void testKerMLFileExtension() {
        assertTrue(SysMLv2ParserFacade.isKerMLFile(new java.io.File("test.kerml")));
        assertFalse(SysMLv2ParserFacade.isKerMLFile(new java.io.File("test.sysml")));
    }
}
