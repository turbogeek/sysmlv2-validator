package com.validator;

/**
 * Centralized error codes for the SysML v2 Validator.
 *
 * <p>Error Code Format: CATEGORY_SPECIFIC_ERROR
 *
 * <h2>Error Categories</h2>
 * <ul>
 *   <li>SYNTAX_* - Parsing and syntax errors</li>
 *   <li>IMPORT_* - Import resolution errors</li>
 *   <li>SEMANTIC_* - Semantic analysis errors</li>
 *   <li>TYPE_* - Type checking errors</li>
 *   <li>IO_* - File I/O errors</li>
 * </ul>
 *
 * <h2>Error Resolution</h2>
 * <p>Each error code is designed to guide users toward resolution:
 * <ul>
 *   <li>Error messages include the offending construct</li>
 *   <li>Spelling suggestions are provided when applicable</li>
 *   <li>Line and column numbers pinpoint the error location</li>
 * </ul>
 */
public final class ErrorCodes {

    private ErrorCodes() {
        // Utility class - no instantiation
    }

    // =========================================================================
    // SYNTAX ERRORS - Parsing and grammar issues
    // =========================================================================

    /**
     * Generic syntax error from ANTLR parser.
     *
     * <p>Cause: The input does not conform to SysML v2 grammar.
     *
     * <p>Resolution: Check the syntax at the indicated line/column.
     * Common issues include:
     * <ul>
     *   <li>Missing semicolons or braces</li>
     *   <li>Mismatched parentheses</li>
     *   <li>Invalid keyword usage</li>
     *   <li>Misspelled keywords (suggestions provided)</li>
     * </ul>
     */
    public static final String SYNTAX_ERROR = "SYNTAX_ERROR";

    /**
     * Unexpected token encountered during parsing.
     *
     * <p>Cause: Parser found a token it didn't expect in the current context.
     *
     * <p>Resolution: Check for typos, missing operators, or incorrect ordering.
     */
    public static final String SYNTAX_UNEXPECTED_TOKEN = "SYNTAX_UNEXPECTED_TOKEN";

    /**
     * Missing required element in syntax construct.
     *
     * <p>Cause: A required syntactic element is absent.
     *
     * <p>Resolution: Add the missing element (name, type, etc.).
     */
    public static final String SYNTAX_MISSING_ELEMENT = "SYNTAX_MISSING_ELEMENT";

    // =========================================================================
    // IMPORT ERRORS - Import statement resolution
    // =========================================================================

    /**
     * Package not found for wildcard import.
     *
     * <p>Cause: The package path in a wildcard import (e.g., import Pkg::*)
     * does not match any defined package.
     *
     * <p>Resolution: Verify the package path. Check spelling (suggestions
     * are provided). Ensure the package is defined before the import.
     */
    public static final String IMPORT_PACKAGE_NOT_FOUND = "IMPORT_PACKAGE_NOT_FOUND";

    /**
     * Specific element not found for import.
     *
     * <p>Cause: The element specified in an import statement
     * (e.g., import Pkg::Element) does not exist.
     *
     * <p>Resolution: Verify the element name. Check if it's defined in
     * the specified package. Spelling suggestions are provided.
     */
    public static final String IMPORT_ELEMENT_NOT_FOUND = "IMPORT_ELEMENT_NOT_FOUND";

    /**
     * Element is not accessible due to visibility.
     *
     * <p>Cause: Attempting to import a private or protected element
     * from outside its allowed scope.
     *
     * <p>Resolution: Change the element's visibility to public, or
     * access it through a proper inheritance/containment relationship.
     */
    public static final String IMPORT_ACCESS_DENIED = "IMPORT_ACCESS_DENIED";

    /**
     * Invalid import syntax.
     *
     * <p>Cause: The import statement does not follow valid syntax.
     * Examples: missing braces in filtered import, missing 'as' in alias.
     *
     * <p>Resolution: Use correct import syntax:
     * <ul>
     *   <li>Wildcard: import Pkg::*</li>
     *   <li>Specific: import Pkg::Element</li>
     *   <li>Filtered: import Pkg::{A, B, C}</li>
     *   <li>Aliased: import Pkg::Element as Alias</li>
     * </ul>
     */
    public static final String IMPORT_INVALID_SYNTAX = "IMPORT_INVALID_SYNTAX";

    // =========================================================================
    // SEMANTIC ERRORS - Semantic analysis issues
    // =========================================================================

