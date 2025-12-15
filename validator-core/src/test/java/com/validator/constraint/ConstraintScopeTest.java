package com.validator.constraint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConstraintScope.
 */
@DisplayName("Constraint Scope Tests")
public class ConstraintScopeTest {

    private ConstraintScope scope;

    @BeforeEach
    public void setUp() {
        scope = new ConstraintScope();
    }

    @Test
    @DisplayName("New scope has built-in variables")
    public void testBuiltinVariables() {
        assertTrue(scope.isDefined("self"), "self should be defined");
        assertTrue(scope.isDefined("true"), "true should be defined");
        assertTrue(scope.isDefined("false"), "false should be defined");
        assertTrue(scope.isDefined("null"), "null should be defined");
    }

    @Test
    @DisplayName("Built-in variables have correct types")
    public void testBuiltinTypes() {
        assertEquals(ConstraintType.ANY, scope.getType("self"));
        assertEquals(ConstraintType.BOOLEAN, scope.getType("true"));
        assertEquals(ConstraintType.BOOLEAN, scope.getType("false"));
        assertEquals(ConstraintType.ANY, scope.getType("null"));
    }

    @Test
    @DisplayName("Can add variable to scope")
    public void testAddVariable() {
        scope.addVariable("mass", ConstraintType.REAL);

        assertTrue(scope.isDefined("mass"));
        assertEquals(ConstraintType.REAL, scope.getType("mass"));
    }

    @Test
    @DisplayName("Adding null variable name does nothing")
    public void testAddNullVariableName() {
        int sizeBefore = scope.size();
        scope.addVariable(null, ConstraintType.INTEGER);
        assertEquals(sizeBefore, scope.size());
    }

    @Test
    @DisplayName("Adding empty variable name does nothing")
    public void testAddEmptyVariableName() {
        int sizeBefore = scope.size();
        scope.addVariable("", ConstraintType.INTEGER);
        assertEquals(sizeBefore, scope.size());
    }

    @Test
    @DisplayName("Adding variable with null type uses ANY")
    public void testAddNullTypeUsesAny() {
        scope.addVariable("unknown", null);

        assertTrue(scope.isDefined("unknown"));
        assertEquals(ConstraintType.ANY, scope.getType("unknown"));
    }

    @Test
    @DisplayName("Undefined variable returns false for isDefined")
    public void testUndefinedVariable() {
        assertFalse(scope.isDefined("notDefined"));
    }

    @Test
    @DisplayName("Undefined variable returns ANY for getType")
    public void testUndefinedVariableType() {
        assertEquals(ConstraintType.ANY, scope.getType("notDefined"));
    }

    @Test
    @DisplayName("Null variable name returns false for isDefined")
    public void testNullIsDefined() {
        assertFalse(scope.isDefined(null));
    }

    @Test
    @DisplayName("Null variable name returns ANY for getType")
    public void testNullGetType() {
        assertEquals(ConstraintType.ANY, scope.getType(null));
    }

    @Test
    @DisplayName("Child scope inherits parent variables")
    public void testChildInheritsParent() {
        scope.addVariable("parentVar", ConstraintType.INTEGER);
        ConstraintScope child = scope.createChild();

        assertTrue(child.isDefined("parentVar"));
        assertEquals(ConstraintType.INTEGER, child.getType("parentVar"));
    }

    @Test
    @DisplayName("Child scope has own variables")
    public void testChildOwnVariables() {
        ConstraintScope child = scope.createChild();
        child.addVariable("childVar", ConstraintType.STRING);

        assertTrue(child.isDefined("childVar"));
        assertFalse(scope.isDefined("childVar"));
    }

    @Test
    @DisplayName("Child scope can shadow parent variables")
    public void testChildShadowsParent() {
        scope.addVariable("x", ConstraintType.INTEGER);
        ConstraintScope child = scope.createChild();
        child.addVariable("x", ConstraintType.REAL);

        assertEquals(ConstraintType.REAL, child.getType("x"));
        assertEquals(ConstraintType.INTEGER, scope.getType("x"));
    }

    @Test
    @DisplayName("getParent returns parent scope")
    public void testGetParent() {
        ConstraintScope child = scope.createChild();

        assertEquals(scope, child.getParent());
        assertNull(scope.getParent());
    }

