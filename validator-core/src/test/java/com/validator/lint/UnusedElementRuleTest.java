package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.ElementType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UnusedElementRule.
 */
@DisplayName("Unused Element Rule Tests")
public class UnusedElementRuleTest {

    private UnusedElementRule rule;
    private LintConfig config;
    private SysMLv2ParserFacade parser;

    @BeforeEach
    public void setUp() {
        rule = new UnusedElementRule();
        config = new LintConfig();
        parser = new SysMLv2ParserFacade();
    }

    @Test
    @DisplayName("Rule has correct metadata")
    public void testRuleMetadata() {
        assertEquals("unused-elements", rule.getRuleId());
        assertEquals("unused", rule.getCategory());
        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().contains("unused"));
    }

    @Test
    @DisplayName("Rule returns correct error codes")
    public void testErrorCodes() {
        String[] codes = rule.getErrorCodes();

        assertTrue(codes.length >= 3);
        assertTrue(containsCode(codes, ErrorCodes.LINT_UNUSED_DEFINITION));
        assertTrue(containsCode(codes, ErrorCodes.LINT_UNUSED_IMPORT));
        assertTrue(containsCode(codes, ErrorCodes.LINT_UNUSED_ATTRIBUTE));
    }

    @Test
    @DisplayName("Unused definition produces warning")
    public void testUnusedDefinitionWarns() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1));
        symbolTable.define(symbol);
        // Definition is never used

        LintContext context = createContext(symbolTable, "part def Vehicle;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .anyMatch(w -> w.getMessage().contains("Vehicle") && w.getMessage().contains("Unused")),
            "Unused definition should produce warning");
    }

    @Test
    @DisplayName("Used definition does not produce warning")
    public void testUsedDefinitionNoWarning() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1));
        symbol.markUsedAsType();  // Mark as used
        symbolTable.define(symbol);

        LintContext context = createContext(symbolTable, "part def Vehicle; part myVehicle : Vehicle;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("Vehicle") && w.getMessage().contains("Unused")),
            "Used definition should not produce warning");
    }

    @Test
    @DisplayName("Unused attribute produces warning")
    public void testUnusedAttributeWarns() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("mass", "mass",
            ElementType.ATTRIBUTE_USAGE, new Location("test.sysml", 2, 5));
        symbolTable.define(symbol);
        // Attribute is never used

        LintContext context = createContext(symbolTable, "part def Vehicle { attribute mass; }");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .anyMatch(w -> w.getMessage().contains("mass") && w.getMessage().contains("Unused")),
            "Unused attribute should produce warning");
    }

    @Test
    @DisplayName("Attribute used in expression does not produce warning")
    public void testUsedAttributeNoWarning() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("mass", "mass",
            ElementType.ATTRIBUTE_USAGE, new Location("test.sysml", 2, 5));
        symbol.markUsedInExpression();  // Mark as used in expression
        symbolTable.define(symbol);

        LintContext context = createContext(symbolTable, "part def Vehicle { attribute mass; }");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("mass")),
            "Used attribute should not produce warning");
    }

    @Test
    @DisplayName("Unused part produces warning")
    public void testUnusedPartWarns() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("engine", "engine",
            ElementType.PART_USAGE, new Location("test.sysml", 2, 5));
        symbolTable.define(symbol);
        // Part is never used

        LintContext context = createContext(symbolTable, "part def Vehicle { part engine; }");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .anyMatch(w -> w.getMessage().contains("engine") && w.getMessage().contains("Unused")),
            "Unused part should produce warning");
    }

    @Test
    @DisplayName("Packages are not flagged as unused")
    public void testPackagesNotFlagged() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("Automotive", "Automotive",
            ElementType.PACKAGE, new Location("test.sysml", 1, 1));
        symbolTable.define(symbol);

        LintContext context = createContext(symbolTable, "package Automotive;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("Automotive")),
            "Packages should not be flagged as unused");
    }

    @Test
    @DisplayName("Symbols without location are skipped")
    public void testSymbolsWithoutLocationSkipped() {
        SymbolTable symbolTable = new SymbolTable();
        // Symbol without location (e.g., from standard library)
        Symbol symbol = new Symbol("Integer", "Integer",
            ElementType.ATTRIBUTE_DEFINITION, null);
        symbolTable.define(symbol);

        LintContext context = createContext(symbolTable, "");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("Integer")),
            "Symbols without location should be skipped");
    }

    @Test
    @DisplayName("Empty symbol table returns empty warnings")
    public void testEmptySymbolTableReturnsEmpty() {
        ParseTree tree = parser.parseString("package Test;", "test.sysml").getParseTree();

        // LintContext requires non-null symbol table, so use an empty one
        SymbolTable emptyTable = new SymbolTable();
        LintContext context = LintContext.builder()
            .parseTree(tree)
            .symbolTable(emptyTable)
            .filePath("test.sysml")
            .config(config)
            .build();

        List<ValidationWarning> warnings = rule.analyze(context);

        assertNotNull(warnings);
        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("Standard library imports are not flagged")
    public void testStandardLibraryImportsNotFlagged() {
        SymbolTable symbolTable = new SymbolTable();
        // Add a standard library import - these should not be flagged
        // The rule checks for ISQ::, SI::, etc. prefixes

        LintContext context = createContext(symbolTable, "import ISQ::*;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("ISQ")),
            "Standard library imports should not be flagged");
    }

    @Test
    @DisplayName("Multiple unused definitions produce multiple warnings")
    public void testMultipleUnusedDefinitions() {
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.define(new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));
        symbolTable.define(new Symbol("Engine", "Engine",
            ElementType.PART_DEFINITION, new Location("test.sysml", 2, 1)));
        symbolTable.define(new Symbol("Wheel", "Wheel",
            ElementType.PART_DEFINITION, new Location("test.sysml", 3, 1)));

        String source = "part def Vehicle; part def Engine; part def Wheel;";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        long unusedCount = warnings.stream()
            .filter(w -> w.getMessage().contains("Unused"))
            .count();
        assertEquals(3, unusedCount, "Should have 3 unused definition warnings");
    }

    @Test
    @DisplayName("Warnings include file location")
    public void testWarningsIncludeLocation() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 5, 10));
        symbolTable.define(symbol);

        LintContext context = createContext(symbolTable, "part def Vehicle;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertFalse(warnings.isEmpty());
        ValidationWarning warning = warnings.get(0);
        assertEquals(5, warning.getLine());
        assertEquals(10, warning.getColumn());
        assertEquals("test.sysml", warning.getFilePath());
    }

    @Test
    @DisplayName("Warnings include suggestions")
    public void testWarningsIncludeSuggestions() {
        SymbolTable symbolTable = new SymbolTable();
        Symbol symbol = new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1));
        symbolTable.define(symbol);

        LintContext context = createContext(symbolTable, "part def Vehicle;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertFalse(warnings.isEmpty());
        ValidationWarning warning = warnings.get(0);
        assertFalse(warning.getSuggestions().isEmpty(),
            "Warning should include suggestion");
    }

    @Test
    @DisplayName("Rule is applicable to .sysml files")
    public void testApplicableToSysml() {
        assertTrue(rule.isApplicable("test.sysml"));
        assertTrue(rule.isApplicable("/path/to/Model.sysml"));
    }

    @Test
    @DisplayName("Rule is applicable to .kerml files")
    public void testApplicableToKerml() {
        assertTrue(rule.isApplicable("test.kerml"));
    }

    @Test
    @DisplayName("Rule is not applicable to other files")
    public void testNotApplicableToOtherFiles() {
        assertFalse(rule.isApplicable("test.txt"));
        assertFalse(rule.isApplicable("test.java"));
        assertFalse(rule.isApplicable(null));
    }

    // Helper methods

    private LintContext createContext(SymbolTable symbolTable, String source) {
        ParseTree tree = parser.parseString(source, "test.sysml").getParseTree();
        return LintContext.builder()
            .parseTree(tree)
            .symbolTable(symbolTable)
            .filePath("test.sysml")
            .config(config)
            .build();
    }

    private boolean containsCode(String[] codes, String code) {
        for (String c : codes) {
            if (c.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
