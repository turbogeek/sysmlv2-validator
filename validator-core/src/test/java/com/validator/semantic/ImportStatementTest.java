package com.validator.semantic;

import com.validator.ast.Location;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the ImportStatement class.
 * Ensures 100% coverage of import statement creation and symbol management.
 */
@DisplayName("ImportStatement Tests")
public class ImportStatementTest {

    private Location testLocation;
    private Symbol testSymbol1;
    private Symbol testSymbol2;

    @BeforeEach
    public void setUp() {
        testLocation = new Location("test.sysml", 5, 1);
        testSymbol1 = new Symbol("Vehicle", "Automotive::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        testSymbol2 = new Symbol("Engine", "Automotive::Engine",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
    }

    @Test
    @DisplayName("Should create import statement with valid parameters")
    public void testImportStatementCreation() {
        // Wildcard import
        ImportStatement wildcard = new ImportStatement("Automotive::*",
            ImportType.WILDCARD, true);
        assertEquals("Automotive::*", wildcard.getImportPath());
        assertEquals(ImportType.WILDCARD, wildcard.getImportType());
        assertTrue(wildcard.isPublic());
        assertNull(wildcard.getAlias());
        assertNotNull(wildcard.getImportedSymbols());
        assertTrue(wildcard.getImportedSymbols().isEmpty());

        // Specific import
        ImportStatement specific = new ImportStatement("Automotive::Vehicle",
            ImportType.SPECIFIC, false);
        assertEquals("Automotive::Vehicle", specific.getImportPath());
        assertEquals(ImportType.SPECIFIC, specific.getImportType());
        assertFalse(specific.isPublic());

        // Filtered import
        ImportStatement filtered = new ImportStatement("Automotive::{Vehicle, Engine}",
            ImportType.FILTERED, true);
        assertEquals("Automotive::{Vehicle, Engine}", filtered.getImportPath());
        assertEquals(ImportType.FILTERED, filtered.getImportType());

        // Aliased import
        ImportStatement aliased = new ImportStatement("Automotive::Vehicle as Car",
            ImportType.ALIAS, true, "Car");
        assertEquals("Automotive::Vehicle as Car", aliased.getImportPath());
        assertEquals(ImportType.ALIAS, aliased.getImportType());
        assertEquals("Car", aliased.getAlias());
    }

    @Test
    @DisplayName("Should enforce null checks on construction")
    public void testNullValidation() {
        // Null import path
        assertThrows(NullPointerException.class, () ->
            new ImportStatement(null, ImportType.WILDCARD, true),
            "Should throw NPE for null import path");

        // Null import type
        assertThrows(NullPointerException.class, () ->
            new ImportStatement("Automotive::*", null, true),
            "Should throw NPE for null import type");

        // Alias can be null for non-aliased imports
        ImportStatement noAlias = new ImportStatement("Automotive::Vehicle",
            ImportType.SPECIFIC, true, null);
        assertNull(noAlias.getAlias());

        // Empty import path should fail
        assertThrows(IllegalArgumentException.class, () ->
            new ImportStatement("", ImportType.WILDCARD, true),
            "Should throw IAE for empty import path");
    }

    @Test
    @DisplayName("Should add and retrieve imported symbols")
    public void testImportedSymbolManagement() {
        ImportStatement importStmt = new ImportStatement("Automotive::*",
            ImportType.WILDCARD, true);

        // Initially empty
        assertTrue(importStmt.getImportedSymbols().isEmpty());

        // Add symbol
        importStmt.addImportedSymbol(testSymbol1);
        Map<String, Symbol> symbols = importStmt.getImportedSymbols();
        assertEquals(1, symbols.size());
        assertTrue(symbols.containsKey("Vehicle"));
        assertEquals(testSymbol1, symbols.get("Vehicle"));

        // Add another symbol
        importStmt.addImportedSymbol(testSymbol2);
        assertEquals(2, importStmt.getImportedSymbols().size());
        assertTrue(importStmt.getImportedSymbols().containsKey("Engine"));

        // Null check
        assertThrows(NullPointerException.class, () ->
            importStmt.addImportedSymbol(null),
            "Should not allow null symbol");
    }

    @Test
    @DisplayName("Should handle aliased imports correctly")
    public void testAliasedImports() {
        // Create aliased import
        ImportStatement aliased = new ImportStatement("Automotive::Vehicle as Car",
            ImportType.ALIAS, true, "Car");

        // Add symbol with original name
        aliased.addImportedSymbol(testSymbol1);

        // Should be accessible by alias, not original name
        Map<String, Symbol> symbols = aliased.getImportedSymbols();
        assertTrue(symbols.containsKey("Car"), "Should use alias as key");
        assertEquals(testSymbol1, symbols.get("Car"));

        // Verify the alias is used
        assertEquals("Car", aliased.getAlias());
    }

    @Test
    @DisplayName("Should resolve symbols by name")
    public void testSymbolResolution() {
        ImportStatement importStmt = new ImportStatement("Automotive::*",
            ImportType.WILDCARD, true);

        importStmt.addImportedSymbol(testSymbol1);
        importStmt.addImportedSymbol(testSymbol2);

        // Resolve existing symbol
        Symbol resolved = importStmt.resolve("Vehicle");
        assertNotNull(resolved);
        assertEquals(testSymbol1, resolved);

        // Resolve another symbol
        resolved = importStmt.resolve("Engine");
        assertNotNull(resolved);
        assertEquals(testSymbol2, resolved);

        // Resolve non-existent symbol
        resolved = importStmt.resolve("NonExistent");
        assertNull(resolved);

        // Null name
        assertNull(importStmt.resolve(null));
    }

    @Test
    @DisplayName("Should handle different import types")
    public void testDifferentImportTypes() {
        // Test all import types
        for (ImportType type : ImportType.values()) {
            ImportStatement importStmt;

            if (type == ImportType.ALIAS) {
                importStmt = new ImportStatement("Package::Element as Alias",
                    type, true, "Alias");
                assertEquals("Alias", importStmt.getAlias());
            } else {
                importStmt = new ImportStatement("Package::Element", type, true);
                assertNull(importStmt.getAlias());
            }

            assertEquals(type, importStmt.getImportType(),
                "Should correctly store import type: " + type);
        }
    }

    @Test
    @DisplayName("Should handle public vs private imports")
    public void testPublicPrivateImports() {
        ImportStatement publicImport = new ImportStatement("Package::Element",
            ImportType.SPECIFIC, true);
        assertTrue(publicImport.isPublic());

        ImportStatement privateImport = new ImportStatement("Package::Element",
            ImportType.SPECIFIC, false);
        assertFalse(privateImport.isPublic());

        // Add symbol to both
        publicImport.addImportedSymbol(testSymbol1);
        privateImport.addImportedSymbol(testSymbol1);

        // Both should be able to resolve
        assertNotNull(publicImport.resolve("Vehicle"));
        assertNotNull(privateImport.resolve("Vehicle"));

        // The difference is in how ImportResolver handles them for re-export
        // Public imports are re-exported, private are not
    }

    @Test
    @DisplayName("Should provide defensive copy of imported symbols")
    public void testDefensiveCopy() {
        ImportStatement importStmt = new ImportStatement("Automotive::*",
            ImportType.WILDCARD, true);

        importStmt.addImportedSymbol(testSymbol1);

        Map<String, Symbol> symbols = importStmt.getImportedSymbols();

        // Attempt to modify returned map
        assertThrows(UnsupportedOperationException.class, () ->
            symbols.put("Fail", testSymbol2),
            "Returned map should be unmodifiable");

        // Original should still have only 1 symbol
        assertEquals(1, importStmt.getImportedSymbols().size());
    }

    @Test
    @DisplayName("Should handle standard library imports")
    public void testStandardLibraryImports() {
        // Import from ISQ
        ImportStatement isqImport = new ImportStatement("ISQ::MassValue",
            ImportType.SPECIFIC, true);
        assertEquals("ISQ::MassValue", isqImport.getImportPath());

        // Import from SI
        ImportStatement siImport = new ImportStatement("SI::*",
            ImportType.WILDCARD, true);
        assertEquals("SI::*", siImport.getImportPath());

        // Import from KerML
        ImportStatement kermlImport = new ImportStatement("KerML::Base",
            ImportType.SPECIFIC, true);
        assertEquals("KerML::Base", kermlImport.getImportPath());

        // Import from SysML
        ImportStatement sysmlImport = new ImportStatement("SysML::Part",
            ImportType.SPECIFIC, true);
        assertEquals("SysML::Part", sysmlImport.getImportPath());
    }

    @Test
    @DisplayName("Should handle filtered import parsing")
    public void testFilteredImportParsing() {
        ImportStatement filtered = new ImportStatement(
            "Automotive::{Vehicle, Engine, Transmission}",
            ImportType.FILTERED, true);

        assertEquals("Automotive::{Vehicle, Engine, Transmission}",
            filtered.getImportPath());
        assertEquals(ImportType.FILTERED, filtered.getImportType());

        // Add the filtered symbols
        filtered.addImportedSymbol(testSymbol1);
        filtered.addImportedSymbol(testSymbol2);

        // Should be able to resolve added symbols
        assertNotNull(filtered.resolve("Vehicle"));
        assertNotNull(filtered.resolve("Engine"));

        // Should not resolve symbols not in the filter
        assertNull(filtered.resolve("Brake"));
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    public void testToString() {
        ImportStatement importStmt = new ImportStatement("Automotive::Vehicle",
            ImportType.SPECIFIC, true);

        String str = importStmt.toString();

        // Should contain key information
        assertTrue(str.contains("Automotive::Vehicle"),
            "toString should contain import path");
        assertTrue(str.contains("SPECIFIC"),
            "toString should contain import type");

        // Aliased import
        ImportStatement aliased = new ImportStatement("Automotive::Vehicle as Car",
            ImportType.ALIAS, true, "Car");
        String aliasedStr = aliased.toString();
        assertTrue(aliasedStr.contains("Car"),
            "toString should contain alias");
    }

    @Test
    @DisplayName("Should handle edge cases")
    public void testEdgeCases() {
        // Very long import path
        String longPath = "Level1::Level2::Level3::Level4::Level5::VeryDeeplyNestedElement";
        ImportStatement longImport = new ImportStatement(longPath,
            ImportType.SPECIFIC, true);
        assertEquals(longPath, longImport.getImportPath());

        // Import with special characters in names
        ImportStatement specialChars = new ImportStatement("Package_1::Element-2",
            ImportType.SPECIFIC, true);
        assertEquals("Package_1::Element-2", specialChars.getImportPath());

        // Wildcard at end
        ImportStatement wildcardEnd = new ImportStatement("A::B::C::*",
            ImportType.WILDCARD, true);
        assertEquals("A::B::C::*", wildcardEnd.getImportPath());

        // Multiple wildcards (invalid but should be handled by parser)
        // ImportStatement handles storage, not validation
        ImportStatement multiWildcard = new ImportStatement("A::*::*",
            ImportType.WILDCARD, true);
        assertEquals("A::*::*", multiWildcard.getImportPath());
    }

    @Test
    @DisplayName("Should handle symbol name conflicts")
    public void testNameConflicts() {
        ImportStatement importStmt = new ImportStatement("Automotive::*",
            ImportType.WILDCARD, true);

        // Add first symbol
        importStmt.addImportedSymbol(testSymbol1);
        assertEquals(1, importStmt.getImportedSymbols().size());

        // Add symbol with same name but different qualified name
        Symbol conflictSymbol = new Symbol("Vehicle", "Aerospace::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        // Adding conflicting symbol should replace the first one
        // (Last import wins in case of name conflict)
        importStmt.addImportedSymbol(conflictSymbol);

        // Should still have 1 symbol (replaced, not added)
        assertEquals(1, importStmt.getImportedSymbols().size());

        // Should resolve to the latest added symbol
        Symbol resolved = importStmt.resolve("Vehicle");
        assertEquals("Aerospace::Vehicle", resolved.getQualifiedName());
    }
}
