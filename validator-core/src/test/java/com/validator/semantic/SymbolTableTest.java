package com.validator.semantic;

import com.validator.ast.Location;
import org.junit.jupiter.api.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the SymbolTable class.
 * Ensures 100% coverage of symbol table operations, scope management, and resolution.
 */
@DisplayName("SymbolTable Tests")
public class SymbolTableTest {

    private SymbolTable symbolTable;
    private Location testLocation;

    @BeforeEach
    public void setUp() {
        symbolTable = new SymbolTable();
        testLocation = new Location("test.sysml", 1, 1);
    }

    @Test
    @DisplayName("Should create symbol table with global scope")
    public void testCreation() {
        assertNotNull(symbolTable);
        assertNotNull(symbolTable.getGlobalScope());
        assertNotNull(symbolTable.getCurrentScope());
        assertEquals(symbolTable.getGlobalScope(), symbolTable.getCurrentScope());
        assertEquals(ScopeType.GLOBAL, symbolTable.getCurrentScope().getType());
    }

    @Test
    @DisplayName("Should enter and exit scopes correctly")
    public void testScopeManagement() {
        // Initially at global scope
        assertEquals(ScopeType.GLOBAL, symbolTable.getCurrentScope().getType());
        assertEquals("", symbolTable.getCurrentScope().getQualifiedName());

        // Enter package scope
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);
        assertEquals("TestPackage", symbolTable.getCurrentScope().getName());
        assertEquals(ScopeType.PACKAGE, symbolTable.getCurrentScope().getType());
        assertEquals("TestPackage", symbolTable.getCurrentScope().getQualifiedName());

        // Enter part definition scope
        symbolTable.enterScope("Vehicle", ScopeType.PART_DEFINITION);
        assertEquals("Vehicle", symbolTable.getCurrentScope().getName());
        assertEquals(ScopeType.PART_DEFINITION, symbolTable.getCurrentScope().getType());
        assertEquals("TestPackage::Vehicle", symbolTable.getCurrentScope().getQualifiedName());

        // Exit back to package scope
        symbolTable.exitScope();
        assertEquals("TestPackage", symbolTable.getCurrentScope().getName());
        assertEquals(ScopeType.PACKAGE, symbolTable.getCurrentScope().getType());

