package com.validator.lint;

import com.validator.ValidationWarning;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
 * Interface for lint rules that analyze SysML v2 models.
 *
 * <p>Each lint rule checks for a specific class of issues and produces
 * warnings when violations are found. Rules can be enabled/disabled via
 * {@link LintConfig}.
 *
 * <h2>Implementing a Lint Rule</h2>
 * <pre>{@code
 * public class UnusedElementRule implements LintRule {
 *     @Override
 *     public String getRuleId() {
 *         return "unused";
 *     }
 *
 *     @Override
 *     public List<ValidationWarning> analyze(LintContext context) {
 *         // Analyze and return warnings
 *     }
 * }
 * }</pre>
 */
public interface LintRule {

    /**
     * Gets the unique identifier for this rule.
     * This is used for enabling/disabling the rule in configuration.
     *
     * @return the rule identifier
     */
    String getRuleId();

    /**
     * Gets the category this rule belongs to.
     * Categories are used for bulk enable/disable in configuration.
     *
     * @return the category name (e.g., "unused", "naming", "complexity")
     */
    String getCategory();

    /**
     * Gets a human-readable description of what this rule checks.
     *
     * @return the rule description
     */
    String getDescription();

    /**
     * Analyzes the given context and returns any warnings found.
     *
     * @param context the lint context containing parse tree, symbol table, and config
     * @return list of warnings (empty if no issues found)
     */
    List<ValidationWarning> analyze(LintContext context);

    /**
     * Checks if this rule is applicable to the given file.
     * Override to skip analysis for certain file types.
     *
     * @param filePath the file path
     * @return true if the rule should be applied
     */
    default boolean isApplicable(String filePath) {
        return filePath != null
                && (filePath.endsWith(".sysml") || filePath.endsWith(".kerml"));
    }

    /**
     * Gets the error codes that this rule can produce.
     *
     * @return array of error codes
     */
    String[] getErrorCodes();
}
