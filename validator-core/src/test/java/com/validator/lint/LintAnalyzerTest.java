package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LintAnalyzer runs the registered rules whose category is enabled and that apply to the file, keeps the warnings
 * whose rule is enabled, and goes on when a rule fails.
 */
@DisplayName("Lint analyzer")
class LintAnalyzerTest {

    private ParseTree tree;
    private SymbolTable symbolTable;

    /** A rule that returns the warnings it is given, or throws, and counts its runs. */
    private static final class FixedRule implements LintRule {
        private final String id;
        private final String category;
        private final List<ValidationWarning> warnings;
        private final boolean fails;
        private int runs;

        FixedRule(String id, String category, List<ValidationWarning> warnings, boolean fails) {
            this.id = id;
            this.category = category;
            this.warnings = warnings;
            this.fails = fails;
        }

        @Override
        public String getRuleId() {
            return id;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getDescription() {
            return "fixed rule " + id;
        }

        @Override
        public List<ValidationWarning> analyze(LintContext context) {
            runs++;
            assertNotNull(context.getParseTree());
            if (fails) {
                throw new IllegalStateException("rule failure");
            }
            return warnings;
        }

        @Override
        public String[] getErrorCodes() {
            return new String[0];
        }
    }

    private static ValidationWarning warning(String code, String message) {
        ValidationWarning.Builder builder = new ValidationWarning.Builder();
        builder.errorCode(code);
        builder.message(message);
        builder.filePath("model.sysml");
        builder.line(1);
        builder.column(1);
        return builder.build();
    }

    @BeforeEach
    void setUp() {
        SysMLv2ParserFacade.ParseResult result = new SysMLv2ParserFacade().parseString("package P;", "model.sysml");
        tree = result.getParseTree();
        symbolTable = SymbolTableBuilder.build(tree, "model.sysml");
    }

    @Test
    @DisplayName("The default rules are registered once each")
    void testDefaultRules() {
        LintAnalyzer analyzer = LintAnalyzer.withDefaultRules();
        List<String> ids = analyzer.getRules().stream().map(LintRule::getRuleId).toList();
        assertEquals(7, ids.size(), ids.toString());
        assertTrue(ids.contains("wildcard-imports"));
        assertTrue(ids.contains("unit-correctness"));
        assertTrue(ids.contains("unit-completeness"));
        assertNotNull(analyzer.getRule("wildcard-imports"));
        assertNull(analyzer.getRule("no-such-rule"));

        LintRule first = analyzer.getRules().get(0);
        analyzer.registerRule(first);
        analyzer.registerRule(null);
        assertEquals(7, analyzer.getRules().size(), "Registering a rule again or null changes nothing");
        assertThrows(UnsupportedOperationException.class, () -> analyzer.getRules().add(first));

        analyzer.unregisterRule("wildcard-imports");
        assertNull(analyzer.getRule("wildcard-imports"));
        assertEquals(6, analyzer.getRules().size());

        LintConfig strict = LintConfig.strict();
        assertSame(strict, LintAnalyzer.withDefaultRules(strict).getConfig());
    }

    @Test
    @DisplayName("A missing configuration is the default configuration")
    void testNullConfiguration() {
        LintAnalyzer analyzer = new LintAnalyzer(null);
        assertNotNull(analyzer.getConfig());
        assertTrue(analyzer.getConfig().isCategoryEnabled("naming"));
        LintConfig disabled = LintConfig.disabled();
        analyzer.setConfig(disabled);
        assertSame(disabled, analyzer.getConfig());
        analyzer.setConfig(null);
        assertTrue(analyzer.getConfig().isCategoryEnabled("naming"));
    }

