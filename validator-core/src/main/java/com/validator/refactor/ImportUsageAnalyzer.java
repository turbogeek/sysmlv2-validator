package com.validator.refactor;

import com.validator.semantic.ImportStatement;
import com.validator.semantic.ImportType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes which symbols from each import statement are actually used in the code.
 *
 * <p>This is essential for refactoring wildcard imports to specific imports.
 * The analyzer walks the parse tree and tracks all symbol references, then
 * determines which imports brought those symbols into scope.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ImportUsageAnalyzer analyzer = new ImportUsageAnalyzer();
 * analyzer.analyze(parseTree, imports, symbolTable);
 *
 * // Get which symbols from a wildcard import are used
 * Set<String> used = analyzer.getUsedSymbols(wildcardImport);
 *
 * // Check if any import is completely unused
 * boolean unused = analyzer.isUnused(import);
 * }</pre>
 */
public class ImportUsageAnalyzer {

    private final Map<ImportStatement, Set<String>> usedSymbols = new HashMap<>();
    private final Map<ImportStatement, Integer> totalAvailable = new HashMap<>();
    private final Set<String> allReferencedNames = new HashSet<>();
    private SymbolTable symbolTable;

    /**
     * Analyzes symbol usage in the parse tree against the provided imports.
     *
     * @param tree the ANTLR parse tree
     * @param imports list of import statements
     * @param symbolTable the symbol table with resolved symbols
     */
    public void analyze(ParseTree tree, List<ImportStatement> imports, SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.usedSymbols.clear();
        this.totalAvailable.clear();
        this.allReferencedNames.clear();

        // Initialize tracking for each import
        for (ImportStatement imp : imports) {
            usedSymbols.put(imp, new HashSet<>());
            totalAvailable.put(imp, estimateAvailableSymbols(imp));
        }

        // Collect all referenced names from the parse tree
        collectReferencedNames(tree);

        // Match referenced names to imports
        matchReferencesToImports(imports);
    }

    /**
     * Collects all identifier references from the parse tree.
     */
    private void collectReferencedNames(ParseTree tree) {
        if (tree == null) {
            return;
        }

        // Skip import declarations to avoid counting the imported symbol itself as "used"
        if (tree.getClass().getSimpleName().equals("ImportDeclarationContext")) {
            return;
        }

        // If this is a terminal node, check if it's an identifier
        if (tree instanceof TerminalNode) {
            String text = tree.getText();
            // Skip keywords and operators
            if (isIdentifier(text)) {
                allReferencedNames.add(text);
            }
        }

        // Recurse into children
        for (int i = 0; i < tree.getChildCount(); i++) {
            collectReferencedNames(tree.getChild(i));
        }
    }

    /**
     * Checks if a string is a valid identifier (not a keyword).
     */
    private boolean isIdentifier(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Skip common keywords and symbols
        if (isKeyword(text) || isPunctuation(text)) {
            return false;
        }

        // Must start with letter or underscore
        char first = text.charAt(0);
        return Character.isLetter(first) || first == '_';
    }

