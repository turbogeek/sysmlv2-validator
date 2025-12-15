package com.validator.constraint;

/**
 * Types used in constraint expression analysis.
 *
 * <p>These types are used for type checking within constraint expressions
 * and are mapped from SysML v2/KerML types.
 */
public enum ConstraintType {

    /**
     * Boolean type for logical values.
     * Corresponds to Boolean in KerML.
     */
    BOOLEAN("Boolean"),

    /**
     * Integer type for whole numbers.
     * Corresponds to Integer in KerML.
     */
    INTEGER("Integer"),

    /**
     * Real number type for floating point values.
     * Corresponds to Real in KerML.
     */
    REAL("Real"),

    /**
     * String type for text values.
     * Corresponds to String in KerML.
     */
    STRING("String"),

    /**
     * Numeric type (could be Integer or Real).
     * Used when exact numeric type is unknown.
     */
    NUMERIC("Numeric"),

    /**
     * Collection/sequence type.
     * Used for multiplicity > 1.
     */
    COLLECTION("Collection"),

    /**
     * Object type for structured values.
     * Used for parts, items, etc.
     */
    OBJECT("Object"),

    /**
     * Any type - used when type cannot be determined.
     */
    ANY("Any"),

    /**
     * Void type - used for expressions that don't return a value.
     */
    VOID("Void"),

    /**
     * Error type - used when type checking fails.
     */
    ERROR("Error");

    private final String name;

    ConstraintType(String name) {
        this.name = name;
    }

    /**
     * Gets the display name of this type.
     *
     * @return the type name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this type is numeric (Integer or Real).
     *
     * @return true if numeric
     */
    public boolean isNumeric() {
        return this == INTEGER || this == REAL || this == NUMERIC;
    }

    /**
     * Checks if this type is compatible with another type.
     *
     * @param other the other type
     * @return true if compatible
     */
    public boolean isCompatibleWith(ConstraintType other) {
        if (this == ANY || other == ANY) {
            return true;
        }
        if (this == other) {
            return true;
        }
        // Numeric types are compatible
        if (this.isNumeric() && other.isNumeric()) {
            return true;
        }
        return false;
    }

    /**
     * Gets the resulting type of a binary operation.
     *
     * @param left left operand type
     * @param right right operand type
     * @param operator the operator
     * @return the result type
     */
    public static ConstraintType resultType(
            ConstraintType left,
            ConstraintType right,
            String operator) {

        if (left == ERROR || right == ERROR) {
            return ERROR;
        }

        // Comparison operators return Boolean
        if (isComparisonOperator(operator)) {
            if (left.isCompatibleWith(right)) {
                return BOOLEAN;
            }
            return ERROR;
        }

        // Logical operators require Boolean operands
        if (isLogicalOperator(operator)) {
            if (left == BOOLEAN && right == BOOLEAN) {
                return BOOLEAN;
            }
            return ERROR;
        }

        // String concatenation (check before arithmetic since + is also arithmetic)
        if ("+".equals(operator) && (left == STRING || right == STRING)) {
            return STRING;
        }

        // Arithmetic operators
        if (isArithmeticOperator(operator)) {
            if (!left.isNumeric() || !right.isNumeric()) {
                return ERROR;
            }
            // Promote to Real if either is Real
            if (left == REAL || right == REAL) {
                return REAL;
            }
            return INTEGER;
        }

        return ANY;
    }

    /**
     * Checks if an operator is a comparison operator.
     */
    private static boolean isComparisonOperator(String op) {
        return "==".equals(op) || "!=".equals(op) || "===".equals(op)
            || "<".equals(op) || ">".equals(op)
            || "<=".equals(op) || ">=".equals(op);
    }

    /**
     * Checks if an operator is a logical operator.
     */
    private static boolean isLogicalOperator(String op) {
        return "and".equals(op) || "or".equals(op) || "not".equals(op)
            || "&&".equals(op) || "||".equals(op) || "!".equals(op)
            || "xor".equals(op) || "implies".equals(op);
    }

    /**
     * Checks if an operator is an arithmetic operator.
     */
    private static boolean isArithmeticOperator(String op) {
        return "+".equals(op) || "-".equals(op) || "*".equals(op)
            || "/".equals(op) || "%".equals(op) || "**".equals(op);
    }

    /**
     * Parses a constraint type from a SysML type name.
     *
     * @param typeName the SysML type name
     * @return the constraint type
     */
    public static ConstraintType fromTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return ANY;
        }

        String lower = typeName.toLowerCase();

        if (lower.contains("boolean") || lower.contains("bool")) {
            return BOOLEAN;
        }
        if (lower.contains("integer") || lower.contains("int")
                || lower.contains("natural")) {
            return INTEGER;
        }
        if (lower.contains("real") || lower.contains("double")
                || lower.contains("float")) {
            return REAL;
        }
        if (lower.contains("string") || lower.contains("text")) {
            return STRING;
        }

        return OBJECT;
    }

    @Override
    public String toString() {
        return name;
    }
}
