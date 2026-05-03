package com.validator.library;

import com.validator.semantic.ElementType;
import com.validator.semantic.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for caching parsed SysML v2 libraries.
 */
public class LibraryCacheData {
    private String sourceName;
    private long lastModified;
    private List<CachedSymbol> symbols = new ArrayList<>();

    public LibraryCacheData() {}

    public LibraryCacheData(String sourceName, long lastModified) {
        this.sourceName = sourceName;
        this.lastModified = lastModified;
    }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public List<CachedSymbol> getSymbols() { return symbols; }
    public void setSymbols(List<CachedSymbol> symbols) { this.symbols = symbols; }

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
}
