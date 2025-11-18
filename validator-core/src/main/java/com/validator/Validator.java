package com.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Main validator interface for SysML v2 and KerML files.
 * Implementations provide different levels of validation (syntax, semantic, etc.)
 */
public interface Validator {

    /**
     * Validate a single file.
     *
     * @param file the file to validate
     * @return validation result with errors and warnings
     * @throws IOException if file cannot be read
     */
    ValidationResult validate(File file) throws IOException;

    /**
     * Validate a single file by path.
     *
     * @param filePath the path to the file
     * @return validation result with errors and warnings
     * @throws IOException if file cannot be read
     */
    ValidationResult validate(Path filePath) throws IOException;

    /**
     * Validate a string of SysML v2 or KerML code.
     *
     * @param sourceCode the source code to validate
     * @param fileName optional file name for error reporting
     * @return validation result with errors and warnings
     */
    ValidationResult validate(String sourceCode, String fileName);

    /**
     * Validate multiple files.
     *
     * @param files the files to validate
     * @return list of validation results
     * @throws IOException if files cannot be read
     */
    List<ValidationResult> validateAll(List<File> files) throws IOException;

    /**
     * Get the validator version.
     *
     * @return version string
     */
    String getVersion();

    /**
     * Get the validator name.
     *
     * @return name string
     */
    String getName();
}
