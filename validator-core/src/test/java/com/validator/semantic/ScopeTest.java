package com.validator.semantic;

import com.validator.ast.Location;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the Scope class.
 * Ensures 100% coverage of scope management, symbol resolution, and imports.
 */
@DisplayName("Scope Tests")
public class ScopeTest {

    private Location testLocation;
    private Scope globalScope;
    private Scope packageScope;
    private Scope partScope;

    @BeforeEach
    public void setUp() {
        testLocation = new Location("test.sysml", 1, 1);

        // Create scope hierarchy: Global -> Package -> Part
        globalScope = new Scope("", ScopeType.GLOBAL, null);
        packageScope = new Scope("TestPackage", ScopeType.PACKAGE, globalScope);
        partScope = new Scope("TestPart", ScopeType.PART_DEFINITION, packageScope);
    }

    @Test
    @DisplayName("Should create scope with valid parameters")
    public void testScopeCreation() {
        Scope scope = new Scope("MyScope", ScopeType.PACKAGE, null);

        assertEquals("MyScope", scope.getName());
        assertEquals(ScopeType.PACKAGE, scope.getType());
        assertNull(scope.getParent());
        assertNotNull(scope.getSymbols());
        assertTrue(scope.getSymbols().isEmpty());
        assertNotNull(scope.getImports());
        assertTrue(scope.getImports().isEmpty());
    }

    @Test
    @DisplayName("Should enforce null checks on construction")
    public void testNullValidation() {
        // Null name
        assertThrows(NullPointerException.class, () ->
            new Scope(null, ScopeType.PACKAGE, null),
            "Should throw NPE for null name");

        // Null scope type
        assertThrows(NullPointerException.class, () ->
            new Scope("MyScope", null, null),
            "Should throw NPE for null scope type");

        // Parent can be null (for global scope)
        Scope scopeWithoutParent = new Scope("Global", ScopeType.GLOBAL, null);
        assertNull(scopeWithoutParent.getParent());

        // Empty name is allowed (for global scope)
        Scope emptyName = new Scope("", ScopeType.GLOBAL, null);
        assertEquals("", emptyName.getName());
    }

    @Test
    @DisplayName("Should calculate qualified names correctly")
    public void testQualifiedName() {
        // Global scope has no prefix
        assertEquals("", globalScope.getQualifiedName());

        // Package scope
        assertEquals("TestPackage", packageScope.getQualifiedName());

        // Part scope (nested)
        assertEquals("TestPackage::TestPart", partScope.getQualifiedName());

        // Deep nesting
        Scope level1 = new Scope("Level1", ScopeType.PACKAGE, null);
        Scope level2 = new Scope("Level2", ScopeType.PACKAGE, level1);
        Scope level3 = new Scope("Level3", ScopeType.PART_DEFINITION, level2);
        Scope level4 = new Scope("Level4", ScopeType.PART_DEFINITION, level3);

        assertEquals("Level1", level1.getQualifiedName());
        assertEquals("Level1::Level2", level2.getQualifiedName());
        assertEquals("Level1::Level2::Level3", level3.getQualifiedName());
        assertEquals("Level1::Level2::Level3::Level4", level4.getQualifiedName());
    }

