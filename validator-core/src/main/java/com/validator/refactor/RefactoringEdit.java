package com.validator.refactor;

import java.util.Objects;

/**
 * Represents a single edit to be applied to a source file.
 *
 * <p>Each edit specifies a range of text to be replaced with new text.
 * The edit includes:
 * <ul>
 *   <li>File path</li>
 *   <li>Start position (line, column)</li>
 *   <li>End position (line, column)</li>
 *   <li>Original text (for verification)</li>
 *   <li>New text to replace it with</li>
 *   <li>Description of the change</li>
 * </ul>
 *
 * <p>Edits can be output in various formats for different tools:
 * <ul>
 *   <li>JSON - for programmatic consumption</li>
 *   <li>LSP/Monaco - for editor integration</li>
 *   <li>Human-readable - for CLI output</li>
 * </ul>
 */
public class RefactoringEdit {

    private final String filePath;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final String oldText;
    private final String newText;
    private final String description;
    private final String errorCode;

    /**
     * Create a new refactoring edit.
     *
     * @param filePath    the file to edit
     * @param startLine   start line (1-based)
     * @param startColumn start column (1-based)
     * @param endLine     end line (1-based)
     * @param endColumn   end column (1-based)
     * @param oldText     the original text (for verification)
     * @param newText     the replacement text
     * @param description human-readable description
     * @param errorCode   associated error code (for diagnostics)
     */
    public RefactoringEdit(String filePath, int startLine, int startColumn,
                           int endLine, int endColumn, String oldText,
                           String newText, String description, String errorCode) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.oldText = oldText;
        this.newText = newText;
        this.description = description;
        this.errorCode = errorCode;
    }

    // Getters

    public String getFilePath() {
        return filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public String getOldText() {
        return oldText;
    }

    public String getNewText() {
        return newText;
    }

    public String getDescription() {
        return description;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Apply this edit to file content.
     *
     * @param fileContent the original file content
     * @return the modified file content
     */
    public String apply(String fileContent) {
        if (fileContent == null) {
            return newText;
        }

        String[] lines = fileContent.split("\n", -1);

        // Calculate character offsets
        int startOffset = calculateOffset(lines, startLine, startColumn);
        int endOffset = calculateOffset(lines, endLine, endColumn);

        if (startOffset < 0 || endOffset < 0 || startOffset > endOffset) {
            throw new IllegalStateException("Invalid edit range");
        }

        StringBuilder result = new StringBuilder();
        result.append(fileContent, 0, startOffset);
        result.append(newText);
        result.append(fileContent.substring(endOffset));

        return result.toString();
    }

    /**
     * Calculate character offset from line and column.
     */
    private int calculateOffset(String[] lines, int line, int column) {
        if (line < 1 || line > lines.length) {
            return -1;
        }

        int offset = 0;
        for (int i = 0; i < line - 1; i++) {
            offset += lines[i].length() + 1; // +1 for newline
        }
        offset += column - 1;
        return offset;
    }

    /**
     * Convert to JSON format.
     *
     * @return JSON representation
     */
    public String toJson() {
        return String.format(
            "{\n"
            + "  \"file\": \"%s\",\n"
            + "  \"range\": {\n"
            + "    \"start\": { \"line\": %d, \"column\": %d },\n"
            + "    \"end\": { \"line\": %d, \"column\": %d }\n"
            + "  },\n"
            + "  \"oldText\": \"%s\",\n"
            + "  \"newText\": \"%s\",\n"
            + "  \"description\": \"%s\",\n"
            + "  \"errorCode\": \"%s\"\n"
            + "}",
            escapeJson(filePath),
            startLine, startColumn,
            endLine, endColumn,
            escapeJson(oldText),
            escapeJson(newText),
            escapeJson(description),
            escapeJson(errorCode)
        );
    }

    /**
     * Convert to Monaco/LSP TextEdit format.
     *
     * @return LSP-compatible JSON
     */
    public String toMonacoEdit() {
        // Monaco uses 0-based lines, 0-based columns
        return String.format(
            "{\n"
            + "  \"range\": {\n"
            + "    \"startLineNumber\": %d,\n"
            + "    \"startColumn\": %d,\n"
            + "    \"endLineNumber\": %d,\n"
            + "    \"endColumn\": %d\n"
            + "  },\n"
            + "  \"text\": \"%s\"\n"
            + "}",
            startLine, startColumn,
            endLine, endColumn,
            escapeJson(newText)
        );
    }

    /**
     * Convert to LSP TextEdit format.
     * LSP uses 0-based line and character positions.
     *
     * @return LSP-compatible JSON
     */
    public String toLspEdit() {
        // LSP uses 0-based positions
        return String.format(
            "{\n"
            + "  \"range\": {\n"
            + "    \"start\": { \"line\": %d, \"character\": %d },\n"
            + "    \"end\": { \"line\": %d, \"character\": %d }\n"
            + "  },\n"
            + "  \"newText\": \"%s\"\n"
            + "}",
            startLine - 1, startColumn - 1,
            endLine - 1, endColumn - 1,
            escapeJson(newText)
        );
    }

    /**
     * Convert to human-readable format for CLI output.
     *
     * @return human-readable string
     */
    public String toHumanReadable() {
        StringBuilder sb = new StringBuilder();
        sb.append(filePath).append(":").append(startLine).append(":").append(startColumn);
        sb.append(": [").append(errorCode).append("] ").append(description).append("\n");
        sb.append("  Replace: ").append(oldText.replace("\n", "\\n")).append("\n");
        sb.append("  With:    ").append(newText.replace("\n", "\\n")).append("\n");
        return sb.toString();
    }

    /**
     * Escape a string for JSON output.
     */
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

    @Override
    public String toString() {
        return String.format("RefactoringEdit{%s:%d:%d-%d:%d '%s' -> '%s'}",
            filePath, startLine, startColumn, endLine, endColumn,
            truncate(oldText, 20), truncate(newText, 20));
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Builder for RefactoringEdit.
     */
    public static class Builder {
        private String filePath;
        private int startLine = 1;
        private int startColumn = 1;
        private int endLine = 1;
        private int endColumn = 1;
        private String oldText = "";
        private String newText = "";
        private String description = "";
        private String errorCode = "";

        public Builder filePath(String path) {
            this.filePath = path;
            return this;
        }

        public Builder startLine(int sl) {
            this.startLine = sl;
            return this;
        }

        public Builder startColumn(int sc) {
            this.startColumn = sc;
            return this;
        }

        public Builder endLine(int el) {
            this.endLine = el;
            return this;
        }

        public Builder endColumn(int ec) {
            this.endColumn = ec;
            return this;
        }

        public Builder oldText(String oldT) {
            this.oldText = oldT;
            return this;
        }

        public Builder newText(String newT) {
            this.newText = newT;
            return this;
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder errorCode(String err) {
            this.errorCode = err;
            return this;
        }

        public RefactoringEdit build() {
            return new RefactoringEdit(filePath, startLine, startColumn,
                endLine, endColumn, oldText, newText, description, errorCode);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
