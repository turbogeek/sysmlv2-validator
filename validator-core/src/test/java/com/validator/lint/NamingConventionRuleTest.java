package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.ElementType;
import com.validator.semantic.ScopeType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NamingConventionRule.
 */
@DisplayName("Naming Convention Rule Tests")
public class NamingConventionRuleTest {

    private NamingConventionRule rule;
    private LintConfig config;
    private SysMLv2ParserFacade parser;

    @BeforeEach
    public void setUp() {
        rule = new NamingConventionRule();
        config = new LintConfig();
        parser = new SysMLv2ParserFacade();
    }

    @Test
    @DisplayName("Rule has correct metadata")
    public void testRuleMetadata() {
        assertEquals("naming-conventions", rule.getRuleId());
        assertEquals("naming", rule.getCategory());
        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().length() > 0);
    }

    @Test
    @DisplayName("Rule returns correct error codes")
    public void testErrorCodes() {
        String[] codes = rule.getErrorCodes();

        assertTrue(codes.length > 0);
        assertTrue(containsCode(codes, ErrorCodes.LINT_NAMING_DEFINITION));
        assertTrue(containsCode(codes, ErrorCodes.LINT_NAMING_USAGE));
        assertTrue(containsCode(codes, ErrorCodes.LINT_NAMING_PACKAGE));
    }

    @Test
    @DisplayName("PascalCase definition passes")
    public void testPascalCaseDefinitionPasses() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "part def Vehicle;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("Vehicle")),
            "PascalCase definition should not produce warning");
    }

    @Test
    @DisplayName("Non-PascalCase definition produces warning")
    public void testNonPascalCaseDefinitionWarns() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("vehicle", "vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "part def vehicle;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .anyMatch(w -> w.getMessage().contains("vehicle") && w.getMessage().contains("PascalCase")),
            "Non-PascalCase definition should produce warning");
    }

    @Test
    @DisplayName("camelCase usage passes")
    public void testCamelCaseUsagePasses() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("myPart", "myPart",
            ElementType.PART_USAGE, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "part myPart;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("myPart")),
            "camelCase usage should not produce warning");
    }

    @Test
    @DisplayName("Non-camelCase usage produces warning")
    public void testNonCamelCaseUsageWarns() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("MyPart", "MyPart",
            ElementType.PART_USAGE, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "part MyPart;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .anyMatch(w -> w.getMessage().contains("MyPart") && w.getMessage().contains("camelCase")),
            "Non-camelCase usage should produce warning");
    }

    @Test
    @DisplayName("PascalCase package passes")
    public void testPascalCasePackagePasses() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("Automotive", "Automotive",
            ElementType.PACKAGE, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "package Automotive;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("Automotive")),
            "PascalCase package should not produce warning");
    }

    @Test
    @DisplayName("Non-PascalCase package produces warning")
    public void testNonPascalCasePackageWarns() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("automotive", "automotive",
            ElementType.PACKAGE, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "package automotive;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .anyMatch(w -> w.getMessage().contains("automotive") && w.getMessage().contains("PascalCase")),
            "Non-PascalCase package should produce warning");
    }

    @Test
    @DisplayName("Private underscore prefix is ignored")
    public void testPrivatePrefixIgnored() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("_internalPart", "_internalPart",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "part def _internalPart;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("_internalPart")),
            "Underscore prefix should be ignored");
    }

    @Test
    @DisplayName("Anonymous names are ignored")
    public void testAnonymousNamesIgnored() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("<anon1>", "<anon1>",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "part def;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("<anon")),
            "Anonymous names should be ignored");
    }

    @Test
    @DisplayName("Disabled naming category produces no warnings")
    public void testDisabledCategoryNoWarnings() {
        config.disableCategory("naming");

        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("badName", "badName",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));

        LintContext context = LintContext.builder()
            .parseTree(parser.parseString("part def badName;", "test.sysml").getParseTree())
            .symbolTable(symbolTable)
            .filePath("test.sysml")
            .config(config)
            .build();

        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.isEmpty(), "Disabled category should produce no warnings");
    }

    @Test
    @DisplayName("Rule is applicable to .sysml files")
    public void testApplicableToSysml() {
        assertTrue(rule.isApplicable("test.sysml"));
        assertTrue(rule.isApplicable("path/to/Model.sysml"));
    }

    @Test
    @DisplayName("Rule is applicable to .kerml files")
    public void testApplicableToKerml() {
        assertTrue(rule.isApplicable("test.kerml"));
        assertTrue(rule.isApplicable("path/to/Base.kerml"));
    }

    @Test
    @DisplayName("Rule is not applicable to other files")
    public void testNotApplicableToOtherFiles() {
        assertFalse(rule.isApplicable("test.txt"));
        assertFalse(rule.isApplicable("test.java"));
        assertFalse(rule.isApplicable(null));
    }

    @Test
    @DisplayName("Warnings include suggestions")
    public void testWarningsIncludeSuggestions() {
        SymbolTable symbolTable = createSymbolTable();
        symbolTable.define(new Symbol("bad_name", "bad_name",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));

        LintContext context = createContext(symbolTable, "part def bad_name;");
        List<ValidationWarning> warnings = rule.analyze(context);

        assertFalse(warnings.isEmpty());
        ValidationWarning warning = warnings.get(0);
        assertFalse(warning.getSuggestions().isEmpty(), "Warning should include suggestion");
    }

    @Test
    @DisplayName("Default severity is WARN")
    public void testDefaultSeverity() {
        assertEquals(LintConfig.Severity.WARN, rule.getDefaultSeverity());
    }

    // Helper methods

    private SymbolTable createSymbolTable() {
        return new SymbolTable();
    }

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