    @Test
    @DisplayName("Should define and lookup symbols")
    public void testDefineAndLookup() {
        Symbol symbol1 = new Symbol("Vehicle", "TestPackage::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol symbol2 = new Symbol("Engine", "TestPackage::Engine",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        // Define symbols
        packageScope.define(symbol1);
        packageScope.define(symbol2);

        // Lookup by name
        Symbol found = packageScope.lookup("Vehicle");
        assertNotNull(found);
        assertEquals(symbol1, found);

        found = packageScope.lookup("Engine");
        assertNotNull(found);
        assertEquals(symbol2, found);

        // Lookup non-existent
        found = packageScope.lookup("NonExistent");
        assertNull(found);

        // Get all symbols
        Map<String, Symbol> allSymbols = packageScope.getSymbols();
        assertEquals(2, allSymbols.size());
        assertTrue(allSymbols.containsKey("Vehicle"));
        assertTrue(allSymbols.containsKey("Engine"));
    }

    @Test
    @DisplayName("Should prevent duplicate symbol definitions")
    public void testDuplicateSymbolPrevention() {
        Symbol symbol1 = new Symbol("Vehicle", "TestPackage::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol symbol2 = new Symbol("Vehicle", "TestPackage::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        // Define first symbol
        packageScope.define(symbol1);
        assertEquals(1, packageScope.getSymbols().size());

        // Attempt to define duplicate
        assertThrows(IllegalStateException.class, () ->
            packageScope.define(symbol2),
            "Should throw ISE when defining duplicate symbol");

        // Should still have only 1 symbol
        assertEquals(1, packageScope.getSymbols().size());
    }

    @Test
    @DisplayName("Should resolve symbols through parent scopes")
    public void testHierarchicalResolution() {
        // Define symbols at different levels
        Symbol globalSymbol = new Symbol("GlobalElement", "GlobalElement",
            ElementType.DATA_TYPE, testLocation, Visibility.PUBLIC);
        Symbol packageSymbol = new Symbol("PackageElement", "TestPackage::PackageElement",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol partSymbol = new Symbol("PartElement", "TestPackage::TestPart::PartElement",
            ElementType.ATTRIBUTE_DEFINITION, testLocation, Visibility.PUBLIC);

        globalScope.define(globalSymbol);
        packageScope.define(packageSymbol);
        partScope.define(partSymbol);

        // Resolve from part scope
        Symbol resolved = partScope.resolve("PartElement");
        assertNotNull(resolved);
        assertEquals(partSymbol, resolved);

        resolved = partScope.resolve("PackageElement");
        assertNotNull(resolved);
        assertEquals(packageSymbol, resolved);

        resolved = partScope.resolve("GlobalElement");
        assertNotNull(resolved);
        assertEquals(globalSymbol, resolved);

        // Resolve from package scope (cannot see part scope)
        resolved = packageScope.resolve("PackageElement");
        assertNotNull(resolved);
        assertEquals(packageSymbol, resolved);

        resolved = packageScope.resolve("GlobalElement");
        assertNotNull(resolved);
        assertEquals(globalSymbol, resolved);

        // Cannot see child scope
        resolved = packageScope.resolve("PartElement");
        assertNull(resolved);
    }

    @Test
    @DisplayName("Should handle imports correctly")
    public void testImports() {
        ImportStatement import1 = new ImportStatement("OtherPackage::*",
            ImportType.WILDCARD, true);
        ImportStatement import2 = new ImportStatement("AnotherPackage::Element",
            ImportType.SPECIFIC, false);

        // Add imports
        packageScope.addImport(import1);
        packageScope.addImport(import2);

        // Verify imports
        assertEquals(2, packageScope.getImports().size());
        assertTrue(packageScope.getImports().contains(import1));
        assertTrue(packageScope.getImports().contains(import2));

        // Null check
        assertThrows(NullPointerException.class, () ->
            packageScope.addImport(null),
            "Should not allow null import");
    }

    @Test
    @DisplayName("Should resolve symbols through imports")
    public void testImportResolution() {
        // Create import with symbols
        ImportStatement importStmt = new ImportStatement("OtherPackage::*",
            ImportType.WILDCARD, true);

        Symbol importedSymbol = new Symbol("ImportedElement", "OtherPackage::ImportedElement",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        importStmt.addImportedSymbol(importedSymbol);

        // Add import to scope
        packageScope.addImport(importStmt);

        // Define a local symbol
        Symbol localSymbol = new Symbol("LocalElement", "TestPackage::LocalElement",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        packageScope.define(localSymbol);

        // Resolve local symbol (should take precedence)
        Symbol resolved = packageScope.resolve("LocalElement");
        assertNotNull(resolved);
        assertEquals(localSymbol, resolved);

        // Resolve imported symbol
        resolved = packageScope.resolve("ImportedElement");
        assertNotNull(resolved);
        assertEquals(importedSymbol, resolved);

        // Resolve non-existent
        resolved = packageScope.resolve("NonExistent");
        assertNull(resolved);
    }

    @Test
    @DisplayName("Should provide defensive copies of collections")
    public void testDefensiveCopies() {
        Symbol symbol = new Symbol("Element", "TestPackage::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        packageScope.define(symbol);

        ImportStatement importStmt = new ImportStatement("OtherPackage::*",
            ImportType.WILDCARD, true);
        packageScope.addImport(importStmt);

        // Get symbols map
        Map<String, Symbol> symbols = packageScope.getSymbols();
        assertThrows(UnsupportedOperationException.class, () ->
            symbols.put("Fail", symbol),
            "Symbols map should be unmodifiable");

        // Get imports list
        assertThrows(UnsupportedOperationException.class, () ->
            packageScope.getImports().add(importStmt),
            "Imports list should be unmodifiable");

        // Original scope should still have correct counts
        assertEquals(1, packageScope.getSymbols().size());
        assertEquals(1, packageScope.getImports().size());
    }

    @Test
    @DisplayName("Should handle different scope types")
    public void testDifferentScopeTypes() {
        ScopeType[] types = {
            ScopeType.GLOBAL,
            ScopeType.PACKAGE,
            ScopeType.PART_DEFINITION,
            ScopeType.PART_USAGE,
            ScopeType.ACTION_DEFINITION,
            ScopeType.ACTION_USAGE,
            ScopeType.STATE_DEFINITION,
            ScopeType.STATE_USAGE,
            ScopeType.REQUIREMENT_DEFINITION,
            ScopeType.REQUIREMENT_USAGE
        };

        for (ScopeType type : types) {
            Scope scope = new Scope("TestScope", type, null);
            assertEquals(type, scope.getType(),
                "Should correctly store scope type: " + type);
        }
    }

    @Test
    @DisplayName("Should handle visibility rules in resolution")
    public void testVisibilityResolution() {
        // Public symbol
        Symbol publicSymbol = new Symbol("PublicElement", "TestPackage::PublicElement",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        packageScope.define(publicSymbol);

        // Private symbol
        Symbol privateSymbol = new Symbol("PrivateElement", "TestPackage::PrivateElement",
            ElementType.PART_DEFINITION, testLocation, Visibility.PRIVATE);
        packageScope.define(privateSymbol);

        // Protected symbol
        Symbol protectedSymbol = new Symbol("ProtectedElement", "TestPackage::ProtectedElement",
            ElementType.PART_DEFINITION, testLocation, Visibility.PROTECTED);
        packageScope.define(protectedSymbol);

        // All should be resolvable from the same scope
        assertNotNull(packageScope.resolve("PublicElement"));
        assertNotNull(packageScope.resolve("PrivateElement"));
        assertNotNull(packageScope.resolve("ProtectedElement"));

        // Visibility enforcement happens at ImportResolver level
        // Scope just stores and returns symbols
    }

    @Test
    @DisplayName("Should handle empty scopes")
    public void testEmptyScope() {
        Scope emptyScope = new Scope("Empty", ScopeType.PACKAGE, null);

        // Empty collections
        assertTrue(emptyScope.getSymbols().isEmpty());
        assertTrue(emptyScope.getImports().isEmpty());

        // Cannot resolve anything
        assertNull(emptyScope.resolve("Anything"));
        assertNull(emptyScope.lookup("Anything"));
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    public void testToString() {
        Symbol symbol = new Symbol("Element", "TestPackage::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        packageScope.define(symbol);

        String str = packageScope.toString();

        // Should contain key information
        assertTrue(str.contains("TestPackage"), "toString should contain scope name");
        assertTrue(str.contains("PACKAGE"), "toString should contain scope type");

        // May contain symbol count
        // Implementation-dependent, but useful for debugging
    }

    @Test
    @DisplayName("Should handle scope hierarchy traversal")
    public void testScopeHierarchyTraversal() {
        // Build hierarchy
        Scope root = new Scope("Root", ScopeType.PACKAGE, null);
        Scope child1 = new Scope("Child1", ScopeType.PART_DEFINITION, root);
        Scope child2 = new Scope("Child2", ScopeType.PART_DEFINITION, root);
        Scope grandchild = new Scope("Grandchild", ScopeType.PART_DEFINITION, child1);

        // Verify parent relationships
        assertNull(root.getParent());
        assertEquals(root, child1.getParent());
        assertEquals(root, child2.getParent());
        assertEquals(child1, grandchild.getParent());

        // Verify qualified names
        assertEquals("Root", root.getQualifiedName());
        assertEquals("Root::Child1", child1.getQualifiedName());
        assertEquals("Root::Child2", child2.getQualifiedName());
        assertEquals("Root::Child1::Grandchild", grandchild.getQualifiedName());

        // Grandchild can see ancestors
        Symbol rootSymbol = new Symbol("RootElement", "Root::RootElement",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        root.define(rootSymbol);

        Symbol resolved = grandchild.resolve("RootElement");
        assertNotNull(resolved);
        assertEquals(rootSymbol, resolved);

        // Siblings cannot see each other
        Symbol child1Symbol = new Symbol("Child1Element", "Root::Child1::Child1Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        child1.define(child1Symbol);

        assertNull(child2.resolve("Child1Element"),
            "Sibling scopes should not see each other's symbols");
    }

    @Test
    @DisplayName("Should handle symbol shadowing")
    public void testSymbolShadowing() {
        // Define symbol in parent
        Symbol parentSymbol = new Symbol("Element", "TestPackage::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        packageScope.define(parentSymbol);

        // Define symbol with same name in child
        Symbol childSymbol = new Symbol("Element", "TestPackage::TestPart::Element",
            ElementType.ATTRIBUTE_DEFINITION, testLocation, Visibility.PUBLIC);
        partScope.define(childSymbol);

        // Resolve from child scope - should find child symbol (shadowing)
        Symbol resolved = partScope.resolve("Element");
        assertNotNull(resolved);
        assertEquals(childSymbol, resolved);
        assertEquals("TestPackage::TestPart::Element", resolved.getQualifiedName());

        // Resolve from parent scope - should find parent symbol
        resolved = packageScope.resolve("Element");
        assertNotNull(resolved);
        assertEquals(parentSymbol, resolved);
        assertEquals("TestPackage::Element", resolved.getQualifiedName());
    }

    @Test
    @DisplayName("Should handle complex import scenarios")
    public void testComplexImportScenarios() {
        // Multiple imports with potential conflicts
        ImportStatement import1 = new ImportStatement("Package1::*",
            ImportType.WILDCARD, true);
        ImportStatement import2 = new ImportStatement("Package2::*",
            ImportType.WILDCARD, true);
        ImportStatement import3 = new ImportStatement("Package3::Element as Alias",
            ImportType.ALIAS, true, "Alias");

        Symbol sym1 = new Symbol("Element", "Package1::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol sym2 = new Symbol("Element", "Package2::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol sym3 = new Symbol("Element", "Package3::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);

        import1.addImportedSymbol(sym1);
        import2.addImportedSymbol(sym2);
        import3.addImportedSymbol(sym3);

        packageScope.addImport(import1);
        packageScope.addImport(import2);
        packageScope.addImport(import3);

        // Resolve conflicting name - last import wins
        Symbol resolved = packageScope.resolve("Element");
        assertNotNull(resolved);
        // The actual resolution behavior depends on import order

        // Resolve by alias
        resolved = packageScope.resolve("Alias");
        assertNotNull(resolved);
        assertEquals(sym3, resolved);
    }
}
