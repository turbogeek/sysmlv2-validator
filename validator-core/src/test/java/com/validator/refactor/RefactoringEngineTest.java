package com.validator.refactor;

import com.validator.refactor.RefactoringEngine.RefactoringPlan;
import com.validator.refactor.RefactoringEngine.OutputFormat;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RefactoringEngineTest {

    @Test
    void testAnalyzeContentWithErrorFreeOnlyBlocksParseErrors() {
        RefactoringEngine engine = new RefactoringEngine();
        engine.setErrorFreeOnly(true);
        
        String invalidContent = "package Pkg {\n  invalid syntax;\n}";
        RefactoringPlan plan = engine.analyzeContent(invalidContent, "test.sysml");
        
        assertFalse(plan.canApply(), "Plan should not be applicable when file has parse errors");
        assertEquals("File has parse errors - refactoring blocked", plan.getBlockedReason());
        assertFalse(plan.getErrors().isEmpty());
    }

    @Test
    void testAnalyzeContentWithoutErrorFreeOnlyAllowsParseErrors() {
        RefactoringEngine engine = new RefactoringEngine();
        engine.setErrorFreeOnly(false);
        
        String invalidContent = "package Pkg {\n  invalid syntax;\n}";
        RefactoringPlan plan = engine.analyzeContent(invalidContent, "test.sysml");
        
        assertNull(plan.getBlockedReason(), "Plan should not be blocked even with parse errors");
    }

    @Test
    void testFormatters() {
        RefactoringEngine engine = new RefactoringEngine();
        
        RefactoringEdit edit = RefactoringEdit.builder()
                .filePath("test.sysml")
                .startLine(1).startColumn(1)
                .endLine(1).endColumn(15)
                .oldText("import ISQ::*;")
                .newText("")
                .description("Remove unused import")
                .build();
                
        RefactoringPlan plan = new RefactoringPlan(
                "test.sysml",
                "import ISQ::*;",
                List.of(edit),
                Collections.emptyList(),
                1, 0, 1
        );
        
        engine.setOutputFormat(OutputFormat.JSON);
        String json = engine.formatOutput(plan);
        assertTrue(json.contains("\"test.sysml\""));
        assertTrue(json.contains("\"wildcardCount\": 1"));
        
        engine.setOutputFormat(OutputFormat.LSP);
        String lsp = engine.formatOutput(plan);
        assertTrue(lsp.contains("\"documentChanges\""));
        assertTrue(lsp.contains("\"uri\": \"file://test.sysml\""));
        
        engine.setOutputFormat(OutputFormat.MONACO);
        String monaco = engine.formatOutput(plan);
        assertTrue(monaco.contains("\"resource\": \"test.sysml\""));
        assertTrue(monaco.contains("\"edits\""));
        
        engine.setOutputFormat(OutputFormat.HUMAN);
        String human = engine.formatOutput(plan);
        assertTrue(human.contains("Unused (can remove): 1"));
    }
}
