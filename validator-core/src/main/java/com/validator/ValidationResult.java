package com.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating a SysML v2 or KerML file.
 * Contains errors, warnings, and metadata about the validation process.
 */
public class ValidationResult {
    private final String filePath;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;
    private final boolean success;
    private final long validationTimeMs;

    public ValidationResult(String filePath, List<ValidationError> errors,
                          List<ValidationWarning> warnings, long validationTimeMs) {
        this.filePath = filePath;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
        this.success = errors.isEmpty();
        this.validationTimeMs = validationTimeMs;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<ValidationWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public boolean isSuccess() {
        return success;
    }

    public long getValidationTimeMs() {
        return validationTimeMs;
    }

    public int getErrorCount() {
        return errors.size();
    }

    public int getWarningCount() {
        return warnings.size();
    }

    @Override
    public String toString() {
        return String.format("ValidationResult{file='%s', errors=%d, warnings=%d, success=%b, time=%dms}",
            filePath, errors.size(), warnings.size(), success, validationTimeMs);
    }
}