    @Test
    @DisplayName("Warnings of enabled rules are kept and a failing rule does not stop the others")
    void testRunsRules() {
        FixedRule naming = new FixedRule("naming", "naming",
            List.of(warning(ErrorCodes.LINT_NAMING_DEFINITION, "bad name"),
                warning(ErrorCodes.LINT_NAMING_USAGE, "bad usage name")), false);
        FixedRule failing = new FixedRule("failing", "naming", null, true);
        FixedRule silent = new FixedRule("silent", "unused", null, false);
        LintConfig config = new LintConfig();
        config.setRuleSeverity(ErrorCodes.LINT_NAMING_USAGE, LintConfig.Severity.OFF);
        LintAnalyzer analyzer = new LintAnalyzer(config);
        analyzer.registerRule(failing);
        analyzer.registerRule(naming);
        analyzer.registerRule(silent);
        analyzer.setProjectDir(null);

        List<ValidationWarning> warnings = analyzer.analyze(tree, symbolTable, "model.sysml");

        assertEquals(List.of("bad name"), warnings.stream().map(ValidationWarning::getMessage).toList(),
            "The warning of the rule turned off is dropped");
        assertEquals(1, failing.runs);
        assertEquals(1, naming.runs);
        assertEquals(1, silent.runs, "A rule that returns null runs and adds nothing");
    }

    @Test
    @DisplayName("Rules are skipped for disabled categories, other files and ignored files")
    void testSkipsRules() {
        FixedRule naming = new FixedRule("naming", "naming",
            List.of(warning(ErrorCodes.LINT_NAMING_DEFINITION, "bad name")), false);
        LintConfig config = new LintConfig();
        LintAnalyzer analyzer = new LintAnalyzer(config);
        analyzer.registerRule(naming);

        assertTrue(analyzer.analyze(tree, symbolTable, "notes.txt").isEmpty(), "Rules apply to .sysml and .kerml");
        assertTrue(analyzer.analyze(null, symbolTable, "model.sysml").isEmpty());
        assertTrue(analyzer.analyze(tree, null, "model.sysml").isEmpty());
        assertEquals(0, naming.runs);

        config.addIgnorePattern("generated.sysml");
        assertTrue(analyzer.analyze(tree, symbolTable, "generated.sysml").isEmpty());
        assertEquals(0, naming.runs);

        config.disableCategory("naming");
        assertTrue(analyzer.analyze(tree, symbolTable, "model.sysml").isEmpty());
        assertEquals(0, naming.runs);

        config.enableCategory("naming");
        assertEquals(1, analyzer.analyze(tree, symbolTable, "model.sysml", "package P;").size());
        assertEquals(1, naming.runs);
    }

    @Test
    @DisplayName("Several files are analyzed and only files with warnings are in the result")
    void testAnalyzeAll() {
        FixedRule naming = new FixedRule("naming", "naming",
            List.of(warning(ErrorCodes.LINT_NAMING_DEFINITION, "bad name")), false);
        LintAnalyzer analyzer = new LintAnalyzer();
        analyzer.registerRule(naming);

        Map<String, LintAnalyzer.ParseTreeAndSymbolTable> files = new LinkedHashMap<>();
        files.put("a.sysml", new LintAnalyzer.ParseTreeAndSymbolTable(tree, symbolTable, "package P;"));
        files.put("b.txt", new LintAnalyzer.ParseTreeAndSymbolTable(tree, symbolTable));
        Map<String, List<ValidationWarning>> results = analyzer.analyzeAll(files);

        assertEquals(List.of("a.sysml"), new ArrayList<>(results.keySet()));
        assertEquals(1, results.get("a.sysml").size());
        LintAnalyzer.ParseTreeAndSymbolTable data = files.get("a.sysml");
        assertSame(tree, data.getParseTree());
        assertSame(symbolTable, data.getSymbolTable());
        assertEquals("package P;", data.getSourceCode());
        assertNull(files.get("b.txt").getSourceCode());
    }

    @Test
    @DisplayName("Statistics count rules, enabled rules and rules by category")
    void testStats() {
        LintConfig config = new LintConfig();
        config.disableCategory("unused");
        LintAnalyzer analyzer = new LintAnalyzer(config);
        analyzer.registerRule(new FixedRule("a", "naming", null, false));
        analyzer.registerRule(new FixedRule("b", "naming", null, false));
        analyzer.registerRule(new FixedRule("c", "unused", null, false));

        LintAnalyzer.LintStats stats = analyzer.getStats();
        assertEquals(3, stats.getTotalRules());
        assertEquals(2, stats.getEnabledRules());
        assertEquals(Map.of("naming", 2, "unused", 1), stats.getRulesByCategory());
        assertThrows(UnsupportedOperationException.class, () -> stats.getRulesByCategory().put("x", 1));
        assertTrue(stats.toString().startsWith("LintStats{total=3, enabled=2, categories="), stats.toString());
    }
}
