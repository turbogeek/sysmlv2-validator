package com.validator.library;

import com.validator.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves import statements against the library index.
 * Validates that imported packages and elements exist.
 */
public class ImportResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportResolver.class);

    private final com.validator.semantic.StandardLibraryManager standardLibraryManager;
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public ImportResolver(com.validator.semantic.StandardLibraryManager standardLibraryManager) {
        this.standardLibraryManager = standardLibraryManager;
    }

    /**
     * Resolve an import statement.
     *
     * @param importName The imported name (e.g., "ScalarValues::Real" or "ISQ::*")
     * @param filePath Source file path for error reporting
     * @param line Line number for error reporting
     * @return true if resolved successfully, false if unresolved
     */
    public boolean resolveImport(String importName, String filePath, int line) {
        // Remove wildcard if present
        String cleanName = importName.replace("::*", "");

        // Check if it's a qualified name or just a package
        if (cleanName.contains("::")) {
            return resolveQualifiedImport(cleanName, importName, filePath, line);
        } else {
            return resolvePackageImport(cleanName, importName, filePath, line);
        }
    }

    private boolean resolveQualifiedImport(String cleanName, String originalImport,
            String filePath, int line) {
        if (standardLibraryManager.resolveSymbol(cleanName) != null) {
            LOGGER.debug("Resolved import: {}", originalImport);
            return true;
        }

        // Check if library is available (if there are no symbols other than builtins, or if it's empty)
        if (standardLibraryManager.getAllSymbols().isEmpty()) {
            addWarning(filePath, line,
                String.format("Cannot resolve import '%s' - "
                + "no standard library configured", originalImport));
            return false;
        }

        // Library is available but element not found
        addError(filePath, line,
            String.format("Cannot resolve import '%s' - "
            + "element not found in standard library", originalImport));
        return false;
    }

    private boolean resolvePackageImport(String packageName, String originalImport,
            String filePath, int line) {
        if (standardLibraryManager.resolveSymbol(packageName) != null) {
            LOGGER.debug("Resolved package import: {}", packageName);
            return true;
        }

        // Check if library is available
        if (standardLibraryManager.getAllSymbols().isEmpty()) {
            addWarning(filePath, line,
                String.format("Cannot resolve import '%s' - "
                + "no standard library configured", originalImport));
            return false;
        }

        // Library is available but package not found
        addError(filePath, line,
            String.format("Cannot resolve import '%s' - "
            + "package not found in standard library", originalImport));
        return false;
    }

    private void addError(String filePath, int line, String message) {
        ValidationError error = new ValidationError.Builder()
            .filePath(filePath)
            .line(line)
            .column(0)
            .message(message)
            .errorCode("IMPORT_RESOLUTION_ERROR")
            .severity(ValidationError.Severity.ERROR)
            .build();
        errors.add(error);
    }

    private void addWarning(String filePath, int line, String message) {
        String warning = String.format("Line %d: %s", line, message);
        warnings.add(warning);
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public void reset() {
        errors.clear();
        warnings.clear();
    }
}
