package com.validator.refactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefactoringEditTest {

    @Test
    void testApplySingleLineReplace() {
        String content = "import ISQ::*;\npackage MyPackage { }";
        
        RefactoringEdit edit = RefactoringEdit.builder()
                .filePath("test.sysml")
                .startLine(1).startColumn(1)
                .endLine(1).endColumn(15) // length of "import ISQ::*;" + 1
                .oldText("import ISQ::*;")
                .newText("import ISQ::mass;")
                .build();

        String modified = edit.apply(content);
        assertEquals("import ISQ::mass;\npackage MyPackage { }", modified);
    }

    @Test
    void testApplyMultiLineReplace() {
        String content = "line1\nline2\nline3";
        
        RefactoringEdit edit = RefactoringEdit.builder()
                .filePath("test.sysml")
                .startLine(1).startColumn(6) // end of line 1 (after line1)
                .endLine(3).endColumn(1) // start of line 3
                .oldText("\nline2\n")
                .newText("\nnew_line2\n")
                .build();

        String modified = edit.apply(content);
        assertEquals("line1\nnew_line2\nline3", modified);
    }

    @Test
    void testApplyInvalidRange() {
        String content = "line1\nline2";
        
        RefactoringEdit edit = RefactoringEdit.builder()
                .filePath("test.sysml")
                .startLine(5).startColumn(1)
                .endLine(5).endColumn(10)
                .oldText("")
                .newText("invalid")
                .build();

        assertThrows(IllegalStateException.class, () -> edit.apply(content));
    }

    @Test
    void testFormatters() {
        RefactoringEdit edit = RefactoringEdit.builder()
                .filePath("test.sysml")
                .startLine(1).startColumn(1)
                .endLine(1).endColumn(10)
                .oldText("old")
                .newText("new")
                .description("desc")
                .errorCode("ERR-01")
                .build();

        assertTrue(edit.toJson().contains("\"file\": \"test.sysml\""));
        assertTrue(edit.toLspEdit().contains("\"line\": 0")); // 0-indexed in LSP
        assertTrue(edit.toMonacoEdit().contains("\"startLineNumber\": 1")); // 1-indexed in Monaco
        assertTrue(edit.toHumanReadable().contains("test.sysml:1:1"));
    }
}