        // Exit back to global scope
        symbolTable.exitScope();
        assertEquals(ScopeType.GLOBAL, symbolTable.getCurrentScope().getType());
    }

    @Test
    @DisplayName("Should prevent exiting beyond global scope")
    public void testExitScopeValidation() {
        // At global scope, cannot exit further
        assertThrows(IllegalStateException.class, () ->
            symbolTable.exitScope(),
            "Should throw ISE when trying to exit global scope");
    }

    @Test
    @DisplayName("Should define and resolve symbols")
    public void testDefineAndResolve() {
        // Enter package scope
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);

        // Define a symbol
        Symbol symbol = new Symbol("Vehicle", "TestPackage::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(symbol);

        // Resolve by simple name
        Symbol resolved = symbolTable.resolve("Vehicle");
        assertNotNull(resolved);
        assertEquals(symbol, resolved);

        // Resolve by qualified name
        resolved = symbolTable.resolveQualified("TestPackage::Vehicle");
        assertNotNull(resolved);
        assertEquals(symbol, resolved);

        // Resolve non-existent
        assertNull(symbolTable.resolve("NonExistent"));
        assertNull(symbolTable.resolveQualified("TestPackage::NonExistent"));
    }

    @Test
    @DisplayName("Should handle nested scope resolution")
    public void testNestedResolution() {
        // Create: Package::Part::Attribute
        symbolTable.enterScope("Package", ScopeType.PACKAGE);
        Symbol pkgSymbol = new Symbol("Package", "Package",
            ElementType.PACKAGE, testLocation, Visibility.PUBLIC);
        symbolTable.define(pkgSymbol);

        symbolTable.enterScope("Part", ScopeType.PART_DEFINITION);
        Symbol partSymbol = new Symbol("Part", "Package::Part",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(partSymbol);

        symbolTable.enterScope("Attribute", ScopeType.PART_DEFINITION);
        Symbol attrSymbol = new Symbol("speed", "Package::Part::Attribute::speed",
            ElementType.ATTRIBUTE_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(attrSymbol);

        // Resolve from deep scope
        Symbol resolved = symbolTable.resolve("speed");
        assertNotNull(resolved);
        assertEquals(attrSymbol, resolved);

        // Resolve parent symbols
        resolved = symbolTable.resolve("Part");
        assertNotNull(resolved);
        assertEquals(partSymbol, resolved);

        resolved = symbolTable.resolve("Package");
        assertNotNull(resolved);
        assertEquals(pkgSymbol, resolved);

        // Exit to Part scope
        symbolTable.exitScope();
        assertEquals("Package::Part", symbolTable.getCurrentScope().getQualifiedName());

        // Can still resolve Part and Package
        assertNotNull(symbolTable.resolve("Part"));
        assertNotNull(symbolTable.resolve("Package"));

        // Cannot resolve child scope symbols
        assertNull(symbolTable.resolve("speed"));
    }

    @Test
    @DisplayName("Should manage global symbol registry")
    public void testGlobalSymbolRegistry() {
        // Define symbols in different scopes
        symbolTable.enterScope("Package1", ScopeType.PACKAGE);
        Symbol sym1 = new Symbol("Element1", "Package1::Element1",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(sym1);
        symbolTable.exitScope();

        symbolTable.enterScope("Package2", ScopeType.PACKAGE);
        Symbol sym2 = new Symbol("Element2", "Package2::Element2",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(sym2);
        symbolTable.exitScope();

        // Back at global scope, resolve qualified names
        Symbol resolved = symbolTable.resolveQualified("Package1::Element1");
        assertNotNull(resolved);
        assertEquals(sym1, resolved);

        resolved = symbolTable.resolveQualified("Package2::Element2");
        assertNotNull(resolved);
        assertEquals(sym2, resolved);

        // Get all symbols
        Collection<Symbol> allSymbols = symbolTable.getAllSymbols();
        assertTrue(allSymbols.size() >= 2);
        assertTrue(allSymbols.contains(sym1));
        assertTrue(allSymbols.contains(sym2));
    }

    @Test
    @DisplayName("Should resolve scopes by qualified name")
    public void testResolveScope() {
        // Create nested scopes
        symbolTable.enterScope("Level1", ScopeType.PACKAGE);
        symbolTable.enterScope("Level2", ScopeType.PACKAGE);
        symbolTable.enterScope("Level3", ScopeType.PART_DEFINITION);

        // Exit back to global
        symbolTable.exitScope();
        symbolTable.exitScope();
        symbolTable.exitScope();

        // Resolve scopes by qualified name
        Scope scope = symbolTable.resolveScope("Level1");
        assertNotNull(scope);
        assertEquals("Level1", scope.getName());

        scope = symbolTable.resolveScope("Level1::Level2");
        assertNotNull(scope);
        assertEquals("Level2", scope.getName());
        assertEquals("Level1::Level2", scope.getQualifiedName());

        scope = symbolTable.resolveScope("Level1::Level2::Level3");
        assertNotNull(scope);
        assertEquals("Level3", scope.getName());
        assertEquals("Level1::Level2::Level3", scope.getQualifiedName());

        // Resolve non-existent scope
        assertNull(symbolTable.resolveScope("NonExistent"));
        assertNull(symbolTable.resolveScope("Level1::NonExistent"));
    }

    @Test
    @DisplayName("Should handle imports")
    public void testImports() {
        symbolTable.enterScope("Package1", ScopeType.PACKAGE);
        Symbol sym1 = new Symbol("Element", "Package1::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(sym1);
        symbolTable.exitScope();

        // Enter another package and add import
        symbolTable.enterScope("Package2", ScopeType.PACKAGE);

        ImportStatement importStmt = new ImportStatement("Package1::Element",
            ImportType.SPECIFIC, true);
        importStmt.addImportedSymbol(sym1);

        symbolTable.addImport(importStmt);

        // Verify import was added to current scope
        List<ImportStatement> imports = symbolTable.getCurrentScope().getImports();
        assertEquals(1, imports.size());
        assertTrue(imports.contains(importStmt));

        // Resolve imported symbol
        Symbol resolved = symbolTable.resolve("Element");
        assertNotNull(resolved);
        assertEquals(sym1, resolved);

        symbolTable.exitScope();
    }

    @Test
    @DisplayName("Should provide statistics")
    public void testStatistics() {
        // Create model structure
        symbolTable.enterScope("VehicleModel", ScopeType.PACKAGE);
        Symbol pkg = new Symbol("VehicleModel", "VehicleModel",
            ElementType.PACKAGE, testLocation, Visibility.PUBLIC);
        symbolTable.define(pkg);

        // Add parts
        for (int i = 0; i < 5; i++) {
            Symbol part = new Symbol("Part" + i, "VehicleModel::Part" + i,
                ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
            symbolTable.define(part);
        }

        // Add requirements
        for (int i = 0; i < 3; i++) {
            Symbol req = new Symbol("Req" + i, "VehicleModel::Req" + i,
                ElementType.REQUIREMENT_DEFINITION, testLocation, Visibility.PUBLIC);
            symbolTable.define(req);
        }

        symbolTable.exitScope();

        // Get statistics
        SymbolTable.SymbolTableStats stats = symbolTable.getStats();

        assertNotNull(stats);
        assertTrue(stats.getSymbolCount() >= 9); // 1 package + 5 parts + 3 reqs
        assertTrue(stats.getScopeCount() >= 2); // global + package

        Map<ElementType, Integer> byType = stats.getSymbolsByType();
        assertNotNull(byType);
        assertTrue(byType.getOrDefault(ElementType.PACKAGE, 0) >= 1);
        assertTrue(byType.getOrDefault(ElementType.PART_DEFINITION, 0) >= 5);
        assertTrue(byType.getOrDefault(ElementType.REQUIREMENT_DEFINITION, 0) >= 3);

        Map<ScopeType, Integer> byScope = stats.getSymbolsByScope();
        assertNotNull(byScope);
        assertTrue(byScope.containsKey(ScopeType.GLOBAL));
        assertTrue(byScope.containsKey(ScopeType.PACKAGE));
    }

    @Test
    @DisplayName("Should handle shadowing correctly")
    public void testShadowing() {
        // Define at package level
        symbolTable.enterScope("Package", ScopeType.PACKAGE);
        Symbol packageSymbol = new Symbol("Element", "Package::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(packageSymbol);

        // Define same name at nested level
        symbolTable.enterScope("Part", ScopeType.PART_DEFINITION);
        Symbol partSymbol = new Symbol("Element", "Package::Part::Element",
            ElementType.ATTRIBUTE_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(partSymbol);

        // From Part scope, should resolve to local (shadowing)
        Symbol resolved = symbolTable.resolve("Element");
        assertNotNull(resolved);
        assertEquals(partSymbol, resolved);
        assertEquals("Package::Part::Element", resolved.getQualifiedName());

        // Exit to Package scope
        symbolTable.exitScope();

        // From Package scope, should resolve to package level
        resolved = symbolTable.resolve("Element");
        assertNotNull(resolved);
        assertEquals(packageSymbol, resolved);
        assertEquals("Package::Element", resolved.getQualifiedName());
    }

    @Test
    @DisplayName("Should get symbols by type")
    public void testGetSymbolsByType() {
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);

        // Define various types
        Symbol part1 = new Symbol("Part1", "TestPackage::Part1",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol part2 = new Symbol("Part2", "TestPackage::Part2",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol req1 = new Symbol("Req1", "TestPackage::Req1",
            ElementType.REQUIREMENT_DEFINITION, testLocation, Visibility.PUBLIC);
        Symbol action1 = new Symbol("Action1", "TestPackage::Action1",
            ElementType.ACTION_DEFINITION, testLocation, Visibility.PUBLIC);

        symbolTable.define(part1);
        symbolTable.define(part2);
        symbolTable.define(req1);
        symbolTable.define(action1);

        symbolTable.exitScope();

        // Get by type
        List<Symbol> parts = symbolTable.getSymbolsByType(ElementType.PART_DEFINITION);
        assertEquals(2, parts.size());
        assertTrue(parts.contains(part1));
        assertTrue(parts.contains(part2));

        List<Symbol> reqs = symbolTable.getSymbolsByType(ElementType.REQUIREMENT_DEFINITION);
        assertEquals(1, reqs.size());
        assertTrue(reqs.contains(req1));

        List<Symbol> actions = symbolTable.getSymbolsByType(ElementType.ACTION_DEFINITION);
        assertEquals(1, actions.size());
        assertTrue(actions.contains(action1));

        List<Symbol> states = symbolTable.getSymbolsByType(ElementType.STATE_DEFINITION);
        assertTrue(states.isEmpty());
    }

    @Test
    @DisplayName("Should handle complex hierarchies")
    public void testComplexHierarchy() {
        // Build: Automotive::Vehicle::Engine::Cylinder
        symbolTable.enterScope("Automotive", ScopeType.PACKAGE);
        Symbol auto = new Symbol("Automotive", "Automotive",
            ElementType.PACKAGE, testLocation, Visibility.PUBLIC);
        symbolTable.define(auto);

        symbolTable.enterScope("Vehicle", ScopeType.PART_DEFINITION);
        Symbol vehicle = new Symbol("Vehicle", "Automotive::Vehicle",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(vehicle);

        symbolTable.enterScope("Engine", ScopeType.PART_DEFINITION);
        Symbol engine = new Symbol("Engine", "Automotive::Vehicle::Engine",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(engine);

        symbolTable.enterScope("Cylinder", ScopeType.PART_DEFINITION);
        Symbol cylinder = new Symbol("Cylinder", "Automotive::Vehicle::Engine::Cylinder",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(cylinder);

        // Verify current scope
        assertEquals("Automotive::Vehicle::Engine::Cylinder",
            symbolTable.getCurrentScope().getQualifiedName());

        // Resolve all levels
        assertNotNull(symbolTable.resolve("Cylinder"));
        assertNotNull(symbolTable.resolve("Engine"));
        assertNotNull(symbolTable.resolve("Vehicle"));
        assertNotNull(symbolTable.resolve("Automotive"));

        // Resolve by qualified names
        assertNotNull(symbolTable.resolveQualified("Automotive::Vehicle"));
        assertNotNull(symbolTable.resolveQualified("Automotive::Vehicle::Engine"));
        assertNotNull(symbolTable.resolveQualified("Automotive::Vehicle::Engine::Cylinder"));

        // Exit all scopes
        symbolTable.exitScope(); // Cylinder
        symbolTable.exitScope(); // Engine
        symbolTable.exitScope(); // Vehicle
        symbolTable.exitScope(); // Automotive

        // Should be at global scope
        assertEquals(ScopeType.GLOBAL, symbolTable.getCurrentScope().getType());

        // Can still resolve qualified names
        assertNotNull(symbolTable.resolveQualified("Automotive::Vehicle::Engine::Cylinder"));
    }

    @Test
    @DisplayName("Should handle null checks in operations")
    public void testNullChecks() {
        // Null scope name
        assertThrows(NullPointerException.class, () ->
            symbolTable.enterScope(null, ScopeType.PACKAGE),
            "Should throw NPE for null scope name");

        // Null scope type
        assertThrows(NullPointerException.class, () ->
            symbolTable.enterScope("Test", null),
            "Should throw NPE for null scope type");

        // Null symbol
        assertThrows(NullPointerException.class, () ->
            symbolTable.define(null),
            "Should throw NPE for null symbol");

        // Null import
        assertThrows(NullPointerException.class, () ->
            symbolTable.addImport(null),
            "Should throw NPE for null import");

        // Null name in resolve
        assertNull(symbolTable.resolve(null));

        // Null qualified name in resolveQualified
        assertNull(symbolTable.resolveQualified(null));

        // Null qualified name in resolveScope
        assertNull(symbolTable.resolveScope(null));
    }

    @Test
    @DisplayName("Should provide defensive copies")
    public void testDefensiveCopies() {
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);
        Symbol symbol = new Symbol("Element", "TestPackage::Element",
            ElementType.PART_DEFINITION, testLocation, Visibility.PUBLIC);
        symbolTable.define(symbol);
        symbolTable.exitScope();

        // Get all symbols
        Collection<Symbol> allSymbols = symbolTable.getAllSymbols();

        // Attempt to modify
        if (allSymbols instanceof List) {
            List<Symbol> symbolList = (List<Symbol>) allSymbols;
            assertThrows(UnsupportedOperationException.class, () ->
                symbolList.add(symbol),
                "All symbols collection should be unmodifiable");
        }

        // Get symbols by type
        List<Symbol> byType = symbolTable.getSymbolsByType(ElementType.PART_DEFINITION);
        assertThrows(UnsupportedOperationException.class, () ->
            byType.add(symbol),
            "Symbols by type list should be unmodifiable");
    }

    @Test
    @DisplayName("Should handle empty symbol table")
    public void testEmptySymbolTable() {
        // Fresh symbol table
        SymbolTable empty = new SymbolTable();

        // Should have global scope
        assertNotNull(empty.getGlobalScope());
        assertNotNull(empty.getCurrentScope());

        // No symbols defined
        Collection<Symbol> allSymbols = empty.getAllSymbols();
        assertTrue(allSymbols.isEmpty());

        // Cannot resolve anything
        assertNull(empty.resolve("Anything"));
        assertNull(empty.resolveQualified("Any::Thing"));

        // Statistics should show empty state
        SymbolTable.SymbolTableStats stats = empty.getStats();
        assertEquals(0, stats.getSymbolCount());
        assertTrue(stats.getScopeCount() >= 1); // At least global scope
    }

    @Test
    @DisplayName("Should maintain scope stack integrity")
    public void testScopeStackIntegrity() {
        // Enter multiple scopes
        symbolTable.enterScope("A", ScopeType.PACKAGE);
        symbolTable.enterScope("B", ScopeType.PACKAGE);
        symbolTable.enterScope("C", ScopeType.PART_DEFINITION);

        assertEquals("A::B::C", symbolTable.getCurrentScope().getQualifiedName());

        // Exit scopes
        symbolTable.exitScope();
        assertEquals("A::B", symbolTable.getCurrentScope().getQualifiedName());

        symbolTable.exitScope();
        assertEquals("A", symbolTable.getCurrentScope().getQualifiedName());

        symbolTable.exitScope();
        assertEquals("", symbolTable.getCurrentScope().getQualifiedName());
        assertEquals(ScopeType.GLOBAL, symbolTable.getCurrentScope().getType());

        // Cannot exit further
        assertThrows(IllegalStateException.class, () ->
            symbolTable.exitScope());
    }
}