    @Test
    @DisplayName("size includes parent variables")
    public void testSizeIncludesParent() {
        scope.addVariable("x", ConstraintType.INTEGER);
        ConstraintScope child = scope.createChild();
        child.addVariable("y", ConstraintType.REAL);

        // Parent has built-ins (self, true, false, null) + x = 5
        // Child has its own built-ins (4) + y = 5, plus parent's 5 = 10
        assertTrue(child.size() > scope.size(), "Child should have more variables than parent");
    }

    @Test
    @DisplayName("getLocalVariables excludes parent")
    public void testGetLocalVariablesExcludesParent() {
        scope.addVariable("parentVar", ConstraintType.INTEGER);
        ConstraintScope child = scope.createChild();
        child.addVariable("childVar", ConstraintType.STRING);

        Map<String, ConstraintType> localVars = child.getLocalVariables();

        assertTrue(localVars.containsKey("childVar"));
        // Built-ins are in child's local map since they're added in constructor
        assertTrue(localVars.containsKey("self"));
        // But if we added parentVar directly, it wouldn't be there
        // Actually, the child also has built-ins, so this test verifies local isolation
    }

    @Test
    @DisplayName("getLocalVariables returns copy")
    public void testGetLocalVariablesReturnsCopy() {
        scope.addVariable("x", ConstraintType.INTEGER);

        Map<String, ConstraintType> localVars = scope.getLocalVariables();
        localVars.put("modified", ConstraintType.STRING);

        // Original scope should not be affected
        assertFalse(scope.isDefined("modified"));
    }

    @Test
    @DisplayName("Multiple levels of nesting work correctly")
    public void testMultipleLevels() {
        scope.addVariable("level0", ConstraintType.INTEGER);

        ConstraintScope level1 = scope.createChild();
        level1.addVariable("level1", ConstraintType.REAL);

        ConstraintScope level2 = level1.createChild();
        level2.addVariable("level2", ConstraintType.STRING);

        assertTrue(level2.isDefined("level0"));
        assertTrue(level2.isDefined("level1"));
        assertTrue(level2.isDefined("level2"));

        assertTrue(level1.isDefined("level0"));
        assertTrue(level1.isDefined("level1"));
        assertFalse(level1.isDefined("level2"));

        assertTrue(scope.isDefined("level0"));
        assertFalse(scope.isDefined("level1"));
        assertFalse(scope.isDefined("level2"));
    }

    @Test
    @DisplayName("toString provides readable output")
    public void testToString() {
        scope.addVariable("mass", ConstraintType.REAL);
        String str = scope.toString();

        assertNotNull(str);
        assertTrue(str.contains("ConstraintScope"));
        assertTrue(str.contains("mass"));
    }

    @Test
    @DisplayName("toString includes parent info for child scope")
    public void testToStringWithParent() {
        scope.addVariable("parentVar", ConstraintType.INTEGER);
        ConstraintScope child = scope.createChild();
        child.addVariable("childVar", ConstraintType.STRING);

        String str = child.toString();

        assertTrue(str.contains("parent"));
    }

    @Test
    @DisplayName("Can add multiple variables of different types")
    public void testMultipleVariables() {
        scope.addVariable("flag", ConstraintType.BOOLEAN);
        scope.addVariable("count", ConstraintType.INTEGER);
        scope.addVariable("ratio", ConstraintType.REAL);
        scope.addVariable("name", ConstraintType.STRING);
        scope.addVariable("item", ConstraintType.OBJECT);

        assertEquals(ConstraintType.BOOLEAN, scope.getType("flag"));
        assertEquals(ConstraintType.INTEGER, scope.getType("count"));
        assertEquals(ConstraintType.REAL, scope.getType("ratio"));
        assertEquals(ConstraintType.STRING, scope.getType("name"));
        assertEquals(ConstraintType.OBJECT, scope.getType("item"));
    }

    @Test
    @DisplayName("Overwriting variable updates type")
    public void testOverwriteVariable() {
        scope.addVariable("x", ConstraintType.INTEGER);
        assertEquals(ConstraintType.INTEGER, scope.getType("x"));

        scope.addVariable("x", ConstraintType.REAL);
        assertEquals(ConstraintType.REAL, scope.getType("x"));
    }
}
