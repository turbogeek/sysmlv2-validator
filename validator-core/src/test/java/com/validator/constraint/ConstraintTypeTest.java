package com.validator.constraint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConstraintType enum.
 */
@DisplayName("Constraint Type Tests")
public class ConstraintTypeTest {

    @Test
    @DisplayName("Boolean type has correct name")
    public void testBooleanName() {
        assertEquals("Boolean", ConstraintType.BOOLEAN.getName());
    }

    @Test
    @DisplayName("Integer type has correct name")
    public void testIntegerName() {
        assertEquals("Integer", ConstraintType.INTEGER.getName());
    }

    @Test
    @DisplayName("Real type has correct name")
    public void testRealName() {
        assertEquals("Real", ConstraintType.REAL.getName());
    }

    @Test
    @DisplayName("String type has correct name")
    public void testStringName() {
        assertEquals("String", ConstraintType.STRING.getName());
    }

    @Test
    @DisplayName("Integer is numeric")
    public void testIntegerIsNumeric() {
        assertTrue(ConstraintType.INTEGER.isNumeric());
    }

    @Test
    @DisplayName("Real is numeric")
    public void testRealIsNumeric() {
        assertTrue(ConstraintType.REAL.isNumeric());
    }

    @Test
    @DisplayName("Numeric is numeric")
    public void testNumericIsNumeric() {
        assertTrue(ConstraintType.NUMERIC.isNumeric());
    }

    @Test
    @DisplayName("Boolean is not numeric")
    public void testBooleanNotNumeric() {
        assertFalse(ConstraintType.BOOLEAN.isNumeric());
    }

    @Test
    @DisplayName("String is not numeric")
    public void testStringNotNumeric() {
        assertFalse(ConstraintType.STRING.isNumeric());
    }

    @Test
    @DisplayName("ANY is compatible with everything")
    public void testAnyCompatibility() {
        assertTrue(ConstraintType.ANY.isCompatibleWith(ConstraintType.BOOLEAN));
        assertTrue(ConstraintType.ANY.isCompatibleWith(ConstraintType.INTEGER));
        assertTrue(ConstraintType.ANY.isCompatibleWith(ConstraintType.REAL));
        assertTrue(ConstraintType.ANY.isCompatibleWith(ConstraintType.STRING));
        assertTrue(ConstraintType.ANY.isCompatibleWith(ConstraintType.OBJECT));
    }

    @Test
    @DisplayName("Everything is compatible with ANY")
    public void testCompatibilityWithAny() {
        assertTrue(ConstraintType.BOOLEAN.isCompatibleWith(ConstraintType.ANY));
        assertTrue(ConstraintType.INTEGER.isCompatibleWith(ConstraintType.ANY));
        assertTrue(ConstraintType.REAL.isCompatibleWith(ConstraintType.ANY));
        assertTrue(ConstraintType.STRING.isCompatibleWith(ConstraintType.ANY));
    }

    @Test
    @DisplayName("Same types are compatible")
    public void testSameTypeCompatibility() {
        assertTrue(ConstraintType.BOOLEAN.isCompatibleWith(ConstraintType.BOOLEAN));
        assertTrue(ConstraintType.INTEGER.isCompatibleWith(ConstraintType.INTEGER));
        assertTrue(ConstraintType.REAL.isCompatibleWith(ConstraintType.REAL));
        assertTrue(ConstraintType.STRING.isCompatibleWith(ConstraintType.STRING));
    }

    @Test
    @DisplayName("Numeric types are mutually compatible")
    public void testNumericCompatibility() {
        assertTrue(ConstraintType.INTEGER.isCompatibleWith(ConstraintType.REAL));
        assertTrue(ConstraintType.REAL.isCompatibleWith(ConstraintType.INTEGER));
        assertTrue(ConstraintType.INTEGER.isCompatibleWith(ConstraintType.NUMERIC));
        assertTrue(ConstraintType.REAL.isCompatibleWith(ConstraintType.NUMERIC));
    }

