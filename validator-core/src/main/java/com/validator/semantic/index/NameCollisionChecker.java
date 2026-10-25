package com.validator.semantic.index;

import com.validator.ErrorCodes;
import com.validator.ValidationError;
import com.validator.ValidationWarning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reports names that make a new element where the modeler most likely meant an existing one: declaring a
 * feature is so easy that {@code attribute mass = 1500.0;} in a usage of {@code Vehicle} creates a second
 * {@code mass} instead of giving the inherited one a value ({@code attribute :>> mass = 1500.0;}). See
 * docs/NAME_COLLISION_ANALYSIS.md for the language rules and the reference implementation's behavior.
 *
 * <ul>
 *   <li>{@link ErrorCodes#DUPLICATE_MEMBER_NAME}: two members with the same name in one namespace.</li>
 *   <li>{@link ErrorCodes#INHERITED_NAME_REDECLARED}: an owned feature has the name of a feature its
 *       owner inherits and does not redefine it (KerML 7.3.2.1, validateNamespaceDistinguishibility).</li>
 *   <li>{@link ErrorCodes#RENAMED_INHERITED_NAME_REUSED}: an owned feature has the name of an ancestor's
 *       feature that the owner inherits under another name, so it is an unrelated new feature.</li>
 *   <li>{@link ErrorCodes#NAME_SIMILAR_TO_INHERITED}: a new feature, or a redefinition that names no
 *       inherited feature, is one edit away from the name of an inherited feature (a likely typo).</li>
 *   <li>{@link ErrorCodes#REDEFINITION_NOT_INHERITED}: {@code :>> name} names no inherited feature and
 *       resolves through the enclosing namespaces or an import instead (e.g. to {@code ISQ::mass}).</li>
 *   <li>{@link ErrorCodes#FEATURE_REFERS_TO_ITSELF}: {@code attribute mass :> mass;} names the feature
 *       being declared, because a namespace's own members are found first.</li>
 *   <li>{@link ErrorCodes#PARAMETER_POSITION_MISMATCH} and {@link ErrorCodes#PARAMETER_REDEFINES_TWO}:
 *       directed parameters redefine the general's parameters by position, not by name.</li>
 *   <li>{@link ErrorCodes#SEMANTIC_BOUND_VALUE_OVERRIDDEN} (error): a redefinition gives a value to a
 *       feature whose value is bound ({@code =}) or initial ({@code :=}), not a default (KerML
 *       validateFeatureValueOverriding).</li>
 * </ul>
 *
 * <p>Parameters, subjects, objectives, actors, stakeholders and connector ends redefine by position, and
 * features in metadata bodies redefine by name, so they are not reported as re-declarations. Only the
 * elements of the validated files are checked. It shares the memoized inheritance queries of an
 * {@link InheritanceChecker}, so one instance should serve all files of one index.</p>
 */
public final class NameCollisionChecker {

    /** Owners whose directed features are parameters, which redefine the general's parameters by position. */
    private static final Set<String> BEHAVIOR_KINDS = Set.of(
        "ActionDefinition", "ActionUsage", "AnalysisDefinition", "AnalysisUsage", "BehaviorDefinition",
        "CalcDefinition", "CalcUsage", "CaseDefinition", "CaseUsage", "ConcernDefinition", "ConcernUsage",
        "ConstraintDefinition", "ConstraintUsage", "FunctionDefinition", "InteractionDefinition",
        "PredicateDefinition", "RequirementDefinition", "RequirementUsage", "StateDefinition", "StateUsage",
        "StepUsage", "UseCaseDefinition", "UseCaseUsage", "VerificationDefinition", "VerificationUsage",
        "ViewpointDefinition", "ViewpointUsage", "PerformUsage", "ExhibitUsage", "IncludeUsage");
    /**
     * Rules that refer to an existing element rather than declaring one ({@code satisfy r by p},
     * {@code metadata M about x}, {@code perform a}, {@code require c}, {@code then join j}), or that the grammar
     * can misread as naming one ({@code transition initial then off} reads 'initial' as the transition's name).
     */
    private static final Set<String> REFERENCE_KINDS = Set.of(
        "MetadataUsage", "SatisfyRequirement", "DependencyDeclaration", "TransitionUsage", "AcceptUsage",
        "SendUsage", "PerformUsage", "ExhibitUsage", "IncludeUsage", "AllocateUsage", "VerifyRequirement",
        "FramedConcern", "RequireConstraint", "AssumeConstraint", "ThenStatement");
    /** Shortest name compared for typos; shorter names (x, id, in1) differ by one edit too often. */
    private static final int MIN_SIMILAR_LENGTH = 4;

    private final ModelIndex index;
    private final InheritanceChecker inheritance;
    private final Map<String, List<String>> inheritedMemo = new HashMap<>();
    private final Map<String, List<String>> parametersMemo = new HashMap<>();
    private final Map<String, Set<String>> redefinedByOwnedMemo = new HashMap<>();
    private final Map<String, Set<String>> closureMemo = new HashMap<>();
    private final Set<String> closureInProgress = new HashSet<>();
    private final Set<String> parametersInProgress = new HashSet<>();

    /**
     * @param index the index of the libraries and every validated file
     */
    public NameCollisionChecker(ModelIndex index) {
        this(index, new InheritanceChecker(index));
    }

    /**
     * @param index the index of the libraries and every validated file
     * @param inheritance the inheritance checker of the same index, whose memoized queries are shared
     */
    public NameCollisionChecker(ModelIndex index, InheritanceChecker inheritance) {
        this.index = index;
        this.inheritance = inheritance;
    }

    /**
     * Check one file in a fresh checker.
     *
     * @return the warnings and errors, in declaration order
     */
    public static List<ValidationError> check(List<IndexedElement> elements, ModelIndex index, String filePath) {
        return new NameCollisionChecker(index).check(elements, filePath);
    }

    /**
     * Check the elements of one file.
     *
     * @param elements the file's indexed elements
     * @param filePath the file path for the diagnostics
     * @return the warnings ({@link ValidationWarning}) and errors, in declaration order
     */
    public List<ValidationError> check(List<IndexedElement> elements, String filePath) {
        List<ValidationError> result = new ArrayList<>();
        for (IndexedElement e : elements) {
            if (e.qualifiedName == null) {
                continue;
            }
            duplicates(e, filePath, result);
            if (e.declaresFeature()) {
                checkFeature(e, filePath, result);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------

    private void duplicates(IndexedElement e, String filePath, List<ValidationError> result) {
        String name = e.name != null ? e.name : e.shortName;
        if (e.redeclarations.isEmpty() || name == null) {
            return;
        }
        if (e.parameter || e.direction != null) {
            return;
        }
        List<IndexedElement.Declaration> all = new ArrayList<>();
        all.add(new IndexedElement.Declaration(e.line, e.column, e.kind));
        all.addAll(e.redeclarations);
        all.removeIf(d -> REFERENCE_KINDS.contains(d.kind));
        if (all.size() < 2) {
            return;
        }
        boolean mixed = false;
        for (IndexedElement.Declaration d : all) {
            mixed |= isDefinition(d.kind) != isDefinition(e.kind);
        }
        String owner = e.parentName == null || e.parentName.isEmpty() ? "the root namespace"
            : "'" + inheritance.display(e.parentName) + "'";
        for (IndexedElement.Declaration d : all) {
            List<String> others = new ArrayList<>();
            for (IndexedElement.Declaration o : all) {
                if (o != d) {
                    others.add(String.valueOf(o.line));
                }
            }
            String message = String.format("'%s' is declared %d times in %s (also at line %s). A namespace has one"
                    + " member per name, so this is a second, different element, and a reference to '%s' may"
                    + " resolve to either.%s Rename or remove one of them.",
                name, all.size(), owner, String.join(", ", others), name,
                mixed ? " The specification allows a definition and a usage to share a name, but the reference"
                    + " implementation and readers cannot tell them apart." : "");
            result.add(warning(filePath, d.line, d.column, ErrorCodes.DUPLICATE_MEMBER_NAME, message, null));
        }
    }

    private void checkFeature(IndexedElement e, String filePath, List<ValidationError> result) {
        String owner = e.parentName == null ? "" : e.parentName;
        IndexedElement ownerRecord = owner.isEmpty() ? null : inheritance.first(owner);
        String ownerKind = ownerRecord == null ? null : ownerRecord.kind;
        boolean byName = "MetadataUsage".equals(ownerKind) || "EnumMember".equals(e.kind);
        if (e.reference || REFERENCE_KINDS.contains(e.kind)) {
            return;
        }

        boundValueOverridden(e, owner, filePath, result);
        if (e.direction != null && ownerKind != null && BEHAVIOR_KINDS.contains(ownerKind)) {
            parameterPosition(e, owner, filePath, result);
        }
        if (byName) {
            return;
        }
        boolean selfReference = refersToItself(e, owner, filePath, result);
        redefinitionTargets(e, owner, filePath, result);
        if (!e.parameter && !selfReference) {
            redeclaration(e, owner, filePath, result);
        }
    }

    /** INHERITED_NAME_REDECLARED, RENAMED_INHERITED_NAME_REUSED and NAME_SIMILAR_TO_INHERITED for a new feature. */
    private void redeclaration(IndexedElement e, String owner, String filePath, List<ValidationError> result) {
        String name = e.name != null ? e.name : e.shortName;
        if (name == null || owner.isEmpty()) {
            return;
        }
        List<String> inherited = inheritedFeatures(owner);
        if (inherited.isEmpty()) {
            return;
        }
        Set<String> redefined = redefinitionClosure(e.qualifiedName);
        Set<String> replaced = redefinedByOwned(owner);
        List<String> same = new ArrayList<>();
        for (String f : inherited) {
            if (!redefined.contains(f) && name.equals(inheritance.effectiveName(f))
                    && !intersects(redefinitionClosure(f), replaced)) {
                same.add(f);
            }
        }
        String ownerName = inheritance.display(owner);
        if (!same.isEmpty()) {
            String target = same.get(0);
            IndexedElement t = inheritance.first(target);
            String valueNote = "";
            if (e.valueText != null) {
                valueNote = String.format(" The value '%s' goes to the new feature; '%s' keeps %s.", e.valueText,
                    inheritance.display(target),
                    t != null && t.valueText != null ? "'" + t.valueText + "'" : "no value");
            }
            String subsets = e.supertypes.stream().anyMatch(s -> s.size() > 1
                    && s.get(s.size() - 1).equals(name) && !e.redefines.contains(s))
                ? " It subsets the inherited feature but does not replace it; use ':>>' (redefines), not ':>'." : "";
            String fix = redefinitionText(e, name);
            String message = String.format("'%s' declares a new feature in '%s' with the same name as the inherited"
                    + " '%s' instead of redefining it, so '%s' has two features named '%s'.%s%s Redefine the"
                    + " inherited feature ('%s'), or give the new feature another name.",
                name, ownerName, inheritance.display(target), ownerName, name, valueNote, subsets, fix);
            result.add(warning(filePath, e.line, e.column, ErrorCodes.INHERITED_NAME_REDECLARED, message, fix));
            return;
        }
        if (!e.redefines.isEmpty()) {
            return;
        }
        // A feature of an ancestor with this name, which the owner inherits under another name
        for (String f : inherited) {
            for (String r : redefinitionClosure(f)) {
                if (!r.equals(f) && name.equals(inheritance.effectiveName(r))
                        && !name.equals(inheritance.effectiveName(f))) {
                    String renamed = InheritanceChecker.sysmlName(inheritance.effectiveName(f));
                    String message = String.format("'%s' in '%s' has the name of '%s', which '%s' inherits renamed as"
                            + " '%s' (through '%s'). This '%s' is a new feature, unrelated to it. To refer to the"
                            + " inherited feature use ':>> %s'; if a new feature is intended, give it another name.",
                        name, ownerName, inheritance.display(r), ownerName, inheritance.effectiveName(f),
                        inheritance.display(inheritance.parentOf(f)), name, renamed);
                    result.add(warning(filePath, e.line, e.column, ErrorCodes.RENAMED_INHERITED_NAME_REUSED, message,
                        null));
                    return;
                }
            }
        }
        // A typo of an inherited name
        String similar = similarInherited(owner, name, inherited);
        if (similar != null && !namesInHeader(e, inheritance.effectiveName(similar))) {
            String other = inheritance.effectiveName(similar);
            String fix = redefinitionText(e, other);
            String message = String.format("'%s' is a new feature of '%s', but '%s' inherits '%s', whose name differs"
                    + " by one letter. If '%s' is meant, redefine it ('%s'); otherwise rename '%s' so the two are"
                    + " not confused.",
                name, ownerName, ownerName, inheritance.display(similar), other, fix, name);
            result.add(warning(filePath, e.line, e.column, ErrorCodes.NAME_SIMILAR_TO_INHERITED, message, fix));
        }
    }

    /**
     * FEATURE_REFERS_TO_ITSELF: a typing or subsetting by the feature's own simple name. The owner's own
     * members are found first, so the name denotes the feature being declared.
     */
    private boolean refersToItself(IndexedElement e, String owner, String filePath, List<ValidationError> result) {
        String name = e.name != null ? e.name : e.shortName;
        if (name == null) {
            return false;
        }
        for (List<String> segs : e.supertypes) {
            if (segs.size() == 1 && segs.get(0).equals(name) && !e.redefines.contains(segs)) {
                String ownerName = owner.isEmpty() ? "its namespace" : "'" + inheritance.display(owner) + "'";
                Set<String> elsewhere = outsideTargets(e.qualifiedName, inheritance.parentOf(owner), segs);
                String example = elsewhere.isEmpty() ? ""
                    : " (for example ':> " + inheritance.display(elsewhere.iterator().next()) + "')";
                String message = String.format("'%s' refers to itself: the name '%s' after ':', ':>' or 'subsets'"
                        + " is looked up in %s first, where it finds the feature being declared. Qualify the intended"
                        + " feature%s, or use ':>> %s' to redefine an inherited '%s'.",
                    name, name, ownerName, example, name, name);
                result.add(warning(filePath, e.line, e.column, ErrorCodes.FEATURE_REFERS_TO_ITSELF, message, null));
                return true;
            }
        }
        return false;
    }

    /** REDEFINITION_NOT_INHERITED and NAME_SIMILAR_TO_INHERITED for {@code :>> name} with a simple name. */
    private void redefinitionTargets(IndexedElement e, String owner, String filePath, List<ValidationError> result) {
        if (owner.isEmpty() || e.redefines.size() != 1 || inheritance.generals(owner).isEmpty() || e.parameter
                || !fullyKnown(owner)) {
            return;
        }
        for (List<String> segs : e.redefines) {
            if (segs.size() != 1) {
                continue;
            }
            String name = segs.get(0);
            if (!inheritance.resolveReference(e.qualifiedName, owner, segs, true).isEmpty()) {
                continue;
            }
            String ownerName = inheritance.display(owner);
            String similar = similarInherited(owner, name, inheritedFeatures(owner));
            Set<String> outside = outsideTargets(e.qualifiedName, owner, segs);
            outside.removeIf(index::isLocal);
            if (!outside.isEmpty()) {
                String found = inheritance.display(outside.iterator().next());
                String hint = similar == null ? ""
                    : " Did you mean ':>> " + InheritanceChecker.sysmlName(inheritance.effectiveName(similar)) + "'?";
                String message = String.format("':>> %s' in '%s' names no feature that '%s' inherits; '%s' resolves"
                        + " through the enclosing namespaces or an import to the library feature '%s', so this"
                        + " redefines '%s'.%s Redefine"
                        + " an inherited feature, or write the qualified name if redefining '%s' is intended.",
                    name, ownerName, ownerName, name, found, found, hint, found);
                result.add(warning(filePath, e.line, e.column, ErrorCodes.REDEFINITION_NOT_INHERITED, message, null));
            } else if (similar != null) {
                String other = InheritanceChecker.sysmlName(inheritance.effectiveName(similar));
                String message = String.format("':>> %s' in '%s' names no feature that '%s' inherits. Did you mean"
                        + " ':>> %s' ('%s')?",
                    name, ownerName, ownerName, other, inheritance.display(similar));
                result.add(warning(filePath, e.line, e.column, ErrorCodes.NAME_SIMILAR_TO_INHERITED, message,
                    ":>> " + other));
            }
        }
    }

    /** What a name resolves to from a namespace, other than the element itself and the namespace's members. */
    private Set<String> outsideTargets(String self, String owner, List<String> segs) {
        ModelIndex.Resolution r = index.resolve(segs, owner);
        Set<String> result = new LinkedHashSet<>();
        if (r.getStatus() == ModelIndex.Status.FOUND) {
            for (String t : r.getTargets()) {
                if (!t.equals(self) && !owner.equals(inheritance.parentOf(t))) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    /** PARAMETER_POSITION_MISMATCH and PARAMETER_REDEFINES_TWO. */
    private void parameterPosition(IndexedElement e, String owner, String filePath, List<ValidationError> result) {
        String name = e.name != null ? e.name : e.shortName;
        int position = e.parameterIndex;
        if (position < 0) {
            return;
        }
        String ownerName = inheritance.display(owner);
        for (String general : inheritance.generals(owner)) {
            List<String> theirs = parameters(general);
            if (position >= theirs.size()) {
                continue;
            }
            String atPosition = theirs.get(position);
            if (atPosition == null) {
                continue;
            }
            String generalName = inheritance.display(general);
            if (!e.redefines.isEmpty()) {
                Set<String> explicit = new LinkedHashSet<>();
                for (List<String> segs : e.redefines) {
                    for (String t : redefinitionTargets(e.qualifiedName, owner, segs)) {
                        explicit.addAll(redefinitionClosure(t));
                    }
                }
                if (!explicit.isEmpty() && !explicit.contains(atPosition)) {
                    String label = name != null ? name : inheritance.effectiveName(e.qualifiedName);
                    String message = String.format("Parameter '%s' redefines '%s' explicitly, but as parameter %d of"
                            + " '%s' it also redefines '%s', the parameter at that position of '%s' (parameters"
                            + " redefine by position; KerML checkFeatureParameterRedefinition), which makes the two the"
                            + " same feature. Declare the parameters in the order of '%s'.",
                        label, inheritance.display(explicit.iterator().next()), position + 1, ownerName,
                        inheritance.display(atPosition), generalName, generalName);
                    result.add(warning(filePath, e.line, e.column, ErrorCodes.PARAMETER_REDEFINES_TWO, message, null));
                }
                continue;
            }
            if (name == null || name.equals(inheritance.effectiveName(atPosition))) {
                continue;
            }
            for (int j = 0; j < theirs.size(); j++) {
                if (j != position && theirs.get(j) != null && name.equals(inheritance.effectiveName(theirs.get(j)))) {
                    String message = String.format("Parameter '%s' is parameter %d of '%s', so it redefines '%s'"
                            + " (parameter %d of '%s') by position, not '%s' (parameter %d), although the names match."
                            + " Parameters redefine by position, not by name: declare them in the order of '%s'.",
                        name, position + 1, ownerName, inheritance.display(atPosition), position + 1, generalName,
                        inheritance.display(theirs.get(j)), j + 1, generalName);
                    result.add(warning(filePath, e.line, e.column, ErrorCodes.PARAMETER_POSITION_MISMATCH, message,
                        null));
                    break;
                }
            }
        }
    }

    /** SEMANTIC_BOUND_VALUE_OVERRIDDEN: a redefinition with a value of a feature whose value is not a default. */
    private void boundValueOverridden(IndexedElement e, String owner, String filePath, List<ValidationError> result) {
        if (e.valueText == null || e.redefines.isEmpty() || isDefault(e.valueText)) {
            return;
        }
        for (String r : redefinitionClosure(e.qualifiedName)) {
            IndexedElement t = r.equals(e.qualifiedName) ? null : inheritance.first(r);
            if (t == null || t.valueText == null || isDefault(t.valueText)) {
                continue;
            }
            String name = e.effectiveName() != null ? e.effectiveName() : "?";
            String kind = t.valueText.startsWith(":=") ? "initial" : "bound";
            String message = String.format("'%s' gives a value to '%s', whose value is %s ('%s'), not a default, and"
                    + " a %s value cannot be overridden (KerML validateFeatureValueOverriding). Make it a default in"
                    + " '%s' ('default %s'), or remove one of the values.",
                name, inheritance.display(r), kind, t.valueText, kind, inheritance.display(inheritance.parentOf(r)),
                t.valueText.replaceFirst("^:?=\\s*", ""));
            result.add(new ValidationError.Builder()
                .filePath(filePath)
                .line(e.line)
                .column(e.column)
                .message(message)
                .errorCode(ErrorCodes.SEMANTIC_BOUND_VALUE_OVERRIDDEN)
                .severity(ValidationError.Severity.ERROR)
                .build());
            return;
        }
    }

    // ------------------------------------------------------------------------------------------

    /**
     * The features a type inherits: the non-private members of its generals, except those that another
     * inherited feature redefines (KerML removes them from the inherited memberships).
     */
    private List<String> inheritedFeatures(String owner) {
        List<String> cached = inheritedMemo.get(owner);
        if (cached != null) {
            return cached;
        }
        Set<String> all = new LinkedHashSet<>();
        for (String general : inheritance.generals(owner)) {
            for (String m : inheritance.members(general)) {
                IndexedElement r = inheritance.first(m);
                if (r != null && !r.isPrivate && r.effectiveName() != null) {
                    all.add(m);
                }
            }
        }
        Set<String> hidden = new HashSet<>();
        for (String m : all) {
            for (String r : redefinitionClosure(m)) {
                if (!r.equals(m)) {
                    hidden.add(r);
                }
            }
        }
        all.removeAll(hidden);
        List<String> result = List.copyOf(all);
        inheritedMemo.put(owner, result);
        return result;
    }

    /** The inherited feature whose name is one edit from {@code name}, unless the owner has a feature by that name. */
    private String similarInherited(String owner, String name, List<String> inherited) {
        if (name.length() < MIN_SIMILAR_LENGTH) {
            return null;
        }
        Set<String> ownNames = new HashSet<>();
        for (IndexedElement o : index.ownedElements(owner)) {
            if (o.effectiveName() != null) {
                ownNames.add(o.effectiveName());
            }
        }
        for (String f : inherited) {
            String other = inheritance.effectiveName(f);
            if (other != null && !other.equals(name) && other.length() >= MIN_SIMILAR_LENGTH
                    && !ownNames.contains(other) && !differOnlyInDigits(name, other) && oneEditApart(name, other)) {
                return f;
            }
        }
        return null;
    }

    /**
     * A type's directed parameters by position, as qualified names (null for an unnamed one): its own, else
     * those of its first general that has some.
     */
    private List<String> parameters(String type) {
        List<String> cached = parametersMemo.get(type);
        if (cached != null) {
            return cached;
        }
        if (!parametersInProgress.add(type)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        try {
            IndexedElement record = inheritance.first(type);
            if (record != null) {
                for (String p : record.parameterNames) {
                    result.add(p.isEmpty() ? null : type + "::" + p);
                }
            }
            if (result.isEmpty()) {
                for (String general : inheritance.generals(type)) {
                    List<String> theirs = parameters(general);
                    if (!theirs.isEmpty()) {
                        result.addAll(theirs);
                        break;
                    }
                }
            }
        } finally {
            parametersInProgress.remove(type);
        }
        List<String> parameters = Collections.unmodifiableList(result);
        parametersMemo.put(type, parameters);
        return parameters;
    }

    /**
     * The features that the owner's own features redefine directly. KerML (and the reference implementation)
     * drop an inherited feature that redefines, directly or indirectly, any of them: it is replaced.
     */
    private Set<String> redefinedByOwned(String owner) {
        Set<String> cached = redefinedByOwnedMemo.get(owner);
        if (cached != null) {
            return cached;
        }
        Set<String> result = new HashSet<>();
        for (IndexedElement o : index.ownedElements(owner)) {
            if (o.qualifiedName == null) {
                continue;
            }
            for (List<String> segs : o.redefines) {
                result.addAll(redefinitionTargets(o.qualifiedName, owner, segs));
            }
        }
        redefinedByOwnedMemo.put(owner, result);
        return result;
    }

    /**
     * The features a redefinition names, resolved as the reference implementation does: a simple name is
     * looked up among the inherited features first and, failing that, in the enclosing namespaces and imports
     * (so {@code :>> mass} can redefine {@code ISQ::mass}).
     */
    private Set<String> redefinitionTargets(String self, String owner, List<String> segs) {
        Set<String> targets = inheritance.resolveReference(self, owner, segs, true);
        if (targets.isEmpty() && segs.size() == 1) {
            targets = outsideTargets(self, owner, segs);
        }
        return targets;
    }

    /** A feature and every feature it redefines, directly or through a chain, per {@link #redefinitionTargets}. */
    private Set<String> redefinitionClosure(String feature) {
        Set<String> cached = closureMemo.get(feature);
        if (cached != null) {
            return cached;
        }
        if (!closureInProgress.add(feature)) {
            return Set.of(feature);
        }
        Set<String> result = new LinkedHashSet<>();
        result.add(feature);
        try {
            for (IndexedElement record : index.recordsOf(feature)) {
                String owner = record.parentName == null ? "" : record.parentName;
                for (List<String> segs : record.redefines) {
                    for (String target : redefinitionTargets(feature, owner, segs)) {
                        if (!target.equals(feature)) {
                            result.addAll(redefinitionClosure(target));
                        }
                    }
                }
            }
        } finally {
            closureInProgress.remove(feature);
        }
        closureMemo.put(feature, result);
        return result;
    }

    /**
     * True when everything the owner inherits is known: the owner and its ancestors belong to the validated
     * files (library types may inherit from implicit supertypes the index does not model) and every
     * supertype written on them resolves.
     */
    private boolean fullyKnown(String owner) {
        Set<String> types = new LinkedHashSet<>(inheritance.ancestors(owner));
        types.add(owner);
        for (String t : types) {
            if (!index.isLocal(t)) {
                return false;
            }
            for (IndexedElement r : index.recordsOf(t)) {
                if (r.opaqueSupertypes) {
                    return false;
                }
                String parent = r.parentName == null ? "" : r.parentName;
                for (List<String> segs : r.supertypes) {
                    if (inheritance.resolveReference(t, parent, segs, r.redefines.contains(segs)).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** True when the element's typings or subsettings name the given simple name (on purpose). */
    private static boolean namesInHeader(IndexedElement e, String name) {
        for (List<String> segs : e.supertypes) {
            if (!segs.isEmpty() && segs.get(segs.size() - 1).equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        for (String x : a) {
            if (b.contains(x)) {
                return true;
            }
        }
        return false;
    }

    /** A redefinition of {@code name} in the element's own style, e.g. "attribute :>> mass : Real = 1500.0;". */
    private static String redefinitionText(IndexedElement e, String name) {
        StringBuilder sb = new StringBuilder();
        if (e.kind != null && e.kind.endsWith("Usage")) {
            String keyword = InheritanceChecker.KEYWORDS.get(e.kind.substring(0, e.kind.length() - "Usage".length()));
            if (keyword != null) {
                sb.append(keyword).append(' ');
            }
        }
        sb.append(":>> ").append(InheritanceChecker.sysmlName(name));
        if (e.typeText != null) {
            sb.append(" : ").append(e.typeText);
        }
        if (e.multiplicityText != null) {
            sb.append(' ').append(e.multiplicityText);
        }
        if (e.valueText != null) {
            sb.append(' ').append(e.valueText);
        }
        return sb.append(';').toString();
    }

    private static boolean isDefault(String valueText) {
        return valueText.startsWith("default");
    }

    private static boolean isDefinition(String kind) {
        return kind != null && kind.endsWith("Definition");
    }

    /** True when the names are equal apart from their digits (port1/port2), which are numbered on purpose. */
    private static boolean differOnlyInDigits(String a, String b) {
        return a.replaceAll("\\d", "").equals(b.replaceAll("\\d", ""));
    }

    /** True when one insertion, deletion, substitution or swap of adjacent letters turns a into b. */
    static boolean oneEditApart(String a, String b) {
        int la = a.length();
        int lb = b.length();
        if (Math.abs(la - lb) > 1) {
            return false;
        }
        if (la == lb) {
            int first = -1;
            int diffs = 0;
            for (int i = 0; i < la; i++) {
                if (a.charAt(i) != b.charAt(i)) {
                    if (diffs == 0) {
                        first = i;
                    }
                    diffs++;
                }
            }
            if (diffs == 1) {
                return true;
            }
            return diffs == 2 && first + 1 < la && a.charAt(first) == b.charAt(first + 1)
                && a.charAt(first + 1) == b.charAt(first) && a.substring(first + 2).equals(b.substring(first + 2));
        }
        String shorter = la < lb ? a : b;
        String longer = la < lb ? b : a;
        int i = 0;
        while (i < shorter.length() && shorter.charAt(i) == longer.charAt(i)) {
            i++;
        }
        return shorter.substring(i).equals(longer.substring(i + 1));
    }

    private static ValidationWarning warning(String filePath, int line, int column, String code, String message,
            String suggestion) {
        ValidationWarning.Builder builder = new ValidationWarning.Builder();
        builder.filePath(filePath).line(line).column(column).message(message).errorCode(code);
        if (suggestion != null) {
            builder.addSuggestion(suggestion);
        }
        return builder.build();
    }
}
