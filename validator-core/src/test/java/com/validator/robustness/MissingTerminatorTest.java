package com.validator.robustness;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dropped semicolons, as the typo robustness harness ({@link TypoRobustnessTest}) makes them. The grammar makes the
 * ';' after most declarations optional, and 56 of 150 dropped semicolons of the harness's seeds were accepted without
 * an error; SysML v2 requires every one of them.
 */
@DisplayName("Dropped semicolons")
class MissingTerminatorTest {

    private final SysMLv2ParserFacade parser = new SysMLv2ParserFacade();

    private List<SyntaxError> errors(String source) {
        return parser.parseString(source, "typo.sysml").getSyntaxErrors();
    }

    @Test
    @DisplayName("A ';' dropped before a declaration on the next line is reported at that declaration")
    void droppedBeforeNextDeclaration() {
        // Before: two parts, no error
        List<SyntaxError> errors =
            errors("part def S {\n    part sensor : Sensor\n    part processor : Processor;\n}\n");
        assertEquals(1, errors.size(), errors.toString());
        assertEquals("missing ';' at 'part'", errors.get(0).getMessage());
        assertEquals(3, errors.get(0).getLine());
        assertEquals(4, errors.get(0).getCharPositionInLine());

        assertEquals(1, errors("state def T {\n    state red\n    state yellow;\n}\n").size());
        assertEquals(1, errors("enum def P {\n    enum Low;\n    enum Medium\n    enum High;\n}\n").size());
        assertEquals(1, errors("port def D {\n    in item dataIn\n    out item dataOut;\n}\n").size());
    }

    @Test
    @DisplayName("A ';' dropped before a '}' or the end of the file is reported there")
    void droppedBeforeEndOfBodyOrFile() {
        List<SyntaxError> errors = errors("part def Display {\n    port input : ~DataPort\n}\n");
        assertEquals(1, errors.size(), errors.toString());
        assertEquals("missing ';' at '}'", errors.get(0).getMessage());
        assertEquals(3, errors.get(0).getLine());
        assertEquals(0, errors.get(0).getCharPositionInLine());

        errors = errors("package P;\npart a : A");
        assertEquals(1, errors.size(), errors.toString());
        assertEquals("missing ';' at '<EOF>'", errors.get(0).getMessage());
    }

    @Test
    @DisplayName("Members that end without ';' in SysML v2, and declarations continued on the next line, are valid")
    void validSyntaxStaysValid() {
        // Documentation, comments and the result expression of a calculation end without ';'
        assertTrue(errors("part def A {\n    doc /* A part. */\n    comment c about A /* Note. */\n}\n").isEmpty());
        assertTrue(errors("calc def Twice {\n    in x : Real;\n    2 * x\n}\n").isEmpty());
        assertTrue(errors("constraint def Positive {\n    in x : Real;\n    x > 0\n}\n").isEmpty());
        // Keywords that continue a declaration on the next line
        assertTrue(errors("part def C {\n    part a;\n    part b;\n    connection ab\n        connect a to b;\n}\n")
            .isEmpty());
        assertTrue(errors("action def F {\n    action a;\n    action b;\n    first a\n        then b;\n}\n").isEmpty());
        // Declarations the grammar splits in two (it accepts them on one line in the OMG release and the library)
        assertTrue(errors("connection def S {\n    end inCart [0..1]\n        item cart : Cart[1];\n"
            + "    end selected [0..*]\n        item product : Product[1];\n}\n").isEmpty());
        assertTrue(errors("part def T {\n    timeslice\n        part b;\n    snapshot\n        part c;\n}\n")
            .isEmpty());
        assertTrue(errors("part def U {\n    satisfy\n        concern c;\n}\n").isEmpty());
    }

    @Test
    @DisplayName("A dropped ';' is reported together with a misspelled keyword, which leaves the tree intact")
    void reportedWithMisspelledKeyword() {
        List<SyntaxError> errors = errors("part def S {\n    part sensor : Sensor\n    part processor : Processor;\n"
            + "    attriubte mass : Real;\n}\n");
        assertEquals(2, errors.size(), errors.toString());
        assertEquals("missing ';' at 'part'", errors.get(1).getMessage());
        assertEquals(3, errors.get(1).getLine());
    }

    @Test
    @DisplayName("Only a tree without syntax errors is checked")
    void recoveredTreesAreNotChecked() {
        // The error recovery of 'part def := ;' leaves members without ';' that are not the user's
        List<SyntaxError> errors = errors("part def S {\n    part def := ;\n    part a : A\n}\n");
        assertTrue(errors.stream().noneMatch(e -> e.getMessage().equals("missing ';' at '}'")), errors.toString());
    }
}
