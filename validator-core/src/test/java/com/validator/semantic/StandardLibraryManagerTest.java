package com.validator.semantic;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the StandardLibraryManager.
 */
@DisplayName("Standard Library Manager Tests")
public class StandardLibraryManagerTest {

    private StandardLibraryManager stdLib;

    @BeforeEach
    public void setUp() {
        stdLib = new StandardLibraryManager();
        stdLib.initializeBuiltins();
    }

    @Test
    @DisplayName("Should have primitive types")
    public void testPrimitiveTypes() {
        assertNotNull(stdLib.resolveSymbol("Boolean"), "Should have Boolean");
        assertNotNull(stdLib.resolveSymbol("Integer"), "Should have Integer");
        assertNotNull(stdLib.resolveSymbol("Real"), "Should have Real");
        assertNotNull(stdLib.resolveSymbol("String"), "Should have String");
        assertNotNull(stdLib.resolveSymbol("Natural"), "Should have Natural");
    }

    @Test
    @DisplayName("Should have KerML base types")
    public void testKerMLTypes() {
        assertNotNull(stdLib.resolveSymbol("KerML::Base"), "Should have Base");
        assertNotNull(stdLib.resolveSymbol("KerML::Anything"), "Should have Anything");
        assertNotNull(stdLib.resolveSymbol("KerML::Object"), "Should have Object");
    }

    @Test
    @DisplayName("Should have SysML base types")
    public void testSysMLTypes() {
        assertNotNull(stdLib.resolveSymbol("SysML::Part"), "Should have Part");
        assertNotNull(stdLib.resolveSymbol("SysML::Action"), "Should have Action");
        assertNotNull(stdLib.resolveSymbol("SysML::Requirement"), "Should have Requirement");
        assertNotNull(stdLib.resolveSymbol("SysML::State"), "Should have State");
        assertNotNull(stdLib.resolveSymbol("SysML::Port"), "Should have Port");
    }

    @Test
    @DisplayName("Should have ISQ quantities")
    public void testISQQuantities() {
        assertNotNull(stdLib.resolveSymbol("ISQ::MassValue"), "Should have MassValue");
        assertNotNull(stdLib.resolveSymbol("ISQ::LengthValue"), "Should have LengthValue");
        assertNotNull(stdLib.resolveSymbol("ISQ::TimeValue"), "Should have TimeValue");
        assertNotNull(stdLib.resolveSymbol("ISQ::TemperatureValue"), "Should have TemperatureValue");
        assertNotNull(stdLib.resolveSymbol("ISQ::VelocityValue"), "Should have VelocityValue");
        assertNotNull(stdLib.resolveSymbol("ISQ::ForceValue"), "Should have ForceValue");
    }

    @Test
    @DisplayName("Should have SI units")
    public void testSIUnits() {
        // Base SI units
        assertNotNull(stdLib.resolveSymbol("SI::m"), "Should have meter");
        assertNotNull(stdLib.resolveSymbol("SI::kg"), "Should have kilogram");
        assertNotNull(stdLib.resolveSymbol("SI::s"), "Should have second");
        assertNotNull(stdLib.resolveSymbol("SI::K"), "Should have kelvin");

        // Derived SI units
        assertNotNull(stdLib.resolveSymbol("SI::N"), "Should have newton");
        assertNotNull(stdLib.resolveSymbol("SI::Pa"), "Should have pascal");
        assertNotNull(stdLib.resolveSymbol("SI::J"), "Should have joule");
        assertNotNull(stdLib.resolveSymbol("SI::W"), "Should have watt");
    }

    @Test
    @DisplayName("Should support unqualified name resolution")
    public void testUnqualifiedResolution() {
        // Simple names should also work
        assertNotNull(stdLib.resolveSymbol("Real"), "Should resolve Real without qualifier");
        assertNotNull(stdLib.resolveSymbol("MassValue"), "Should resolve MassValue without qualifier");
        assertNotNull(stdLib.resolveSymbol("Part"), "Should resolve Part without qualifier");
    }

