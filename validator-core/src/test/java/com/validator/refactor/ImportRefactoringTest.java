package com.validator.refactor;

import com.validator.semantic.ImportStatement;
import com.validator.semantic.ImportType;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImportRefactoringTest {

    @Test
    void testRefactorUnusedWildcard() {
        ImportUsageAnalyzer mockAnalyzer = new ImportUsageAnalyzer() {
            @Override
            public void analyze(ParseTree parseTree, List<ImportStatement> imports, SymbolTable symbolTable) {}

            @Override
            public Set<String> getUsedSymbols(ImportStatement imp) {
                return Collections.emptySet();
            }
        };

        ImportRefactoring refactoring = new ImportRefactoring(mockAnalyzer);
        ImportStatement imp = new ImportStatement("ISQ::*", ImportType.WILDCARD, true);

        List<RefactoringEdit> edits = refactoring.refactor(null, List.of(imp), null, "test.sysml");

        assertEquals(1, edits.size());
        RefactoringEdit edit = edits.get(0);
        assertEquals("LINT002", edit.getErrorCode());
        assertEquals("", edit.getNewText());
        assertEquals("Remove unused wildcard import", edit.getDescription());
    }

    @Test
    void testRefactorUsedWildcardSpecificImports() {
        ImportUsageAnalyzer mockAnalyzer = new ImportUsageAnalyzer() {
            @Override
            public void analyze(ParseTree parseTree, List<ImportStatement> imports, SymbolTable symbolTable) {}

            @Override
            public Set<String> getUsedSymbols(ImportStatement imp) {
                return new HashSet<>(Arrays.asList("MassValue", "LengthValue"));
            }
        };

        ImportRefactoring refactoring = new ImportRefactoring(mockAnalyzer);
        refactoring.setMaxSpecificImports(5);
        ImportStatement imp = new ImportStatement("ISQ::*", ImportType.WILDCARD, true);

        List<RefactoringEdit> edits = refactoring.refactor(null, List.of(imp), null, "test.sysml");

        assertEquals(1, edits.size());
        RefactoringEdit edit = edits.get(0);
        assertEquals("LINT017", edit.getErrorCode());
        
        // Output should be sorted: LengthValue, MassValue
        String expected = "public import ISQ::LengthValue;\npublic import ISQ::MassValue;";
        assertEquals(expected, edit.getNewText());
        assertTrue(edit.getDescription().contains("2 specific imports"));
    }

    @Test
    void testRefactorUsedWildcardFilteredImport() {
        ImportUsageAnalyzer mockAnalyzer = new ImportUsageAnalyzer() {
            @Override
            public void analyze(ParseTree parseTree, List<ImportStatement> imports, SymbolTable symbolTable) {}

            @Override
            public Set<String> getUsedSymbols(ImportStatement imp) {
                return new HashSet<>(Arrays.asList("A", "B", "C", "D"));
            }
        };

        ImportRefactoring refactoring = new ImportRefactoring(mockAnalyzer);
        refactoring.setMaxSpecificImports(3); // Less than 4 used symbols
        ImportStatement imp = new ImportStatement("Pkg::*", ImportType.WILDCARD, false); // private import

        List<RefactoringEdit> edits = refactoring.refactor(null, List.of(imp), null, "test.sysml");

        assertEquals(1, edits.size());
        RefactoringEdit edit = edits.get(0);
        
        // Output should use filtered import {A, B, C, D}
        String expected = "import Pkg::{A, B, C, D};";
        assertEquals(expected, edit.getNewText());
        assertTrue(edit.getDescription().contains("filtered import"));
    }
}
