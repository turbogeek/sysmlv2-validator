package com.validator.semantic;

import com.validator.ast.Location;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the Symbol class.
 * Ensures 100% coverage of symbol creation, relationships, and defensive copies.
 */
@DisplayName("Symbol Tests")
public class SymbolTest {

    private Location testLocation;
    private Symbol baseSymbol;
    private Symbol derivedSymbol;

    @BeforeEach
    public void setUp() {
        testLocation = new Location("test.sysml", 10, 5);
        baseSymbol = new Symbol("BasePart", "Package::BasePart",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        derivedSymbol = new Symbol("DerivedPart", "Package::DerivedPart",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
    }

    @Test
    @DisplayName("Should create symbol with valid parameters")
    public void testSymbolCreation() {
        Symbol symbol = new Symbol("Vehicle", "Automotive::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        assertEquals("Vehicle", symbol.getName());
        assertEquals("Automotive::Vehicle", symbol.getQualifiedName());
        assertEquals(ElementType.PART_DEFINITION, symbol.getType());
        assertEquals(testLocation, symbol.getLocation());
        assertEquals(Visibility.PUBLIC, symbol.getVisibility());
        assertNotNull(symbol.getRedefinitions());
        assertNotNull(symbol.getSpecializations());
        assertNotNull(symbol.getSubsettings());
        assertTrue(symbol.getRedefinitions().isEmpty());
    }

    @Test
    @DisplayName("Should enforce null checks on construction")
    public void testNullValidation() {
        // Null name
        assertThrows(NullPointerException.class, () ->
            new Symbol(null, "Package::Element", ElementType.PART_DEFINITION,
                testLocation, Visibility.PUBLIC),
            "Should throw NPE for null name");

        // Null qualified name
        assertThrows(NullPointerException.class, () ->
            new Symbol("Element", null, ElementType.PART_DEFINITION,
                testLocation, Visibility.PUBLIC),
            "Should throw NPE for null qualified name");

        // Null element type
        assertThrows(NullPointerException.class, () ->
            new Symbol("Element", "Package::Element", null,
                testLocation, Visibility.PUBLIC),
            "Should throw NPE for null element type");

        // Location can be null (for generated symbols)
        Symbol symbolWithoutLocation = new Symbol("Generated", "Package::Generated",
            ElementType.PART_DEFINITION, null, Visibility.PUBLIC);
        assertNull(symbolWithoutLocation.getLocation());

        // Visibility defaults to PRIVATE if null
        Symbol symbolWithoutVisibility = new Symbol("Private", "Package::Private",
            ElementType.PART_DEFINITION, testLocation, null);
        assertEquals(Visibility.PRIVATE, symbolWithoutVisibility.getVisibility());
    }

    @Test
    @DisplayName("Should manage redefinitions with defensive copies")
    public void testRedefinitions() {
        // Add redefinition
        derivedSymbol.addRedefinition(baseSymbol);

        List<Symbol> redefinitions = derivedSymbol.getRedefinitions();
        assertEquals(1, redefinitions.size());
        assertEquals(baseSymbol, redefinitions.get(0));

        // Add another redefinition
        Symbol anotherBase = new Symbol("AnotherBase", "Package::AnotherBase",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        derivedSymbol.addRedefinition(anotherBase);

        assertEquals(2, derivedSymbol.getRedefinitions().size());

        // Verify defensive copy - modifications to returned list should not affect symbol
        List<Symbol> retrievedList = derivedSymbol.getRedefinitions();
        assertThrows(UnsupportedOperationException.class, () ->
            retrievedList.add(new Symbol("Fail", "Package::Fail",
                ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC)),
            "Returned list should be unmodifiable");

        // Original symbol should still have 2 redefinitions
        assertEquals(2, derivedSymbol.getRedefinitions().size());

        // Null check
        assertThrows(NullPointerException.class, () ->
            derivedSymbol.addRedefinition(null),
            "Should not allow null redefinition");
    }

    @Test
    @DisplayName("Should manage specializations with defensive copies")
    public void testSpecializations() {
        // Add specialization
        derivedSymbol.addSpecialization(baseSymbol);

        List<Symbol> specializations = derivedSymbol.getSpecializations();
        assertEquals(1, specializations.size());
        assertEquals(baseSymbol, specializations.get(0));

        // Add multiple specializations
        Symbol interface1 = new Symbol("Interface1", "Package::Interface1",
            ElementType.INTERFACE_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol interface2 = new Symbol("Interface2", "Package::Interface2",
            ElementType.INTERFACE_DEFINITION, testLocation, Visibility.PUBLIC);

        derivedSymbol.addSpecialization(interface1);
        derivedSymbol.addSpecialization(interface2);

        assertEquals(3, derivedSymbol.getSpecializations().size());

        // Verify defensive copy
        List<Symbol> retrievedList = derivedSymbol.getSpecializations();
        assertThrows(UnsupportedOperationException.class, () ->
            retrievedList.clear(),
            "Returned list should be unmodifiable");

        // Original symbol should still have 3 specializations
        assertEquals(3, derivedSymbol.getSpecializations().size());

        // Null check
        assertThrows(NullPointerException.class, () ->
            derivedSymbol.addSpecialization(null),
            "Should not allow null specialization");
    }

    @Test
    @DisplayName("Should manage subsettings with defensive copies")
    public void testSubsettings() {
        // Add subsetting
        Symbol generalProperty = new Symbol("generalProperty", "Package::Part::generalProperty",
            ElementType.ATTRIBUTE_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol specificProperty = new Symbol("specificProperty", "Package::SpecificPart::specificProperty",
            ElementType.ATTRIBUTE_DEFINITION, testLocation, Visibility.PUBLIC);

        specificProperty.addSubsetting(generalProperty);

        List<Symbol> subsettings = specificProperty.getSubsettings();
        assertEquals(1, subsettings.size());
        assertEquals(generalProperty, subsettings.get(0));

        // Verify defensive copy
        List<Symbol> retrievedList = specificProperty.getSubsettings();
        assertThrows(UnsupportedOperationException.class, () ->
            retrievedList.add(new Symbol("Fail", "Package::Fail",
                ElementType.ATTRIBUTE_DEFINITION, testLocation, Visibility.PUBLIC)),
            "Returned list should be unmodifiable");

        // Original symbol should still have 1 subsetting
        assertEquals(1, specificProperty.getSubsettings().size());

        // Null check
        assertThrows(NullPointerException.class, () ->
            specificProperty.addSubsetting(null),
            "Should not allow null subsetting");
    }

    @Test
    @DisplayName("Should implement equals and hashCode based on qualified name")
    public void testEqualsAndHashCode() {
        Symbol symbol1 = new Symbol("Vehicle", "Automotive::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol symbol2 = new Symbol("Vehicle", "Automotive::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PRIVATE);
        Symbol symbol3 = new Symbol("Vehicle", "Aerospace::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol symbol4 = new Symbol("Car", "Automotive::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        // Same qualified name = equal (regardless of other attributes)
        assertEquals(symbol1, symbol2);
        assertEquals(symbol1.hashCode(), symbol2.hashCode());

        // Different qualified name = not equal (even if same simple name)
        assertNotEquals(symbol1, symbol3);

        // Same qualified name but different simple name = still equal
        // (This tests that equality is based on qualified name only)
        assertEquals(symbol1, symbol4);

        // Reflexivity
        assertEquals(symbol1, symbol1);

        // Null safety
        assertNotEquals(symbol1, null);

        // Different type
        assertNotEquals(symbol1, "Automotive::Vehicle");
    }

    @Test
    @DisplayName("Should support AST node attachment")
    public void testASTNodeAttachment() {
        Symbol symbol = new Symbol("Element", "Package::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        // Initially null
        assertNull(symbol.getAstNode());

        // Set AST node (using a string as mock AST node)
        Object mockAstNode = "MockParseTreeNode";
        symbol.setAstNode(mockAstNode);

        assertEquals(mockAstNode, symbol.getAstNode());

        // Can set to null
        symbol.setAstNode(null);
        assertNull(symbol.getAstNode());
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    public void testToString() {
        Symbol symbol = new Symbol("Vehicle", "Automotive::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        String str = symbol.toString();

        // Should contain key information
        assertTrue(str.contains("Vehicle"), "toString should contain symbol name");
        assertTrue(str.contains("Automotive::Vehicle"), "toString should contain qualified name");
        assertTrue(str.contains("PART_DEFINITION"), "toString should contain element type");
        assertTrue(str.contains("PUBLIC"), "toString should contain visibility");
    }

    @Test
    @DisplayName("Should handle different element types")
    public void testDifferentElementTypes() {
        ElementType[] types = {
            ElementType.PACKAGE,
            ElementType.PART_DEFINITION,
            ElementType.PART_USAGE,
            ElementType.ACTION_DEFINITION,
            ElementType.ACTION_USAGE,
            ElementType.STATE_DEFINITION,
            ElementType.STATE_USAGE,
            ElementType.REQUIREMENT_DEFINITION,
            ElementType.REQUIREMENT_USAGE,
            ElementType.ATTRIBUTE_DEFINITION,
            ElementType.ATTRIBUTE_USAGE,
            ElementType.PORT_DEFINITION,
            ElementType.PORT_USAGE,
            ElementType.CONNECTION_DEFINITION,
            ElementType.CONNECTION_USAGE,
            ElementType.INTERFACE_DEFINITION,
            ElementType.INTERFACE_USAGE,
            ElementType.ITEM_DEFINITION,
            ElementType.ITEM_USAGE,
            ElementType.DATA_TYPE,
            ElementType.ENUMERATION
        };

        for (ElementType type : types) {
            Symbol symbol = new Symbol("Element", "Package::Element",
                type, testLocation, Visibility.PUBLIC);
            assertEquals(type, symbol.getType(),
                "Should correctly store element type: " + type);
        }
    }

    @Test
    @DisplayName("Should handle different visibility levels")
    public void testDifferentVisibilities() {
        for (Visibility visibility : Visibility.values()) {
            Symbol symbol = new Symbol("Element", "Package::Element",
                ElementType.PART_DEFINITION, testLocation, visibility);
            assertEquals(visibility, symbol.getVisibility(),
                "Should correctly store visibility: " + visibility);
        }
    }

    @Test
    @DisplayName("Should handle complex relationship chains")
    public void testComplexRelationships() {
        // Create a hierarchy: Base -> Middle -> Derived
        Symbol base = new Symbol("Base", "Pkg::Base",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol middle = new Symbol("Middle", "Pkg::Middle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol derived = new Symbol("Derived", "Pkg::Derived",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        // Middle specializes Base
        middle.addSpecialization(base);

        // Derived specializes Middle and redefines Base
        derived.addSpecialization(middle);
        derived.addRedefinition(base);

        // Verify relationships
        assertEquals(1, middle.getSpecializations().size());
        assertEquals(base, middle.getSpecializations().get(0));

        assertEquals(1, derived.getSpecializations().size());
        assertEquals(middle, derived.getSpecializations().get(0));

        assertEquals(1, derived.getRedefinitions().size());
        assertEquals(base, derived.getRedefinitions().get(0));

        // Base should have no relationships
        assertTrue(base.getSpecializations().isEmpty());
        assertTrue(base.getRedefinitions().isEmpty());
        assertTrue(base.getSubsettings().isEmpty());
    }
}