    /**
     * Duplicate symbol definition.
     *
     * <p>Cause: Two elements with the same name are defined in the same scope.
     *
     * <p>Resolution: Rename one of the elements or move to a different scope.
     */
    public static final String SEMANTIC_DUPLICATE_SYMBOL = "SEMANTIC_DUPLICATE_SYMBOL";

    /**
     * Unresolved reference.
     *
     * <p>Cause: A name reference could not be resolved to any known symbol.
     *
     * <p>Resolution: Define the referenced element, import it, or fix spelling.
     */
    public static final String SEMANTIC_UNRESOLVED_REFERENCE = "SEMANTIC_UNRESOLVED_REFERENCE";

    /**
     * Invalid specialization relationship.
     *
     * <p>Cause: A specialization (e.g., :> or specializes) references
     * an incompatible type.
     *
     * <p>Resolution: Ensure the specialized element is of compatible kind.
     */
    public static final String SEMANTIC_INVALID_SPECIALIZATION = "SEMANTIC_INVALID_SPECIALIZATION";

    /**
     * Invalid redefinition.
     *
     * <p>Cause: A redefinition does not properly constrain or override
     * the original feature.
     *
     * <p>Resolution: Ensure redefinition conforms to the original feature type.
     */
    public static final String SEMANTIC_INVALID_REDEFINITION = "SEMANTIC_INVALID_REDEFINITION";

    /**
     * Circular dependency detected.
     *
     * <p>Cause: A cycle exists in inheritance or containment hierarchy.
     *
     * <p>Resolution: Restructure the model to break the cycle.
     */
    public static final String SEMANTIC_CIRCULAR_DEPENDENCY = "SEMANTIC_CIRCULAR_DEPENDENCY";

    // =========================================================================
    // TYPE ERRORS - Type checking issues
    // =========================================================================

    /**
     * Type mismatch in assignment or binding.
     *
     * <p>Cause: A value or feature type does not match the expected type.
     *
     * <p>Resolution: Use the correct type or add explicit conversion.
     */
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";

    /**
     * Invalid multiplicity.
     *
     * <p>Cause: A multiplicity constraint is violated.
     *
     * <p>Resolution: Adjust cardinality to match the constraint.
     */
    public static final String TYPE_INVALID_MULTIPLICITY = "TYPE_INVALID_MULTIPLICITY";

    // =========================================================================
    // I/O ERRORS - File operations
    // =========================================================================

    /**
     * File could not be read.
     *
     * <p>Cause: The file does not exist, is not accessible, or has I/O errors.
     *
     * <p>Resolution: Verify the file path and permissions.
     */
    public static final String IO_ERROR = "IO_ERROR";

    /**
     * File is not a supported type.
     *
     * <p>Cause: The file extension is not .sysml, .kerml, or .kpar.
     *
     * <p>Resolution: Use a supported file type.
     */
    public static final String IO_UNSUPPORTED_FILE_TYPE = "IO_UNSUPPORTED_FILE_TYPE";

    /**
     * KPAR archive is corrupted or invalid.
     *
     * <p>Cause: The KPAR file is not a valid ZIP archive or is corrupted.
     *
     * <p>Resolution: Verify the KPAR file integrity, re-export if necessary.
     */
    public static final String IO_INVALID_KPAR = "IO_INVALID_KPAR";

    // =========================================================================
    // Utility methods
    // =========================================================================

    /**
     * Creates a user-friendly message for an error code.
     *
     * @param errorCode the error code
     * @return human-readable category name
     */
    public static String getCategoryDescription(String errorCode) {
        if (errorCode == null) {
            return "Unknown";
        }
        if (errorCode.startsWith("SYNTAX_")) {
            return "Syntax Error";
        }
        if (errorCode.startsWith("IMPORT_")) {
            return "Import Error";
        }
        if (errorCode.startsWith("SEMANTIC_")) {
            return "Semantic Error";
        }
        if (errorCode.startsWith("TYPE_")) {
            return "Type Error";
        }
        if (errorCode.startsWith("IO_")) {
            return "I/O Error";
        }
        return "Validation Error";
    }

    /**
     * Checks if an error code represents a fatal error.
     *
     * @param errorCode the error code
     * @return true if the error is fatal (prevents further processing)
     */
    public static boolean isFatal(String errorCode) {
        // I/O errors are fatal - can't continue without the file
        if (errorCode != null && errorCode.startsWith("IO_")) {
            return true;
        }
        // Syntax errors are fatal for that file
        if (SYNTAX_ERROR.equals(errorCode)) {
            return true;
        }
        return false;
    }
}
