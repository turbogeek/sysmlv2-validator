package com.validator.refactor;

import com.validator.semantic.ImportStatement;
import com.validator.semantic.ImportType;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Refactoring for converting wildcard imports to specific imports.
 *
 * <p>This refactoring analyzes which symbols from a wildcard import
 * are actually used and generates specific import statements for
 * only those symbols.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Before
 * import ISQ::*;
 *
 * part def Vehicle {
 *     attribute mass : MassValue;
 *     attribute length : LengthValue;
 * }
 *
 * // After
 * import ISQ::MassValue;
 * import ISQ::LengthValue;
 *
 * part def Vehicle {
 *     attribute mass : MassValue;
 *     attribute length : LengthValue;
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ImportRefactoring refactoring = new ImportRefactoring();
 * List<RefactoringEdit> edits = refactoring.refactor(parseTree, imports, symbolTable, filePath);
 * }</pre>
 */
public class ImportRefactoring {

    private final ImportUsageAnalyzer usageAnalyzer;
    private boolean preserveComments = true;
    private boolean sortImports = true;
    private int maxSpecificImports = 10; // If more than this, suggest filtered import

    /**
     * Creates a new import refactoring instance.
     */
    public ImportRefactoring() {
        this.usageAnalyzer = new ImportUsageAnalyzer();
    }

    /**
     * Creates a new import refactoring instance with custom analyzer.
     *
     * @param usageAnalyzer the analyzer to use
     */
    public ImportRefactoring(ImportUsageAnalyzer usageAnalyzer) {
        this.usageAnalyzer = usageAnalyzer;
    }

    /**
     * Sets whether to preserve comments near imports.
     *
     * @param preserveComments true to preserve comments
     */
    public void setPreserveComments(boolean preserveComments) {
        this.preserveComments = preserveComments;
    }

    /**
     * Sets whether to sort imports alphabetically.
     *
     * @param sortImports true to sort imports
     */
    public void setSortImports(boolean sortImports) {
        this.sortImports = sortImports;
    }

    /**
     * Sets the maximum number of specific imports before suggesting filtered import.
     *
     * @param maxSpecificImports maximum count
     */
    public void setMaxSpecificImports(int maxSpecificImports) {
        this.maxSpecificImports = maxSpecificImports;
    }

    /**
     * Analyzes and generates refactoring edits for wildcard imports.
     *
     * @param parseTree the ANTLR parse tree
     * @param imports list of import statements
     * @param symbolTable the symbol table
     * @param filePath the source file path
     * @return list of refactoring edits
     */
    public List<RefactoringEdit> refactor(ParseTree parseTree, List<ImportStatement> imports,
                                           SymbolTable symbolTable, String filePath) {
        // Analyze usage
        usageAnalyzer.analyze(parseTree, imports, symbolTable);

        List<RefactoringEdit> edits = new ArrayList<>();

        for (ImportStatement imp : imports) {
            if (imp.getImportType() == ImportType.WILDCARD) {
                RefactoringEdit edit = refactorWildcardImport(imp, filePath);
                if (edit != null) {
                    edits.add(edit);
                }
            }
        }

        return edits;
    }

    /**
     * Refactors a single wildcard import.
     *
     * @param imp the wildcard import
     * @param filePath the source file path
     * @return refactoring edit, or null if no refactoring needed
     */
    private RefactoringEdit refactorWildcardImport(ImportStatement imp, String filePath) {
        Set<String> usedSymbols = usageAnalyzer.getUsedSymbols(imp);

        if (usedSymbols.isEmpty()) {
            // Wildcard import is completely unused - suggest removal
            return createRemovalEdit(imp, filePath);
        }

        List<String> sortedSymbols = new ArrayList<>(usedSymbols);
        if (sortImports) {
            Collections.sort(sortedSymbols);
        }

        String basePath = getBasePath(imp.getImportPath());
        String visibility = imp.isPublic() ? "public " : "";

        String newText;
        String description;

        if (sortedSymbols.size() > maxSpecificImports) {
            // Too many symbols - suggest filtered import
            newText = buildFilteredImport(visibility, basePath, sortedSymbols);
            description = String.format("Replace wildcard with filtered import (%d symbols)",
                    sortedSymbols.size());
        } else {
            // Generate specific imports
            newText = buildSpecificImports(visibility, basePath, sortedSymbols);
            description = String.format("Replace wildcard with %d specific imports",
                    sortedSymbols.size());
        }

        String oldText = buildOriginalImportText(imp);

        int line = 1;
        int column = 1;
        int endColumn = column + oldText.length();

        if (imp.getLocation() != null) {
            line = imp.getLocation().getLine();
            column = imp.getLocation().getColumn();
            endColumn = column + oldText.length();
        }

        return RefactoringEdit.builder()
                .filePath(filePath)
                .startLine(line)
                .startColumn(column)
                .endLine(line)
                .endColumn(endColumn)
                .oldText(oldText)
                .newText(newText)
                .description(description)
                .errorCode("LINT017")
                .build();
    }

