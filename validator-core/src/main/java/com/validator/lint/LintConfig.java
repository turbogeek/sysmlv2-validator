package com.validator.lint;

import com.validator.ErrorCodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Configuration for lint analysis.
 * Supports enabling/disabling warning categories and individual rules.
 *
 * <p>Configuration can be loaded from a .sysml-lint.json file or set programmatically.
 *
 * <h2>Warning Categories</h2>
 * <ul>
 *   <li>unused - Unused definitions and imports</li>
 *   <li>missing-types - Missing type definitions</li>
 *   <li>naming - Naming convention violations</li>
 *   <li>documentation - Documentation issues</li>
 *   <li>complexity - Complexity warnings</li>
 *   <li>best-practices - Best practice violations</li>
 * </ul>
 */
public class LintConfig {

    /**
     * Severity level for lint rules.
     */
    public enum Severity {
        /** Rule is disabled */
        OFF,
        /** Rule produces informational messages */
        INFO,
        /** Rule produces warnings (default) */
        WARN,
        /** Rule produces errors */
        ERROR
    }

    /** Default maximum nesting depth before warning */
    public static final int DEFAULT_MAX_NESTING_DEPTH = 5;

    /** Default maximum elements per package before warning */
    public static final int DEFAULT_MAX_PACKAGE_SIZE = 50;

    /** Default maximum parameters per action before warning */
    public static final int DEFAULT_MAX_PARAMETERS = 7;

    // Category configurations
    private final Map<String, Severity> categorySeverities = new java.util.HashMap<>();

    // Individual rule overrides (takes precedence over category)
    private final Map<String, Severity> ruleSeverities = new java.util.HashMap<>();

    // Disabled categories
    private final Set<String> disabledCategories = new HashSet<>();

    // CLI/programmatic overrides that take precedence over config files
    private final Map<String, Severity> cliOverrides = new java.util.HashMap<>();

    // Whether to allow CLI overrides
    private boolean allowCliOverrides = true;

    // Whether spell checking is enabled (separate from other lint)
    private boolean spellCheckEnabled = true;

    // Complexity thresholds
    private int maxNestingDepth = DEFAULT_MAX_NESTING_DEPTH;
    private int maxPackageSize = DEFAULT_MAX_PACKAGE_SIZE;
    private int maxParameters = DEFAULT_MAX_PARAMETERS;

    // File patterns to ignore
    private final Set<Pattern> ignorePatterns = new HashSet<>();

    // Path to user dictionary
    private Path dictionaryPath;

    /**
     * Creates a default configuration with all warnings enabled.
     */
    public LintConfig() {
        // Set default severities for all categories
        categorySeverities.put("unused", Severity.WARN);
        categorySeverities.put("missing-types", Severity.WARN);
        categorySeverities.put("naming", Severity.WARN);
        categorySeverities.put("documentation", Severity.OFF); // Off by default
        categorySeverities.put("complexity", Severity.WARN);
        categorySeverities.put("best-practices", Severity.INFO);
        categorySeverities.put("imports", Severity.WARN); // Wildcard/public import warnings
        categorySeverities.put("units", Severity.WARN); // Value-unit completeness/correctness warnings
    }

    /**
     * Creates a configuration with all lint checks disabled.
     *
     * @return a disabled configuration
     */
    public static LintConfig disabled() {
        LintConfig config = new LintConfig();
        config.disableAll();
        return config;
    }

    /**
     * Creates a strict configuration with all checks as errors.
     *
     * @return a strict configuration
     */
    public static LintConfig strict() {
        LintConfig config = new LintConfig();
        config.categorySeverities.replaceAll((k, v) -> Severity.ERROR);
        config.categorySeverities.put("documentation", Severity.WARN);
        return config;
    }

    /** Config file name for project-local config */
    public static final String PROJECT_CONFIG_NAME = ".sysml-lint.json";

    /** Config file name for user home config */
    public static final String USER_CONFIG_NAME = ".sysml-lint.json";

    /** Config directory name in user home */
    public static final String USER_CONFIG_DIR = ".sysml-validator";

    /**
     * Loads configuration from a file.
     *
     * @param configPath path to the configuration file
     * @return the loaded configuration
     * @throws IOException if the file cannot be read
     */
    public static LintConfig load(Path configPath) throws IOException {
        LintConfig config = new LintConfig();

        if (!Files.exists(configPath)) {
            return config;
        }

        String content = Files.readString(configPath);
        config.parseConfig(content);
        return config;
    }

