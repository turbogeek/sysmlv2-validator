package com.validator.refactor;

import com.validator.parser.SysMLv2ParserFacade;
import com.validator.semantic.ImportStatement;
import com.validator.semantic.SymbolTable;
import com.validator.semantic.SymbolTableBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main engine for orchestrating refactoring operations.
 *
 * <p>The engine validates that files are error-free before performing
 * refactorings, applies edits safely, and provides multiple output formats
 * for tool integration.
 *
 * <h2>Safety Checks</h2>
 * <ul>
 *   <li>Files must be error-free before refactoring</li>
 *   <li>Edits are validated against current file content</li>
 *   <li>Backup can be created before applying changes</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * RefactoringEngine engine = new RefactoringEngine();
 *
 * // Analyze without applying
 * RefactoringPlan plan = engine.analyze("model.sysml");
 *
 * // Apply if file has no errors
 * if (plan.canApply()) {
 *     engine.apply(plan);
 * }
 * }</pre>
 *
 * <h2>Output Formats</h2>
 * <ul>
 *   <li>Human-readable (console)</li>
 *   <li>JSON (programmatic)</li>
 *   <li>LSP/Monaco (editor integration)</li>
 * </ul>
 */
public class RefactoringEngine {

    private final SysMLv2ParserFacade parser;
    private final ImportRefactoring importRefactoring;
    private boolean createBackup = true;
    private boolean errorFreeOnly = true;
    private OutputFormat outputFormat = OutputFormat.HUMAN;

    /**
     * Output format options.
     */
    public enum OutputFormat {
        HUMAN,
        JSON,
        LSP,
        MONACO
    }

    /**
     * Creates a new refactoring engine.
     */
    public RefactoringEngine() {
        this.parser = new SysMLv2ParserFacade();
        this.importRefactoring = new ImportRefactoring();
    }

    /**
     * Sets whether to create backups before applying refactorings.
     *
     * @param createBackup true to create backups
     */
    public void setCreateBackup(boolean createBackup) {
        this.createBackup = createBackup;
    }

    /**
     * Sets whether to only refactor error-free files.
     *
     * @param errorFreeOnly true to require error-free files
     */
    public void setErrorFreeOnly(boolean errorFreeOnly) {
        this.errorFreeOnly = errorFreeOnly;
    }

    /**
     * Sets the output format for refactoring results.
     *
     * @param outputFormat the output format
     */
    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * Analyzes a file for possible refactorings.
     *
     * @param filePath path to the SysML v2 file
     * @return refactoring plan with edits and validation status
     */
    public RefactoringPlan analyze(String filePath) {
        return analyze(Path.of(filePath));
    }

    /**
     * Analyzes a file for possible refactorings.
     *
     * @param filePath path to the SysML v2 file
     * @return refactoring plan with edits and validation status
     */
    public RefactoringPlan analyze(Path filePath) {
        String filePathStr = filePath.toString();

        // Read file content
        String content;
        try {
            content = Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return RefactoringPlan.error(filePathStr, "Cannot read file: " + e.getMessage());
        }

        return analyzeContent(content, filePathStr);
    }

    /**
     * Analyzes content for possible refactorings.
     *
     * @param content the SysML v2 source content
     * @param filePath the file path (for error reporting)
     * @return refactoring plan
     */
    public RefactoringPlan analyzeContent(String content, String filePath) {
        // Parse the file
        SysMLv2ParserFacade.ParseResult parseResult = parser.parseString(content, filePath);

        // Check for parse errors
        List<String> parseErrors = new ArrayList<>();
        if (parseResult.hasErrors()) {
            for (var error : parseResult.getSyntaxErrors()) {
                parseErrors.add(String.format("%s:%d:%d: %s",
                        filePath, error.getLine(), error.getCharPositionInLine(),
                        error.getMessage()));
            }
        }

        if (errorFreeOnly && !parseErrors.isEmpty()) {
            return RefactoringPlan.blocked(filePath, parseErrors,
                    "File has parse errors - refactoring blocked");
        }

        // Build symbol table
        SymbolTable symbolTable = SymbolTableBuilder.build(parseResult.getParseTree(), filePath);

        // Get imports from symbol table's global scope
        List<ImportStatement> imports = symbolTable.getGlobalScope().getAllImports();

        // Analyze import refactorings
        ImportRefactoring.RefactoringResult result = importRefactoring.analyze(
                parseResult.getParseTree(), imports, symbolTable, filePath);

        return new RefactoringPlan(
                filePath,
                content,
                result.getEdits(),
                parseErrors,
                result.getWildcardCount(),
                result.getRefactoredCount(),
                result.getUnusedCount()
        );
    }

