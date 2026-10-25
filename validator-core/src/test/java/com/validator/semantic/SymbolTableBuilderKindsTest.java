package com.validator.semantic;

import com.validator.parser.SysMLv2ParserFacade;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The symbols SymbolTableBuilder makes for each kind of SysML v2 and KerML definition and usage, the scopes that
 * definitions open for their members, visibility, imports and library packages. SymbolTableBuilderTest covers the
 * common cases; this covers every kind.
 */
@DisplayName("Symbol table builder: every kind of element")
class SymbolTableBuilderKindsTest {

    private static SymbolTable build(String source, String fileName) {
        SysMLv2ParserFacade.ParseResult result = new SysMLv2ParserFacade().parseString(source, fileName);
        assertFalse(result.hasErrors(), "Test input should parse: " + result.getSyntaxErrors());
        return SymbolTableBuilder.build(result.getParseTree(), fileName);
    }

    private static Symbol symbol(SymbolTable table, String qualifiedName) {
        Symbol symbol = table.resolveQualifiedDirect(qualifiedName);
        assertNotNull(symbol, "No symbol " + qualifiedName + " in " + table.getAllSymbols().stream()
            .map(Symbol::getQualifiedName).toList());
        return symbol;
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "part def D;         | PART_DEFINITION",
        "action def D;       | ACTION_DEFINITION",
        "state def D;        | STATE_DEFINITION",
        "requirement def D;  | REQUIREMENT_DEFINITION",
        "view def D;         | VIEW_DEFINITION",
        "viewpoint def D;    | VIEWPOINT_DEFINITION",
        "constraint def D;   | CONSTRAINT_DEFINITION",
        "attribute def D;    | ATTRIBUTE_DEFINITION",
        "port def D;         | PORT_DEFINITION",
        "connection def D;   | CONNECTION_DEFINITION",
        "interface def D;    | INTERFACE_DEFINITION",
        "allocation def D;   | ALLOCATION_DEFINITION",
        "item def D;         | ITEM_DEFINITION",
        "enum def D;         | ENUM_DEFINITION",
        "calc def D;         | CALC_DEFINITION",
        "analysis def D;     | ANALYSIS_DEFINITION",
        "case def D;         | CASE_DEFINITION",
        "verification def D; | VERIFICATION_DEFINITION",
        "use case def D;     | USE_CASE_DEFINITION"
    })
    @DisplayName("SysML v2 definitions")
    void testSysmlDefinitions(String declaration, ElementType type) {
        SymbolTable table = build("package P { " + declaration + " }", "kinds.sysml");
        Symbol symbol = symbol(table, "P::D");
        assertEquals(type, symbol.getType());
        assertEquals("D", symbol.getName());
        assertEquals("kinds.sysml", symbol.getLocation().getFileName());
        assertEquals(1, symbol.getLocation().getLine());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "part u;          | PART_USAGE",
        "action u;        | ACTION_USAGE",
        "state u;         | STATE_USAGE",
        "attribute u;     | ATTRIBUTE_USAGE",
        "port u;          | PORT_USAGE",
        "item u;          | ITEM_USAGE",
        "requirement u;   | REQUIREMENT_USAGE",
        "constraint u { } | CONSTRAINT_USAGE",
        "connection u;    | CONNECTION_USAGE",
        "calc u;          | CALC_USAGE",
        "view u;          | VIEW_USAGE"
    })
    @DisplayName("SysML v2 usages, inside the definition that owns them")
    void testSysmlUsages(String declaration, ElementType type) {
        SymbolTable table = build("package P { part def Owner { " + declaration + " } }", "kinds.sysml");
        assertEquals(type, symbol(table, "P::Owner::u").getType());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "datatype D;                | DATATYPE_DEFINITION",
        "class D;                   | CLASS_DEFINITION",
        "struct D;                  | STRUCT_DEFINITION",
        "behavior D;                | BEHAVIOR_DEFINITION",
        "function D;                | FUNCTION_DEFINITION",
        "predicate D;               | PREDICATE_DEFINITION",
        "interaction D;             | INTERACTION_DEFINITION",
        "metaclass D;               | METACLASS_DEFINITION",
        "classifier D;              | CLASSIFIER_DEFINITION",
        "type D specializes Base;   | TYPE_DEFINITION",
        "feature D;                 | FEATURE_DEFINITION",
        "connector D from a to b;   | CONNECTOR_DEFINITION"
    })
    @DisplayName("KerML types, features and connectors")
    void testKermlElements(String declaration, ElementType type) {
        SymbolTable table = build("package K { " + declaration + " }", "kinds.kerml");
        assertEquals(type, symbol(table, "K::D").getType());
    }

    @ParameterizedTest(name = "{0} with a body")
    @CsvSource({
        "part def", "action def", "state def", "requirement def", "view def", "viewpoint def", "constraint def",
        "connection def", "allocation def", "calc def", "analysis def", "case def", "verification def"
    })
    @DisplayName("SysML v2 definitions qualify the names of their members")
    void testSysmlDefinitionScopes(String keyword) {
        SymbolTable table = build("package P { " + keyword + " D { attribute x; } attribute later; }", "s.sysml");
        assertEquals(ElementType.ATTRIBUTE_USAGE, symbol(table, "P::D::x").getType());
        assertNotNull(symbol(table, "P::later"), "The scope ends with the definition");
    }

    @ParameterizedTest(name = "{0} with a body")
    @CsvSource({
        "class", "struct", "behavior", "function", "predicate", "interaction", "metaclass", "classifier"
    })
    @DisplayName("KerML classifiers qualify the names of their features")
    void testKermlScopes(String keyword) {
        SymbolTable table = build("package K { " + keyword + " D { feature x; } feature later; }", "s.kerml");
        assertEquals(ElementType.FEATURE_DEFINITION, symbol(table, "K::D::x").getType());
        assertNotNull(symbol(table, "K::later"));
    }

    @Test
    @DisplayName("A connector's body features are qualified by the connector")
    void testConnectorScope() {
        SymbolTable table = build("package K { feature a; feature b; connector c from a to b { feature x; } }",
            "c.kerml");
        assertNotNull(symbol(table, "K::c::x"));
    }

    @Test
    @DisplayName("An enumeration definition opens a scope, and its values are not symbols")
    void testEnumScope() {
        SymbolTable table = build("package P { enum def Color { enum red; } attribute later; }", "e.sysml");
        assertNotNull(symbol(table, "P::Color"));
        assertNull(table.resolveQualifiedDirect("P::Color::red"));
        assertNotNull(symbol(table, "P::later"));
    }

    @Test
    @DisplayName("Visibility applies to one element and then resets to public")
    void testVisibility() {
        SymbolTable table = build("package P { private part def Hidden; part def Shown; protected attribute a; }",
            "v.sysml");
        assertEquals(Visibility.PRIVATE, symbol(table, "P::Hidden").getVisibility());
        assertEquals(Visibility.PUBLIC, symbol(table, "P::Shown").getVisibility());
        assertEquals(Visibility.PROTECTED, symbol(table, "P::a").getVisibility());
    }

    @Test
    @DisplayName("Imports are recorded in the scope of their package as wildcard or specific")
    void testImports() {
        SymbolTable table = build("package P { import ISQ::*; import SI::kg; import Lib::**; }", "i.sysml");
        List<ImportStatement> imports = table.resolveScope("P").getImports();
        assertEquals(List.of("ISQ::*", "SI::kg", "Lib::**"), imports.stream().map(ImportStatement::getImportPath).toList());
        assertEquals(List.of(ImportType.WILDCARD, ImportType.SPECIFIC, ImportType.WILDCARD),
            imports.stream().map(ImportStatement::getImportType).toList());
    }

    @Disabled("bug: SymbolTableBuilder.enterImportDeclaration takes an import as private only when the import's own "
        + "text starts with 'private', but namespaceBodyElement owns the visibility, so 'private import' is public")
    @Test
    @DisplayName("A private import is not public")
    void testPrivateImport() {
        SymbolTable table = build("package P { private import ISQ::*; }", "i.sysml");
        assertFalse(table.resolveScope("P").getImports().get(0).isPublic());
    }

    @Test
    @DisplayName("A standard library package is a package scope")
    void testLibraryPackage() {
        SymbolTable table = build("standard library package Lib { part def A; } part def B;", "l.sysml");
        assertEquals(ElementType.PACKAGE, symbol(table, "Lib").getType());
        assertNotNull(symbol(table, "Lib::A"));
        assertNotNull(symbol(table, "B"), "The library package's scope ends with it");
    }

    @Test
    @DisplayName("Unnamed definitions open no scope and a duplicate name keeps the first symbol")
    void testUnnamedAndDuplicates() {
        SymbolTable table = build("package P { part def { attribute x; } part def A; part def A; }", "u.sysml");
        assertNotNull(symbol(table, "P::x"), "The members of an unnamed definition belong to the package");
        assertEquals(1, table.getAllSymbols().stream().filter(s -> s.getQualifiedName().equals("P::A")).count());
    }

    @Disabled("bug: SymbolTableBuilder opens no scope for attribute, port, interface, item, use case, datatype and type "
        + "definitions, so their features are qualified by the enclosing package and features of the same name in two "
        + "such definitions collide")
    @Test
    @DisplayName("Attribute and port definitions qualify the names of their members")
    void testMembersOfDefinitionsWithoutScope() {
        SymbolTable table = build("package P { attribute def A { attribute x; } port def B { attribute x; } }",
            "m.sysml");
        assertNotNull(table.resolveQualifiedDirect("P::A::x"));
        assertNotNull(table.resolveQualifiedDirect("P::B::x"));
    }

    @Test
    @DisplayName("A null parse tree or library is rejected")
    void testNullArguments() {
        SysMLv2ParserFacade.ParseResult result = new SysMLv2ParserFacade().parseString("package P;", "n.sysml");
        assertThrows(NullPointerException.class, () -> SymbolTableBuilder.build(null, "n.sysml"));
        assertThrows(NullPointerException.class, () -> SymbolTableBuilder.build(result.getParseTree(), "n.sysml", null));
        assertTrue(SymbolTableBuilder.build(result.getParseTree(), null).getAllSymbols().stream()
            .anyMatch(s -> s.getLocation() != null && "<unknown>".equals(s.getLocation().getFileName())));
    }
}
