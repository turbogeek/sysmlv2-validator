package com.validator.semantic;

import com.validator.ast.Location;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for symbol table functionality.
 * Tests complex scenarios like cross-package references, deep nesting, etc.
 */
@DisplayName("Symbol Table Integration Tests")
public class SymbolTableIntegrationTest {

    private SymbolTable symbolTable;
    private StandardLibraryManager standardLibrary;

    @BeforeEach
    public void setUp() {
        symbolTable = new SymbolTable();
        standardLibrary = new StandardLibraryManager();
        standardLibrary.initializeBuiltins();
    }

    @Test
    @DisplayName("Should handle complex model hierarchy")
    public void testComplexModelHierarchy() {
        // Create: Package1::Package2::PartA::PartB::Attribute
        symbolTable.enterScope("Package1", ScopeType.PACKAGE);
        defineSymbol("Package1", ElementType.PACKAGE);

        symbolTable.enterScope("Package2", ScopeType.PACKAGE);
        defineSymbol("Package2", ElementType.PACKAGE);

        symbolTable.enterScope("PartA", ScopeType.PART_DEFINITION);
        defineSymbol("PartA", ElementType.PART_DEFINITION);

        symbolTable.enterScope("PartB", ScopeType.PART_DEFINITION);
        defineSymbol("PartB", ElementType.PART_DEFINITION);

        defineSymbol("speed", ElementType.ATTRIBUTE_DEFINITION);

        // Verify qualified name is correct
        Symbol speedAttr = symbolTable.lookup("speed");
        assertNotNull(speedAttr);
        assertEquals("Package1::Package2::PartA::PartB::speed",
            speedAttr.getQualifiedName());

        // Exit all scopes
        symbolTable.exitScope(); // PartB
        symbolTable.exitScope(); // PartA
        symbolTable.exitScope(); // Package2
        symbolTable.exitScope(); // Package1

        // Verify symbol is in global registry
        Symbol resolved = symbolTable.resolveQualified("Package1::Package2::PartA::PartB::speed");
        assertNotNull(resolved);
        assertEquals("speed", resolved.getName());
    }

    @Test
    @DisplayName("Should handle cross-package references")
    public void testCrossPackageReferences() {
        // Package A has a part
        symbolTable.enterScope("PackageA", ScopeType.PACKAGE);
        defineSymbol("PackageA", ElementType.PACKAGE);
        defineSymbol("PartA", ElementType.PART_DEFINITION);
        symbolTable.exitScope();

        // Package B imports and uses PartA
        symbolTable.enterScope("PackageB", ScopeType.PACKAGE);
        defineSymbol("PackageB", ElementType.PACKAGE);

        // Add import
        ImportStatement importStmt = new ImportStatement("PackageA::PartA",
            ImportType.SPECIFIC, true);
        symbolTable.addImport(importStmt);

        // Resolve the import
        ImportResolver resolver = new ImportResolver(symbolTable, standardLibrary);
        resolver.resolveImport(importStmt, symbolTable.getCurrentScope());

        // Verify import resolved
        assertEquals(1, importStmt.getImportedSymbols().size());
        assertTrue(importStmt.getImportedSymbols().containsKey("PartA"));

        symbolTable.exitScope();
    }

    @Test
    @DisplayName("Should handle import chains (A imports B imports C)")
    public void testImportChains() {
        // Package C has a type
        symbolTable.enterScope("PackageC", ScopeType.PACKAGE);
        defineSymbol("PackageC", ElementType.PACKAGE);
        defineSymbol("TypeC", ElementType.DATA_TYPE);
        symbolTable.exitScope();

        // Package B imports C
        symbolTable.enterScope("PackageB", ScopeType.PACKAGE);
        defineSymbol("PackageB", ElementType.PACKAGE);

        ImportStatement importC = new ImportStatement("PackageC::TypeC",
            ImportType.SPECIFIC, true);
        symbolTable.addImport(importC);

        ImportResolver resolver = new ImportResolver(symbolTable, standardLibrary);
        resolver.resolveImport(importC, symbolTable.getCurrentScope());

        // Define TypeB which uses TypeC
        defineSymbol("TypeB", ElementType.DATA_TYPE);
        symbolTable.exitScope();

        // Package A imports B
        symbolTable.enterScope("PackageA", ScopeType.PACKAGE);
        defineSymbol("PackageA", ElementType.PACKAGE);

        ImportStatement importB = new ImportStatement("PackageB::TypeB",
            ImportType.SPECIFIC, true);
        symbolTable.addImport(importB);
        resolver.resolveImport(importB, symbolTable.getCurrentScope());

        // Verify imports resolved
        assertTrue(importB.getImportedSymbols().containsKey("TypeB"));
        assertTrue(importC.getImportedSymbols().containsKey("TypeC"));

        symbolTable.exitScope();
    }

