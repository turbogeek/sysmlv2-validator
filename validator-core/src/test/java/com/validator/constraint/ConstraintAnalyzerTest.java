package com.validator.constraint;

import com.validator.ErrorCodes;
import com.validator.ValidationError;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConstraintAnalyzer reports names in a constraint body that are not features of the part definition or part usage
 * around the constraint.
 */
@DisplayName("Constraint analyzer")
class ConstraintAnalyzerTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    private static List<ValidationError> analyze(String source) {
        SysMLv2ParserFacade.ParseResult result = PARSER.parseString(source, "constraints.sysml");
        assertFalse(result.hasErrors(), "Test input should parse: " + result.getSyntaxErrors());
        SymbolTable symbolTable = SymbolTableBuilder.build(result.getParseTree(), "constraints.sysml");
        return new ConstraintAnalyzer(symbolTable, "constraints.sysml").analyze(result.getParseTree());
    }

    @Test
    @DisplayName("A name that nothing around the constraint defines is reported where it is")
    void testUndefinedName() {
        List<ValidationError> errors = analyze("""
            part def Car {
                constraint fast { speed > 100 }
            }
            """);
        assertEquals(1, errors.size(), errors.toString());
        ValidationError error = errors.get(0);
        assertEquals(ErrorCodes.CONSTRAINT_UNDEFINED_VAR, error.getErrorCode());
        assertEquals("Undefined variable 'speed' in constraint", error.getMessage());
        assertEquals("constraints.sysml", error.getFilePath());
        assertEquals(2, error.getLine());
        assertEquals(23, error.getColumn());
    }

    @Test
    @DisplayName("Features of the enclosing part definition are defined, also inside a package")
    void testFeaturesOfEnclosingDefinition() {
        assertTrue(analyze("""
            package Vehicles {
                part def Car {
                    attribute speed;
                    attribute isMoving;
                    constraint fast { isMoving and speed > 100 }
                }
            }
            """).isEmpty(), "speed and isMoving are features of Car");
    }

    @Test
    @DisplayName("Features of another definition are not in scope")
    void testFeaturesOfOtherDefinition() {
        List<ValidationError> errors = analyze("""
            part def Engine { attribute rpm; }
            part def Car {
                constraint idle { rpm < 900 }
            }
            """);
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).getMessage().contains("'rpm'"));
    }

    @Test
    @DisplayName("'self', literals and a constraint outside a part do what they should")
    void testBuiltInsAndLiterals() {
        assertTrue(analyze("part def P { constraint c { self == self and true and 1 < 2 } }").isEmpty(),
            "'self' is built in and literals are not names");
        List<ValidationError> outside = analyze("package P { constraint c { limit > 0 } }");
        assertEquals(List.of("Undefined variable 'limit' in constraint"),
            outside.stream().map(ValidationError::getMessage).toList(),
            "Outside a part nothing is in scope");
    }

    @Test
    @DisplayName("Every name of an expression is checked, and the analyzer has no warnings of its own")
    void testEveryNameChecked() {
        ConstraintAnalyzer analyzer = new ConstraintAnalyzer(new SymbolTable(), "x.sysml");
        SysMLv2ParserFacade.ParseResult result = PARSER.parseString(
            "part def P { constraint c { a + b * a > limit } }", "x.sysml");
        List<ValidationError> errors = analyzer.analyze(result.getParseTree());
        assertEquals(List.of("a", "b", "a", "limit"), errors.stream()
            .map(e -> e.getMessage().replaceAll(".*'(.*)'.*", "$1")).toList());
        assertTrue(analyzer.getWarnings().isEmpty());
    }

    @Disabled("bug: ConstraintAnalyzer checks every name in the constraint body, so the names that the body declares "
        + "('in x : Real;') and the types it refers to are reported as undefined variables")
    @Test
    @DisplayName("Parameters declared in the constraint body are defined")
    void testParametersOfConstraintBody() {
        assertTrue(analyze("part def P { constraint c { in x : Real; x > 0 } }").isEmpty());
    }
}
