package com.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The categories of lint and constraint codes, which LintConfig uses to turn classes of warnings on and off, and
 * the category descriptions of the code prefixes that ErrorCodesTest does not cover.
 */
@DisplayName("Error code categories")
class ErrorCodesCategoryTest {

    @ParameterizedTest(name = "{0} is in {1}")
    @CsvSource({
        "LINT001, unused", "LINT002, unused", "LINT004b, unused", "LINT004c, unused",
        "LINT003, missing-types", "LINT004, missing-types", "LINT004a, missing-types",
        "LINT005, naming", "LINT006, naming", "LINT006a, naming",
        "LINT007, documentation", "LINT016, documentation", "LINT015, documentation",
        "LINT008, complexity", "LINT013, complexity", "LINT014, complexity",
        "LINT009, best-practices", "LINT010, best-practices", "LINT011, best-practices", "LINT012, best-practices",
        "LINT017, imports", "LINT018, imports", "LINT019, imports",
        "LINT999, other"
    })
    @DisplayName("Lint codes map to their categories")
    void testLintCategory(String code, String category) {
        assertEquals(category, ErrorCodes.getLintCategory(code));
        assertTrue(ErrorCodes.isLintWarning(code));
        assertFalse(ErrorCodes.isConstraintError(code));
        assertEquals("Lint Warning", ErrorCodes.getCategoryDescription(code));
    }

    @Test
    @DisplayName("The lint code constants are the codes mapped")
    void testLintConstants() {
        assertEquals("unused", ErrorCodes.getLintCategory(ErrorCodes.LINT_UNUSED_DEFINITION));
        assertEquals("missing-types", ErrorCodes.getLintCategory(ErrorCodes.LINT_UNTYPED_PART));
        assertEquals("naming", ErrorCodes.getLintCategory(ErrorCodes.LINT_NAMING_PACKAGE));
        assertEquals("documentation", ErrorCodes.getLintCategory(ErrorCodes.LINT_TODO_FOUND));
        assertEquals("complexity", ErrorCodes.getLintCategory(ErrorCodes.LINT_DEEP_NESTING));
        assertEquals("best-practices", ErrorCodes.getLintCategory(ErrorCodes.LINT_IMPLICIT_VISIBILITY));
        assertEquals("imports", ErrorCodes.getLintCategory(ErrorCodes.LINT_PUBLIC_IMPORT));
    }

    @ParameterizedTest(name = "{0} is in {1}")
    @CsvSource({
        "CONST001, variable-scope",
        "CONST002, type-checking", "CONST005, type-checking",
        "CONST003, constraint-logic", "CONST004, constraint-logic",
        "CONST006, units",
        "CONST007, safety",
        "CONST999, other"
    })
    @DisplayName("Constraint codes map to their categories")
    void testConstraintCategory(String code, String category) {
        assertEquals(category, ErrorCodes.getConstraintCategory(code));
        assertTrue(ErrorCodes.isConstraintError(code));
        assertFalse(ErrorCodes.isLintWarning(code));
        assertEquals("Constraint Error", ErrorCodes.getCategoryDescription(code));
    }

    @Test
    @DisplayName("Other codes have no lint or constraint category")
    void testOtherCodes() {
        assertNull(ErrorCodes.getLintCategory(null));
        assertNull(ErrorCodes.getLintCategory(ErrorCodes.SYNTAX_ERROR));
        assertNull(ErrorCodes.getConstraintCategory(null));
        assertNull(ErrorCodes.getConstraintCategory("LINT001"));
        assertFalse(ErrorCodes.isLintWarning(null));
        assertFalse(ErrorCodes.isConstraintError(null));
        assertEquals("Constraint Error", ErrorCodes.getCategoryDescription(ErrorCodes.CONSTRAINT_UNIT_MISMATCH));
        assertFalse(ErrorCodes.isFatal(ErrorCodes.LINT_UNUSED_DEFINITION));
        assertFalse(ErrorCodes.isFatal(null));
    }
}
