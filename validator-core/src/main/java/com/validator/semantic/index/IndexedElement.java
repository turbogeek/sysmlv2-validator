package com.validator.semantic.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One named or anonymous namespace member, as needed for name resolution: where it is, what it is
 * called (declared name and short name), which namespaces it imports and which types it specializes,
 * types, subsets or redefines (its supertypes, whose members it inherits).
 *
 * <p>For the inheritance check ({@link InheritanceChecker}) it also records which supertypes are
 * redefinitions, whether the element is a parameter or private, where it is declared, and short source
 * text of its typing, multiplicity and value.</p>
 *
 * <p>Plain fields so the record is cached as-is with each library file. Methods are not named like
 * bean getters so they are not serialized.</p>
 */
public class IndexedElement {
    /** Rules other than usages that declare features (KerML features, parameters, requirement members). */
    private static final Set<String> FEATURE_KINDS = Set.of(
        "FeatureDefinition", "ConnectorDefinition", "BindingConnectorDefinition", "SimpleFeature",
        "AnonymousRedefines", "ImplicitFeatureDeclaration", "SimpleParameter", "ConstraintParameter",
        "EnumMember", "InParameter", "OutParameter", "InoutParameter", "ReturnFeature", "SubjectDeclaration",
        "ObjectiveRequirement", "AssumeConstraint", "RequireConstraint", "FramedConcern", "VerifyRequirement",
        "EntryAction", "DoAction", "ExitAction");

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
    /** Line of the declaration (1-based), 0 if unknown. */
    public int line;
    /** Column of the declaration (0-based). */
    public int column;
    /** For an alias: the aliased qualified name, as segments. */
    public List<String> aliasTarget;
    /**
     * Supertypes as qualified-name segments, resolved from the owning namespace: typings (':'),
     * specializations and subsettings (':>', 'subsets', '::>'), redefinitions (':>>') and conjugations.
     * These are the element's general types.
     */
    public List<List<String>> supertypes = new ArrayList<>();
    /** The redefined features among the supertypes, as qualified-name segments. */
    public List<List<String>> redefines = new ArrayList<>();
    /** True when a supertype is a feature chain or otherwise not a plain qualified name. */
    public boolean opaqueSupertypes;
    /**
     * True for a parameter or connector end (directed features, 'return', 'subject', 'objective',
     * actors, stakeholders, 'end' features). These redefine the general's parameters by position, which
     * the index does not model.
     */
    public boolean parameter;
    /** True for a member declared 'private': specializations do not inherit it. */
    public boolean isPrivate;
    /** The typing as written, e.g. "Real" or "A, B", or null. */
    public String typeText;
    /** The multiplicity as written, e.g. "[0..*]", or null. */
    public String multiplicityText;
    /** The value as written, e.g. "= 4", with whitespace collapsed and long values shortened, or null. */
    public String valueText;
    /** Imports owned by this namespace. */
    public List<ImportRecord> imports = new ArrayList<>();

    /**
     * The name the element is known by: its declared name or short name, else (for an unnamed
     * redefinition such as {@code :>> mass = 5;}) the name of the feature it redefines. Null if none.
     */
    public String effectiveName() {
        if (name != null) {
            return name;
        }
        if (shortName != null) {
            return shortName;
        }
        if (!redefines.isEmpty() && !redefines.get(0).isEmpty()) {
            List<String> first = redefines.get(0);
            return first.get(first.size() - 1);
        }
        return null;
    }

    /**
     * True when the element is a feature: a usage or another rule that declares one (a KerML feature, a
     * parameter, a requirement member), not a definition, package or metadata usage.
     */
    public boolean declaresFeature() {
        if (kind == null) {
            return false;
        }
        return kind.endsWith("Usage") && !kind.equals("MetadataUsage") || FEATURE_KINDS.contains(kind);
    }

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