    /**
     * Checks if text is a SysML v2 keyword.
     */
    private boolean isKeyword(String text) {
        switch (text) {
            case "package":
            case "import":
            case "public":
            case "private":
            case "protected":
            case "part":
            case "def":
            case "attribute":
            case "item":
            case "connection":
            case "port":
            case "action":
            case "state":
            case "constraint":
            case "requirement":
            case "calc":
            case "case":
            case "analysis":
            case "verification":
            case "view":
            case "viewpoint":
            case "rendering":
            case "metadata":
            case "alias":
            case "abstract":
            case "variation":
            case "variant":
            case "allocation":
            case "flow":
            case "interface":
            case "ref":
            case "in":
            case "out":
            case "inout":
            case "bind":
            case "first":
            case "then":
            case "until":
            case "after":
            case "parallel":
            case "merge":
            case "decide":
            case "join":
            case "fork":
            case "entry":
            case "exit":
            case "do":
            case "accept":
            case "send":
            case "via":
            case "assert":
            case "assume":
            case "require":
            case "satisfy":
            case "verify":
            case "expose":
            case "filter":
            case "render":
            case "comment":
            case "doc":
            case "rep":
            case "language":
            case "about":
            case "true":
            case "false":
            case "null":
            case "if":
            case "else":
            case "loop":
            case "while":
            case "for":
            case "return":
            case "redefines":
            case "subsets":
            case "specializes":
            case "ordered":
            case "nonunique":
            case "readonly":
            case "derived":
            case "end":
            case "all":
            case "from":
            case "to":
            case "succession":
            case "transition":
            case "occurrence":
            case "exhibit":
            case "perform":
            case "hastype":
            case "istype":
            case "as":
            case "default":
            case "timeslice":
            case "snapshot":
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if text is punctuation.
     */
    private boolean isPunctuation(String text) {
        if (text.length() > 3) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches collected references to import statements.
     */
    private void matchReferencesToImports(List<ImportStatement> imports) {
        for (String name : allReferencedNames) {
            for (ImportStatement imp : imports) {
                if (couldProvide(imp, name)) {
                    usedSymbols.get(imp).add(name);
                    break; // First matching import wins
                }
            }
        }
    }

    /**
     * Checks if an import could provide a given symbol name.
     */
    private boolean couldProvide(ImportStatement imp, String name) {
        String path = imp.getImportPath();

        switch (imp.getImportType()) {
            case SPECIFIC:
                // import Pkg::Element - provides "Element"
                String importedName = getLastSegment(path);
                return importedName.equals(name);

            case WILDCARD:
                // import Pkg::* - could provide any name from Pkg
                // We can't know for sure without package contents,
                // so check if the symbol table has a matching symbol
                String basePath = path.replace("::*", "");
                String qualifiedName = basePath + "::" + name;
                return symbolTable != null && symbolTable.resolve(qualifiedName) != null;

            case FILTERED:
                // import Pkg::{A, B, C} - provides A, B, or C
                return path.contains(name);

            case ALIAS:
                // import Pkg::Element as Alias - provides "Alias"
                if (path.contains(" as ")) {
                    String alias = path.substring(path.lastIndexOf(" as ") + 4).trim();
                    return alias.equals(name);
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * Gets the last segment of a qualified name.
     */
    private String getLastSegment(String qualifiedName) {
        int lastSep = qualifiedName.lastIndexOf("::");
        if (lastSep >= 0) {
            return qualifiedName.substring(lastSep + 2);
        }
        return qualifiedName;
    }

    /**
     * Estimates the number of symbols available from an import.
     * For wildcard imports, this would ideally query the package contents.
     */
    private int estimateAvailableSymbols(ImportStatement imp) {
        if (imp.getImportType() == ImportType.WILDCARD) {
            // Without actual package contents, estimate based on common libraries
            String path = imp.getImportPath();
            if (path.startsWith("ISQ::")) {
                return 847; // ISQ has many quantity types
            } else if (path.startsWith("SI::")) {
                return 125; // SI units
            } else if (path.startsWith("USCustomaryUnits::")) {
                return 89;
            } else if (path.startsWith("ScalarFunctions::")) {
                return 45;
            } else if (path.startsWith("VectorFunctions::")) {
                return 32;
            } else if (path.startsWith("Collections::")) {
                return 28;
            } else {
                return 100; // Unknown package, estimate
            }
        } else if (imp.getImportType() == ImportType.SPECIFIC) {
            return 1;
        } else if (imp.getImportType() == ImportType.FILTERED) {
            // Count elements in filter list
            String path = imp.getImportPath();
            int braceStart = path.indexOf('{');
            int braceEnd = path.indexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                String elements = path.substring(braceStart + 1, braceEnd);
                return elements.split(",").length;
            }
            return 1;
        }
        return 1;
    }

    /**
     * Returns the set of symbols from this import that were actually used.
     *
     * @param imp the import statement
     * @return set of used symbol names
     */
    public Set<String> getUsedSymbols(ImportStatement imp) {
        return usedSymbols.getOrDefault(imp, Collections.emptySet());
    }

    /**
     * Returns the estimated number of symbols available from this import.
     *
     * @param imp the import statement
     * @return estimated count of available symbols
     */
    public int getAvailableCount(ImportStatement imp) {
        return totalAvailable.getOrDefault(imp, 0);
    }

    /**
     * Checks if an import is completely unused.
     *
     * @param imp the import statement
     * @return true if no symbols from the import are used
     */
    public boolean isUnused(ImportStatement imp) {
        Set<String> used = getUsedSymbols(imp);
        return used.isEmpty();
    }

    /**
     * Checks if an import has unused symbols (for wildcard imports).
     *
     * @param imp the import statement
     * @return true if some available symbols are not used
     */
    public boolean hasUnusedSymbols(ImportStatement imp) {
        if (imp.getImportType() != ImportType.WILDCARD) {
            return false;
        }
        Set<String> used = getUsedSymbols(imp);
        int available = getAvailableCount(imp);
        return used.size() < available;
    }

    /**
     * Gets refactoring suggestions for a wildcard import.
     *
     * @param imp the wildcard import to refactor
     * @return list of specific import paths to replace the wildcard
     */
    public List<String> getSuggestedSpecificImports(ImportStatement imp) {
        if (imp.getImportType() != ImportType.WILDCARD) {
            return Collections.emptyList();
        }

        Set<String> used = getUsedSymbols(imp);
        if (used.isEmpty()) {
            return Collections.emptyList();
        }

        String basePath = imp.getImportPath().replace("::*", "");
        List<String> suggestions = new ArrayList<>();
        for (String name : used) {
            suggestions.add(basePath + "::" + name);
        }

        // Sort for consistent output
        Collections.sort(suggestions);
        return suggestions;
    }

    /**
     * Generates the refactoring edit for replacing a wildcard import.
     *
     * @param imp the wildcard import to refactor
     * @param filePath the source file path
     * @return the refactoring edit, or null if no refactoring needed
     */
    public RefactoringEdit generateRefactoring(ImportStatement imp, String filePath) {
        List<String> specific = getSuggestedSpecificImports(imp);
        if (specific.isEmpty()) {
            return null;
        }

        // Build new import text
        StringBuilder newText = new StringBuilder();
        String visibility = imp.isPublic() ? "public " : "";
        for (int i = 0; i < specific.size(); i++) {
            if (i > 0) {
                newText.append("\n");
            }
            newText.append(visibility).append("import ").append(specific.get(i)).append(";");
        }

        // Build old text
        String oldText = visibility + "import " + imp.getImportPath() + ";";

        // Get location
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
                .endLine(line)
                .endColumn(column + oldText.length())
                .oldText(oldText)
                .newText(newText.toString())
                .description("Replace wildcard import with " + specific.size() + " specific imports")
                .errorCode("LINT017")
                .build();
    }

    /**
     * Gets usage statistics for an import.
     *
     * @param imp the import statement
     * @return usage statistics
     */
    public ImportUsageStats getStats(ImportStatement imp) {
        Set<String> used = getUsedSymbols(imp);
        int available = getAvailableCount(imp);
        return new ImportUsageStats(
                imp.getImportPath(),
                used.size(),
                available,
                imp.getImportType() == ImportType.WILDCARD
        );
    }

    /**
     * Statistics about import usage.
     */
    public static class ImportUsageStats {
        private final String importPath;
        private final int usedCount;
        private final int availableCount;
        private final boolean isWildcard;

        public ImportUsageStats(String importPath, int usedCount,
                                int availableCount, boolean isWildcard) {
            this.importPath = importPath;
            this.usedCount = usedCount;
            this.availableCount = availableCount;
            this.isWildcard = isWildcard;
        }

        public String getImportPath() {
            return importPath;
        }

        public int getUsedCount() {
            return usedCount;
        }

        public int getAvailableCount() {
            return availableCount;
        }

        public boolean isWildcard() {
            return isWildcard;
        }

        public int getUnusedCount() {
            return availableCount - usedCount;
        }

        public double getUsagePercentage() {
            if (availableCount == 0) {
                return 100.0;
            }
            return (usedCount * 100.0) / availableCount;
        }

        @Override
        public String toString() {
            if (isWildcard) {
                return String.format("%s: %d/%d symbols used (%.1f%%)",
                        importPath, usedCount, availableCount, getUsagePercentage());
            } else {
                return String.format("%s: %s", importPath,
                        usedCount > 0 ? "used" : "unused");
            }
        }
    }
}
