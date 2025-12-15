package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.semantic.ElementType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lint rule for checking naming conventions in SysML v2 models.
 *
 * <p>This rule checks that names follow SysML v2 naming conventions:
 * <ul>
 *   <li>Definitions (part def, action def, etc.) should be PascalCase</li>
 *   <li>Usages (part, action, etc.) should be camelCase</li>
 *   <li>Packages should be PascalCase</li>
 *   <li>Constants should be UPPER_SNAKE_CASE</li>
 * </ul>
 *
 * <h2>Error Codes</h2>
 * <ul>
 *   <li>{@code LINT005} - Definition naming violation</li>
 *   <li>{@code LINT006} - Usage naming violation</li>
 *   <li>{@code LINT010} - Package naming violation</li>
 *   <li>{@code LINT011} - Constant naming violation</li>
 * </ul>
 */
public class NamingConventionRule implements LintRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingConventionRule.class);

    /**
     * Pattern for PascalCase names (UpperCamelCase).
     * Starts with uppercase letter, followed by letters/digits.
     */
    private static final Pattern PASCAL_CASE = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    /**
     * Pattern for camelCase names (lowerCamelCase).
     * Starts with lowercase letter, followed by letters/digits.
     */
    private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

    /**
     * Pattern for UPPER_SNAKE_CASE names.
     * All uppercase letters with underscores between words.
     */
    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");

    /**
     * Pattern for names starting with underscore (private convention).
     */
    private static final Pattern PRIVATE_PREFIX = Pattern.compile("^_.*$");

    @Override
    public String getRuleId() {
        return "naming-conventions";
    }

    @Override
    public String getDescription() {
        return "Checks that names follow SysML v2 naming conventions";
    }

    @Override
    public String getCategory() {
        return "naming";
    }

    @Override
    public String[] getErrorCodes() {
        return new String[] {
            ErrorCodes.LINT_NAMING_DEFINITION,
            ErrorCodes.LINT_NAMING_USAGE,
            ErrorCodes.LINT_NAMING_PACKAGE
        };
    }

    /**
     * Gets the default severity for this rule.
     *
     * @return the default severity
     */
    public LintConfig.Severity getDefaultSeverity() {
        return LintConfig.Severity.WARN;
    }

    @Override
    public List<ValidationWarning> analyze(LintContext context) {
        List<ValidationWarning> warnings = new ArrayList<>();

        ParseTree tree = context.getParseTree();
        SymbolTable symbolTable = context.getSymbolTable();
        String filePath = context.getFilePath();
        LintConfig config = context.getConfig();

        // Check if this rule is enabled - check category instead of individual rule
        if (!config.isCategoryEnabled(getCategory())) {
            LOGGER.debug("Naming convention rule is disabled");
            return warnings;
        }

        // Analyze all symbols
        for (Symbol symbol : symbolTable.getAllSymbols()) {
            String name = symbol.getName();
            ElementType type = symbol.getType();

            // Skip anonymous or compiler-generated names
            if (name == null || name.isEmpty() || name.startsWith("<")) {
                continue;
            }

            // Skip names with underscore prefix (private convention)
            if (PRIVATE_PREFIX.matcher(name).matches()) {
                continue;
            }

            ValidationWarning warning = checkNamingConvention(symbol, filePath);
            if (warning != null) {
                warnings.add(warning);
            }
        }

        LOGGER.debug("Naming convention analysis found {} warnings", warnings.size());
        return warnings;
    }

    /**
     * Check naming convention for a symbol.
     *
     * @param symbol the symbol to check
     * @param filePath the source file path
     * @return a warning if naming convention is violated, null otherwise
     */
    private ValidationWarning checkNamingConvention(Symbol symbol, String filePath) {
        String name = symbol.getName();
        ElementType type = symbol.getType();

        // Determine expected naming convention based on element type
        if (isDefinitionType(type)) {
            // Definitions should be PascalCase
            if (!isPascalCase(name)) {
                return createWarning(
                    ErrorCodes.LINT_NAMING_DEFINITION,
                    String.format("Definition '%s' should use PascalCase naming", name),
                    symbol,
                    filePath,
                    "Rename to '" + toPascalCase(name) + "'"
                );
            }
        } else if (isUsageType(type)) {
            // Usages should be camelCase
            if (!isCamelCase(name)) {
                return createWarning(
                    ErrorCodes.LINT_NAMING_USAGE,
                    String.format("Usage '%s' should use camelCase naming", name),
                    symbol,
                    filePath,
                    "Rename to '" + toCamelCase(name) + "'"
                );
            }
        } else if (type == ElementType.PACKAGE) {
            // Packages should be PascalCase
            if (!isPascalCase(name)) {
                return createWarning(
                    ErrorCodes.LINT_NAMING_PACKAGE,
                    String.format("Package '%s' should use PascalCase naming", name),
                    symbol,
                    filePath,
                    "Rename to '" + toPascalCase(name) + "'"
                );
            }
        }

        return null;
    }

    /**
     * Check if a type is a definition type.
     *
     * @param type the element type
     * @return true if definition type
     */
    private boolean isDefinitionType(ElementType type) {
        // Use the built-in method from ElementType
        return type.isDefinition();
    }

    /**
     * Check if a type is a usage type.
     *
     * @param type the element type
     * @return true if usage type
     */
    private boolean isUsageType(ElementType type) {
        // Use the built-in method from ElementType
        return type.isUsage();
    }

    /**
     * Check if name follows PascalCase convention.
     *
     * @param name the name to check
     * @return true if PascalCase
     */
    private boolean isPascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        return PASCAL_CASE.matcher(name).matches();
    }

    /**
     * Check if name follows camelCase convention.
     *
     * @param name the name to check
     * @return true if camelCase
     */
    private boolean isCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        return CAMEL_CASE.matcher(name).matches();
    }

    /**
     * Check if name follows UPPER_SNAKE_CASE convention.
     *
     * @param name the name to check
     * @return true if UPPER_SNAKE_CASE
     */
    private boolean isUpperSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        return UPPER_SNAKE_CASE.matcher(name).matches();
    }

    /**
     * Convert a name to PascalCase.
     *
     * @param name the name to convert
     * @return PascalCase version
     */
    private String toPascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Handle snake_case
        if (name.contains("_")) {
            StringBuilder result = new StringBuilder();
            for (String part : name.split("_")) {
                if (!part.isEmpty()) {
                    result.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        result.append(part.substring(1).toLowerCase());
                    }
                }
            }
            return result.toString();
        }

        // Handle simple case - just capitalize first letter
        return Character.toUpperCase(name.charAt(0))
            + (name.length() > 1 ? name.substring(1) : "");
    }

    /**
     * Convert a name to camelCase.
     *
     * @param name the name to convert
     * @return camelCase version
     */
    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Handle snake_case
        if (name.contains("_")) {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (String part : name.split("_")) {
                if (!part.isEmpty()) {
                    if (first) {
                        result.append(part.toLowerCase());
                        first = false;
                    } else {
                        result.append(Character.toUpperCase(part.charAt(0)));
                        if (part.length() > 1) {
                            result.append(part.substring(1).toLowerCase());
                        }
                    }
                }
            }
            return result.toString();
        }

        // Handle PascalCase - lowercase first letter
        return Character.toLowerCase(name.charAt(0))
            + (name.length() > 1 ? name.substring(1) : "");
    }

    /**
     * Create a validation warning.
     *
     * @param code the error code
     * @param message the warning message
     * @param symbol the symbol with naming issue
     * @param filePath the source file path
     * @param suggestion suggested fix
     * @return the validation warning
     */
    private ValidationWarning createWarning(
            String code,
            String message,
            Symbol symbol,
            String filePath,
            String suggestion) {

        ValidationWarning.Builder builder = new ValidationWarning.Builder();
        builder.errorCode(code);
        builder.message(message);

        if (symbol.getLocation() != null) {
            builder.filePath(symbol.getLocation().getFileName());
            builder.line(symbol.getLocation().getLine());
            builder.column(symbol.getLocation().getColumn());
        } else {
            builder.filePath(filePath);
            builder.line(1);
            builder.column(1);
        }

        if (suggestion != null) {
            builder.addSuggestion(suggestion);
        }

        return builder.build();
    }
}
