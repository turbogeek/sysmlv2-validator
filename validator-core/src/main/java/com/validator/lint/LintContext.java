package com.validator.lint;

import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Context object passed to lint rules during analysis.
 * Contains all the information needed to analyze a SysML v2 model.
 */
public class LintContext {

    private final ParseTree parseTree;
    private final SymbolTable symbolTable;
    private final String filePath;
    private final String sourceCode;
    private final LintConfig config;
    private final Path projectDir;

    /**
     * Creates a new lint context.
     *
     * @param parseTree the ANTLR parse tree
     * @param symbolTable the symbol table built from the parse tree
     * @param filePath the path to the source file
     * @param sourceCode the original source code (optional)
     * @param config the lint configuration
     * @param projectDir the project directory (for finding config files)
     */
    public LintContext(ParseTree parseTree, SymbolTable symbolTable, String filePath,
                       String sourceCode, LintConfig config, Path projectDir) {
        this.parseTree = Objects.requireNonNull(parseTree, "parseTree cannot be null");
        this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable cannot be null");
        this.filePath = filePath;
        this.sourceCode = sourceCode;
        this.config = config != null ? config : new LintConfig();
        this.projectDir = projectDir;
    }

    /**
     * Builder for LintContext.
     */
    public static class Builder {
        private ParseTree parseTree;
        private SymbolTable symbolTable;
        private String filePath;
        private String sourceCode;
        private LintConfig config;
        private Path projectDir;

        public Builder parseTree(ParseTree parseTree) {
            this.parseTree = parseTree;
            return this;
        }

        public Builder symbolTable(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder sourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
            return this;
        }

        public Builder config(LintConfig config) {
            this.config = config;
            return this;
        }

        public Builder projectDir(Path projectDir) {
            this.projectDir = projectDir;
            return this;
        }

        public LintContext build() {
            return new LintContext(parseTree, symbolTable, filePath, sourceCode,
                    config, projectDir);
        }
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the ANTLR parse tree.
     */
    public ParseTree getParseTree() {
        return parseTree;
    }

    /**
     * Gets the symbol table.
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * Gets the source file path.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Gets the file name without directory path.
     */
    public String getFileName() {
        if (filePath == null) {
            return "<unknown>";
        }
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }

    /**
     * Gets the original source code.
     */
    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * Gets the lint configuration.
     */
    public LintConfig getConfig() {
        return config;
    }

    /**
     * Gets the project directory.
     */
    public Path getProjectDir() {
        return projectDir;
    }

    /**
     * Checks if a specific rule is enabled.
     *
     * @param errorCode the error code to check
     * @return true if the rule is enabled
     */
    public boolean isRuleEnabled(String errorCode) {
        return config.isRuleEnabled(errorCode);
    }

    /**
     * Checks if a category of rules is enabled.
     *
     * @param category the category to check
     * @return true if the category is enabled
     */
    public boolean isCategoryEnabled(String category) {
        return config.isCategoryEnabled(category);
    }
}
