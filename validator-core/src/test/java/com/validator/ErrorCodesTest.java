package com.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ErrorCodes utility class.
 */
@DisplayName("Error Codes Tests")
class ErrorCodesTest {

    @Test
    @DisplayName("Should categorize syntax errors correctly")
    void testSyntaxErrorCategory() {
        assertEquals("Syntax Error", ErrorCodes.getCategoryDescription(ErrorCodes.SYNTAX_ERROR));
        assertEquals("Syntax Error", ErrorCodes.getCategoryDescription(ErrorCodes.SYNTAX_UNEXPECTED_TOKEN));
        assertEquals("Syntax Error", ErrorCodes.getCategoryDescription(ErrorCodes.SYNTAX_MISSING_ELEMENT));
    }

    @Test
    @DisplayName("Should categorize import errors correctly")
    void testImportErrorCategory() {
        assertEquals("Import Error", ErrorCodes.getCategoryDescription(ErrorCodes.IMPORT_PACKAGE_NOT_FOUND));
        assertEquals("Import Error", ErrorCodes.getCategoryDescription(ErrorCodes.IMPORT_ELEMENT_NOT_FOUND));
        assertEquals("Import Error", ErrorCodes.getCategoryDescription(ErrorCodes.IMPORT_ACCESS_DENIED));
        assertEquals("Import Error", ErrorCodes.getCategoryDescription(ErrorCodes.IMPORT_INVALID_SYNTAX));
    }

    @Test
    @DisplayName("Should categorize semantic errors correctly")
    void testSemanticErrorCategory() {
        assertEquals("Semantic Error", ErrorCodes.getCategoryDescription(ErrorCodes.SEMANTIC_DUPLICATE_SYMBOL));
        assertEquals("Semantic Error", ErrorCodes.getCategoryDescription(ErrorCodes.SEMANTIC_UNRESOLVED_REFERENCE));
        assertEquals("Semantic Error", ErrorCodes.getCategoryDescription(ErrorCodes.SEMANTIC_INVALID_SPECIALIZATION));
        assertEquals("Semantic Error", ErrorCodes.getCategoryDescription(ErrorCodes.SEMANTIC_INVALID_REDEFINITION));
        assertEquals("Semantic Error", ErrorCodes.getCategoryDescription(ErrorCodes.SEMANTIC_CIRCULAR_DEPENDENCY));
    }

    @Test
    @DisplayName("Should categorize type errors correctly")
    void testTypeErrorCategory() {
        assertEquals("Type Error", ErrorCodes.getCategoryDescription(ErrorCodes.TYPE_MISMATCH));
        assertEquals("Type Error", ErrorCodes.getCategoryDescription(ErrorCodes.TYPE_INVALID_MULTIPLICITY));
    }

    @Test
    @DisplayName("Should categorize I/O errors correctly")
    void testIOErrorCategory() {
        assertEquals("I/O Error", ErrorCodes.getCategoryDescription(ErrorCodes.IO_ERROR));
        assertEquals("I/O Error", ErrorCodes.getCategoryDescription(ErrorCodes.IO_UNSUPPORTED_FILE_TYPE));
        assertEquals("I/O Error", ErrorCodes.getCategoryDescription(ErrorCodes.IO_INVALID_KPAR));
    }

    @Test
    @DisplayName("Should handle null error code")
    void testNullErrorCode() {
        assertEquals("Unknown", ErrorCodes.getCategoryDescription(null));
    }

    @Test
    @DisplayName("Should handle unknown error code")
    void testUnknownErrorCode() {
        assertEquals("Validation Error", ErrorCodes.getCategoryDescription("UNKNOWN_ERROR"));
    }

    @Test
    @DisplayName("Should identify fatal I/O errors")
    void testFatalIOErrors() {
        assertTrue(ErrorCodes.isFatal(ErrorCodes.IO_ERROR));
        assertTrue(ErrorCodes.isFatal(ErrorCodes.IO_UNSUPPORTED_FILE_TYPE));
        assertTrue(ErrorCodes.isFatal(ErrorCodes.IO_INVALID_KPAR));
    }

    @Test
    @DisplayName("Should identify fatal syntax errors")
    void testFatalSyntaxErrors() {
        assertTrue(ErrorCodes.isFatal(ErrorCodes.SYNTAX_ERROR));
    }

    @Test
    @DisplayName("Should identify non-fatal errors")
    void testNonFatalErrors() {
        assertFalse(ErrorCodes.isFatal(ErrorCodes.IMPORT_PACKAGE_NOT_FOUND));
        assertFalse(ErrorCodes.isFatal(ErrorCodes.SEMANTIC_DUPLICATE_SYMBOL));
        assertFalse(ErrorCodes.isFatal(ErrorCodes.TYPE_MISMATCH));
        assertFalse(ErrorCodes.isFatal(null));
    }

    @Test
    @DisplayName("Should have non-empty error codes")
    void testErrorCodesNotEmpty() {
        assertFalse(ErrorCodes.SYNTAX_ERROR.isEmpty());
        assertFalse(ErrorCodes.IMPORT_PACKAGE_NOT_FOUND.isEmpty());
        assertFalse(ErrorCodes.SEMANTIC_DUPLICATE_SYMBOL.isEmpty());
        assertFalse(ErrorCodes.TYPE_MISMATCH.isEmpty());
        assertFalse(ErrorCodes.IO_ERROR.isEmpty());
    }

    @Test
    @DisplayName("Error codes should follow naming convention")
    void testErrorCodeNamingConvention() {
        // All error codes should be uppercase with underscores
        assertTrue(ErrorCodes.SYNTAX_ERROR.matches("[A-Z_]+"));
        assertTrue(ErrorCodes.IMPORT_PACKAGE_NOT_FOUND.matches("[A-Z_]+"));
        assertTrue(ErrorCodes.SEMANTIC_DUPLICATE_SYMBOL.matches("[A-Z_]+"));
        assertTrue(ErrorCodes.TYPE_MISMATCH.matches("[A-Z_]+"));
        assertTrue(ErrorCodes.IO_ERROR.matches("[A-Z_]+"));
    }
}