    /**
     * Loads configuration with cascading precedence:
     * 1. Project-local config (.sysml-lint.json in project directory)
     * 2. User home config (~/.sysml-validator/.sysml-lint.json)
     * 3. Default configuration
     *
     * <p>Project config overrides user config which overrides defaults.
     *
     * @param projectDir the project directory to search for local config
     * @return the merged configuration
     */
    public static LintConfig loadWithDefaults(Path projectDir) {
        LintConfig config = new LintConfig();

        // 1. Load user home config first (lowest precedence)
        Path userConfigDir = Path.of(System.getProperty("user.home"), USER_CONFIG_DIR);
        Path userConfigPath = userConfigDir.resolve(USER_CONFIG_NAME);
        if (Files.exists(userConfigPath)) {
            try {
                String content = Files.readString(userConfigPath);
                config.parseConfig(content);
            } catch (IOException e) {
                // Ignore user config errors, use defaults
            }
        }

        // 2. Load project-local config (higher precedence)
        if (projectDir != null) {
            Path projectConfigPath = projectDir.resolve(PROJECT_CONFIG_NAME);
            if (Files.exists(projectConfigPath)) {
                try {
                    String content = Files.readString(projectConfigPath);
                    config.parseConfig(content);
                } catch (IOException e) {
                    // Ignore project config errors, use what we have
                }
            }
        }

        return config;
    }

    /**
     * Gets the user config directory path.
     *
     * @return path to user config directory
     */
    public static Path getUserConfigDir() {
        return Path.of(System.getProperty("user.home"), USER_CONFIG_DIR);
    }

    /**
     * Gets the user config file path.
     *
     * @return path to user config file
     */
    public static Path getUserConfigPath() {
        return getUserConfigDir().resolve(USER_CONFIG_NAME);
    }

    /**
     * Saves current configuration to the user config file.
     *
     * @throws IOException if the file cannot be written
     */
    public void saveToUserConfig() throws IOException {
        Path configDir = getUserConfigDir();
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
        saveToFile(getUserConfigPath());
    }

    /**
     * Saves current configuration to a file.
     *
     * @param configPath the path to save to
     * @throws IOException if the file cannot be written
     */
    public void saveToFile(Path configPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"rules\": {\n");

        boolean first = true;
        for (Map.Entry<String, Severity> entry : categorySeverities.entrySet()) {
            if (!first) {
                sb.append(",\n");
            }
            sb.append("    \"").append(entry.getKey()).append("\": \"")
              .append(entry.getValue().name().toLowerCase()).append("\"");
            first = false;
        }
        sb.append("\n  },\n");

        sb.append("  \"thresholds\": {\n");
        sb.append("    \"maxDepth\": ").append(maxNestingDepth).append(",\n");
        sb.append("    \"maxPackageSize\": ").append(maxPackageSize).append(",\n");
        sb.append("    \"maxParameters\": ").append(maxParameters).append("\n");
        sb.append("  }");

        if (dictionaryPath != null) {
            sb.append(",\n  \"dictionary\": \"").append(dictionaryPath.toString()
                    .replace("\\", "/")).append("\"");
        }

        sb.append("\n}\n");

