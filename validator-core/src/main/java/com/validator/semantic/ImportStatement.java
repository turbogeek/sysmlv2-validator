package com.validator.semantic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an import statement in SysML v2.
 * Examples:
 * - import ISQ::*        (wildcard)
 * - import SI::kg        (specific)
 * - import Pkg::{A, B}   (filtered)
 * - import Pkg::El as A  (alias)
 */
public class ImportStatement {
    private final String importPath;
    private final ImportType importType;
    private final boolean isPublic;
    private final String alias;
    private final Map<String, Symbol> importedSymbols = new LinkedHashMap<>();

    /**
     * Create an import statement.
     *
     * @param importPath the import path (e.g., "ISQ::*" or "SI::kg")
     * @param importType the type of import
     * @param isPublic whether the import is public
     */
    public ImportStatement(String importPath, ImportType importType, boolean isPublic) {
        this(importPath, importType, isPublic, null);
    }

    /**
     * Create an import statement with optional alias.
     *
     * @param importPath the import path
     * @param importType the type of import
     * @param isPublic whether the import is public
     * @param alias the alias name (may be null)
     */
    public ImportStatement(String importPath, ImportType importType, boolean isPublic, String alias) {
        Objects.requireNonNull(importPath, "Import path cannot be null");
        if (importPath.isEmpty()) {
            throw new IllegalArgumentException("Import path cannot be empty");
        }
        Objects.requireNonNull(importType, "Import type cannot be null");

        this.importPath = importPath;
        this.importType = importType;
        this.isPublic = isPublic;
        this.alias = alias;
    }

    /**
     * Get the import path (e.g., "ISQ::*" or "SI::kg").
     */
    public String getImportPath() {
        return importPath;
    }

    /**
     * Get the type of import.
     */
    public ImportType getImportType() {
        return importType;
    }

    /**
     * Check if this is a public import.
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * Get the alias name (if any).
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Get the package name from the import path.
     * For "ISQ::*" returns "ISQ".
     * For "SI::kg" returns "SI".
     */
    public String getPackageName() {
        String path = importPath.replace("::*", "");
        int lastSep = path.lastIndexOf("::");
        if (lastSep > 0) {
            return path.substring(0, lastSep);
        }
        return path;
    }

    /**
     * Get the element name from the import path.
     * For "SI::kg" returns "kg".
     * For "ISQ::*" returns "*".
     */
    public String getElementName() {
        if (importPath.endsWith("::*")) {
            return "*";
        }
        int lastSep = importPath.lastIndexOf("::");
        if (lastSep >= 0) {
            return importPath.substring(lastSep + 2);
        }
        return importPath;
    }

    /**
     * Check if this is a wildcard import.
     */
    public boolean isWildcard() {
        return importType == ImportType.WILDCARD || importPath.endsWith("::*");
    }

    /**
     * Add an imported symbol (resolved during semantic analysis).
     *
     * @param symbol the symbol to add
     */
    public void addImportedSymbol(Symbol symbol) {
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        importedSymbols.put(symbol.getName(), symbol);
    }

    /**
     * Get all imported symbols as a map from name to symbol.
     *
     * @return unmodifiable map of imported symbols
     */
    public Map<String, Symbol> getImportedSymbols() {
        return Collections.unmodifiableMap(importedSymbols);
    }

    /**
     * Resolve an imported symbol by name.
     *
     * @param name the simple name to look up
     * @return the symbol if found, null otherwise
     */
    public Symbol resolve(String name) {
        if (name == null) {
            return null;
        }

        // If this import has an alias, check if the name matches the alias
        if (alias != null && alias.equals(name)) {
            // Return the first/only symbol for aliased imports
            return importedSymbols.isEmpty() ? null : importedSymbols.values().iterator().next();
        }

        // Otherwise look for a symbol with matching name
        return importedSymbols.get(name);
    }

    /**
     * Resolve an imported symbol by name (alias for resolve).
     *
     * @param name the simple name to look up
     * @return the symbol if found, null otherwise
     */
    public Symbol resolveImportedSymbol(String name) {
        return resolve(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImportStatement that = (ImportStatement) o;
        return importPath.equals(that.importPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(importPath);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isPublic) {
            sb.append("private ");
        }
        sb.append("import ").append(importPath);
        if (alias != null) {
            sb.append(" as ").append(alias);
        }
        return sb.toString();
    }
}
