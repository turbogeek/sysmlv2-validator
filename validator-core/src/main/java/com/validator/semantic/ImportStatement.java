package com.validator.semantic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an import statement in a SysML v2 model.
 * Supports various import types: wildcard, specific, aliased, public/private.
 */
public class ImportStatement {
    private final String importPath;
    private final ImportType importType;
    private final boolean isPublic;
    private final String alias; // For aliased imports
    private final Map<String, Symbol> importedSymbols; // Resolved symbols

    public ImportStatement(String importPath, ImportType importType, boolean isPublic) {
        this(importPath, importType, isPublic, null);
    }

    public ImportStatement(String importPath, ImportType importType, boolean isPublic, String alias) {
        this.importPath = Objects.requireNonNull(importPath, "Import path cannot be null");
        this.importType = Objects.requireNonNull(importType, "Import type cannot be null");
        this.isPublic = isPublic;
        this.alias = alias;
        this.importedSymbols = new LinkedHashMap<>();
    }

    public String getImportPath() {
        return importPath;
    }

    public ImportType getImportType() {
        return importType;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getAlias() {
        return alias;
    }

    /**
     * Add a resolved symbol from this import.
     */
    public void addImportedSymbol(Symbol symbol) {
        String key = (alias != null) ? alias : symbol.getName();
        importedSymbols.put(key, symbol);
    }

    /**
     * Resolve a name through this import.
     * Returns null if the name is not imported by this statement.
     */
    public Symbol resolve(String name) {
        return importedSymbols.get(name);
    }

    public Map<String, Symbol> getImportedSymbols() {
        return Collections.unmodifiableMap(importedSymbols);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isPublic) {
            sb.append("public ");
        } else {
            sb.append("private ");
        }
        sb.append("import ");
        sb.append(importPath);
        if (importType == ImportType.WILDCARD) {
            sb.append("::*");
        }
        if (alias != null) {
            sb.append(" as ").append(alias);
        }
        return sb.toString();
    }
}
