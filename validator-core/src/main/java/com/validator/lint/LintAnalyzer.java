package com.validator.lint;

import com.validator.ValidationWarning;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main lint analyzer that orchestrates all lint rules.
 *
 * <p>The analyzer runs registered lint rules against SysML v2 models
 * and collects warnings. Rules can be enabled/disabled via configuration.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * LintAnalyzer analyzer = new LintAnalyzer();
 * analyzer.registerRule(new UnusedElementRule());
 * analyzer.registerRule(new NamingConventionRule());
 *
 * List<ValidationWarning> warnings = analyzer.analyze(parseTree, symbolTable, config);
 * }</pre>
 */
public class LintAnalyzer {

    private final List<LintRule> rules = new CopyOnWriteArrayList<>();
    private LintConfig config;
    private Path projectDir;

    /**
     * Creates a new analyzer with default configuration.
     */
    public LintAnalyzer() {
        this.config = new LintConfig();
    }

    /**
     * Creates a new analyzer with the specified configuration.
     *
     * @param config the lint configuration
     */
    public LintAnalyzer(LintConfig config) {
        this.config = config != null ? config : new LintConfig();
    }

    /**
     * Creates an analyzer with all built-in rules registered.
     *
     * @return analyzer with default rules
     */
    public static LintAnalyzer withDefaultRules() {
        LintAnalyzer analyzer = new LintAnalyzer();
        analyzer.registerDefaultRules();
        return analyzer;
    }

    /**
     * Creates an analyzer with all built-in rules and custom configuration.
     *
     * @param config the lint configuration
     * @return analyzer with default rules and custom config
     */
    public static LintAnalyzer withDefaultRules(LintConfig config) {
        LintAnalyzer analyzer = new LintAnalyzer(config);
        analyzer.registerDefaultRules();
        return analyzer;
    }

    /**
     * Registers all built-in lint rules.
     */
    public void registerDefaultRules() {
        // Register built-in rules
        registerRule(new UnusedElementRule());
        registerRule(new UsageWithoutDefinitionRule());
        registerRule(new NamingConventionRule());
        registerRule(new WildcardImportRule());
        registerRule(new PublicImportRule());
        registerRule(new ValueUnitCorrectnessRule());
        registerRule(new ValueUnitCompletenessRule());
        // registerRule(new DocumentationRule());  // TODO: implement
        // registerRule(new ComplexityRule());     // TODO: implement
    }

    /**
     * Registers a lint rule.
     *
     * @param rule the rule to register
     */
    public void registerRule(LintRule rule) {
        if (rule != null && !rules.contains(rule)) {
            rules.add(rule);
        }
    }

    /**
     * Unregisters a lint rule.
     *
     * @param ruleId the ID of the rule to unregister
     */
    public void unregisterRule(String ruleId) {
        rules.removeIf(r -> r.getRuleId().equals(ruleId));
    }

