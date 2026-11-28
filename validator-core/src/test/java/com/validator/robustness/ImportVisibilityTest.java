package com.validator.robustness;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Imports without a visibility. SysML v2 8.2.2.5.1 and KerML 8.2.3.4.2 require 'public', 'private' or
 * 'protected' before every 'import'; the grammar made it optional, so {@code import ScalarValues::*;} was accepted.
 */
@DisplayName("Import visibility")
class ImportVisibilityTest {

    private final SysMLv2ParserFacade parser = new SysMLv2ParserFacade();

    private List<SyntaxError> errors(String source, String fileName) {
        return parser.parseString(source, fileName).getSyntaxErrors();
    }

    @Test
    @DisplayName("A bare namespace import is reported at 'import'")
    void bareNamespaceImport() {
        List<SyntaxError> errors = errors("package P {\n    import ScalarValues::*;\n    part def A;\n}\n", "p.sysml");
        assertEquals(1, errors.size(), errors.toString());
        assertEquals("missing visibility ('public', 'private' or 'protected') at 'import'", errors.get(0).getMessage());
        assertEquals(2, errors.get(0).getLine());
        assertEquals(4, errors.get(0).getCharPositionInLine());
    }

    @Test
    @DisplayName("Bare membership, recursive, 'all' and top-level imports are each reported")
    void otherBareImports() {
        List<SyntaxError> errors = errors(
            "import GeometryLibrary::*;\npackage P {\n    import SI::kg;\n    import all ISQ::**;\n"
                + "    import Lib::*[@Safety];\n}\n", "p.sysml");
        assertEquals(4, errors.size(), errors.toString());
    }

    @Test
    @DisplayName("Imports with a visibility, filter packages and expose are accepted")
    void visibleImportsAccepted() {
        List<SyntaxError> errors = errors(
            "package P {\n    private import ScalarValues::*;\n    public import SI::kg;\n"
                + "    protected import all ISQ::**;\n    private import Lib::*[@Safety];\n"
                + "    view v { expose P::**; }\n}\n", "p.sysml");
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    @DisplayName("KerML files need the visibility too")
    void kermlBareImport() {
        List<SyntaxError> errors = errors("package P {\n    import ScalarValues::*;\n}\n", "p.kerml");
        assertEquals(1, errors.size(), errors.toString());
    }
}
