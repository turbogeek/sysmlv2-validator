package com.validator.library;

import com.validator.semantic.ElementType;
import com.validator.semantic.Visibility;
import com.validator.semantic.index.IndexedElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for caching parsed SysML v2 libraries.
 *
 * <p>An entry is valid only for the {@link #getCacheKey() cache key} it was written with (cache format,
 * grammar and symbol-extraction code). Within that key it is matched to its source by size and
 * modification time first, then by SHA-256 of the content when the time has changed.</p>
 */
public class LibraryCacheData {
    private String sourceName;
    private String sourcePath;
    private long lastModified;
    private long size = -1;
    private String sha256;
    private String cacheKey;
    private List<CachedSymbol> symbols = new ArrayList<>();
    private List<CachedDiagnostic> parseErrors = new ArrayList<>();
    private List<IndexedElement> elements = new ArrayList<>();

    public LibraryCacheData() {}

    public LibraryCacheData(String sourceName, long lastModified) {
        this.sourceName = sourceName;
        this.lastModified = lastModified;
    }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }

    public List<CachedSymbol> getSymbols() { return symbols; }
    public void setSymbols(List<CachedSymbol> symbols) { this.symbols = symbols; }

    public List<CachedDiagnostic> getParseErrors() { return parseErrors; }
    public void setParseErrors(List<CachedDiagnostic> parseErrors) { this.parseErrors = parseErrors; }

    /** Name-resolution index of the file (namespaces, members, imports, supertypes). */
    public List<IndexedElement> getElements() { return elements; }
    public void setElements(List<IndexedElement> elements) { this.elements = elements; }

    public static class CachedSymbol {
        private String name;
        private String qualifiedName;
        private ElementType type;
        private Visibility visibility;

        private List<String> specializations = new ArrayList<>();
        private List<String> redefinitions = new ArrayList<>();
        private List<String> subsettings = new ArrayList<>();

        public CachedSymbol() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getQualifiedName() { return qualifiedName; }
        public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }

        public ElementType getType() { return type; }
        public void setType(ElementType type) { this.type = type; }

        public Visibility getVisibility() { return visibility; }
        public void setVisibility(Visibility visibility) { this.visibility = visibility; }

        public List<String> getSpecializations() { return specializations; }
        public void setSpecializations(List<String> specializations) { this.specializations = specializations; }

        public List<String> getRedefinitions() { return redefinitions; }
        public void setRedefinitions(List<String> redefinitions) { this.redefinitions = redefinitions; }

        public List<String> getSubsettings() { return subsettings; }
        public void setSubsettings(List<String> subsettings) { this.subsettings = subsettings; }
    }

    /** A syntax error found while parsing a library file, kept so it is reported on every load. */
    public static class CachedDiagnostic {
        private int line;
        private int column;
        private String message;

        public CachedDiagnostic() {}

        public CachedDiagnostic(int line, int column, String message) {
            this.line = line;
            this.column = column;
            this.message = message;
        }

        public int getLine() { return line; }
        public void setLine(int line) { this.line = line; }

        public int getColumn() { return column; }
        public void setColumn(int column) { this.column = column; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
