package com.validator.semantic;

import com.validator.ast.Location;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for import resolution.
 */
@DisplayName("Import Resolver Tests")
public class ImportResolverTest {

    private SymbolTable symbolTable;
    private StandardLibraryManager standardLibrary;
    private ImportResolver importResolver;

    @BeforeEach
    public void setUp() {
        symbolTable = new SymbolTable();
        standardLibrary = new StandardLibraryManager();
        standardLibrary.initializeBuiltins();
        importResolver = new ImportResolver(symbolTable, standardLibrary);

        // Create test model structure
        createTestModel();
    }

    @Test
    @DisplayName("Should resolve wildcard import")
    public void testWildcardImport() {
        // Create wildcard import
        ImportStatement importStmt = new ImportStatement("TestPackage::*", ImportType.WILDCARD, true);

        // Resolve
        Scope currentScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, currentScope);

        // Verify
        assertFalse(importStmt.getImportedSymbols().isEmpty(), "Should have imported symbols");
        assertTrue(importStmt.getImportedSymbols().containsKey("Vehicle"),
            "Should have imported Vehicle");
        assertTrue(importStmt.getImportedSymbols().containsKey("SpeedRequirement"),
            "Should have imported SpeedRequirement");
    }

    @Test
    @DisplayName("Should resolve specific import")
    public void testSpecificImport() {
        // Create specific import
        ImportStatement importStmt = new ImportStatement("TestPackage::Vehicle",
            ImportType.SPECIFIC, true);

        // Resolve
        Scope currentScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, currentScope);

        // Verify
        assertEquals(1, importStmt.getImportedSymbols().size(), "Should have imported 1 symbol");
        assertTrue(importStmt.getImportedSymbols().containsKey("Vehicle"),
            "Should have imported Vehicle");
        assertEquals("TestPackage::Vehicle",
            importStmt.getImportedSymbols().get("Vehicle").getQualifiedName());
    }

    @Test
    @DisplayName("Should resolve filtered import")
    public void testFilteredImport() {
        // Create filtered import
        ImportStatement importStmt = new ImportStatement("TestPackage::{Vehicle, SpeedRequirement}",
            ImportType.FILTERED, true);

        // Resolve
        Scope currentScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, currentScope);

        // Verify
        assertEquals(2, importStmt.getImportedSymbols().size(), "Should have imported 2 symbols");
        assertTrue(importStmt.getImportedSymbols().containsKey("Vehicle"));
        assertTrue(importStmt.getImportedSymbols().containsKey("SpeedRequirement"));
    }

    @Test
    @DisplayName("Should resolve aliased import")
    public void testAliasedImport() {
        // Create aliased import
        ImportStatement importStmt = new ImportStatement("TestPackage::Vehicle as Car",
            ImportType.ALIAS, true, "Car");

        // Resolve
        Scope currentScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, currentScope);

        // Verify
        assertEquals(1, importStmt.getImportedSymbols().size(), "Should have imported 1 symbol");
        assertTrue(importStmt.getImportedSymbols().containsKey("Car"),
            "Should use alias 'Car'");
        assertEquals("TestPackage::Vehicle",
            importStmt.getImportedSymbols().get("Car").getQualifiedName());
    }

    @Test
    @DisplayName("Should resolve standard library import")
    public void testStandardLibraryImport() {
        // Import from ISQ (standard library)
        ImportStatement importStmt = new ImportStatement("ISQ::MassValue",
            ImportType.SPECIFIC, true);

        // Resolve
        Scope currentScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, currentScope);

        // Verify
        assertEquals(1, importStmt.getImportedSymbols().size(), "Should have imported MassValue");
        assertTrue(importStmt.getImportedSymbols().containsKey("MassValue"));
        assertEquals("ISQ::MassValue",
            importStmt.getImportedSymbols().get("MassValue").getQualifiedName());
    }

    @Test
    @DisplayName("Should resolve wildcard import from standard library")
    public void testStandardLibraryWildcardImport() {
        // Import all from SI units
        ImportStatement importStmt = new ImportStatement("SI::*",
            ImportType.WILDCARD, true);

        // Resolve
        Scope currentScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, currentScope);

        // Verify
        assertFalse(importStmt.getImportedSymbols().isEmpty(), "Should have imported SI units");
        assertTrue(importStmt.getImportedSymbols().containsKey("m"), "Should have meter");
        assertTrue(importStmt.getImportedSymbols().containsKey("kg"), "Should have kilogram");
        assertTrue(importStmt.getImportedSymbols().containsKey("s"), "Should have second");
    }

    @Test
    @DisplayName("Should handle unresolved import")
    public void testUnresolvedImport() {
        // Try to import non-existent package
        ImportStatement importStmt = new ImportStatement("NonExistent::Element",
            ImportType.SPECIFIC, true);

        // Resolve
        Scope currentScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, currentScope);

        // Verify
        assertTrue(importStmt.getImportedSymbols().isEmpty(), "Should have no imported symbols");
        assertFalse(importResolver.getErrors().isEmpty(), "Should have error");
        assertTrue(importResolver.getErrors().get(0).contains("NonExistent"),
            "Error should mention the unresolved import");
    }

    @Test
    @DisplayName("Should respect visibility rules")
    public void testVisibilityRules() {
        // Create private symbol
        symbolTable.enterScope("PrivatePackage", ScopeType.PACKAGE);
        Symbol privateSymbol = new Symbol("PrivateElement", "PrivatePackage::PrivateElement",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1), Visibility.PRIVATE);
        symbolTable.define(privateSymbol);
        symbolTable.exitScope();

        // Try to import from different package
        ImportStatement importStmt = new ImportStatement("PrivatePackage::PrivateElement",
            ImportType.SPECIFIC, true);

        Scope globalScope = symbolTable.getGlobalScope();
        importResolver.resolveImport(importStmt, globalScope);

        // Private elements should not be imported from outside package
        assertTrue(importStmt.getImportedSymbols().isEmpty() || importResolver.getErrors().size() > 0,
            "Should not import private symbol from different package");
    }

    @Test
    @DisplayName("Should get import resolution statistics")
    public void testImportStats() {
        // Add some imports to a scope
        Scope scope = symbolTable.getGlobalScope();
        ImportStatement import1 = new ImportStatement("ISQ::MassValue", ImportType.SPECIFIC, true);
        ImportStatement import2 = new ImportStatement("TestPackage::*", ImportType.WILDCARD, true);

        scope.addImport(import1);
        scope.addImport(import2);

        // Resolve all
        importResolver.resolveAllImports();

        // Get stats
        ImportResolver.ImportResolutionStats stats = importResolver.getStats();

        assertEquals(2, stats.getTotalImports(), "Should have 2 imports");
        assertEquals(2, stats.getResolvedImports(), "Should have resolved both imports");
        assertEquals(0, stats.getUnresolvedImports(), "Should have no unresolved imports");
    }

    /**
     * Create a test model with some symbols to import.
     */
    private void createTestModel() {
        // Create a package
        symbolTable.enterScope("TestPackage", ScopeType.PACKAGE);
        Symbol packageSymbol = new Symbol("TestPackage", "TestPackage",
            ElementType.PACKAGE, new Location("test.sysml", 1, 1));
        symbolTable.define(packageSymbol);

        // Add some elements to the package
        Symbol vehicleSymbol = new Symbol("Vehicle", "TestPackage::Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 3, 1), Visibility.PUBLIC);
        symbolTable.define(vehicleSymbol);

        Symbol reqSymbol = new Symbol("SpeedRequirement", "TestPackage::SpeedRequirement",
            ElementType.REQUIREMENT_DEFINITION, new Location("test.sysml", 10, 1), Visibility.PUBLIC);
        symbolTable.define(reqSymbol);

        symbolTable.exitScope();
    }
}
