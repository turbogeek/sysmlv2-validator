package com.validator.constraint;

import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Type checker for constraint expressions.
 *
 * <p>This class performs type inference and checking on constraint expressions
 * to detect type errors before runtime. It supports:
 * <ul>
 *   <li>Type inference from variable declarations</li>
 *   <li>Operator type checking</li>
 *   <li>Unit compatibility verification</li>
 *   <li>Literal type inference</li>
 * </ul>
 *
 * <h2>Type System</h2>
 * <p>The type system is based on KerML basic types:
 * <ul>
 *   <li>Boolean - logical true/false</li>
 *   <li>Integer - whole numbers</li>
 *   <li>Real - floating point numbers</li>
 *   <li>String - text values</li>
 *   <li>Any - unknown/generic type</li>
 * </ul>
 */
public class ExpressionTypeChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionTypeChecker.class);

    private final SymbolTable symbolTable;
    private final Map<String, ConstraintType> typeCache = new HashMap<>();

    /**
     * Creates a new expression type checker.
     *
     * @param symbolTable the symbol table for type resolution
     */
    public ExpressionTypeChecker(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * Infers the type of an expression.
     *
     * @param expr the expression parse tree
     * @param scope the variable scope
     * @return the inferred type
     */
    public ConstraintType inferType(ParseTree expr, ConstraintScope scope) {
        if (expr == null) {
            return ConstraintType.ANY;
        }

        String text = com.validator.parser.SysMLv2ParserFacade.getOriginalText(expr);

        // Check for literals
        ConstraintType literalType = inferLiteralType(text);
        if (literalType != ConstraintType.ANY) {
            return literalType;
        }

        // Check for variable reference
        if (scope.isDefined(text)) {
            return scope.getType(text);
        }

        // Check in symbol table
        Symbol symbol = symbolTable.resolve(text);
        if (symbol != null) {
            return inferSymbolType(symbol);
        }

        // For compound expressions, infer from children
        return inferCompoundType(expr, scope);
    }

    /**
     * Infers the type of a literal value.
     *
     * @param text the literal text
     * @return the inferred type
     */
    private ConstraintType inferLiteralType(String text) {
        if (text == null || text.isEmpty()) {
            return ConstraintType.ANY;
        }

        // Boolean literals
        if ("true".equals(text) || "false".equals(text)) {
            return ConstraintType.BOOLEAN;
        }

        // String literals (quoted)
        if ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'"))) {
            return ConstraintType.STRING;
        }

        // Integer literals
        if (text.matches("-?\\d+")) {
            return ConstraintType.INTEGER;
        }

        // Real literals
        if (text.matches("-?\\d+\\.\\d*([eE][+-]?\\d+)?")
                || text.matches("-?\\d*\\.\\d+([eE][+-]?\\d+)?")) {
            return ConstraintType.REAL;
        }

        // Null literal
        if ("null".equals(text)) {
            return ConstraintType.ANY;
        }

        return ConstraintType.ANY;
    }

    /**
     * Infers the type from a symbol.
     *
     * @param symbol the symbol
     * @return the inferred type
     */
    private ConstraintType inferSymbolType(Symbol symbol) {
        // Infer from name patterns since we don't have explicit type references
        String name = symbol.getName();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("bool") || lower.startsWith("is") || lower.startsWith("has")) {
                return ConstraintType.BOOLEAN;
            }
            if (lower.contains("count") || lower.contains("num") || lower.contains("index")) {
                return ConstraintType.INTEGER;
            }
            if (lower.contains("rate") || lower.contains("ratio") || lower.contains("value")) {
                return ConstraintType.REAL;
            }
            if (lower.contains("name") || lower.contains("text") || lower.contains("string")) {
                return ConstraintType.STRING;
            }
        }
        return ConstraintType.ANY;
    }

    /**
     * Infers the type of a compound expression.
     *
     * @param expr the expression
     * @param scope the variable scope
     * @return the inferred type
     */
    private ConstraintType inferCompoundType(ParseTree expr, ConstraintScope scope) {
        int childCount = expr.getChildCount();

        if (childCount == 0) {
            return ConstraintType.ANY;
        }

        // Unary expression
        if (childCount == 2) {
            String operator = expr.getChild(0).getText();
            if ("not".equals(operator) || "!".equals(operator)) {
                return ConstraintType.BOOLEAN;
            }
            if ("-".equals(operator) || "+".equals(operator)) {
                return inferType(expr.getChild(1), scope);
            }
        }

        // Binary expression (left op right)
        if (childCount == 3) {
            String operator = expr.getChild(1).getText();
            ConstraintType leftType = inferType(expr.getChild(0), scope);
            ConstraintType rightType = inferType(expr.getChild(2), scope);

            return ConstraintType.resultType(leftType, rightType, operator);
        }

        // Default to first child type
        return inferType(expr.getChild(0), scope);
    }

    /**
     * Checks if an expression is type-correct.
     *
     * @param expr the expression
     * @param expectedType the expected type
     * @param scope the variable scope
     * @return true if the expression matches the expected type
     */
    public boolean checkType(ParseTree expr, ConstraintType expectedType, ConstraintScope scope) {
        ConstraintType actualType = inferType(expr, scope);
        return actualType.isCompatibleWith(expectedType);
    }

    /**
     * Checks if a binary operation is type-correct.
     *
     * @param left left operand type
     * @param right right operand type
     * @param operator the operator
     * @return true if the operation is valid
     */
    public boolean checkBinaryOperation(
            ConstraintType left,
            ConstraintType right,
            String operator) {

        ConstraintType result = ConstraintType.resultType(left, right, operator);
        return result != ConstraintType.ERROR;
    }

    /**
     * Gets the expected operand type for an operator.
     *
     * @param operator the operator
     * @return the expected operand type
     */
    public ConstraintType getExpectedOperandType(String operator) {
        // Logical operators expect boolean
        if ("and".equals(operator) || "or".equals(operator) || "not".equals(operator)
                || "implies".equals(operator) || "xor".equals(operator)
                || "&&".equals(operator) || "||".equals(operator) || "!".equals(operator)) {
            return ConstraintType.BOOLEAN;
        }

        // Arithmetic operators expect numeric
        if ("+".equals(operator) || "-".equals(operator) || "*".equals(operator)
                || "/".equals(operator) || "%".equals(operator) || "**".equals(operator)) {
            return ConstraintType.NUMERIC;
        }

        // Comparison operators can compare compatible types
        return ConstraintType.ANY;
    }

    /**
     * Clears the type cache.
     */
    public void clearCache() {
        typeCache.clear();
    }
}
