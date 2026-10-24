package com.validator.semantic.index;

import java.util.ArrayList;
import java.util.List;

/**
 * One named or anonymous namespace member, as needed for name resolution: where it is, what it is
 * called (declared name and short name), which namespaces it imports and which types it specializes,
 * types, subsets or redefines (its supertypes, whose members it inherits).
 *
 * <p>Plain fields so the record is cached as-is with each library file.</p>
 */
public class IndexedElement {
    /** Qualified name. Names are stored without surrounding quotes. Anonymous elements get a unique segment. */
    public String qualifiedName;
    /** Qualified name of the owning namespace, "" for a root member. */
    public String parentName;
    /** Declared name, or null for an anonymous element. */
    public String name;
    /** Declared short name ({@code <short>}), or null. */
    public String shortName;
    /** Parse-rule kind, e.g. PartDefinition, PartUsage, PackageDeclaration. */
    public String kind;
    /** For an alias: the aliased qualified name, as segments. */
    public List<String> aliasTarget;
    /** Supertypes as qualified-name segments, resolved from the owning namespace. */
    public List<List<String>> supertypes = new ArrayList<>();
    /** True when a supertype is a feature chain or otherwise not a plain qualified name. */
    public boolean opaqueSupertypes;
    /** Imports owned by this namespace. */
    public List<ImportRecord> imports = new ArrayList<>();

    /** An import owned by a namespace. */
    public static class ImportRecord {
        /** Imported qualified name, as segments. */
        public List<String> target = new ArrayList<>();
        /** One of MEMBERS (::*), RECURSIVE (::** or ::*::**), ELEMENT (no wildcard). */
        public String mode;
        /** Public imports are re-exported to importers and qualified-name navigation. */
        public boolean isPublic;

        public ImportRecord() {}

        public ImportRecord(List<String> target, String mode, boolean isPublic) {
            this.target = target;
            this.mode = mode;
            this.isPublic = isPublic;
        }
    }
}