        Files.writeString(configPath, sb.toString());
    }

    /**
     * Parses configuration from JSON-like content.
     * Simple parsing without external JSON library.
     */
    private void parseConfig(String content) {
        // Simple key-value parsing for rules
        // Format: "category": "severity" or "category": "off"
        for (String category : categorySeverities.keySet()) {
            if (content.contains("\"" + category + "\"")) {
                if (content.contains("\"" + category + "\": \"off\"")
                        || content.contains("\"" + category + "\":\"off\"")) {
                    disableCategory(category);
                } else if (content.contains("\"" + category + "\": \"error\"")
                        || content.contains("\"" + category + "\":\"error\"")) {
                    setCategorySeverity(category, Severity.ERROR);
                } else if (content.contains("\"" + category + "\": \"info\"")
                        || content.contains("\"" + category + "\":\"info\"")) {
                    setCategorySeverity(category, Severity.INFO);
                }
            }
        }

        // Parse maxDepth if present
        java.util.regex.Matcher depthMatcher = Pattern.compile(
                "\"maxDepth\"\\s*:\\s*(\\d+)").matcher(content);
        if (depthMatcher.find()) {
            maxNestingDepth = Integer.parseInt(depthMatcher.group(1));
        }

        // Parse maxPackageSize if present
        java.util.regex.Matcher sizeMatcher = Pattern.compile(
                "\"maxPackageSize\"\\s*:\\s*(\\d+)").matcher(content);
        if (sizeMatcher.find()) {
            maxPackageSize = Integer.parseInt(sizeMatcher.group(1));
        }

        // Parse dictionary path if present
        java.util.regex.Matcher dictMatcher = Pattern.compile(
                "\"dictionary\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
        if (dictMatcher.find()) {
            dictionaryPath = Path.of(dictMatcher.group(1));
        }
    }

    /**
     * Disables all lint checks.
     */
    public void disableAll() {
        categorySeverities.replaceAll((k, v) -> Severity.OFF);
    }

    /**
     * Enables all lint checks with default severity.
     */
    public void enableAll() {
        categorySeverities.put("unused", Severity.WARN);
        categorySeverities.put("missing-types", Severity.WARN);
        categorySeverities.put("naming", Severity.WARN);
        categorySeverities.put("documentation", Severity.WARN);
        categorySeverities.put("complexity", Severity.WARN);
        categorySeverities.put("best-practices", Severity.WARN);
        categorySeverities.put("imports", Severity.WARN);
        disabledCategories.clear();
    }

    /**
     * Disables a category of lint checks.
     *
     * @param category the category to disable
     */
    public void disableCategory(String category) {
        disabledCategories.add(category);
        categorySeverities.put(category, Severity.OFF);
    }

    /**
     * Enables a category of lint checks.
     *
     * @param category the category to enable
     */
    public void enableCategory(String category) {
        disabledCategories.remove(category);
        if (categorySeverities.get(category) == Severity.OFF) {
            categorySeverities.put(category, Severity.WARN);
        }
    }

    /**
     * Sets the severity for a category.
     *
     * @param category the category
     * @param severity the severity level
     */
    public void setCategorySeverity(String category, Severity severity) {
        categorySeverities.put(category, severity);
        if (severity == Severity.OFF) {
            disabledCategories.add(category);
        } else {
            disabledCategories.remove(category);
        }
    }

    /**
     * Sets the severity for a specific rule (overrides category).
     *
     * @param errorCode the error code
     * @param severity the severity level
     */
    public void setRuleSeverity(String errorCode, Severity severity) {
        ruleSeverities.put(errorCode, severity);
    }

    /**
     * Checks if a category is enabled.
     *
     * @param category the category name
     * @return true if the category is enabled
     */
    public boolean isCategoryEnabled(String category) {
        return !disabledCategories.contains(category)
                && categorySeverities.getOrDefault(category, Severity.OFF) != Severity.OFF;
    }

    /**
     * Checks if a specific rule is enabled.
     *
     * @param errorCode the error code
     * @return true if the rule is enabled
     */
    public boolean isRuleEnabled(String errorCode) {
        // Check rule-specific override first
        Severity ruleSeverity = ruleSeverities.get(errorCode);
        if (ruleSeverity != null) {
            return ruleSeverity != Severity.OFF;
        }

        // Fall back to category
        String category = ErrorCodes.getLintCategory(errorCode);
        if (category == null) {
            category = ErrorCodes.getConstraintCategory(errorCode);
        }
        return category != null && isCategoryEnabled(category);
    }

    /**
     * Gets the severity for a rule.
     *
     * @param errorCode the error code
     * @return the severity level
     */
    public Severity getRuleSeverity(String errorCode) {
        // Check rule-specific override first
        Severity ruleSeverity = ruleSeverities.get(errorCode);
        if (ruleSeverity != null) {
            return ruleSeverity;
        }

        // Fall back to category
        String category = ErrorCodes.getLintCategory(errorCode);
        if (category == null) {
            category = ErrorCodes.getConstraintCategory(errorCode);
        }
        return category != null
                ? categorySeverities.getOrDefault(category, Severity.WARN)
                : Severity.WARN;
    }

    /**
     * Gets the maximum nesting depth threshold.
     */
    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    /**
     * Sets the maximum nesting depth threshold.
     */
    public void setMaxNestingDepth(int depth) {
        this.maxNestingDepth = depth;
    }

    /**
     * Gets the maximum package size threshold.
     */
    public int getMaxPackageSize() {
        return maxPackageSize;
    }

    /**
     * Sets the maximum package size threshold.
     */
    public void setMaxPackageSize(int size) {
        this.maxPackageSize = size;
    }

    /**
     * Gets the maximum parameters threshold.
     */
    public int getMaxParameters() {
        return maxParameters;
    }

    /**
     * Sets the maximum parameters threshold.
     */
    public void setMaxParameters(int params) {
        this.maxParameters = params;
    }

    /**
     * Gets the user dictionary path.
     */
    public Path getDictionaryPath() {
        return dictionaryPath;
    }

    /**
     * Sets the user dictionary path.
     */
    public void setDictionaryPath(Path path) {
        this.dictionaryPath = path;
    }

    /**
     * Adds a file pattern to ignore during lint analysis.
     *
     * @param pattern glob-style pattern (e.g., "**&#47;test&#47;**")
     */
    public void addIgnorePattern(String pattern) {
        // Convert glob to regex
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replace("?", ".");
        ignorePatterns.add(Pattern.compile(regex));
    }

    /**
     * Checks if a file path should be ignored.
     *
     * @param filePath the file path to check
     * @return true if the file should be ignored
     */
    public boolean shouldIgnore(String filePath) {
        for (Pattern pattern : ignorePatterns) {
            if (pattern.matcher(filePath).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all disabled categories.
     */
    public Set<String> getDisabledCategories() {
        return Collections.unmodifiableSet(disabledCategories);
    }

    // =========================================================================
    // CLI Override Support
    // =========================================================================

    /**
     * Sets a CLI override that takes precedence over config file settings.
     * This allows users to enable checks via command line even if disabled in config.
     *
     * @param category the category to override
     * @param severity the severity to use
     */
    public void setCliOverride(String category, Severity severity) {
        if (allowCliOverrides) {
            cliOverrides.put(category, severity);
        }
    }

    /**
     * Sets a CLI override for a specific rule.
     *
     * @param errorCode the error code to override
     * @param severity the severity to use
     */
    public void setRuleCliOverride(String errorCode, Severity severity) {
        if (allowCliOverrides) {
            cliOverrides.put(errorCode, severity);
        }
    }

    /**
     * Clears all CLI overrides.
     */
    public void clearCliOverrides() {
        cliOverrides.clear();
    }

    /**
     * Checks if CLI overrides are allowed.
     */
    public boolean isAllowCliOverrides() {
        return allowCliOverrides;
    }

    /**
     * Sets whether CLI overrides are allowed.
     * When false, config files have final say.
     */
    public void setAllowCliOverrides(boolean allow) {
        this.allowCliOverrides = allow;
    }

    /**
     * Checks if spell checking is enabled.
     * Spell check can be enabled/disabled independently of other lint rules.
     */
    public boolean isSpellCheckEnabled() {
        return spellCheckEnabled;
    }

    /**
     * Enables or disables spell checking.
     * This is a CLI override that works even if disabled in config.
     */
    public void setSpellCheckEnabled(boolean enabled) {
        this.spellCheckEnabled = enabled;
    }

    /**
     * Gets the effective severity for a category, considering CLI overrides.
     *
     * @param category the category
     * @return the effective severity
     */
    public Severity getEffectiveCategorySeverity(String category) {
        // CLI override has highest precedence
        Severity cliSeverity = cliOverrides.get(category);
        if (cliSeverity != null) {
            return cliSeverity;
        }
        return categorySeverities.getOrDefault(category, Severity.OFF);
    }

    /**
     * Checks if a category is effectively enabled, considering CLI overrides.
     */
    public boolean isCategoryEffectivelyEnabled(String category) {
        return getEffectiveCategorySeverity(category) != Severity.OFF;
    }

    // =========================================================================
    // Directory-Specific Config Support
    // =========================================================================

    /**
     * Loads configuration for a specific file, searching up the directory tree.
     * This allows different directories to have different lint configurations.
     *
     * @param filePath the file being analyzed
     * @return the merged configuration for that file's location
     */
    public static LintConfig loadForFile(Path filePath) {
        if (filePath == null) {
            return new LintConfig();
        }

        LintConfig config = new LintConfig();
        Path parent = filePath.getParent();

        // Search up directory tree for config files
        java.util.List<Path> configPaths = new java.util.ArrayList<>();
        while (parent != null) {
            Path configPath = parent.resolve(PROJECT_CONFIG_NAME);
            if (Files.exists(configPath)) {
                configPaths.add(0, configPath); // Add at start for precedence
            }
            parent = parent.getParent();
        }

        // Also check user config
        Path userConfigPath = getUserConfigPath();
        if (Files.exists(userConfigPath)) {
            configPaths.add(0, userConfigPath); // User config has lowest precedence
        }

        // Apply configs in order (user first, then directory tree bottom-up)
        for (Path configPath : configPaths) {
            try {
                String content = Files.readString(configPath);
                config.parseConfig(content);
            } catch (IOException e) {
                // Skip invalid configs
            }
        }

        return config;
    }

    /**
     * Creates a config for a specific directory that inherits from parent configs.
     *
     * @param directory the directory
     * @return the merged configuration
     */
    public static LintConfig loadForDirectory(Path directory) {
        if (directory == null) {
            return new LintConfig();
        }

        // Create a dummy file path to reuse the file-based logic
        Path dummyFile = directory.resolve("dummy.sysml");
        return loadForFile(dummyFile);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LintConfig{\n");
        sb.append("  categories: ").append(categorySeverities).append("\n");
        sb.append("  disabled: ").append(disabledCategories).append("\n");
        sb.append("  maxNestingDepth: ").append(maxNestingDepth).append("\n");
        sb.append("  maxPackageSize: ").append(maxPackageSize).append("\n");
        sb.append("  maxParameters: ").append(maxParameters).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