    /**
     * Gets all registered rules.
     *
     * @return unmodifiable list of rules
     */
    public List<LintRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * Gets a rule by ID.
     *
     * @param ruleId the rule ID
     * @return the rule, or null if not found
     */
    public LintRule getRule(String ruleId) {
        return rules.stream()
                .filter(r -> r.getRuleId().equals(ruleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets the lint configuration.
     *
     * @param config the configuration
     */
    public void setConfig(LintConfig config) {
        this.config = config != null ? config : new LintConfig();
    }

    /**
     * Gets the current lint configuration.
     *
     * @return the configuration
     */
    public LintConfig getConfig() {
        return config;
    }

    /**
     * Sets the project directory for configuration lookup.
     *
     * @param projectDir the project directory
     */
    public void setProjectDir(Path projectDir) {
        this.projectDir = projectDir;
    }

    /**
     * Analyzes a SysML v2 model and returns warnings.
     *
     * @param parseTree the ANTLR parse tree
     * @param symbolTable the symbol table
     * @param filePath the source file path
     * @return list of warnings
     */
    public List<ValidationWarning> analyze(ParseTree parseTree, SymbolTable symbolTable,
                                            String filePath) {
        return analyze(parseTree, symbolTable, filePath, null);
    }

    /**
     * Analyzes a SysML v2 model with source code and returns warnings.
     *
     * @param parseTree the ANTLR parse tree
     * @param symbolTable the symbol table
     * @param filePath the source file path
     * @param sourceCode the original source code
     * @return list of warnings
     */
    public List<ValidationWarning> analyze(ParseTree parseTree, SymbolTable symbolTable,
                                            String filePath, String sourceCode) {
        if (parseTree == null || symbolTable == null) {
            return Collections.emptyList();
        }

        // Check if file should be ignored
        if (filePath != null && config.shouldIgnore(filePath)) {
            return Collections.emptyList();
        }

        // Build context
        LintContext context = LintContext.builder()
                .parseTree(parseTree)
                .symbolTable(symbolTable)
                .filePath(filePath)
                .sourceCode(sourceCode)
                .config(config)
                .projectDir(projectDir)
                .build();

        // Run all enabled rules
        List<ValidationWarning> warnings = new ArrayList<>();

        for (LintRule rule : rules) {
            // Skip if rule's category is disabled
            if (!config.isCategoryEnabled(rule.getCategory())) {
                continue;
            }

            // Skip if rule is not applicable to this file
            if (!rule.isApplicable(filePath)) {
                continue;
            }

            try {
                List<ValidationWarning> ruleWarnings = rule.analyze(context);
                if (ruleWarnings != null) {
                    // Filter out warnings for disabled individual rules
                    for (ValidationWarning warning : ruleWarnings) {
                        if (config.isRuleEnabled(warning.getErrorCode())) {
                            warnings.add(warning);
                        }
                    }
                }
            } catch (Exception e) {
                // Log error but continue with other rules
                System.err.println("Error in lint rule " + rule.getRuleId() + ": " + e.getMessage());
            }
        }

        return warnings;
    }

    /**
     * Analyzes multiple files and returns aggregated warnings.
     *
     * @param files map of file path to (parseTree, symbolTable) pairs
     * @return map of file path to warnings
     */
    public Map<String, List<ValidationWarning>> analyzeAll(
            Map<String, ParseTreeAndSymbolTable> files) {
        Map<String, List<ValidationWarning>> results = new HashMap<>();

        for (Map.Entry<String, ParseTreeAndSymbolTable> entry : files.entrySet()) {
            String filePath = entry.getKey();
            ParseTreeAndSymbolTable data = entry.getValue();

            List<ValidationWarning> warnings = analyze(
                    data.getParseTree(),
                    data.getSymbolTable(),
                    filePath,
                    data.getSourceCode()
            );

            if (!warnings.isEmpty()) {
                results.put(filePath, warnings);
            }
        }

        return results;
    }

    /**
     * Gets statistics about registered rules.
     *
     * @return statistics object
     */
    public LintStats getStats() {
        return new LintStats(rules, config);
    }

    /**
     * Helper class to hold parse tree and symbol table for batch analysis.
     */
    public static class ParseTreeAndSymbolTable {
        private final ParseTree parseTree;
        private final SymbolTable symbolTable;
        private final String sourceCode;

        public ParseTreeAndSymbolTable(ParseTree parseTree, SymbolTable symbolTable) {
            this(parseTree, symbolTable, null);
        }

        public ParseTreeAndSymbolTable(ParseTree parseTree, SymbolTable symbolTable,
                                        String sourceCode) {
            this.parseTree = parseTree;
            this.symbolTable = symbolTable;
            this.sourceCode = sourceCode;
        }

        public ParseTree getParseTree() {
            return parseTree;
        }

        public SymbolTable getSymbolTable() {
            return symbolTable;
        }

        public String getSourceCode() {
            return sourceCode;
        }
    }

    /**
     * Statistics about lint rules.
     */
    public static class LintStats {
        private final int totalRules;
        private final int enabledRules;
        private final Map<String, Integer> rulesByCategory;

        public LintStats(List<LintRule> rules, LintConfig config) {
            this.totalRules = rules.size();
            this.rulesByCategory = new HashMap<>();

            int enabled = 0;
            for (LintRule rule : rules) {
                String category = rule.getCategory();
                rulesByCategory.merge(category, 1, Integer::sum);

                if (config.isCategoryEnabled(category)) {
                    enabled++;
                }
            }
            this.enabledRules = enabled;
        }

        public int getTotalRules() {
            return totalRules;
        }

        public int getEnabledRules() {
            return enabledRules;
        }

        public Map<String, Integer> getRulesByCategory() {
            return Collections.unmodifiableMap(rulesByCategory);
        }

        @Override
        public String toString() {
            return String.format("LintStats{total=%d, enabled=%d, categories=%s}",
                    totalRules, enabledRules, rulesByCategory);
        }
    }
}