    @Test
    @DisplayName("Boolean is not compatible with String")
    public void testBooleanStringIncompatible() {
        assertFalse(ConstraintType.BOOLEAN.isCompatibleWith(ConstraintType.STRING));
        assertFalse(ConstraintType.STRING.isCompatibleWith(ConstraintType.BOOLEAN));
    }

    @Test
    @DisplayName("Integer is not compatible with String")
    public void testIntegerStringIncompatible() {
        assertFalse(ConstraintType.INTEGER.isCompatibleWith(ConstraintType.STRING));
        assertFalse(ConstraintType.STRING.isCompatibleWith(ConstraintType.INTEGER));
    }

    @Test
    @DisplayName("Comparison operators return Boolean")
    public void testComparisonResultType() {
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.INTEGER, ConstraintType.INTEGER, "=="));
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.REAL, ConstraintType.REAL, "!="));
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.INTEGER, ConstraintType.REAL, "<"));
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.STRING, ConstraintType.STRING, "=="));
    }

    @Test
    @DisplayName("Incompatible comparison returns ERROR")
    public void testIncompatibleComparisonError() {
        assertEquals(ConstraintType.ERROR,
            ConstraintType.resultType(ConstraintType.STRING, ConstraintType.INTEGER, "<"));
    }

    @Test
    @DisplayName("Logical operators return Boolean")
    public void testLogicalResultType() {
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.BOOLEAN, ConstraintType.BOOLEAN, "and"));
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.BOOLEAN, ConstraintType.BOOLEAN, "or"));
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.BOOLEAN, ConstraintType.BOOLEAN, "&&"));
        assertEquals(ConstraintType.BOOLEAN,
            ConstraintType.resultType(ConstraintType.BOOLEAN, ConstraintType.BOOLEAN, "||"));
    }

    @Test
    @DisplayName("Logical operators with non-Boolean return ERROR")
    public void testLogicalNonBooleanError() {
        assertEquals(ConstraintType.ERROR,
            ConstraintType.resultType(ConstraintType.INTEGER, ConstraintType.BOOLEAN, "and"));
        assertEquals(ConstraintType.ERROR,
            ConstraintType.resultType(ConstraintType.BOOLEAN, ConstraintType.STRING, "or"));
    }

    @Test
    @DisplayName("Arithmetic with integers returns INTEGER")
    public void testArithmeticIntegerResult() {
        assertEquals(ConstraintType.INTEGER,
            ConstraintType.resultType(ConstraintType.INTEGER, ConstraintType.INTEGER, "+"));
        assertEquals(ConstraintType.INTEGER,
            ConstraintType.resultType(ConstraintType.INTEGER, ConstraintType.INTEGER, "-"));
        assertEquals(ConstraintType.INTEGER,
            ConstraintType.resultType(ConstraintType.INTEGER, ConstraintType.INTEGER, "*"));
    }

    @Test
    @DisplayName("Arithmetic with Real promotes to REAL")
    public void testArithmeticRealPromotion() {
        assertEquals(ConstraintType.REAL,
            ConstraintType.resultType(ConstraintType.INTEGER, ConstraintType.REAL, "+"));
        assertEquals(ConstraintType.REAL,
            ConstraintType.resultType(ConstraintType.REAL, ConstraintType.INTEGER, "*"));
        assertEquals(ConstraintType.REAL,
            ConstraintType.resultType(ConstraintType.REAL, ConstraintType.REAL, "/"));
    }

    @Test
    @DisplayName("Arithmetic with non-numeric returns ERROR")
    public void testArithmeticNonNumericError() {
        // String + Integer is now string concatenation, not arithmetic error
        // Use other arithmetic operators to test non-numeric error
        assertEquals(ConstraintType.ERROR,
            ConstraintType.resultType(ConstraintType.STRING, ConstraintType.INTEGER, "-"));
        assertEquals(ConstraintType.ERROR,
            ConstraintType.resultType(ConstraintType.BOOLEAN, ConstraintType.REAL, "*"));
    }

    @Test
    @DisplayName("String concatenation returns STRING")
    public void testStringConcatenation() {
        assertEquals(ConstraintType.STRING,
            ConstraintType.resultType(ConstraintType.STRING, ConstraintType.STRING, "+"));
        // Note: String + Integer returns ERROR in strict type checking
        // because implicit conversion is not supported in the current implementation
    }

    @Test
    @DisplayName("ERROR propagates through operations")
    public void testErrorPropagation() {
        assertEquals(ConstraintType.ERROR,
            ConstraintType.resultType(ConstraintType.ERROR, ConstraintType.INTEGER, "+"));
        assertEquals(ConstraintType.ERROR,
            ConstraintType.resultType(ConstraintType.BOOLEAN, ConstraintType.ERROR, "and"));
    }

    @Test
    @DisplayName("fromTypeName parses Boolean")
    public void testFromTypeNameBoolean() {
        assertEquals(ConstraintType.BOOLEAN, ConstraintType.fromTypeName("Boolean"));
        assertEquals(ConstraintType.BOOLEAN, ConstraintType.fromTypeName("boolean"));
        assertEquals(ConstraintType.BOOLEAN, ConstraintType.fromTypeName("Bool"));
    }

    @Test
    @DisplayName("fromTypeName parses Integer")
    public void testFromTypeNameInteger() {
        assertEquals(ConstraintType.INTEGER, ConstraintType.fromTypeName("Integer"));
        assertEquals(ConstraintType.INTEGER, ConstraintType.fromTypeName("integer"));
        assertEquals(ConstraintType.INTEGER, ConstraintType.fromTypeName("Int"));
        assertEquals(ConstraintType.INTEGER, ConstraintType.fromTypeName("Natural"));
    }

    @Test
    @DisplayName("fromTypeName parses Real")
    public void testFromTypeNameReal() {
        assertEquals(ConstraintType.REAL, ConstraintType.fromTypeName("Real"));
        assertEquals(ConstraintType.REAL, ConstraintType.fromTypeName("real"));
        assertEquals(ConstraintType.REAL, ConstraintType.fromTypeName("Double"));
        assertEquals(ConstraintType.REAL, ConstraintType.fromTypeName("Float"));
    }

    @Test
    @DisplayName("fromTypeName parses String")
    public void testFromTypeNameString() {
        assertEquals(ConstraintType.STRING, ConstraintType.fromTypeName("String"));
        assertEquals(ConstraintType.STRING, ConstraintType.fromTypeName("string"));
        assertEquals(ConstraintType.STRING, ConstraintType.fromTypeName("Text"));
    }

    @Test
    @DisplayName("fromTypeName returns OBJECT for unknown types")
    public void testFromTypeNameUnknown() {
        assertEquals(ConstraintType.OBJECT, ConstraintType.fromTypeName("Vehicle"));
        assertEquals(ConstraintType.OBJECT, ConstraintType.fromTypeName("MyCustomType"));
    }

    @Test
    @DisplayName("fromTypeName handles null and empty")
    public void testFromTypeNameNullEmpty() {
        assertEquals(ConstraintType.ANY, ConstraintType.fromTypeName(null));
        assertEquals(ConstraintType.ANY, ConstraintType.fromTypeName(""));
    }

    @Test
    @DisplayName("toString returns name")
    public void testToString() {
        assertEquals("Boolean", ConstraintType.BOOLEAN.toString());
        assertEquals("Integer", ConstraintType.INTEGER.toString());
        assertEquals("Real", ConstraintType.REAL.toString());
        assertEquals("String", ConstraintType.STRING.toString());
        assertEquals("Any", ConstraintType.ANY.toString());
        assertEquals("Error", ConstraintType.ERROR.toString());
    }
}
