package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.ImportStatement;
import com.validator.semantic.ImportType;
import com.validator.semantic.Scope;
import com.validator.semantic.ScopeType;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WildcardImportRule.
 */
@DisplayName("Wildcard Import Rule Tests")
public class WildcardImportRuleTest {

    private WildcardImportRule rule;
    private LintConfig config;
    private SysMLv2ParserFacade parser;

    @BeforeEach
    public void setUp() {
        rule = new WildcardImportRule();
        config = new LintConfig();
        parser = new SysMLv2ParserFacade();
    }

    @Test
    @DisplayName("Rule has correct metadata")
    public void testRuleMetadata() {
        assertEquals("wildcard-imports", rule.getRuleId());
        assertEquals("imports", rule.getCategory());
        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().toLowerCase().contains("wildcard"));
    }

    @Test
    @DisplayName("Rule returns correct error codes")
    public void testErrorCodes() {
        String[] codes = rule.getErrorCodes();

        assertEquals(2, codes.length);
        assertTrue(containsCode(codes, ErrorCodes.LINT_WILDCARD_IMPORT));
        assertTrue(containsCode(codes, ErrorCodes.LINT_RECURSIVE_WILDCARD));
    }

    @Test
    @DisplayName("Wildcard import produces warning")
    public void testWildcardImportWarns() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::*", ImportType.WILDCARD, false);
        imp.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "import ISQ::*;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(1, warnings.size());
        ValidationWarning warning = warnings.get(0);
        assertEquals(ErrorCodes.LINT_WILDCARD_IMPORT, warning.getErrorCode());
        assertTrue(warning.getMessage().contains("ISQ::*"));
    }

    @Test
    @DisplayName("Recursive wildcard import produces specific warning")
    public void testRecursiveWildcardWarns() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::*::*", ImportType.WILDCARD, false);
        imp.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "import ISQ::*::*;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(1, warnings.size());
        ValidationWarning warning = warnings.get(0);
        assertEquals(ErrorCodes.LINT_RECURSIVE_WILDCARD, warning.getErrorCode());
        assertTrue(warning.getMessage().toLowerCase().contains("recursive"));
    }

    @Test
    @DisplayName("Specific import does not produce warning")
    public void testSpecificImportNoWarning() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("SI::kg", ImportType.SPECIFIC, false);
        imp.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "import SI::kg;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.isEmpty(), "Specific import should not produce warning");
    }

    @Test
    @DisplayName("Empty imports return empty warnings")
    public void testEmptyImportsNoWarnings() {
        SymbolTable symbolTable = new SymbolTable();
        // No imports added

        LintContext context = createContext(symbolTable, "package Test;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("Multiple wildcard imports produce multiple warnings")
    public void testMultipleWildcardImports() {
        SymbolTable symbolTable = new SymbolTable();

        ImportStatement imp1 = new ImportStatement("ISQ::*", ImportType.WILDCARD, false);
        imp1.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp1);

        ImportStatement imp2 = new ImportStatement("SI::*", ImportType.WILDCARD, false);
        imp2.setLocation(new Location("test.sysml", 2, 1));
        symbolTable.addImport(imp2);

        LintContext context = createContext(symbolTable, "import ISQ::*;\nimport SI::*;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(2, warnings.size());
    }

    @Test
    @DisplayName("Warning includes suggestion for specific imports")
    public void testWarningIncludesSuggestion() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::*", ImportType.WILDCARD, false);
        imp.setLocation(new Location("test.sysml", 3, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "import ISQ::*;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertFalse(warnings.isEmpty());
        ValidationWarning warning = warnings.get(0);
        assertFalse(warning.getSuggestions().isEmpty(), "Warning should include suggestion");
        assertTrue(warning.getSuggestions().get(0).toLowerCase().contains("specific"));
    }

    @Test
    @DisplayName("Warning includes file location")
    public void testWarningIncludesLocation() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::*", ImportType.WILDCARD, false);
        imp.setLocation(new Location("test.sysml", 5, 10));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "import ISQ::*;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertFalse(warnings.isEmpty());
        ValidationWarning warning = warnings.get(0);
        assertEquals(5, warning.getLine());
        assertEquals(10, warning.getColumn());
        assertEquals("test.sysml", warning.getFilePath());
    }

    @Test
    @DisplayName("Wildcard import in nested scope produces warning")
    public void testWildcardInNestedScope() {
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.enterScope("Automotive", ScopeType.PACKAGE);

        ImportStatement imp = new ImportStatement("ISQ::*", ImportType.WILDCARD, false);
        imp.setLocation(new Location("test.sysml", 2, 5));
        symbolTable.addImport(imp);

        symbolTable.exitScope();

        LintContext context = createContext(symbolTable, "package Automotive { import ISQ::*; }");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(1, warnings.size());
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
