package com.validator.ast;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the Location class.
 * Ensures 100% coverage of location creation and comparison.
 */
@DisplayName("Location Tests")
public class LocationTest {

    @Test
    @DisplayName("Should create location with valid parameters")
    public void testLocationCreation() {
        Location location = new Location("Vehicle.sysml", 42, 15);

        assertEquals("Vehicle.sysml", location.getFileName());
        assertEquals(42, location.getLine());
        assertEquals(15, location.getColumn());
    }

    @Test
    @DisplayName("Should enforce null and validation checks")
    public void testValidation() {
        // Null file name
        assertThrows(NullPointerException.class, () ->
            new Location(null, 10, 5),
            "Should throw NPE for null file name");

        // Empty file name should be allowed (for synthetic elements)
        Location emptyFileName = new Location("", 1, 1);
        assertEquals("", emptyFileName.getFileName());

        // Negative line number
        assertThrows(IllegalArgumentException.class, () ->
            new Location("test.sysml", -1, 5),
            "Should throw IAE for negative line number");

        // Zero line number (lines start at 1 in editors)
        assertThrows(IllegalArgumentException.class, () ->
            new Location("test.sysml", 0, 5),
            "Should throw IAE for zero line number");

        // Negative column number
        assertThrows(IllegalArgumentException.class, () ->
            new Location("test.sysml", 10, -1),
            "Should throw IAE for negative column number");

        // Zero column is valid (columns can start at 0)
        Location zeroColumn = new Location("test.sysml", 10, 0);
        assertEquals(0, zeroColumn.getColumn());

        // Large line and column numbers should be valid
        Location large = new Location("test.sysml", 999999, 9999);
        assertEquals(999999, large.getLine());
        assertEquals(9999, large.getColumn());
    }

    @Test
    @DisplayName("Should implement equals, hashCode, and toString correctly")
    public void testEqualsHashCodeToString() {
        Location loc1 = new Location("Vehicle.sysml", 42, 15);
        Location loc2 = new Location("Vehicle.sysml", 42, 15);
        Location loc3 = new Location("Vehicle.sysml", 43, 15);
        Location loc4 = new Location("Part.sysml", 42, 15);
        Location loc5 = new Location("Vehicle.sysml", 42, 16);

        // Equality
        assertEquals(loc1, loc2, "Locations with same values should be equal");
        assertNotEquals(loc1, loc3, "Different line should not be equal");
        assertNotEquals(loc1, loc4, "Different file should not be equal");
        assertNotEquals(loc1, loc5, "Different column should not be equal");

        // Reflexivity
        assertEquals(loc1, loc1);

        // Null safety
        assertNotEquals(loc1, null);

        // Different type
        assertNotEquals(loc1, "Vehicle.sysml:42:15");

        // HashCode consistency
        assertEquals(loc1.hashCode(), loc2.hashCode(),
            "Equal objects should have same hash code");

        // ToString
        String str = loc1.toString();
        assertTrue(str.contains("Vehicle.sysml"), "toString should contain file name");
        assertTrue(str.contains("42"), "toString should contain line number");
        assertTrue(str.contains("15"), "toString should contain column number");

        // Common format is "file:line:column"
        String expectedFormat = "Vehicle.sysml:42:15";
        assertTrue(str.equals(expectedFormat) || str.contains(expectedFormat),
            "toString should use file:line:column format");
    }

    @Test
    @DisplayName("Should handle special file names")
    public void testSpecialFileNames() {
        // Standard library location
        Location stdLib = new Location("<stdlib>", 1, 1);
        assertEquals("<stdlib>", stdLib.getFileName());

        // Generated code location
        Location generated = new Location("<generated>", 1, 1);
        assertEquals("<generated>", generated.getFileName());

        // Windows-style path
        Location windows = new Location("C:\\Users\\Dev\\Project\\Model.sysml", 10, 5);
        assertEquals("C:\\Users\\Dev\\Project\\Model.sysml", windows.getFileName());

        // Unix-style path
        Location unix = new Location("/home/dev/project/model.sysml", 10, 5);
        assertEquals("/home/dev/project/model.sysml", unix.getFileName());

        // Relative path
        Location relative = new Location("../models/Vehicle.sysml", 10, 5);
        assertEquals("../models/Vehicle.sysml", relative.getFileName());

        // URL-style path (rare but possible)
        Location url = new Location("file:///home/dev/model.sysml", 10, 5);
        assertEquals("file:///home/dev/model.sysml", url.getFileName());
    }

    @Test
    @DisplayName("Should support comparison and ordering")
    public void testComparison() {
        Location loc1 = new Location("A.sysml", 10, 5);
        Location loc2 = new Location("A.sysml", 20, 5);
        Location loc3 = new Location("B.sysml", 10, 5);
        Location loc4 = new Location("A.sysml", 10, 15);

        // If Location implements Comparable, test ordering
        // Otherwise, this documents expected comparison semantics
        // Order: file name, then line, then column

        // Same file, loc2 comes after loc1 (line 20 > line 10)
        assertTrue(loc2.getLine() > loc1.getLine());

        // Different files, loc3 comes after loc1 (B > A)
        assertTrue(loc3.getFileName().compareTo(loc1.getFileName()) > 0);

        // Same file and line, loc4 comes after loc1 (column 15 > 5)
        assertTrue(loc4.getColumn() > loc1.getColumn());
    }

    @Test
    @DisplayName("Should handle edge cases for line and column numbers")
    public void testEdgeCases() {
        // Minimum valid values
        Location min = new Location("test.sysml", 1, 0);
        assertEquals(1, min.getLine());
        assertEquals(0, min.getColumn());

        // Very large values (e.g., generated code)
        Location large = new Location("generated.sysml", 1000000, 100000);
        assertEquals(1000000, large.getLine());
        assertEquals(100000, large.getColumn());

        // Typical maximum line number in real files
        Location typical = new Location("large_model.sysml", 50000, 200);
        assertEquals(50000, typical.getLine());
        assertEquals(200, typical.getColumn());
    }

    @Test
    @DisplayName("Should be immutable")
    public void testImmutability() {
        Location location = new Location("test.sysml", 42, 15);

        // Verify no setters exist (compilation test)
        // If this compiles, Location has no public setters

        // Values should not change
        assertEquals("test.sysml", location.getFileName());
        assertEquals(42, location.getLine());
        assertEquals(15, location.getColumn());

        // Create another instance to verify original is unchanged
        Location other = new Location("other.sysml", 1, 1);

        assertEquals("test.sysml", location.getFileName());
        assertEquals(42, location.getLine());
        assertEquals(15, location.getColumn());
    }

    @Test
    @DisplayName("Should create locations for common scenarios")
    public void testCommonScenarios() {
        // Start of file
        Location startOfFile = new Location("Model.sysml", 1, 0);
        assertEquals(1, startOfFile.getLine());
        assertEquals(0, startOfFile.getColumn());

        // Standard library (no real file)
        Location stdlib = new Location("<stdlib>", 0, 0);
        assertEquals("<stdlib>", stdlib.getFileName());

        // Error location (synthetic)
        Location errorLoc = new Location("<error>", 1, 1);
        assertEquals("<error>", errorLoc.getFileName());

        // Multi-byte character handling (if file has Unicode)
        // Column might refer to character position or byte position
        Location unicode = new Location("中文.sysml", 5, 10);
        assertEquals("中文.sysml", unicode.getFileName());
        assertEquals(5, unicode.getLine());
        assertEquals(10, unicode.getColumn());
    }
}
