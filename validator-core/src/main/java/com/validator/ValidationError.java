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

    private ValidationError(Builder builder) {
        this.filePath = builder.filePath;
        this.line = builder.line;
        this.column = builder.column;
        this.message = builder.message;
        this.errorCode = builder.errorCode;
        this.severity = builder.severity;
        this.suggestions = new ArrayList<>(builder.suggestions);
        this.context = builder.context;
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
        private String filePath;
        private int line;
        private int column;
        private String message;
        private String errorCode;
        private Severity severity = Severity.ERROR;
        private List<String> suggestions = new ArrayList<>();
        private String context;

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder column(int column) {
            this.column = column;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder addSuggestion(String suggestion) {
            this.suggestions.add(suggestion);
            return this;
        }

        public Builder suggestions(List<String> suggestions) {
            this.suggestions = new ArrayList<>(suggestions);
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public ValidationError build() {
            return new ValidationError(this);
        }
    }
}
