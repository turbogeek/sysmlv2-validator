package com.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a validation error with location, message, and optional suggestions.
 */
public class ValidationError {
    private final String filePath;
    private final int line;
    private final int column;
    private final String message;
    private final String errorCode;
    private final Severity severity;
    private final List<String> suggestions;
    private final String context; // Source code context around the error

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    protected ValidationError(Builder builder) {
        this.filePath = builder.builderFilePath;
        this.line = builder.builderLine;
        this.column = builder.builderColumn;
        this.message = builder.builderMessage;
        this.errorCode = builder.builderErrorCode;
        this.severity = builder.builderSeverity;
        this.suggestions = new ArrayList<>(builder.builderSuggestions);
        this.context = builder.builderContext;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Severity getSeverity() {
        return severity;
    }

    public List<String> getSuggestions() {
        return new ArrayList<>(suggestions);
    }

    public String getContext() {
        return context;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] %s:%d:%d\n", severity, filePath, line, column));
        sb.append(String.format("  %s: %s\n", errorCode, message));
        if (!suggestions.isEmpty()) {
            sb.append("  Suggestions:\n");
            for (String suggestion : suggestions) {
                sb.append(String.format("    - %s\n", suggestion));
            }
        }
        return sb.toString();
    }

    public static class Builder {
        private String builderFilePath;
        private int builderLine;
        private int builderColumn;
        private String builderMessage;
        private String builderErrorCode;
        private Severity builderSeverity = Severity.ERROR;
        private List<String> builderSuggestions = new ArrayList<>();
        private String builderContext;

        public Builder filePath(String filePath) {
            this.builderFilePath = filePath;
            return this;
        }

        public Builder line(int line) {
            this.builderLine = line;
            return this;
        }

        public Builder column(int column) {
            this.builderColumn = column;
            return this;
        }

        public Builder message(String message) {
            this.builderMessage = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.builderErrorCode = errorCode;
            return this;
        }

        public Builder severity(Severity severity) {
            this.builderSeverity = severity;
            return this;
        }

        public Builder addSuggestion(String suggestion) {
            this.builderSuggestions.add(suggestion);
            return this;
        }

        public Builder suggestions(List<String> suggestions) {
            this.builderSuggestions = new ArrayList<>(suggestions);
            return this;
        }

        public Builder context(String context) {
            this.builderContext = context;
            return this;
        }

        public ValidationError build() {
            return new ValidationError(this);
        }
    }
}