    @Test
    @DisplayName("Should handle wildcard imports with many symbols")
    public void testWildcardImportPerformance() {
        // Create a package with many symbols
        symbolTable.enterScope("LargePackage", ScopeType.PACKAGE);
        defineSymbol("LargePackage", ElementType.PACKAGE);

        // Add 100 symbols
        for (int i = 0; i < 100; i++) {
            defineSymbol("Symbol" + i, ElementType.PART_DEFINITION);
        }
        symbolTable.exitScope();

        // Import all with wildcard
        symbolTable.enterScope("UserPackage", ScopeType.PACKAGE);
        ImportStatement wildcard = new ImportStatement("LargePackage::*",
            ImportType.WILDCARD, true);
        symbolTable.addImport(wildcard);

        long startTime = System.currentTimeMillis();
        ImportResolver resolver = new ImportResolver(symbolTable, standardLibrary);
        resolver.resolveImport(wildcard, symbolTable.getCurrentScope());
        long endTime = System.currentTimeMillis();

        // Verify all 101 symbols imported (package + 100 parts)
        assertTrue(wildcard.getImportedSymbols().size() >= 100);

        // Should be fast (<100ms)
        assertTrue(endTime - startTime < 100,
            String.format("Import took %d ms, expected <100ms", endTime - startTime));

        symbolTable.exitScope();
    }

    @Test
    @DisplayName("Should handle standard library imports efficiently")
    public void testStandardLibraryImports() {
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);

        // Import ISQ quantities
        ImportStatement isqImport = new ImportStatement("ISQ::*",
            ImportType.WILDCARD, true);
        symbolTable.addImport(isqImport);

        ImportResolver resolver = new ImportResolver(symbolTable, standardLibrary);
        long startTime = System.currentTimeMillis();
        resolver.resolveImport(isqImport, symbolTable.getCurrentScope());
        long endTime = System.currentTimeMillis();

        // Should import all ISQ quantity types
        assertTrue(isqImport.getImportedSymbols().size() >= 10);
        assertTrue(isqImport.getImportedSymbols().containsKey("MassValue"));
        assertTrue(isqImport.getImportedSymbols().containsKey("LengthValue"));

        // Should be very fast
        assertTrue(endTime - startTime < 50,
            String.format("Standard library import took %d ms, expected <50ms", endTime - startTime));

        symbolTable.exitScope();
    }

    @Test
    @DisplayName("Should handle symbols with relationships")
    public void testSymbolRelationships() {
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);

        // Create base part
        Symbol basePart = new Symbol("BasePart", "TestPackage::BasePart",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1));
        symbolTable.define(basePart);

        // Create specialized part
        Symbol specializedPart = new Symbol("SpecializedPart", "TestPackage::SpecializedPart",
            ElementType.PART_DEFINITION, new Location("test.sysml", 5, 1));
        specializedPart.addSpecialization(basePart);
        symbolTable.define(specializedPart);

        // Create redefined part
        Symbol redefinedPart = new Symbol("RedefinedPart", "TestPackage::RedefinedPart",
            ElementType.PART_DEFINITION, new Location("test.sysml", 10, 1));
        redefinedPart.addRedefinition(basePart);
        symbolTable.define(redefinedPart);

        // Verify relationships
        assertEquals(1, specializedPart.getSpecializations().size());
        assertEquals(basePart, specializedPart.getSpecializations().get(0));

        assertEquals(1, redefinedPart.getRedefinitions().size());
        assertEquals(basePart, redefinedPart.getRedefinitions().get(0));

        symbolTable.exitScope();
    }

    @Test
    @DisplayName("Should provide accurate statistics")
    public void testStatistics() {
        // Create a realistic model
        symbolTable.enterScope("VehicleModel", ScopeType.PACKAGE);
        defineSymbol("VehicleModel", ElementType.PACKAGE);

        // Add some parts
        for (int i = 0; i < 10; i++) {
            defineSymbol("Part" + i, ElementType.PART_DEFINITION);
        }

        // Add some requirements
        for (int i = 0; i < 5; i++) {
            defineSymbol("Req" + i, ElementType.REQUIREMENT_DEFINITION);
        }

        // Add some actions
        for (int i = 0; i < 3; i++) {
            defineSymbol("Action" + i, ElementType.ACTION_DEFINITION);
        }

        symbolTable.exitScope();

        // Get statistics
        SymbolTable.SymbolTableStats stats = symbolTable.getStats();

        // Verify counts
        assertTrue(stats.getSymbolCount() >= 19); // 1 package + 10 parts + 5 reqs + 3 actions
        assertTrue(stats.getSymbolsByType().get(ElementType.PART_DEFINITION) >= 10);
        assertTrue(stats.getSymbolsByType().get(ElementType.REQUIREMENT_DEFINITION) >= 5);
        assertTrue(stats.getSymbolsByType().get(ElementType.ACTION_DEFINITION) >= 3);
    }

    /**
     * Helper to define a symbol in the current scope.
     */
    private void defineSymbol(String name, ElementType type) {
        String qualifiedName = symbolTable.getCurrentScope().getQualifiedName();
        if (!qualifiedName.isEmpty()) {
            qualifiedName += "::";
        }
        qualifiedName += name;

        Symbol symbol = new Symbol(name, qualifiedName, type, new Location("test.sysml", 1, 1));
        symbolTable.define(symbol);
    }
}