    /**
     * Creates an edit to remove an unused import.
     */
    private RefactoringEdit createRemovalEdit(ImportStatement imp, String filePath) {
        String oldText = buildOriginalImportText(imp);

        int line = 1;
        int column = 1;

        if (imp.getLocation() != null) {
            line = imp.getLocation().getLine();
            column = imp.getLocation().getColumn();
        }

        return RefactoringEdit.builder()
                .filePath(filePath)
                .startLine(line)
                .startColumn(column)
                .endLine(line + 1)  // Include the newline
                .endColumn(1)
                .oldText(oldText + "\n")
                .newText("")
                .description("Remove unused wildcard import")
                .errorCode("LINT002")  // LINT_UNUSED_IMPORT
                .build();
    }

    /**
     * Builds specific import statements.
     */
    private String buildSpecificImports(String visibility, String basePath, List<String> symbols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < symbols.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(visibility).append("import ").append(basePath)
              .append("::").append(symbols.get(i)).append(";");
        }
        return sb.toString();
    }

    /**
     * Builds a filtered import statement.
     */
    private String buildFilteredImport(String visibility, String basePath, List<String> symbols) {
        StringBuilder sb = new StringBuilder();
        sb.append(visibility).append("import ").append(basePath).append("::{");
        for (int i = 0; i < symbols.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(symbols.get(i));
        }
        sb.append("};");
        return sb.toString();
    }

    /**
     * Builds the original import text.
     */
    private String buildOriginalImportText(ImportStatement imp) {
        String visibility = imp.isPublic() ? "public " : "";
        return visibility + "import " + imp.getImportPath() + ";";
    }

    /**
     * Gets the base path from a wildcard import path.
     */
    private String getBasePath(String importPath) {
        // Remove ::* or ::*::* suffix
        String path = importPath;
        while (path.endsWith("::*")) {
            path = path.substring(0, path.length() - 3);
        }
        return path;
    }

    /**
     * Gets the usage analyzer for inspection.
     *
     * @return the usage analyzer
     */
    public ImportUsageAnalyzer getUsageAnalyzer() {
        return usageAnalyzer;
    }

    /**
     * Generates a refactoring result with statistics.
     *
     * @param parseTree the parse tree
     * @param imports the imports
     * @param symbolTable the symbol table
     * @param filePath the file path
     * @return refactoring result
     */
    public RefactoringResult analyze(ParseTree parseTree, List<ImportStatement> imports,
                                      SymbolTable symbolTable, String filePath) {
        List<RefactoringEdit> edits = refactor(parseTree, imports, symbolTable, filePath);

        int wildcardCount = 0;
        int unusedCount = 0;
        int refactoredCount = 0;

        for (ImportStatement imp : imports) {
            if (imp.getImportType() == ImportType.WILDCARD) {
                wildcardCount++;
                Set<String> used = usageAnalyzer.getUsedSymbols(imp);
                if (used.isEmpty()) {
                    unusedCount++;
                } else {
                    refactoredCount++;
                }
            }
        }

        return new RefactoringResult(
                filePath,
                edits,
                wildcardCount,
                refactoredCount,
                unusedCount
        );
    }

    /**
     * Result of import refactoring analysis.
     */
    public static class RefactoringResult {
        private final String filePath;
        private final List<RefactoringEdit> edits;
        private final int wildcardCount;
        private final int refactoredCount;
        private final int unusedCount;

        public RefactoringResult(String filePath, List<RefactoringEdit> edits,
                                  int wildcardCount, int refactoredCount, int unusedCount) {
            this.filePath = filePath;
            this.edits = edits;
            this.wildcardCount = wildcardCount;
            this.refactoredCount = refactoredCount;
            this.unusedCount = unusedCount;
        }

        public String getFilePath() {
            return filePath;
        }

        public List<RefactoringEdit> getEdits() {
            return edits;
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

        public boolean hasEdits() {
            return !edits.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("RefactoringResult{file='%s', wildcards=%d, refactored=%d, unused=%d, edits=%d}",
                    filePath, wildcardCount, refactoredCount, unusedCount, edits.size());
        }
    }
}