    /**
     * Applies a refactoring plan to the file.
     *
     * @param plan the refactoring plan
     * @return result of applying the plan
     */
    public ApplyResult apply(RefactoringPlan plan) {
        if (!plan.canApply()) {
            return ApplyResult.blocked(plan.getBlockedReason());
        }

        if (plan.getEdits().isEmpty()) {
            return ApplyResult.noChanges();
        }

        Path filePath = Path.of(plan.getFilePath());
        String content = plan.getOriginalContent();

        // Create backup if requested
        if (createBackup) {
            try {
                Path backupPath = Path.of(plan.getFilePath() + ".bak");
                Files.writeString(backupPath, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                return ApplyResult.error("Cannot create backup: " + e.getMessage());
            }
        }

        // Apply edits in reverse order (bottom to top) to maintain line numbers
        List<RefactoringEdit> edits = new ArrayList<>(plan.getEdits());
        edits.sort((a, b) -> {
            int lineCmp = Integer.compare(b.getStartLine(), a.getStartLine());
            if (lineCmp != 0) {
                return lineCmp;
            }
            return Integer.compare(b.getStartColumn(), a.getStartColumn());
        });

        String modified = content;
        for (RefactoringEdit edit : edits) {
            try {
                modified = edit.apply(modified);
            } catch (Exception e) {
                return ApplyResult.error("Error applying edit: " + e.getMessage());
            }
        }

        // Write the modified content
        try {
            Files.writeString(filePath, modified, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return ApplyResult.error("Cannot write file: " + e.getMessage());
        }

        return ApplyResult.success(edits.size());
    }

    /**
     * Formats refactoring plan output.
     *
     * @param plan the refactoring plan
     * @return formatted output string
     */
    public String formatOutput(RefactoringPlan plan) {
        switch (outputFormat) {
            case JSON:
                return formatJson(plan);
            case LSP:
                return formatLsp(plan);
            case MONACO:
                return formatMonaco(plan);
            case HUMAN:
            default:
                return formatHuman(plan);
        }
    }

    /**
     * Formats as human-readable text.
     */
    private String formatHuman(RefactoringPlan plan) {
        StringBuilder sb = new StringBuilder();

        if (!plan.canApply()) {
            sb.append("=== Cannot refactor: ").append(plan.getBlockedReason()).append(" ===\n\n");
            for (String error : plan.getErrors()) {
                sb.append("  ").append(error).append("\n");
            }
            return sb.toString();
        }

        if (plan.getEdits().isEmpty()) {
            sb.append(plan.getFilePath()).append(": No wildcard imports to refactor\n");
            return sb.toString();
        }

        sb.append("=== Import Refactoring: ").append(plan.getFilePath()).append(" ===\n\n");
        sb.append("Wildcard imports found: ").append(plan.getWildcardCount()).append("\n");
        sb.append("Can be refactored: ").append(plan.getRefactoredCount()).append("\n");
        sb.append("Unused (can remove): ").append(plan.getUnusedCount()).append("\n\n");

        for (RefactoringEdit edit : plan.getEdits()) {
            sb.append(edit.toHumanReadable()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats as JSON.
     */
    private String formatJson(RefactoringPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"file\": \"").append(escapeJson(plan.getFilePath())).append("\",\n");
        sb.append("  \"canApply\": ").append(plan.canApply()).append(",\n");

        if (!plan.canApply()) {
            sb.append("  \"blockedReason\": \"").append(escapeJson(plan.getBlockedReason()))
              .append("\",\n");
            sb.append("  \"errors\": [");
            for (int i = 0; i < plan.getErrors().size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\n    \"").append(escapeJson(plan.getErrors().get(i))).append("\"");
            }
            sb.append("\n  ]\n");
        } else {
            sb.append("  \"statistics\": {\n");
            sb.append("    \"wildcardCount\": ").append(plan.getWildcardCount()).append(",\n");
            sb.append("    \"refactoredCount\": ").append(plan.getRefactoredCount()).append(",\n");
            sb.append("    \"unusedCount\": ").append(plan.getUnusedCount()).append("\n");
            sb.append("  },\n");
            sb.append("  \"edits\": [");
            for (int i = 0; i < plan.getEdits().size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\n    ").append(plan.getEdits().get(i).toJson()
                        .replace("\n", "\n    "));
            }
            sb.append("\n  ]\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Formats as LSP TextDocumentEdit.
     */
    private String formatLsp(RefactoringPlan plan) {
        if (!plan.canApply() || plan.getEdits().isEmpty()) {
            return "{ \"documentChanges\": [] }";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"documentChanges\": [\n");
        sb.append("    {\n");
        sb.append("      \"textDocument\": {\n");
        sb.append("        \"uri\": \"file://").append(escapeJson(plan.getFilePath()))
          .append("\",\n");
        sb.append("        \"version\": null\n");
        sb.append("      },\n");
        sb.append("      \"edits\": [");

        for (int i = 0; i < plan.getEdits().size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\n        ").append(plan.getEdits().get(i).toLspEdit()
                    .replace("\n", "\n        "));
        }

        sb.append("\n      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Formats as Monaco editor edit operations.
     */
    private String formatMonaco(RefactoringPlan plan) {
        if (!plan.canApply() || plan.getEdits().isEmpty()) {
            return "{ \"edits\": [] }";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"resource\": \"").append(escapeJson(plan.getFilePath())).append("\",\n");
        sb.append("  \"edits\": [");

        for (int i = 0; i < plan.getEdits().size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\n    ").append(plan.getEdits().get(i).toMonacoEdit()
                    .replace("\n", "\n    "));
        }

        sb.append("\n  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Refactoring plan containing edits and validation status.
     */
    public static class RefactoringPlan {
        private final String filePath;
        private final String originalContent;
        private final List<RefactoringEdit> edits;
        private final List<String> errors;
        private final int wildcardCount;
        private final int refactoredCount;
        private final int unusedCount;
        private final String blockedReason;
        private final boolean blocked;

        public RefactoringPlan(String filePath, String originalContent, List<RefactoringEdit> edits,
                               List<String> errors, int wildcardCount, int refactoredCount,
                               int unusedCount) {
            this.filePath = filePath;
            this.originalContent = originalContent;
            this.edits = edits;
            this.errors = errors;
            this.wildcardCount = wildcardCount;
            this.refactoredCount = refactoredCount;
            this.unusedCount = unusedCount;
            this.blockedReason = null;
            this.blocked = false;
        }

        private RefactoringPlan(String filePath, List<String> errors, String blockedReason) {
            this.filePath = filePath;
            this.originalContent = null;
            this.edits = Collections.emptyList();
            this.errors = errors;
            this.wildcardCount = 0;
            this.refactoredCount = 0;
            this.unusedCount = 0;
            this.blockedReason = blockedReason;
            this.blocked = true;
        }

        public static RefactoringPlan error(String filePath, String error) {
            return new RefactoringPlan(filePath, List.of(error), error);
        }

        public static RefactoringPlan blocked(String filePath, List<String> errors, String reason) {
            return new RefactoringPlan(filePath, errors, reason);
        }

        public String getFilePath() {
            return filePath;
        }

        public String getOriginalContent() {
            return originalContent;
        }

        public List<RefactoringEdit> getEdits() {
            return edits;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getWildcardCount() {
            return wildcardCount;
        }

        public int getRefactoredCount() {
            return refactoredCount;
        }

        public int getUnusedCount() {
            return unusedCount;
        }

        public boolean canApply() {
            return !blocked && !edits.isEmpty();
        }

        public String getBlockedReason() {
            return blockedReason;
        }
    }

    /**
     * Result of applying refactorings.
     */
    public static class ApplyResult {
        private final boolean success;
        private final int appliedCount;
        private final String error;

        private ApplyResult(boolean success, int appliedCount, String error) {
            this.success = success;
            this.appliedCount = appliedCount;
            this.error = error;
        }

        public static ApplyResult success(int appliedCount) {
            return new ApplyResult(true, appliedCount, null);
        }

        public static ApplyResult noChanges() {
            return new ApplyResult(true, 0, null);
        }

        public static ApplyResult error(String error) {
            return new ApplyResult(false, 0, error);
        }

        public static ApplyResult blocked(String reason) {
            return new ApplyResult(false, 0, "Blocked: " + reason);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getAppliedCount() {
            return appliedCount;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            if (success) {
                return appliedCount > 0
                        ? "Applied " + appliedCount + " refactorings successfully"
                        : "No changes to apply";
            } else {
                return "Failed: " + error;
            }
        }
    }
}
