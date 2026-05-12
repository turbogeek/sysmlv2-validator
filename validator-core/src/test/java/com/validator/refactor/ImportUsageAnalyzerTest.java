package com.validator.refactor;

import com.validator.ast.Location;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.ImportStatement;
import com.validator.semantic.ImportType;
import com.validator.semantic.Symbol;
import com.validator.semantic.ElementType;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImportUsageAnalyzerTest {

    private ImportUsageAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ImportUsageAnalyzer();
    }

    @Test
    void testWildcardUsageIsTracked() {
        // Parse simple SysML containing references to symbols
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
        String sysml = "import Pkg::*; package MyPkg { part myPart : TypeA; }";
        ParseTree tree = parser.parseString(sysml, "test.sysml").getParseTree();

        // Create mock symbol table with TypeA mapped to Pkg::TypeA
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.define(new Symbol("Pkg::TypeA", "TypeA", ElementType.CLASS, new Location("test.sysml", 1, 1)));

        ImportStatement imp = new ImportStatement("Pkg::*", ImportType.WILDCARD, false);
        imp.setLocation(new Location("test.sysml", 1, 1));
        
        analyzer.analyze(tree, List.of(imp), symbolTable);

        assertTrue(analyzer.hasUnusedSymbols(imp));
        assertFalse(analyzer.isUnused(imp));

        Set<String> used = analyzer.getUsedSymbols(imp);
        assertTrue(used.contains("TypeA"), "TypeA should be detected as used");
        
        List<String> suggestions = analyzer.getSuggestedSpecificImports(imp);
        assertEquals(1, suggestions.size());
        assertEquals("Pkg::TypeA", suggestions.get(0));

        RefactoringEdit edit = analyzer.generateRefactoring(imp, "test.sysml");
        assertNotNull(edit);
        assertEquals("import Pkg::*;", edit.getOldText());
        assertEquals("import Pkg::TypeA;", edit.getNewText());
        
        ImportUsageAnalyzer.ImportUsageStats stats = analyzer.getStats(imp);
        assertEquals(1, stats.getUsedCount());
        assertTrue(stats.getAvailableCount() > 0);
        assertTrue(stats.isWildcard());
        assertNotNull(stats.toString());
    }

    @Test
    void testSpecificUsageIsTracked() {
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
        String sysml = "import Pkg::TypeB; package MyPkg { part myPart : TypeB; }";
        ParseTree tree = parser.parseString(sysml, "test.sysml").getParseTree();

        ImportStatement imp = new ImportStatement("Pkg::TypeB", ImportType.SPECIFIC, true);
        imp.setLocation(new Location("test.sysml", 1, 1));

        analyzer.analyze(tree, List.of(imp), new SymbolTable());

        assertFalse(analyzer.isUnused(imp));
        Set<String> used = analyzer.getUsedSymbols(imp);
        assertTrue(used.contains("TypeB"));

        // No refactoring suggestions for specific imports
        assertNull(analyzer.generateRefactoring(imp, "test.sysml"));
        
        ImportUsageAnalyzer.ImportUsageStats stats = analyzer.getStats(imp);
        assertEquals(1, stats.getUsedCount());
        assertEquals(1, stats.getAvailableCount());
        assertFalse(stats.isWildcard());
        assertNotNull(stats.toString());
    }

    @Test
    void testFilteredUsageIsTracked() {
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
        String sysml = "package MyPkg { part myPart : TypeC; }";
        ParseTree tree = parser.parseString(sysml, "test.sysml").getParseTree();

        ImportStatement imp = new ImportStatement("Pkg::{TypeC, TypeD}", ImportType.FILTERED, false);

        analyzer.analyze(tree, List.of(imp), new SymbolTable());

        assertFalse(analyzer.isUnused(imp));
        Set<String> used = analyzer.getUsedSymbols(imp);
        assertTrue(used.contains("TypeC"));
        assertFalse(used.contains("TypeD"));
        
        assertEquals(2, analyzer.getAvailableCount(imp));
    }

    @Test
    void testAliasUsageIsTracked() {
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
        String sysml = "import Pkg::LongType as T; package MyPkg { part myPart : T; }";
        ParseTree tree = parser.parseString(sysml, "test.sysml").getParseTree();

        ImportStatement imp = new ImportStatement("Pkg::LongType as T", ImportType.ALIAS, false);

        analyzer.analyze(tree, List.of(imp), new SymbolTable());

        assertFalse(analyzer.isUnused(imp));
        Set<String> used = analyzer.getUsedSymbols(imp);
        assertTrue(used.contains("T"));
    }

    @Test
    void testUnusedImport() {
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
        String sysml = "import Pkg::TypeE; package MyPkg { part myPart : TypeF; }";
        ParseTree tree = parser.parseString(sysml, "test.sysml").getParseTree();

        ImportStatement imp = new ImportStatement("Pkg::TypeE", ImportType.SPECIFIC, false);
        analyzer.analyze(tree, List.of(imp), new SymbolTable());

        assertTrue(analyzer.isUnused(imp));
        assertEquals(0, analyzer.getUsedSymbols(imp).size());
        
        ImportUsageAnalyzer.ImportUsageStats stats = analyzer.getStats(imp);
        assertEquals(0, stats.getUsedCount());
        assertEquals(1, stats.getUnusedCount());
        assertEquals(0.0, stats.getUsagePercentage(), 0.01);
    }
    
    @Test
    void testEstimateAvailableSymbols() {
        SysMLv2ParserFacade parser = new SysMLv2ParserFacade();
        ParseTree tree = parser.parseString("package A {}", "test.sysml").getParseTree();
        
        ImportStatement impIsq = new ImportStatement("ISQ::*", ImportType.WILDCARD, false);
        ImportStatement impSi = new ImportStatement("SI::*", ImportType.WILDCARD, false);
        ImportStatement impUs = new ImportStatement("USCustomaryUnits::*", ImportType.WILDCARD, false);
        ImportStatement impScalar = new ImportStatement("ScalarFunctions::*", ImportType.WILDCARD, false);
        ImportStatement impVector = new ImportStatement("VectorFunctions::*", ImportType.WILDCARD, false);
        ImportStatement impCollections = new ImportStatement("Collections::*", ImportType.WILDCARD, false);
        ImportStatement impUnknown = new ImportStatement("Unknown::*", ImportType.WILDCARD, false);
        
        analyzer.analyze(tree, Arrays.asList(impIsq, impSi, impUs, impScalar, impVector, impCollections, impUnknown), new SymbolTable());
        
        assertEquals(847, analyzer.getAvailableCount(impIsq));
        assertEquals(125, analyzer.getAvailableCount(impSi));
        assertEquals(89, analyzer.getAvailableCount(impUs));
        assertEquals(45, analyzer.getAvailableCount(impScalar));
        assertEquals(32, analyzer.getAvailableCount(impVector));
        assertEquals(28, analyzer.getAvailableCount(impCollections));
        assertEquals(100, analyzer.getAvailableCount(impUnknown));
    }
}
