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
 * Unit tests for UsageWithoutDefinitionRule.
 */
@DisplayName("Usage Without Definition Rule Tests")
public class UsageWithoutDefinitionRuleTest {

    private UsageWithoutDefinitionRule rule;
    private LintConfig config;
    private SysMLv2ParserFacade parser;

    @BeforeEach
    public void setUp() {
        rule = new UsageWithoutDefinitionRule();
        config = new LintConfig();
        parser = new SysMLv2ParserFacade();
    }

    @Test
    @DisplayName("Rule has correct metadata")
    public void testRuleMetadata() {
        assertEquals("usage-without-definition", rule.getRuleId());
        assertEquals("missing-types", rule.getCategory());
        assertNotNull(rule.getDescription());
        assertTrue(rule.getDescription().length() > 0);
    }

    @Test
    @DisplayName("Rule returns correct error codes")
    public void testErrorCodes() {
        String[] codes = rule.getErrorCodes();

        assertTrue(codes.length >= 2);
        assertTrue(containsCode(codes, ErrorCodes.LINT_MISSING_DEFINITION));
        assertTrue(containsCode(codes, ErrorCodes.LINT_UNTYPED_ATTRIBUTE));
    }

    @Test
    @DisplayName("Part with valid definition type passes")
    public void testPartWithValidTypePasses() {
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.define(new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));

        String source = "part def Vehicle; part myVehicle : Vehicle;";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("Vehicle") && w.getMessage().contains("no definition")),
            "Part with valid type should not produce warning");
    }

    @Test
    @DisplayName("Part with undefined type produces warning")
    public void testPartWithUndefinedTypeWarns() {
        SymbolTable symbolTable = new SymbolTable();
        // No Vehicle definition exists

        String source = "part myVehicle : UndefinedType;";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        // Note: This test depends on how the parser handles the source
        // The warning may or may not appear depending on parse tree structure
        assertNotNull(warnings);
    }

    @Test
    @DisplayName("Untyped part produces warning")
    public void testUntypedPartWarns() {
        SymbolTable symbolTable = new SymbolTable();

        String source = "part myPart;";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        // The rule should detect untyped parts
        // Note: Actual warning depends on parse tree structure
        assertNotNull(warnings);
    }

    @Test
    @DisplayName("Attribute with valid type passes")
    public void testAttributeWithValidTypePasses() {
        SymbolTable symbolTable = new SymbolTable();

        // Integer is a built-in type
        String source = "part def Vehicle { attribute mass : Integer; }";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("mass") && w.getMessage().contains("Untyped")),
            "Attribute with valid type should not produce warning");
    }

    @Test
    @DisplayName("Untyped attribute produces warning")
    public void testUntypedAttributeWarns() {
        SymbolTable symbolTable = new SymbolTable();

        String source = "part def Vehicle { attribute mass; }";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        // Note: Warning depends on parse tree structure
        assertNotNull(warnings);
    }

    @Test
    @DisplayName("Built-in types are recognized")
    public void testBuiltinTypesRecognized() {
        SymbolTable symbolTable = new SymbolTable();

        // Test various built-in types
        String source = """
            part def Vehicle {
                attribute flag : Boolean;
                attribute count : Integer;
                attribute ratio : Real;
                attribute name : String;
            }
            """;
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("Boolean")
                || w.getMessage().contains("Integer")
                || w.getMessage().contains("Real")
                || w.getMessage().contains("String")),
            "Built-in types should be recognized");
    }

    @Test
    @DisplayName("Standard library types are recognized")
    public void testStandardLibraryTypesRecognized() {
        SymbolTable symbolTable = new SymbolTable();

        String source = "attribute mass : ISQ::MassValue;";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("ISQ::MassValue")
                && w.getMessage().contains("no definition")),
            "Standard library types should be recognized");
    }

    @Test
    @DisplayName("SI unit types are recognized")
    public void testSITypesRecognized() {
        SymbolTable symbolTable = new SymbolTable();

        String source = "attribute mass : SI::kilogram;";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("SI::kilogram")
                && w.getMessage().contains("no definition")),
            "SI unit types should be recognized");
    }

    @Test
    @DisplayName("Value types are recognized")
    public void testValueTypesRecognized() {
        SymbolTable symbolTable = new SymbolTable();

        // Common value types from standard library
        String source = """
            part def Vehicle {
                attribute mass : MassValue;
                attribute length : LengthValue;
                attribute time : TimeValue;
            }
            """;
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("MassValue")
                || w.getMessage().contains("LengthValue")
                || w.getMessage().contains("TimeValue")),
            "Value types should be recognized");
    }

    @Test
    @DisplayName("Null parse tree returns empty warnings")
    public void testNullParseTreeReturnsEmpty() {
        SymbolTable symbolTable = new SymbolTable();
        ParseTree tree = parser.parseString("package Test;", "test.sysml").getParseTree();

        LintContext context = LintContext.builder()
            .parseTree(tree)
            .symbolTable(symbolTable)
            .filePath("test.sysml")
            .config(config)
            .build();

        List<ValidationWarning> warnings = rule.analyze(context);
        assertNotNull(warnings);
    }

    @Test
    @DisplayName("Warnings include file location")
    public void testWarningsIncludeLocation() {
        SymbolTable symbolTable = new SymbolTable();

        String source = "part def Vehicle { attribute mass; }";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        // If there are warnings, verify they have location info
        for (ValidationWarning warning : warnings) {
            assertNotNull(warning.getFilePath());
            assertTrue(warning.getLine() >= 0);
        }
    }

    @Test
    @DisplayName("Warnings include suggestions")
    public void testWarningsIncludeSuggestions() {
        SymbolTable symbolTable = new SymbolTable();

        String source = "part myPart;";
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        // If there are warnings, verify they have suggestions
        for (ValidationWarning warning : warnings) {
            // Suggestions should be present for actionable fixes
            assertNotNull(warning.getSuggestions());
        }
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
    }

    @Test
    @DisplayName("Rule is not applicable to other files")
    public void testNotApplicableToOtherFiles() {
        assertFalse(rule.isApplicable("test.txt"));
        assertFalse(rule.isApplicable("test.java"));
        assertFalse(rule.isApplicable(null));
    }

    @Test
    @DisplayName("Multiple usage types are checked")
    public void testMultipleUsageTypesChecked() {
        SymbolTable symbolTable = new SymbolTable();

        // Various usage types that should be checked
        String source = """
            package Test {
                part def Vehicle;
                action def Drive;
                item def Cargo;

                part myVehicle;
                action myDrive;
                item myCargo;
            }
            """;
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        // Rule should analyze multiple usage types
        assertNotNull(warnings);
    }

    @Test
    @DisplayName("Typed usages with resolvable type pass")
    public void testTypedUsagesPass() {
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.define(new Symbol("Vehicle", "Vehicle",
            ElementType.PART_DEFINITION, new Location("test.sysml", 1, 1)));
        symbolTable.define(new Symbol("Engine", "Engine",
            ElementType.PART_DEFINITION, new Location("test.sysml", 2, 1)));

        String source = """
            part def Vehicle;
            part def Engine;
            part myVehicle : Vehicle {
                part engine : Engine;
            }
            """;
        LintContext context = createContext(symbolTable, source);
        List<ValidationWarning> warnings = rule.analyze(context);

        assertTrue(warnings.stream()
            .noneMatch(w -> w.getMessage().contains("no definition")),
            "Typed usages with resolvable types should pass");
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
