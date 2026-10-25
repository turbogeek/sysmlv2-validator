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
 *   <li>{@link ErrorCodes#SEMANTIC_TYPED_BY_USAGE} (error): {@code attribute mass : mass;} or
 *       {@code attribute mass : ISQ::mass;} types a usage by a usage (itself, or the library quantity)
 *       where a definition ({@code MassValue}) or a subsetting ({@code :> ISQ::mass}) was meant.</li>
 *   <li>Aliases: an owned feature with the name of an inherited alias is reported as
 *       {@link ErrorCodes#INHERITED_NAME_REDECLARED}; {@link ErrorCodes#ALIAS_NAME_COLLISION}: an alias takes
 *       the name of an inherited member that it does not stand for, or a type inherits an alias and another
 *       member with one name; {@link ErrorCodes#INHERITED_ALIAS_DROPPED}: a redefinition drops the inherited
 *       aliases of the feature it redefines (KerML removeRedefinedFeatures), so their names stop resolving.</li>
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
    /**
     * Usages that SysML requires to be typed by definitions (validateUsageType and its specializations); the
     * reference implementation reports each typed by a usage as an error.
     */
    private static final Set<String> DEFINITION_TYPED_KINDS = Set.of(
        "AttributeUsage", "PartUsage", "ItemUsage", "PortUsage", "ActionUsage", "OccurrenceUsage", "RefUsage",
        "StateUsage", "RequirementUsage", "ConstraintUsage", "CalcUsage", "EnumUsage", "InterfaceUsage",
        "ActorUsage", "StakeholderUsage", "SubjectDeclaration", "InParameter", "OutParameter", "InoutParameter");
    /** Shortest name compared for typos; shorter names (x, id, in1) differ by one edit too often. */
    private static final int MIN_SIMILAR_LENGTH = 4;

    private final ModelIndex index;
    private final InheritanceChecker inheritance;
    private final Map<String, List<String>> inheritedMemo = new HashMap<>();
    private final Map<String, Set<String>> hiddenMemo = new HashMap<>();
    private final Map<String, List<String>> parametersMemo = new HashMap<>();
    private final Map<String, Set<String>> redefinedByOwnedMemo = new HashMap<>();
    private final Map<String, Set<String>> closureMemo = new HashMap<>();
    private final Map<String, List<InheritedAlias>> visibleAliasesMemo = new HashMap<>();
    private final Map<String, List<InheritedAlias>> inheritedAliasesMemo = new HashMap<>();
    private final Set<String> closureInProgress = new HashSet<>();
    private final Set<String> parametersInProgress = new HashSet<>();
    private final Set<String> aliasesInProgress = new HashSet<>();

    /** An alias of a feature, as seen by a type that inherits it: the alias member and the feature it stands for. */
    private record InheritedAlias(String alias, String feature) { }

    /** The typings reported as {@link ErrorCodes#SEMANTIC_TYPED_BY_USAGE}, and the typing to suggest in fixes. */
    private record TypedByUsage(Set<List<String>> typings, String typeText) { }

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
            if (e.aliasTarget != null) {
                aliasCollision(e, filePath, result);
            } else if (e.declaresFeature()) {
                checkFeature(e, filePath, result);
            }
            inheritedAliasConflicts(e, filePath, result);
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
        TypedByUsage typed = typedByUsage(e, owner, filePath, result);
        boolean selfReference = refersToItself(e, owner, typed.typings(), filePath, result);
        redefinitionTargets(e, owner, filePath, result);
        redefinedThroughAlias(e, owner, filePath, result);
        droppedAliases(e, owner, filePath, result);
        if (!e.parameter && !selfReference) {
            redeclaration(e, owner, typed.typeText(), filePath, result);
        }
    }

    /**
     * INHERITED_NAME_REDECLARED, RENAMED_INHERITED_NAME_REUSED and NAME_SIMILAR_TO_INHERITED for a new feature.
     *
     * @param typeText the typing to write in a suggested redefinition: the element's own, or a definition in
     *     place of a usage it is wrongly typed by (null for none)
     */
    private void redeclaration(IndexedElement e, String owner, String typeText, String filePath,
            List<ValidationError> result) {
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
            String fix = redefinitionText(e, name, typeText);
            String message = String.format("'%s' declares a new feature in '%s' with the same name as the inherited"
                    + " '%s' instead of redefining it, so '%s' has two features named '%s'.%s%s Redefine the"
                    + " inherited feature ('%s'), or give the new feature another name.",
                name, ownerName, inheritance.display(target), ownerName, name, valueNote, subsets, fix);
            result.add(warning(filePath, e.line, e.column, ErrorCodes.INHERITED_NAME_REDECLARED, message, fix));
            return;
        }
        // The name of an inherited alias ('alias weight for mass;' in a general)
        for (InheritedAlias a : inheritedAliases(owner)) {
            String aliasName = overlap(e, inheritance.first(a.alias()));
            if (aliasName == null || redefined.contains(a.feature())) {
                continue;
            }
            String featureName = inheritance.effectiveName(a.feature());
            String fix = redefinitionText(e, featureName, typeText);
            String valueNote = e.valueText == null ? ""
                : String.format(" The value '%s' goes to the new feature, not to '%s'.", e.valueText,
                    inheritance.display(a.feature()));
            String message = String.format("'%s' declares a new feature in '%s' with the name of the alias '%s', which"
                    + " '%s' inherits for '%s', instead of redefining that feature, so '%s' names two members of"
                    + " '%s'.%s Redefine the inherited feature ('%s'; a redefinition drops the inherited aliases of the"
                    + " feature it redefines, so also declare 'alias %s for %s;' in '%s' if the name '%s' is still"
                    + " used), or give the new feature another name.",
                aliasName, ownerName, inheritance.display(a.alias()), ownerName, inheritance.display(a.feature()),
                aliasName, ownerName, valueNote, fix, InheritanceChecker.sysmlName(aliasName),
                InheritanceChecker.sysmlName(featureName), ownerName, aliasName);
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
            String fix = redefinitionText(e, other, typeText);
            String message = String.format("'%s' is a new feature of '%s', but '%s' inherits '%s', whose name differs"
                    + " by one letter. If '%s' is meant, redefine it ('%s'); otherwise rename '%s' so the two are"
                    + " not confused.",
                name, ownerName, ownerName, inheritance.display(similar), other, fix, name);
            result.add(warning(filePath, e.line, e.column, ErrorCodes.NAME_SIMILAR_TO_INHERITED, message, fix));
        }
    }

    /**
     * SEMANTIC_TYPED_BY_USAGE: a usage typed by a usage, most often by its own name ({@code attribute mass :
     * mass;}, where the owner's own members are found first), by a library quantity ({@code attribute mass :
     * ISQ::mass;}) or by a unit ({@code attribute mass : kg;}), where a definition ({@code : MassValue}) or a
     * subsetting ({@code :> ISQ::mass}) was meant.
     *
     * @return the typings reported, and the type to suggest in their place
     */
    private TypedByUsage typedByUsage(IndexedElement e, String owner, String filePath, List<ValidationError> result) {
        Set<List<String>> reported = new LinkedHashSet<>();
        if (!DEFINITION_TYPED_KINDS.contains(e.kind) || e.typeText == null
                || filePath != null && filePath.endsWith(".kerml")) {
            return new TypedByUsage(reported, e.typeText);
        }
        String name = e.name != null ? e.name : e.shortName;
        String ownerName = owner.isEmpty() ? "its namespace" : "'" + inheritance.display(owner) + "'";
        String suggested = null;
        for (List<String> segs : typings(e)) {
            boolean self = segs.size() == 1 && segs.get(0).equals(name);
            String usage;
            if (self) {
                usage = e.qualifiedName;
            } else {
                usage = usageNamed(segs, owner);
                if (usage == null) {
                    continue;
                }
            }
            reported.add(segs);
            String written = String.join("::", segs);
            // What was probably meant: the same-named feature outside the owner (ISQ::mass), or the usage itself
            String meant = usage;
            if (self) {
                Set<String> elsewhere = outsideTargets(e.qualifiedName, inheritance.parentOf(owner), segs);
                meant = elsewhere.isEmpty() ? null : elsewhere.iterator().next();
            }
            IndexedElement meantRecord = meant == null ? null : inheritance.first(meant);
            String meantType = meantRecord == null ? null : meantRecord.typeText;
            String quantity = meant == null ? null : quantityOfUnit(meant);
            String fix = "";
            if (quantity != null) {
                meantType = quantity;
                fix = String.format(" '%s' is a unit: type the feature by its quantity (': %s') and give values in"
                    + " the unit ('= 1.0 [%s]').", inheritance.display(meant), quantity, written);
            } else if (meant != null) {
                String subset = self ? inheritance.display(meant) : written;
                fix = meantType != null
                    ? String.format(" Type it by a definition (': %s', the type of '%s') or subset the feature"
                        + " (':> %s').", meantType, inheritance.display(meant), subset)
                    : String.format(" Subset the feature instead (':> %s').", subset);
            }
            suggested = suggested != null ? suggested : meantType;
            String what = self
                ? String.format("'%s' is typed by itself: the name '%s' after ':' is looked up in %s first, where it"
                    + " finds the feature being declared.", name, written, ownerName)
                : String.format("'%s' is typed by '%s', which is %s.",
                    name == null ? "?" : name, inheritance.display(usage), article(usage));
            String message = what + " A usage is a feature, and a usage's types must be classifiers, such as"
                + " definitions (SysML 8.3.6.4, Usage::definition; the reference implementation reports an error)."
                + fix;
            result.add(new ValidationError.Builder()
                .filePath(filePath)
                .line(e.line)
                .column(e.column)
                .message(message)
                .errorCode(ErrorCodes.SEMANTIC_TYPED_BY_USAGE)
                .severity(ValidationError.Severity.ERROR)
                .build());
        }
        return new TypedByUsage(reported, reported.isEmpty() ? e.typeText : suggested);
    }

    /**
     * For a unit such as {@code SI::kg} (an attribute usage typed {@code MassUnit}, a kind of
     * {@code MeasurementUnit}): the quantity value definition its values belong to, {@code ISQ::MassValue},
     * named as the ISQ library names it ({@code XUnit} / {@code XValue}); null if it is not a unit or no such
     * definition is known.
     */
    private String quantityOfUnit(String usage) {
        IndexedElement r = inheritance.first(usage);
        if (r == null || !"AttributeUsage".equals(r.kind) || r.typeText == null || !r.typeText.endsWith("Unit")) {
            return null;
        }
        List<List<String>> types = typings(r);
        if (types.size() != 1) {
            return null;
        }
        ModelIndex.Resolution type = index.resolve(types.get(0), inheritance.parentOf(usage));
        if (type.getStatus() != ModelIndex.Status.FOUND) {
            return null;
        }
        boolean unit = false;
        for (String t : type.getTargets()) {
            Set<String> ancestors = new LinkedHashSet<>(inheritance.ancestors(t));
            ancestors.add(t);
            unit |= ancestors.stream().anyMatch(a -> a.endsWith("::MeasurementUnit"));
        }
        String unitType = types.get(0).get(types.get(0).size() - 1);
        String quantity = unitType.substring(0, unitType.length() - "Unit".length()) + "Value";
        return unit && index.resolve(List.of("ISQ", quantity), "").getStatus() == ModelIndex.Status.FOUND
            ? quantity : null;
    }

    /** The usage (or KerML feature) a typing names, aliases followed; null unless every target is one. */
    private String usageNamed(List<String> segs, String owner) {
        ModelIndex.Resolution r = index.resolve(segs, owner);
        if (r.getStatus() != ModelIndex.Status.FOUND || r.getTargets().isEmpty()) {
            return null;
        }
        Set<String> targets = new LinkedHashSet<>();
        for (String t : r.getTargets()) {
            IndexedElement record = inheritance.first(t);
            if (record != null && record.aliasTarget != null) {
                targets.addAll(inheritance.aliasTargets(record));
            } else {
                targets.add(t);
            }
        }
        for (String t : targets) {
            IndexedElement record = inheritance.first(t);
            if (record == null || !record.declaresFeature()) {
                return null;
            }
        }
        return targets.isEmpty() ? null : targets.iterator().next();
    }

    /** "an attribute usage", "a part usage", ... for a usage; "a feature" otherwise. */
    private String article(String usage) {
        IndexedElement r = inheritance.first(usage);
        if (r == null || r.kind == null || !r.kind.endsWith("Usage")) {
            return "a feature";
        }
        String keyword = InheritanceChecker.KEYWORDS.get(r.kind.substring(0, r.kind.length() - "Usage".length()));
        if (keyword == null) {
            return "a usage";
        }
        return ("aeiou".indexOf(keyword.charAt(0)) >= 0 ? "an " : "a ") + keyword + " usage";
    }

    /** The typings written on an element ({@code : A, ~B}), as qualified-name segments. */
    private static List<List<String>> typings(IndexedElement e) {
        List<List<String>> result = new ArrayList<>();
        if (e.typeText == null) {
            return result;
        }
        for (String part : e.typeText.split(",")) {
            String text = part.trim();
            if (text.startsWith("~")) {
                text = text.substring(1);
            }
            if (text.isEmpty() || text.contains(".")) {
                continue;
            }
            List<String> segs = new ArrayList<>();
            for (String seg : text.split("::")) {
                segs.add(seg.length() > 1 && seg.startsWith("'") && seg.endsWith("'")
                    ? seg.substring(1, seg.length() - 1) : seg);
            }
            result.add(segs);
        }
        return result;
    }

    /**
     * FEATURE_REFERS_TO_ITSELF: a typing or subsetting by the feature's own simple name. The owner's own
     * members are found first, so the name denotes the feature being declared.
     *
     * @param skip typings already reported as {@link ErrorCodes#SEMANTIC_TYPED_BY_USAGE}
     */
    private boolean refersToItself(IndexedElement e, String owner, Set<List<String>> skip, String filePath,
            List<ValidationError> result) {
        String name = e.name != null ? e.name : e.shortName;
        if (name == null) {
            return false;
        }
        for (List<String> segs : e.supertypes) {
            if (segs.size() == 1 && segs.get(0).equals(name) && !e.redefines.contains(segs) && !skip.contains(segs)) {
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

    /**
     * ALIAS_NAME_COLLISION for an owned alias: it takes the name of a member the owner inherits but stands for
     * something else. Owned members are found first, so the name now means the alias's target in the owner and
     * its specializations.
     */
    private void aliasCollision(IndexedElement e, String filePath, List<ValidationError> result) {
        String owner = e.parentName == null ? "" : e.parentName;
        if (owner.isEmpty() || inheritance.generals(owner).isEmpty()) {
            return;
        }
        Set<String> targets = inheritance.aliasTargets(e);
        if (targets.isEmpty()) {
            return;
        }
        Set<String> replaced = redefinedByOwned(owner);
        String hidden = null;
        String name = null;
        for (String f : inheritedFeatures(owner)) {
            name = overlap(e, inheritance.first(f));
            if (name != null && !targets.contains(f) && !intersects(redefinitionClosure(f), replaced)) {
                hidden = "'" + inheritance.display(f) + "'";
                break;
            }
        }
        if (hidden == null) {
            for (InheritedAlias a : inheritedAliases(owner)) {
                name = overlap(e, inheritance.first(a.alias()));
                if (name != null && !targets.contains(a.feature())) {
                    hidden = "'" + inheritance.display(a.feature()) + "' (through the alias '"
                        + inheritance.display(a.alias()) + "')";
                    break;
                }
            }
        }
        if (hidden == null) {
            return;
        }
        String ownerName = inheritance.display(owner);
        String target = inheritance.display(targets.iterator().next());
        // A private alias is not visible to specializations
        String reach = e.isPrivate ? String.format("'%s' in '%s' now means '%s'", name, ownerName, target)
            : String.format("'%s' in '%s' and its specializations now means '%s', and ':>> %s' in a specialization"
                + " redefines it", name, ownerName, target, InheritanceChecker.sysmlName(name));
        String message = String.format("Alias '%s' in '%s' stands for '%s', but '%s' also inherits %s under the name"
                + " '%s'. Owned members are found first, so %s. Give the alias another name.",
            name, ownerName, target, ownerName, hidden, name, reach);
        result.add(warning(filePath, e.line, e.column, ErrorCodes.ALIAS_NAME_COLLISION, message, null));
    }

    /**
     * ALIAS_NAME_COLLISION for a type that inherits, from different generals, an alias and another member with
     * the same name: the name resolves to the one from the general listed first.
     */
    private void inheritedAliasConflicts(IndexedElement e, String filePath, List<ValidationError> result) {
        List<InheritedAlias> aliases = inheritedAliases(e.qualifiedName);
        if (aliases.isEmpty()) {
            return;
        }
        String type = e.qualifiedName;
        Set<String> own = ownNames(type);
        Set<String> replaced = redefinedByOwned(type);
        Set<String> reported = new HashSet<>();
        for (InheritedAlias a : aliases) {
            IndexedElement alias = inheritance.first(a.alias());
            for (String name : names(alias)) {
                if (own.contains(name) || !reported.add(name)) {
                    continue;
                }
                String aliasFrom = generalShowingAlias(type, a);
                String other = null;
                String otherFrom = null;
                for (String f : inheritedFeatures(type)) {
                    if (!f.equals(a.feature()) && name.equals(inheritance.effectiveName(f))
                            && !intersects(redefinitionClosure(f), replaced)) {
                        other = "the feature '" + inheritance.display(f) + "'";
                        otherFrom = generalWithMember(type, f);
                        break;
                    }
                }
                for (InheritedAlias b : aliases) {
                    if (other == null && !b.feature().equals(a.feature())
                            && names(inheritance.first(b.alias())).contains(name)) {
                        other = "the alias '" + inheritance.display(b.alias()) + "' for '"
                            + inheritance.display(b.feature()) + "'";
                        otherFrom = generalShowingAlias(type, b);
                    }
                }
                if (other == null || aliasFrom == null || otherFrom == null || aliasFrom.equals(otherFrom)) {
                    reported.remove(name);
                    continue;
                }
                List<String> generals = inheritance.generals(type);
                boolean aliasFirst = generals.indexOf(aliasFrom) < generals.indexOf(otherFrom);
                String typeName = inheritance.display(type);
                String message = String.format("'%s' inherits two different members named '%s': the alias '%s' for"
                        + " '%s' (from '%s') and %s (from '%s'). '%s' in '%s' resolves to the one from the general"
                        + " listed first, '%s'. Rename one of them, or redefine the feature in '%s' under a distinct"
                        + " name.",
                    typeName, name, inheritance.display(a.alias()), inheritance.display(a.feature()),
                    inheritance.display(aliasFrom), other, inheritance.display(otherFrom), name, typeName,
                    inheritance.display(aliasFirst ? aliasFrom : otherFrom), typeName);
                result.add(warning(filePath, e.line, e.column, ErrorCodes.ALIAS_NAME_COLLISION, message, null));
            }
        }
    }

    /**
     * ALIAS_NAME_COLLISION for {@code :>> name} that a general's alias turns away from the inherited feature of
     * that name: {@code alias mass for ISQ::mass;} in Car makes {@code :>> mass} in a specialization of Car
     * redefine {@code ISQ::mass}, and {@code Vehicle::mass} stays inherited beside it.
     */
    private void redefinedThroughAlias(IndexedElement e, String owner, String filePath,
            List<ValidationError> result) {
        if (owner.isEmpty() || e.redefines.size() != 1 || e.redefines.get(0).size() != 1) {
            return;
        }
        String name = e.redefines.get(0).get(0);
        Set<String> direct = redefinitionTargets(e.qualifiedName, owner, e.redefines.get(0));
        InheritedAlias through = null;
        for (String general : inheritance.generals(owner)) {
            for (InheritedAlias a : visibleAliases(general)) {
                if (direct.contains(a.feature()) && names(inheritance.first(a.alias())).contains(name)) {
                    through = a;
                    break;
                }
            }
            if (through != null) {
                break;
            }
        }
        if (through == null) {
            return;
        }
        Set<String> redefined = redefinitionClosure(e.qualifiedName);
        Set<String> replaced = redefinedByOwned(owner);
        for (String f : inheritedFeatures(owner)) {
            if (name.equals(inheritance.effectiveName(f)) && !redefined.contains(f)
                    && !intersects(redefinitionClosure(f), replaced)) {
                String ownerName = inheritance.display(owner);
                String message = String.format("':>> %s' in '%s' redefines '%s', because '%s' is the alias '%s' for it;"
                        + " '%s' still inherits '%s', so it now has two features named '%s'. To redefine '%s', write"
                        + " ':>> %s', and give the alias another name.",
                    name, ownerName, inheritance.display(through.feature()), name, inheritance.display(through.alias()),
                    ownerName, inheritance.display(f), name, inheritance.display(f), inheritance.qualifiedReference(f));
                result.add(warning(filePath, e.line, e.column, ErrorCodes.ALIAS_NAME_COLLISION, message, null));
                return;
            }
        }
    }

    /**
     * INHERITED_ALIAS_DROPPED: a redefinition removes the owner's inherited memberships of the redefined
     * feature, aliases included (KerML removeRedefinedFeatures), so an alias declared in a general for that
     * feature no longer resolves in the owner, unless the owner declares the name again.
     */
    private void droppedAliases(IndexedElement e, String owner, String filePath, List<ValidationError> result) {
        if (e.redefines.isEmpty() || owner.isEmpty()) {
            return;
        }
        List<InheritedAlias> aliases = inheritableAliases(owner);
        if (aliases.isEmpty()) {
            return;
        }
        Set<String> direct = new LinkedHashSet<>();
        for (List<String> segs : e.redefines) {
            direct.addAll(redefinitionTargets(e.qualifiedName, owner, segs));
        }
        if (direct.isEmpty()) {
            return;
        }
        String effective = e.name != null ? e.name
            : e.shortName != null ? e.shortName : inheritance.effectiveName(direct.iterator().next());
        Set<String> own = ownNames(owner);
        String label = e.name != null ? "'" + e.name + "'" : "':>> " + String.join("::", e.redefines.get(0)) + "'";
        String ownerName = inheritance.display(owner);
        Set<String> reported = new HashSet<>();
        for (InheritedAlias a : aliases) {
            List<String> aliasNames = names(inheritance.first(a.alias()));
            if (aliasNames.isEmpty() || aliasNames.contains(effective) || !Collections.disjoint(aliasNames, own)
                    || !intersects(redefinitionClosure(a.feature()), direct) || !reported.add(a.alias())) {
                continue;
            }
            String aliasName = aliasNames.get(0);
            String dropped = String.format("%s redefines '%s', so '%s' no longer has '%s', the alias that '%s' declares"
                    + " for it: a redefinition drops every inherited membership of the feature it redefines, aliases"
                    + " included (KerML removeRedefinedFeatures), and the redefining feature is named '%s'.",
                label, inheritance.display(a.feature()), ownerName, aliasName,
                inheritance.display(inheritance.parentOf(a.alias())), effective);
            // Another general may still supply a member with that name, which the name now means
            String other = inheritedMemberNamed(owner, aliasName, a.feature());
            if (other != null) {
                result.add(warning(filePath, e.line, e.column, ErrorCodes.INHERITED_ALIAS_DROPPED, dropped
                    + String.format(" '%s' in '%s' now means %s.", aliasName, ownerName, other), null));
                continue;
            }
            String message = dropped + String.format(" '%s' does not resolve in '%s' or its specializations. If the"
                    + " name is still used, declare the alias again in '%s' ('alias %s for %s;').",
                aliasName, ownerName, ownerName, InheritanceChecker.sysmlName(aliasName),
                InheritanceChecker.sysmlName(effective));
            result.add(warning(filePath, e.line, e.column, ErrorCodes.INHERITED_ALIAS_DROPPED, message,
                "alias " + InheritanceChecker.sysmlName(aliasName) + " for " + InheritanceChecker.sysmlName(effective)
                    + ";"));
        }
    }

    /**
     * A member the owner still inherits under the name, other than the given feature and the features the
     * owner's own features replace, described for a message ("the feature 'Scale::weight'"); null if none.
     */
    private String inheritedMemberNamed(String owner, String name, String except) {
        Set<String> replaced = redefinedByOwned(owner);
        for (String f : inheritedFeatures(owner)) {
            if (!f.equals(except) && name.equals(inheritance.effectiveName(f))
                    && !intersects(redefinitionClosure(f), replaced)) {
                return "the feature '" + inheritance.display(f) + "'";
            }
        }
        for (InheritedAlias b : inheritedAliases(owner)) {
            if (!b.feature().equals(except) && names(inheritance.first(b.alias())).contains(name)) {
                return "'" + inheritance.display(b.feature()) + "', through the alias '"
                    + inheritance.display(b.alias()) + "'";
            }
        }
        return null;
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
        hiddenMemo.put(owner, hidden);
        return result;
    }

    /** The features that a feature inherited by the owner redefines, which the owner therefore does not inherit. */
    private Set<String> hiddenFeatures(String owner) {
        inheritedFeatures(owner);
        return hiddenMemo.getOrDefault(owner, Set.of());
    }

    /** The aliases of features that a type shows its specializations: its own non-private ones and inherited ones. */
    private List<InheritedAlias> visibleAliases(String type) {
        List<InheritedAlias> cached = visibleAliasesMemo.get(type);
        if (cached != null) {
            return cached;
        }
        if (!aliasesInProgress.add(type)) {
            return List.of();
        }
        List<InheritedAlias> result = new ArrayList<>();
        try {
            for (IndexedElement o : index.ownedElements(type)) {
                if (o.aliasTarget == null || o.isPrivate || o.qualifiedName == null) {
                    continue;
                }
                for (String t : inheritance.aliasTargets(o)) {
                    IndexedElement r = inheritance.first(t);
                    if (r != null && r.declaresFeature()) {
                        result.add(new InheritedAlias(o.qualifiedName, t));
                        break;
                    }
                }
            }
            result.addAll(inheritedAliases(type));
        } finally {
            aliasesInProgress.remove(type);
        }
        List<InheritedAlias> aliases = List.copyOf(result);
        visibleAliasesMemo.put(type, aliases);
        return aliases;
    }

    /** The aliases of features that a type's generals show it, before any is dropped (KerML inheritableMemberships). */
    private List<InheritedAlias> inheritableAliases(String owner) {
        Set<InheritedAlias> result = new LinkedHashSet<>();
        for (String general : inheritance.generals(owner)) {
            result.addAll(visibleAliases(general));
        }
        return List.copyOf(result);
    }

    /**
     * The aliases of features that a type inherits: the inheritable ones, except the aliases of a feature that
     * the type's own features redefine or that another inherited feature redefines. KerML removeRedefinedFeatures
     * drops every membership of such a feature, aliases included.
     */
    private List<InheritedAlias> inheritedAliases(String owner) {
        List<InheritedAlias> cached = inheritedAliasesMemo.get(owner);
        if (cached != null) {
            return cached;
        }
        List<InheritedAlias> inheritable = inheritableAliases(owner);
        List<InheritedAlias> result = new ArrayList<>();
        if (!inheritable.isEmpty()) {
            Set<String> replaced = redefinedByOwned(owner);
            Set<String> hidden = hiddenFeatures(owner);
            for (InheritedAlias a : inheritable) {
                if (!hidden.contains(a.feature()) && !intersects(redefinitionClosure(a.feature()), replaced)) {
                    result.add(a);
                }
            }
        }
        List<InheritedAlias> aliases = List.copyOf(result);
        inheritedAliasesMemo.put(owner, aliases);
        return aliases;
    }

    /** The first general of a type, in declaration order, that shows it the alias. */
    private String generalShowingAlias(String type, InheritedAlias alias) {
        for (String general : inheritance.generals(type)) {
            if (visibleAliases(general).contains(alias)) {
                return general;
            }
        }
        return null;
    }

    /** The first general of a type, in declaration order, of which the feature is a member. */
    private String generalWithMember(String type, String feature) {
        for (String general : inheritance.generals(type)) {
            if (inheritance.members(general).contains(feature)) {
                return general;
            }
        }
        return null;
    }

    /** The declared names and short names of a namespace's own members (not names derived from redefinitions). */
    private Set<String> ownNames(String namespace) {
        Set<String> result = new HashSet<>();
        for (IndexedElement o : index.ownedElements(namespace)) {
            result.addAll(names(o));
        }
        return result;
    }

    /** An element's declared name and short name, if any. */
    private static List<String> names(IndexedElement e) {
        List<String> result = new ArrayList<>(2);
        if (e != null && e.name != null) {
            result.add(e.name);
        }
        if (e != null && e.shortName != null) {
            result.add(e.shortName);
        }
        return result;
    }

    /**
     * The name that two elements share: a declared name or short name of one equal to a declared name or short
     * name of the other (the second element's effective name counts for an unnamed redefinition); null if none.
     */
    private static String overlap(IndexedElement a, IndexedElement b) {
        if (a == null || b == null) {
            return null;
        }
        List<String> theirs = names(b);
        if (theirs.isEmpty() && b.effectiveName() != null) {
            theirs.add(b.effectiveName());
        }
        for (String n : names(a)) {
            if (theirs.contains(n)) {
                return n;
            }
        }
        return null;
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

    /**
     * A redefinition of {@code name} in the element's own style with the given typing (none if null), e.g.
     * "attribute :>> mass : Real = 1500.0;".
     */
    private static String redefinitionText(IndexedElement e, String name, String typeText) {
        StringBuilder sb = new StringBuilder();
        if (e.kind != null && e.kind.endsWith("Usage")) {
            String keyword = InheritanceChecker.KEYWORDS.get(e.kind.substring(0, e.kind.length() - "Usage".length()));
            if (keyword != null) {
                sb.append(keyword).append(' ');
            }
        }
        sb.append(":>> ").append(InheritanceChecker.sysmlName(name));
        if (typeText != null) {
            sb.append(" : ").append(typeText);
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
