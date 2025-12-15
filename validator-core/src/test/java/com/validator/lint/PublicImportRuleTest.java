package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.ImportStatement;
import com.validator.semantic.ImportType;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PublicImportRule.
 */
@DisplayName("Public Import Rule Tests")
public class PublicImportRuleTest {

    private PublicImportRule rule;
    private LintConfig config;
    private SysMLv2ParserFacade parser;

    @BeforeEach
    public void setUp() {
        rule = new PublicImportRule();
        config = new LintConfig();
        parser = new SysMLv2ParserFacade();
    }

    @Test
    @DisplayName("Rule has correct metadata")
    public void testRuleMetadata() {
        assertEquals("public-imports", rule.getRuleId());
        assertEquals("imports", rule.getCategory());
        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().toLowerCase().contains("public"));
    }

    @Test
    @DisplayName("Rule returns correct error codes")
    public void testErrorCodes() {
        String[] codes = rule.getErrorCodes();

        assertEquals(1, codes.length);
        assertTrue(containsCode(codes, ErrorCodes.LINT_PUBLIC_IMPORT));
    }

    @Test
    @DisplayName("Public import produces warning")
    public void testPublicImportWarns() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::MassValue", ImportType.SPECIFIC, true);
        imp.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "public import ISQ::MassValue;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(1, warnings.size());
        ValidationWarning warning = warnings.get(0);
        assertEquals(ErrorCodes.LINT_PUBLIC_IMPORT, warning.getErrorCode());
        assertTrue(warning.getMessage().contains("ISQ::MassValue"));
    }

    @Test
    @DisplayName("Public wildcard import mentions all elements")
    public void testPublicWildcardImportWarns() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::*", ImportType.WILDCARD, true);
        imp.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "public import ISQ::*;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(1, warnings.size());
        ValidationWarning warning = warnings.get(0);
        assertEquals(ErrorCodes.LINT_PUBLIC_IMPORT, warning.getErrorCode());
        assertTrue(warning.getMessage().toLowerCase().contains("all elements")
                || warning.getMessage().toLowerCase().contains("wildcard"));
    }

    @Test
    @DisplayName("Private import does not produce warning")
    public void testPrivateImportNoWarning() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::MassValue", ImportType.SPECIFIC, false);
        imp.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "import ISQ::MassValue;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.isEmpty(), "Private import should not produce warning");
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
    @DisplayName("Multiple public imports produce multiple warnings")
    public void testMultiplePublicImports() {
        SymbolTable symbolTable = new SymbolTable();

        ImportStatement imp1 = new ImportStatement("ISQ::MassValue", ImportType.SPECIFIC, true);
        imp1.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp1);

        ImportStatement imp2 = new ImportStatement("SI::kg", ImportType.SPECIFIC, true);
        imp2.setLocation(new Location("test.sysml", 2, 1));
        symbolTable.addImport(imp2);

        LintContext context = createContext(symbolTable,
            "public import ISQ::MassValue;\npublic import SI::kg;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(2, warnings.size());
    }

    @Test
    @DisplayName("Warning includes suggestion")
    public void testWarningIncludesSuggestion() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::MassValue", ImportType.SPECIFIC, true);
        imp.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "public import ISQ::MassValue;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertFalse(warnings.isEmpty());
        ValidationWarning warning = warnings.get(0);
        assertFalse(warning.getSuggestions().isEmpty(), "Warning should include suggestion");
    }

    @Test
    @DisplayName("Warning includes file location")
    public void testWarningIncludesLocation() {
        SymbolTable symbolTable = new SymbolTable();
        ImportStatement imp = new ImportStatement("ISQ::MassValue", ImportType.SPECIFIC, true);
        imp.setLocation(new Location("test.sysml", 5, 10));
        symbolTable.addImport(imp);

        LintContext context = createContext(symbolTable, "public import ISQ::MassValue;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertFalse(warnings.isEmpty());
        ValidationWarning warning = warnings.get(0);
        assertEquals(5, warning.getLine());
        assertEquals(10, warning.getColumn());
        assertEquals("test.sysml", warning.getFilePath());
    }

    @Test
    @DisplayName("Mixed imports only warn for public ones")
    public void testMixedImportsOnlyWarnPublic() {
        SymbolTable symbolTable = new SymbolTable();

        // Private import - no warning
        ImportStatement imp1 = new ImportStatement("ISQ::MassValue", ImportType.SPECIFIC, false);
        imp1.setLocation(new Location("test.sysml", 1, 1));
        symbolTable.addImport(imp1);

        // Public import - warning
        ImportStatement imp2 = new ImportStatement("SI::kg", ImportType.SPECIFIC, true);
        imp2.setLocation(new Location("test.sysml", 2, 1));
        symbolTable.addImport(imp2);

        LintContext context = createContext(symbolTable,
            "import ISQ::MassValue;\npublic import SI::kg;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getMessage().contains("SI::kg"));
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
