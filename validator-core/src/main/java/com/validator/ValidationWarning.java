package com.validator;

/**
 * Represents a validation warning (non-fatal issue).
 */
public class ValidationWarning extends ValidationError {

    private ValidationWarning(Builder builder) {
        super(builder);
    }

    public static class Builder extends ValidationError.Builder {
        public Builder() {
            super.severity(Severity.WARNING);
        }

        @Override
        public ValidationWarning build() {
            return new ValidationWarning(this);
        }
    }
}
