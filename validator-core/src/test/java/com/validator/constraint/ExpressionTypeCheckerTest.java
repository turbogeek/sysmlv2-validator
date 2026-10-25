package com.validator.constraint;

import com.validator.parser.SysMLv2Lexer;
import com.validator.parser.SysMLv2Parser;
import com.validator.semantic.ElementType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.Visibility;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ExpressionTypeChecker infers the type of an expression from its literals, the constraint scope, symbol names and
 * the operators that combine them.
 */
@DisplayName("Expression type checker")
class ExpressionTypeCheckerTest {

    private SymbolTable symbolTable;
    private ExpressionTypeChecker checker;
    private ConstraintScope scope;

    @BeforeEach
    void setUp() {
        symbolTable = new SymbolTable();
        checker = new ExpressionTypeChecker(symbolTable);
        scope = new ConstraintScope();
    }

    private static ParseTree expression(String text) {
        SysMLv2Parser parser = new SysMLv2Parser(new CommonTokenStream(new SysMLv2Lexer(CharStreams.fromString(text))));
        parser.removeErrorListeners();
        return parser.expression();
    }

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource(delimiter = '|', value = {
        "true            | BOOLEAN",
        "false           | BOOLEAN",
        "\"text\"        | STRING",
        "42              | INTEGER",
        "3.25            | REAL",
        "1.5e3           | REAL",
        "null            | ANY",
        "1 + 2           | INTEGER",
        "1 + 2.5         | REAL",
        "6 * 7 - 1       | INTEGER",
        "\"a\" + 1       | STRING",
        "1 < 2           | BOOLEAN",
        "true and false  | BOOLEAN",
        "true + 1        | ERROR",
        "1 and true      | ERROR",
        "not true        | BOOLEAN",
        "- 4             | INTEGER",
        "unknownThing    | ANY"
    })
    @DisplayName("Literals and operators")
    void testInfersType(String text, ConstraintType expected) {
        assertEquals(expected, checker.inferType(expression(text.trim()), scope));
    }

    @Test
    @DisplayName("A null expression is of any type")
    void testNullExpression() {
        assertEquals(ConstraintType.ANY, checker.inferType(null, scope));
    }

    @Test
    @DisplayName("Variables of the constraint scope have their declared type")
    void testScopeVariables() {
        scope.addVariable("x", ConstraintType.INTEGER);
        scope.addVariable("ok", ConstraintType.BOOLEAN);
        assertEquals(ConstraintType.INTEGER, checker.inferType(expression("x"), scope));
        assertEquals(ConstraintType.INTEGER, checker.inferType(expression("-x"), scope));
        assertEquals(ConstraintType.REAL, checker.inferType(expression("x * 0.5"), scope));
        assertEquals(ConstraintType.ERROR, checker.inferType(expression("ok + x"), scope));
    }

    @ParameterizedTest(name = "a symbol named {0} is {1}")
    @CsvSource({
        "isReady, BOOLEAN", "hasFuel, BOOLEAN", "boolFlag, BOOLEAN",
        "wheelCount, INTEGER", "serialNum, INTEGER", "index, INTEGER",
        "flowRate, REAL", "gearRatio, REAL", "value, REAL",
        "partName, STRING", "labelText, STRING", "idString, STRING",
        "mass, ANY"
    })
    @DisplayName("Symbols of the symbol table get a type from their name")
    void testSymbolTypesFromNames(String name, ConstraintType expected) {
        symbolTable.define(new Symbol(name, name, ElementType.ATTRIBUTE_USAGE, null, Visibility.PUBLIC));
        assertEquals(expected, checker.inferType(expression(name), scope));
    }

    @Test
    @DisplayName("checkType accepts compatible types and rejects others")
    void testCheckType() {
        assertTrue(checker.checkType(expression("1 + 2"), ConstraintType.REAL, scope), "Numeric types are compatible");
        assertTrue(checker.checkType(expression("1 < 2"), ConstraintType.BOOLEAN, scope));
        assertTrue(checker.checkType(expression("anything"), ConstraintType.STRING, scope), "ANY is compatible");
        assertFalse(checker.checkType(expression("true"), ConstraintType.INTEGER, scope));
        checker.clearCache();
        assertFalse(checker.checkType(expression("\"s\""), ConstraintType.BOOLEAN, scope));
    }

    @Test
    @DisplayName("checkBinaryOperation rejects operations whose result is an error")
    void testCheckBinaryOperation() {
        assertTrue(checker.checkBinaryOperation(ConstraintType.INTEGER, ConstraintType.REAL, "*"));
        assertTrue(checker.checkBinaryOperation(ConstraintType.BOOLEAN, ConstraintType.BOOLEAN, "or"));
        assertTrue(checker.checkBinaryOperation(ConstraintType.STRING, ConstraintType.STRING, "=="));
        assertFalse(checker.checkBinaryOperation(ConstraintType.INTEGER, ConstraintType.BOOLEAN, "+"));
        assertFalse(checker.checkBinaryOperation(ConstraintType.STRING, ConstraintType.BOOLEAN, "<"));
        assertFalse(checker.checkBinaryOperation(ConstraintType.ERROR, ConstraintType.INTEGER, "=="));
    }

    @ParameterizedTest(name = "''{0}'' expects {1}")
    @CsvSource({
        "and, BOOLEAN", "or, BOOLEAN", "not, BOOLEAN", "implies, BOOLEAN", "xor, BOOLEAN",
        "&&, BOOLEAN", "'||', BOOLEAN", "!, BOOLEAN",
        "+, NUMERIC", "-, NUMERIC", "*, NUMERIC", "/, NUMERIC", "%, NUMERIC", "**, NUMERIC",
        "==, ANY", "<, ANY"
    })
    @DisplayName("Operators expect operands of their kind")
    void testExpectedOperandType(String operator, ConstraintType expected) {
        assertEquals(expected, checker.getExpectedOperandType(operator));
    }
}
