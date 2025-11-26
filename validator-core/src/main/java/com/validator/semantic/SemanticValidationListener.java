package com.validator.semantic;

import com.validator.ValidationError;
import com.validator.library.ImportResolver;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserBaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ANTLR listener for semantic validation of SysML v2 models.
 * Currently focuses on:
 * - Resolving import statements against standard library
 *
 * Future enhancements will add full name resolution and type checking.
 */
public class SemanticValidationListener extends SysMLv2ParserBaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticValidationListener.class);

    // Pattern to extract import names from text
    private static final Pattern IMPORT_PATTERN =
        Pattern.compile("import\\s+(?:public\\s+|private\\s+)?(\\w+(?:::\\w+|::\\*)+)");

    private final String filePath;
    private final ImportResolver importResolver;
    private final List<ValidationError> errors;
    private final List<String> warnings;

    public SemanticValidationListener(String filePath,
            ImportResolver importResolver,
            List<ValidationError> errors,
            List<String> warnings) {
        this.filePath = filePath;
        this.importResolver = importResolver;
        this.errors = errors;
        this.warnings = warnings;
    }

    @Override
    public void enterNamespaceDeclaration(SysMLv2Parser.NamespaceDeclarationContext ctx) {
        // Check for import statements in namespace declarations
        String text = ctx.getText();
        if (text.contains("import")) {
            processImports(text, ctx.getStart().getLine());
        }
    }

    /**
     * Extract and resolve all import statements from the text.
     */
    private void processImports(String text, int line) {
        Matcher matcher = IMPORT_PATTERN.matcher(text);
        while (matcher.find()) {
            String importName = matcher.group(1);
            LOGGER.debug("Processing import: {} at line {}", importName, line);

            // Resolve the import
            boolean resolved = importResolver.resolveImport(importName, filePath, line);

            if (resolved) {
                LOGGER.trace("Successfully resolved import: {}", importName);
            } else {
                LOGGER.debug("Failed to resolve import: {}", importName);
            }
        }
    }
}