    @Test
    @DisplayName("Should get symbols by package")
    public void testGetSymbolsByPackage() {
        List<Symbol> isqSymbols = stdLib.getSymbolsInPackage("ISQ");
        assertFalse(isqSymbols.isEmpty(), "ISQ package should have symbols");
        assertTrue(isqSymbols.size() >= 10, "ISQ should have at least 10 quantity types");

        List<Symbol> siSymbols = stdLib.getSymbolsInPackage("SI");
        assertFalse(siSymbols.isEmpty(), "SI package should have symbols");
        assertTrue(siSymbols.size() >= 10, "SI should have at least 10 units");

        List<Symbol> kermlSymbols = stdLib.getSymbolsInPackage("KerML");
        assertFalse(kermlSymbols.isEmpty(), "KerML package should have symbols");
    }

    @Test
    @DisplayName("Should identify primitive types")
    public void testIsPrimitiveType() {
        assertTrue(stdLib.isPrimitiveType("Boolean"));
        assertTrue(stdLib.isPrimitiveType("Integer"));
        assertTrue(stdLib.isPrimitiveType("Real"));
        assertTrue(stdLib.isPrimitiveType("String"));
        assertTrue(stdLib.isPrimitiveType("Natural"));

        assertFalse(stdLib.isPrimitiveType("MassValue"));
        assertFalse(stdLib.isPrimitiveType("Part"));
    }

    @Test
    @DisplayName("Should identify quantity types")
    public void testIsQuantityType() {
        assertTrue(stdLib.isQuantityType("MassValue"));
        assertTrue(stdLib.isQuantityType("LengthValue"));
        assertTrue(stdLib.isQuantityType("VelocityValue"));
        assertTrue(stdLib.isQuantityType("QuantityValue"));

        assertFalse(stdLib.isQuantityType("Real"));
        assertFalse(stdLib.isQuantityType("Part"));
    }

    @Test
    @DisplayName("Should identify units")
    public void testIsUnit() {
        assertTrue(stdLib.isUnit("m"));
        assertTrue(stdLib.isUnit("kg"));
        assertTrue(stdLib.isUnit("s"));
        assertTrue(stdLib.isUnit("N"));
        assertTrue(stdLib.isUnit("Pa"));

        assertFalse(stdLib.isUnit("Real"));
        assertFalse(stdLib.isUnit("MassValue"));
    }

    @Test
    @DisplayName("Should return null for non-existent symbols")
    public void testNonExistentSymbol() {
        assertNull(stdLib.resolveSymbol("NonExistent"));
        assertNull(stdLib.resolveSymbol("Fake::Type"));
    }

    @Test
    @DisplayName("Should provide statistics")
    public void testStatistics() {
        StandardLibraryManager.StandardLibraryStats stats = stdLib.getStats();

        assertTrue(stats.getSymbolCount() > 0, "Should have symbols");
        assertTrue(stats.getPackageCount() >= 4, "Should have at least 4 packages (KerML, SysML, ISQ, SI)");
        assertFalse(stats.getSymbolsByType().isEmpty(), "Should have type breakdown");

        System.out.println(stats); // Print for visual verification
    }

    @Test
    @DisplayName("Should have all symbols with PUBLIC visibility")
    public void testSymbolVisibility() {
        for (Symbol symbol : stdLib.getAllSymbols()) {
            assertEquals(Visibility.PUBLIC, symbol.getVisibility(),
                "All standard library symbols should be PUBLIC");
        }
    }

    @Test
    @DisplayName("Should have <stdlib> as location")
    public void testSymbolLocations() {
        Symbol realSymbol = stdLib.resolveSymbol("Real");
        assertNotNull(realSymbol.getLocation());
        assertEquals("<stdlib>", realSymbol.getLocation().getFileName(),
            "Standard library symbols should have <stdlib> as location");
    }
}
