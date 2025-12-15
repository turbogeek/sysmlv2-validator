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
    // LINT WARNINGS - Code quality and best practice issues
    // =========================================================================

    /**
     * Definition is declared but never used.
     *
     * <p>Cause: A part def, action def, or other definition is declared but
     * never referenced as a type or specialized.
     *
     * <p>Resolution: Remove the unused definition or add a usage.
     */
    public static final String LINT_UNUSED_DEFINITION = "LINT001";

    /**
     * Import is declared but never used.
     *
     * <p>Cause: An import statement brings in symbols that are never referenced.
     *
     * <p>Resolution: Remove the unused import.
     */
    public static final String LINT_UNUSED_IMPORT = "LINT002";

    /**
     * Usage references a type that is not defined.
     *
     * <p>Cause: A part, attribute, or other usage references a type that
     * doesn't exist in scope.
     *
     * <p>Resolution: Define the referenced type or import it.
     */
    public static final String LINT_MISSING_DEFINITION = "LINT003";

    /**
     * Attribute declared without a type.
     *
     * <p>Cause: An attribute usage does not specify a type.
     *
     * <p>Resolution: Add a type annotation (e.g., attribute x : Real).
     */
    public static final String LINT_UNTYPED_ATTRIBUTE = "LINT004";

    /**
     * Part declared without a type.
     *
     * <p>Cause: A part usage does not specify a type.
     *
     * <p>Resolution: Add a type annotation (e.g., part x : PartType).
     */
    public static final String LINT_UNTYPED_PART = "LINT004a";

    /**
     * Attribute is declared but never referenced.
     *
     * <p>Cause: An attribute is defined but never used in constraints,
     * calculations, or other expressions.
     *
     * <p>Resolution: Remove the unused attribute or reference it.
     */
    public static final String LINT_UNUSED_ATTRIBUTE = "LINT004b";

    /**
     * Part is declared but never referenced.
     *
     * <p>Cause: A part usage is defined but never referenced in connections,
     * constraints, or other relationships.
     *
     * <p>Resolution: Remove the unused part or reference it.
     */
    public static final String LINT_UNUSED_PART = "LINT004c";

    /**
     * Definition name doesn't follow PascalCase convention.
     *
     * <p>Cause: A part def, action def, or other definition uses lowercase
     * or incorrect naming.
     *
     * <p>Resolution: Rename to PascalCase (e.g., VehicleController).
     */
    public static final String LINT_NAMING_DEFINITION = "LINT005";

    /**
     * Usage name doesn't follow camelCase convention.
     *
     * <p>Cause: A part usage, attribute, or other usage uses PascalCase
     * or incorrect naming.
     *
     * <p>Resolution: Rename to camelCase (e.g., engineController).
     */
    public static final String LINT_NAMING_USAGE = "LINT006";

    /**
     * Package name doesn't follow PascalCase convention.
     *
     * <p>Cause: A package uses lowercase or incorrect naming.
     *
     * <p>Resolution: Rename to PascalCase (e.g., VehicleSystem).
     */
    public static final String LINT_NAMING_PACKAGE = "LINT006a";

    /**
     * Public definition missing documentation.
     *
     * <p>Cause: A public definition does not have a doc annotation.
     *
     * <p>Resolution: Add documentation using doc keyword.
     */
    public static final String LINT_MISSING_DOC = "LINT007";

    /**
     * Nesting exceeds recommended depth.
     *
     * <p>Cause: Definitions are nested more than 5 levels deep.
     *
     * <p>Resolution: Refactor into separate packages or use imports.
     */
    public static final String LINT_DEEP_NESTING = "LINT008";

    /**
     * Definition has no body or content.
     *
     * <p>Cause: A definition is declared with semicolon but no body.
     *
     * <p>Resolution: Add attributes, parts, or other content, or remove if unused.
     */
    public static final String LINT_EMPTY_DEFINITION = "LINT009";

    /**
     * Same element imported multiple times.
     *
     * <p>Cause: Duplicate import statements for the same element.
     *
     * <p>Resolution: Remove the redundant import.
     */
    public static final String LINT_REDUNDANT_IMPORT = "LINT010";

    /**
     * Local definition shadows an imported name.
     *
     * <p>Cause: A local definition has the same name as an imported symbol.
     *
     * <p>Resolution: Rename the local definition or use qualified names.
     */
    public static final String LINT_SHADOW_DEFINITION = "LINT011";

    /**
     * Definition lacks explicit visibility modifier.
     *
     * <p>Cause: A definition doesn't specify public, private, or protected.
     *
     * <p>Resolution: Add explicit visibility for clarity.
     */
    public static final String LINT_IMPLICIT_VISIBILITY = "LINT012";

    /**
     * Package contains too many elements.
     *
     * <p>Cause: A package has more than 50 direct child elements.
     *
     * <p>Resolution: Split into sub-packages for better organization.
     */
    public static final String LINT_LARGE_PACKAGE = "LINT013";

    /**
     * Action has too many parameters.
     *
     * <p>Cause: An action definition has more than 7 in/out parameters.
     *
     * <p>Resolution: Consider using a parameter object pattern.
     */
    public static final String LINT_MANY_PARAMETERS = "LINT014";

    /**
     * TODO or FIXME marker found in documentation.
     *
     * <p>Cause: Documentation contains TODO or FIXME markers.
     *
     * <p>Resolution: Complete the TODO or remove if resolved.
     */
    public static final String LINT_TODO_FOUND = "LINT015";

    /**
     * Empty documentation string.
     *
     * <p>Cause: Documentation uses doc "" or doc with empty content.
     *
     * <p>Resolution: Add meaningful documentation content.
     */
    public static final String LINT_EMPTY_DOC = "LINT016";

    /**
     * Wildcard import (::*) detected.
     *
     * <p>Cause: An import statement uses wildcard (e.g., import ISQ::*)
     * which imports all elements from a package.
     *
     * <p>Resolution: Replace with specific imports for only the elements used.
     * This makes dependencies explicit and avoids scope pollution.
     */
    public static final String LINT_WILDCARD_IMPORT = "LINT017";

    /**
     * Recursive wildcard import (::*::* or ::**) detected.
     *
     * <p>Cause: An import statement uses recursive wildcard which imports
     * all elements from a package and all nested packages.
     *
     * <p>Resolution: Replace with specific imports. Recursive wildcards
     * create very large scopes and should be avoided.
     */
    public static final String LINT_RECURSIVE_WILDCARD = "LINT018";

    /**
     * Public import detected.
     *
     * <p>Cause: An import statement uses 'public import' which re-exports
     * the imported elements to importers of this package.
     *
     * <p>Resolution: Consider using private import unless re-exporting is
     * intentional (facade pattern). Public imports create transitive dependencies.
     */
    public static final String LINT_PUBLIC_IMPORT = "LINT019";

    // =========================================================================
    // CONSTRAINT ERRORS - OCL-like constraint validation issues
    // =========================================================================

    /**
     * Variable in constraint is not defined.
     *
     * <p>Cause: A variable referenced in a constraint expression is not
     * in scope (not an attribute or parameter of the containing element).
     *
     * <p>Resolution: Define the variable or correct the reference.
     */
    public static final String CONSTRAINT_UNDEFINED_VAR = "CONST001";

    /**
     * Type mismatch in constraint expression.
     *
     * <p>Cause: Operands in a constraint expression have incompatible types
     * (e.g., comparing String to Integer).
     *
     * <p>Resolution: Use compatible types or add explicit conversion.
     */
    public static final String CONSTRAINT_TYPE_MISMATCH = "CONST002";

    /**
     * Constraint does not evaluate to boolean.
     *
     * <p>Cause: A constraint expression doesn't produce a boolean result.
     *
     * <p>Resolution: Ensure the expression is a boolean comparison or predicate.
     */
    public static final String CONSTRAINT_NOT_BOOLEAN = "CONST003";

    /**
     * Constraint is always true or always false.
     *
     * <p>Cause: Static analysis determines the constraint can never fail
     * or always fails.
     *
     * <p>Resolution: Review the constraint logic for correctness.
     */
    public static final String CONSTRAINT_UNREACHABLE = "CONST004";

    /**
     * Invalid operator for operand types.
     *
     * <p>Cause: An operator is used with incompatible operand types
     * (e.g., String - Integer).
     *
     * <p>Resolution: Use an operator valid for the operand types.
     */
    public static final String CONSTRAINT_INVALID_OPERATOR = "CONST005";

    /**
     * Incompatible units in constraint expression.
     *
     * <p>Cause: Values with incompatible units are compared
     * (e.g., meters vs kilograms).
     *
     * <p>Resolution: Ensure units are compatible for the operation.
     */
    public static final String CONSTRAINT_UNIT_MISMATCH = "CONST006";

    /**
     * Potential division by zero in constraint.
     *
     * <p>Cause: A division operation may have a zero divisor.
     *
     * <p>Resolution: Add a guard condition or ensure non-zero divisor.
     */
    public static final String CONSTRAINT_DIVIDE_BY_ZERO = "CONST007";

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
        if (errorCode.startsWith("LINT")) {
            return "Lint Warning";
        }
        if (errorCode.startsWith("CONST")) {
            return "Constraint Error";
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

    /**
     * Gets the lint category for a lint error code.
     * Used for enabling/disabling classes of lint warnings.
     *
     * @param errorCode the lint error code
     * @return the category name, or null if not a lint code
     */
    public static String getLintCategory(String errorCode) {
        if (errorCode == null || !errorCode.startsWith("LINT")) {
            return null;
        }
        // Map error codes to categories for easy enable/disable
        if (LINT_UNUSED_DEFINITION.equals(errorCode)
                || LINT_UNUSED_IMPORT.equals(errorCode)
                || LINT_UNUSED_ATTRIBUTE.equals(errorCode)
                || LINT_UNUSED_PART.equals(errorCode)) {
            return "unused";
        }
        if (LINT_MISSING_DEFINITION.equals(errorCode)
                || LINT_UNTYPED_ATTRIBUTE.equals(errorCode)
                || LINT_UNTYPED_PART.equals(errorCode)) {
            return "missing-types";
        }
        if (LINT_NAMING_DEFINITION.equals(errorCode)
                || LINT_NAMING_USAGE.equals(errorCode)
                || LINT_NAMING_PACKAGE.equals(errorCode)) {
            return "naming";
        }
        if (LINT_MISSING_DOC.equals(errorCode)
                || LINT_EMPTY_DOC.equals(errorCode)
                || LINT_TODO_FOUND.equals(errorCode)) {
            return "documentation";
        }
        if (LINT_DEEP_NESTING.equals(errorCode)
                || LINT_LARGE_PACKAGE.equals(errorCode)
                || LINT_MANY_PARAMETERS.equals(errorCode)) {
            return "complexity";
        }
        if (LINT_EMPTY_DEFINITION.equals(errorCode)
                || LINT_REDUNDANT_IMPORT.equals(errorCode)
                || LINT_SHADOW_DEFINITION.equals(errorCode)
                || LINT_IMPLICIT_VISIBILITY.equals(errorCode)) {
            return "best-practices";
        }
        if (LINT_WILDCARD_IMPORT.equals(errorCode)
                || LINT_RECURSIVE_WILDCARD.equals(errorCode)
                || LINT_PUBLIC_IMPORT.equals(errorCode)) {
            return "imports";
        }
        return "other";
    }

    /**
     * Gets the constraint category for a constraint error code.
     *
     * @param errorCode the constraint error code
     * @return the category name, or null if not a constraint code
     */
    public static String getConstraintCategory(String errorCode) {
        if (errorCode == null || !errorCode.startsWith("CONST")) {
            return null;
        }
        if (CONSTRAINT_UNDEFINED_VAR.equals(errorCode)) {
            return "variable-scope";
        }
        if (CONSTRAINT_TYPE_MISMATCH.equals(errorCode)
                || CONSTRAINT_INVALID_OPERATOR.equals(errorCode)) {
            return "type-checking";
        }
        if (CONSTRAINT_NOT_BOOLEAN.equals(errorCode)
                || CONSTRAINT_UNREACHABLE.equals(errorCode)) {
            return "constraint-logic";
        }
        if (CONSTRAINT_UNIT_MISMATCH.equals(errorCode)) {
            return "units";
        }
        if (CONSTRAINT_DIVIDE_BY_ZERO.equals(errorCode)) {
            return "safety";
        }
        return "other";
    }

    /**
     * Checks if an error code is a lint warning.
     *
     * @param errorCode the error code
     * @return true if it's a lint warning
     */
    public static boolean isLintWarning(String errorCode) {
        return errorCode != null && errorCode.startsWith("LINT");
    }

    /**
     * Checks if an error code is a constraint error.
     *
     * @param errorCode the error code
     * @return true if it's a constraint error
     */
    public static boolean isConstraintError(String errorCode) {
        return errorCode != null && errorCode.startsWith("CONST");
    }
}
