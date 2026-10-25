package com.validator.semantic.index;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reports multiple inheritance that leaves a contradiction which the inheriting element does not resolve.
 *
 * <p>A definition or usage D with two or more general types (the specializations {@code :>} of a
 * definition; the typings, subsettings and redefinitions of a usage) inherits every feature of its
 * generals except those it redefines. Two problems are reported as warnings:</p>
 * <ul>
 *   <li>{@link ErrorCodes#DIAMOND_INHERITANCE_CONFLICT}: a feature f of a common ancestor is redefined
 *       along two branches by different features, and D does not redefine f (or either branch's
 *       redefinition). D then holds both redefinitions of f. Reported when they contradict each other
 *       (different literal values or enumeration literals, unrelated types, multiplicities without a common
 *       value, or contradicting nested redefinitions) or merely repeat each other; not when one only refines
 *       the other compatibly (see {@link #classify}).</li>
 *   <li>{@link ErrorCodes#MULTIPLE_INHERITANCE_NAME_CONFLICT}: two generals contribute different features
 *       with the same name, neither redefining the other, and D does not redefine that name.</li>
 * </ul>
 *
 * <p>A redefinition along a branch is the nearest one walking from the general up to the ancestor,
 * following redefinition chains. D resolves a conflict by owning a feature that redefines f, one of the
 * branch redefinitions, or a feature of that name ({@code attribute :>> f = 4;}).</p>
 *
 * <p>Not reported: a feature inherited along two paths without being redefined (it is the same feature);
 * redundant specialization ({@code D :> B, A} where {@code B :> A}: B's redefinition is the more specific
 * one); a conflict that one general already holds by itself (it is reported on that general, or settled
 * there); a conflict that another inherited feature merges by redefining both branches;
 * parameters and connector ends (they redefine by position, which the index does not model); private
 * features (not inherited); the implicit library supertypes of definitions and usages (not written in the
 * model, so not indexed); and names that do not resolve. Only the elements of the validated files are
 * checked, never library elements.</p>
 *
 * <p>Generals, ancestors, inherited features and redefinition chains are memoized, so one checker should
 * serve all files of one index (the index must not change while the checker is used).</p>
 */
public final class InheritanceChecker {

    private static final int MAX_ALIAS_DEPTH = 8;
    /** How deep the bodies of conflicting redefinitions are compared. */
    private static final int MAX_NESTING = 3;

    /** Usage keyword by rule kind without "Usage", for the refactoring example in messages. */
    private static final Map<String, String> KEYWORDS = Map.ofEntries(
        Map.entry("Part", "part"), Map.entry("Attribute", "attribute"), Map.entry("Item", "item"),
        Map.entry("Port", "port"), Map.entry("Action", "action"), Map.entry("State", "state"),
        Map.entry("Ref", "ref"), Map.entry("Occurrence", "occurrence"), Map.entry("Requirement", "requirement"),
        Map.entry("Constraint", "constraint"), Map.entry("Calc", "calc"), Map.entry("Connection", "connection"),
        Map.entry("Interface", "interface"), Map.entry("Enum", "enum"), Map.entry("View", "view"));

    private final ModelIndex index;
    private final Map<String, List<String>> generalsMemo = new HashMap<>();
    private final Map<String, Set<String>> ancestorsMemo = new HashMap<>();
    private final Map<String, List<String>> membersMemo = new HashMap<>();
    private final Map<String, Set<String>> closureMemo = new HashMap<>();
    private final Map<String, Set<Value>> valuesMemo = new HashMap<>();
    /** Features with a redefinition (in their chain) that does not resolve. */
    private final Set<String> unresolvedChains = new HashSet<>();
    private final Set<String> generalsInProgress = new HashSet<>();
    private final Set<String> ancestorsInProgress = new HashSet<>();
    private final Set<String> membersInProgress = new HashSet<>();
    private final Set<String> closureInProgress = new HashSet<>();

    /**
     * @param index the index of the libraries and every validated file
     */
    public InheritanceChecker(ModelIndex index) {
        this.index = index;
    }

    /**
     * Check one file in a fresh checker.
     *
     * @param elements the file's indexed elements
     * @param index the index of the libraries and validated files
     * @param filePath the file path for the warnings
     * @return the warnings, in declaration order
     */
    public static List<ValidationWarning> check(List<IndexedElement> elements, ModelIndex index, String filePath) {
        return new InheritanceChecker(index).check(elements, filePath);
    }

    /**
     * Check the elements of one file.
     *
     * @param elements the file's indexed elements
     * @param filePath the file path for the warnings
     * @return the warnings, in declaration order
     */
    public List<ValidationWarning> check(List<IndexedElement> elements, String filePath) {
        List<ValidationWarning> warnings = new ArrayList<>();
        for (IndexedElement element : elements) {
            // One written supertype can give at most one general (or a duplicate declaration)
            if (element.supertypes.size() >= 2 && element.qualifiedName != null) {
                checkElement(element, filePath, warnings);
            }
        }
        return warnings;
    }

    // ------------------------------------------------------------------------------------------

    /**
     * A conflict between features that several generals contribute.
     *
     * @param ancestorFeature the common redefined feature (diamond), or null for a name conflict
     * @param name the name of the feature in conflict
     * @param features each conflicting feature mapped to the indexes of the generals it comes through
     */
    private record Conflict(String ancestorFeature, String name, Map<String, BitSet> features) {
        Conflict(String ancestorFeature, String name) {
            this(ancestorFeature, name, new LinkedHashMap<>());
        }
    }

    /** Whether the redefinitions in a diamond contradict each other, repeat each other, or refine compatibly. */
    private enum Verdict { CONTRADICTION, DUPLICATE, COMPATIBLE }

    private void checkElement(IndexedElement element, String filePath, List<ValidationWarning> warnings) {
        String d = element.qualifiedName;
        List<String> generals = generals(d);
        if (generals.size() < 2) {
            return;
        }
        List<Conflict> conflicts = conflicts(generals, d);
        if (conflicts.isEmpty()) {
            return;
        }

        // What D redefines itself: by resolved target, and by name when it names the feature by its simple
        // name (or by a name that does not resolve). ':>> Other::f' of an unrelated Other does not resolve f.
        Set<String> resolvedNames = new HashSet<>();
        Set<String> resolvedTargets = new HashSet<>();
        for (IndexedElement owned : index.ownedElements(d)) {
            for (List<String> segs : owned.redefines) {
                Set<String> targets = resolveReference(owned.qualifiedName, d, segs, true);
                resolvedTargets.addAll(targets);
                if (segs.size() == 1 || targets.isEmpty()) {
                    resolvedNames.add(segs.get(segs.size() - 1));
                }
            }
        }

        for (Conflict c : conflicts) {
            if (resolved(c, resolvedNames, resolvedTargets)) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            Verdict verdict = c.ancestorFeature() == null ? null : classify(c.features().keySet(), reasons, 0);
            if (verdict != Verdict.COMPATIBLE) {
                warnings.add(warning(element, filePath, generals, c, reasons));
            }
        }
    }

    /**
     * Conflicts among the features that several types contribute: diamonds (different redefinitions of a
     * common feature) and name conflicts (unrelated features with the same name).
     *
     * @param generals the types whose features are combined
     * @param owner the element that combines them (its own features are skipped), or null
     */
    private List<Conflict> conflicts(List<String> generals, String owner) {
        // Which generals contribute each inherited feature. A general that another general specializes adds
        // nothing (D :> B, A where B :> A): its features are already inherited through the other.
        Map<String, BitSet> contributors = new LinkedHashMap<>();
        for (int i = 0; i < generals.size(); i++) {
            if (redundant(generals, i)) {
                continue;
            }
            for (String m : members(generals.get(i))) {
                IndexedElement record = first(m);
                if (record == null || record.parameter || record.isPrivate
                        || owner != null && owner.equals(record.parentName)) {
                    continue;
                }
                contributors.computeIfAbsent(m, k -> new BitSet()).set(i);
            }
        }
        if (contributors.size() < 2) {
            return List.of();
        }

        Map<String, Conflict> diamonds = new LinkedHashMap<>();
        Map<String, Conflict> nameClashes = new TreeMap<>();
        List<String> features = new ArrayList<>(contributors.keySet());

        // Diamonds: features that redefine a common feature, grouped by that feature
        Map<String, List<String>> byRedefined = new LinkedHashMap<>();
        for (String m : features) {
            for (String redefined : closure(m)) {
                byRedefined.computeIfAbsent(redefined, k -> new ArrayList<>()).add(m);
            }
        }
        Set<String> seenPairs = new HashSet<>();
        for (List<String> group : byRedefined.values()) {
            for (int x = 0; x < group.size(); x++) {
                for (int y = x + 1; y < group.size(); y++) {
                    String g = group.get(x);
                    String h = group.get(y);
                    if (!seenPairs.add(g + "\u0000" + h) || !fromDifferentGenerals(contributors, g, h)
                            || heldByOneGeneral(contributors, byRedefined, g, h)) {
                        continue;
                    }
                    Set<String> cg = closure(g);
                    Set<String> ch = closure(h);
                    if (cg.contains(h) || ch.contains(g) || merged(byRedefined, g, h)) {
                        continue; // one is the more specific redefinition of the other, or a third merges both
                    }
                    String common = mostSpecificCommon(cg, ch);
                    Conflict c = diamonds.computeIfAbsent(common, k -> new Conflict(k, effectiveName(k)));
                    c.features().computeIfAbsent(g, k -> new BitSet()).or(contributors.get(g));
                    c.features().computeIfAbsent(h, k -> new BitSet()).or(contributors.get(h));
                }
            }
        }

        // Name conflicts: unrelated features with the same name
        Map<String, List<String>> byName = new LinkedHashMap<>();
        for (String m : features) {
            String name = effectiveName(m);
            if (name != null) {
                byName.computeIfAbsent(name, k -> new ArrayList<>()).add(m);
            }
        }
        for (Map.Entry<String, List<String>> entry : byName.entrySet()) {
            List<String> group = entry.getValue();
            for (int x = 0; x < group.size(); x++) {
                for (int y = x + 1; y < group.size(); y++) {
                    String g = group.get(x);
                    String h = group.get(y);
                    if (!fromDifferentGenerals(contributors, g, h) || intersects(closure(g), closure(h))
                            || merged(byRedefined, g, h) || heldByOneGeneral(contributors, byRedefined, g, h)
                            || unresolvedChains.contains(g) || unresolvedChains.contains(h)) {
                        continue; // related, or unknown whether they are
                    }
                    Conflict c = nameClashes.computeIfAbsent(entry.getKey(), k -> new Conflict(null, k));
                    c.features().computeIfAbsent(g, k -> new BitSet()).or(contributors.get(g));
                    c.features().computeIfAbsent(h, k -> new BitSet()).or(contributors.get(h));
                }
            }
        }

        List<Conflict> result = new ArrayList<>(diamonds.values());
        result.addAll(nameClashes.values());
        return result;
    }

    /**
     * Classify the redefinitions of one feature that an element inherits along different branches. They
     * contradict when two have different literal values or enumeration literals, unrelated types (neither
     * specializes the other) or multiplicities with no common value, or when their bodies redefine a nested
     * feature in a contradicting way. They are duplicates when they are written the same way (equal numbers
     * counting as the same) and add nothing nested.
     * Anything else (one narrows the type, adds a value or multiplicity the other leaves open, or adds
     * nested features the other lacks) is a compatible refinement, which is not reported.
     *
     * @param reasons receives what contradicts, e.g. "different value"
     */
    private Verdict classify(Collection<String> features, List<String> reasons, int depth) {
        List<IndexedElement> records = new ArrayList<>();
        for (String f : features) {
            IndexedElement r = first(f);
            if (r != null) {
                records.add(r);
            }
        }
        // Only literal values and enumeration literals can be shown to differ ('= mRef.dimensions' and '= 3'
        // may well agree). A redefinition without a value of its own has the values of the features it
        // redefines.
        Set<String> literalValues = new HashSet<>();
        for (String f : features) {
            for (Value value : values(f)) {
                if (value.key() != null) {
                    literalValues.add(value.key());
                }
            }
        }
        if (literalValues.size() > 1) {
            reasons.add("different value");
        }
        if (unrelatedTypes(records)) {
            reasons.add("different type");
        }
        if (disjointMultiplicities(records)) {
            reasons.add("different multiplicity");
        }

        List<String> withBodies = new ArrayList<>();
        for (String f : features) {
            for (IndexedElement owned : index.ownedElements(f)) {
                if (owned.declaresFeature() && owned.effectiveName() != null) {
                    withBodies.add(f);
                    break;
                }
            }
        }
        if (withBodies.size() >= 2 && depth < MAX_NESTING) {
            Set<String> clashing = new TreeSet<>();
            for (Conflict nested : conflicts(withBodies, null)) {
                Verdict verdict = nested.ancestorFeature() == null ? Verdict.CONTRADICTION
                    : classify(nested.features().keySet(), new ArrayList<>(), depth + 1);
                if (verdict == Verdict.CONTRADICTION) {
                    clashing.add("'" + nested.name() + "'");
                }
            }
            if (!clashing.isEmpty()) {
                reasons.add("conflicting nested " + (clashing.size() == 1 ? "feature " : "features ")
                    + joinWithAnd(new ArrayList<>(clashing)));
            }
        }
        if (!reasons.isEmpty()) {
            return Verdict.CONTRADICTION;
        }
        // Redefinitions written the same way repeat each other (equal literals such as '4' and '4.0' count as
        // the same); two that add nothing do not
        Set<String> texts = new HashSet<>();
        for (IndexedElement r : records) {
            String key = r.valueText == null ? null : valueKey(r);
            texts.add(squash(r.typeText) + "\u0000" + squash(r.multiplicityText) + "\u0000"
                + (key != null ? key : squash(r.valueText)));
        }
        boolean empty = texts.equals(Set.of("\u0000\u0000"));
        return texts.size() <= 1 && !empty && withBodies.isEmpty() ? Verdict.DUPLICATE : Verdict.COMPATIBLE;
    }

    private static final Pattern LITERAL_VALUE = Pattern.compile(
        "(?:default\\s*)?(?:=|:=)?\\s*(?:[-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?(?:\\s*\\[[^\\]]*\\])?"
            + "|\"(?:[^\"\\\\]|\\\\.)*\"|true|false|null)");

    /** A value's number and optional unit, after 'default', '=' or ':=' and whitespace are removed. */
    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile(
        "([-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)(\\[[^\\]]*\\])?");

    /** A value that is a name, possibly qualified, e.g. {@code Color::red} or {@code 'Fuel Kind'::diesel}. */
    private static final Pattern NAME_VALUE = Pattern.compile(
        "(?:'(?:[^'\\\\]|\\\\.)*'|[A-Za-z_]\\w*)(?:\\s*::\\s*(?:'(?:[^'\\\\]|\\\\.)*'|[A-Za-z_]\\w*))*");

    /**
     * A value as written, and the key it is compared by when it is a literal or an enumeration literal (null
     * for any other expression, which cannot be compared).
     */
    private record Value(String text, String key) { }

    /** A value that is a number (with an optional unit), a string, a Boolean or null. */
    static boolean isLiteralValue(String valueText) {
        return valueText != null && LITERAL_VALUE.matcher(valueText.trim()).matches();
    }

    /**
     * The key a literal value is compared by, or null when it is not a literal. Numbers are compared by
     * value ({@code 4}, {@code 4.0} and {@code 4e0} are equal), units and strings as written, and defaults
     * like bound values.
     */
    static String literalKey(String valueText) {
        if (!isLiteralValue(valueText)) {
            return null;
        }
        String literal = valueText.trim().replaceFirst("^default\\s*", "").replaceFirst("^:?=\\s*", "");
        if (literal.startsWith("\"")) {
            return literal; // spaces inside a string are part of the value
        }
        literal = squash(literal);
        Matcher number = NUMBER_WITH_UNIT.matcher(literal);
        if (number.matches()) {
            String unit = number.group(2) == null ? "" : number.group(2);
            return new BigDecimal(number.group(1)).stripTrailingZeros() + unit;
        }
        return literal;
    }

    /**
     * The key a feature's own value is compared by: its literal key, or for a name that resolves to
     * enumeration literals (e.g. {@code = Color::red}) the qualified names of those literals; else null.
     */
    private String valueKey(IndexedElement r) {
        String literal = literalKey(r.valueText);
        if (literal != null || r.valueText == null) {
            return literal;
        }
        String name = r.valueText.trim().replaceFirst("^default\\s*", "").replaceFirst("^:?=\\s*", "");
        if (!NAME_VALUE.matcher(name).matches()) {
            return null;
        }
        List<String> segs = new ArrayList<>();
        for (String seg : name.split("::")) {
            segs.add(ModelIndexBuilder.normalize(seg.trim()));
        }
        // Names in a value resolve in the feature (whose types' members it has), else around it
        ModelIndex.Resolution res = index.resolve(segs, r.qualifiedName);
        if (res.getStatus() != ModelIndex.Status.FOUND) {
            return null;
        }
        Set<String> literals = new TreeSet<>(dealias(res.getTargets(), 0));
        for (String target : literals) {
            if (!isEnumerationLiteral(first(target))) {
                return null;
            }
        }
        return literals.isEmpty() ? null : "enum:" + String.join(",", literals);
    }

    /** True for an enumeration literal: a member of an enumeration definition. */
    private boolean isEnumerationLiteral(IndexedElement r) {
        if (r == null || r.kind == null) {
            return false;
        }
        if ("EnumMember".equals(r.kind)) {
            return true;
        }
        IndexedElement owner = r.parentName == null ? null : first(r.parentName);
        return "EnumUsage".equals(r.kind) && owner != null && "EnumDefinition".equals(owner.kind);
    }

    /**
     * The values a feature is given: its own, else (for a redefinition without a value) the values of the
     * features it redefines.
     */
    private Set<Value> values(String feature) {
        Set<Value> cached = valuesMemo.get(feature);
        if (cached != null) {
            return cached;
        }
        valuesMemo.put(feature, Set.of()); // guards a redefinition cycle
        Set<Value> result = new LinkedHashSet<>();
        IndexedElement r = first(feature);
        if (r != null && r.valueText != null) {
            result.add(new Value(r.valueText, valueKey(r)));
        } else if (r != null) {
            String owner = r.parentName == null ? "" : r.parentName;
            for (List<String> segs : r.redefines) {
                for (String target : resolveReference(feature, owner, segs, true)) {
                    if (!target.equals(feature)) {
                        result.addAll(values(target));
                    }
                }
            }
        }
        valuesMemo.put(feature, result);
        return result;
    }

    /** True when two of the features are typed by types neither of which specializes the other. */
    private boolean unrelatedTypes(List<IndexedElement> records) {
        List<Set<String>> typings = new ArrayList<>();
        for (IndexedElement r : records) {
            Set<String> types = types(r);
            if (types != null) {
                typings.add(types);
            }
        }
        for (int x = 0; x < typings.size(); x++) {
            for (int y = x + 1; y < typings.size(); y++) {
                if (!covered(typings.get(x), typings.get(y)) && !covered(typings.get(y), typings.get(x))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True when every type in {@code a} is the same as, a general of, or a specialization of one in {@code b}. */
    private boolean covered(Set<String> a, Set<String> b) {
        for (String x : a) {
            boolean related = false;
            for (String y : b) {
                if (x.equals(y) || ancestors(x).contains(y) || ancestors(y).contains(x)) {
                    related = true;
                    break;
                }
            }
            if (!related) {
                return false;
            }
        }
        return true;
    }

    /** The resolved types a feature is typed by, or null when it has none or one does not resolve. */
    private Set<String> types(IndexedElement r) {
        if (r.typeText == null) {
            return null;
        }
        Set<String> result = new LinkedHashSet<>();
        for (String written : r.typeText.split(",")) {
            String name = written.trim();
            if (name.startsWith("~")) {
                name = name.substring(1);
            }
            List<String> segs = new ArrayList<>();
            for (String seg : name.split("::")) {
                segs.add(ModelIndexBuilder.normalize(seg));
            }
            ModelIndex.Resolution res = index.resolve(segs, r.parentName == null ? "" : r.parentName);
            if (res.getStatus() != ModelIndex.Status.FOUND) {
                return null;
            }
            result.addAll(dealias(res.getTargets(), 0));
        }
        return result;
    }

    /** True when two of the features have multiplicities that share no value, e.g. [1] and [2..*]. */
    private static boolean disjointMultiplicities(List<IndexedElement> records) {
        List<long[]> ranges = new ArrayList<>();
        for (IndexedElement r : records) {
            long[] range = range(r.multiplicityText);
            if (range != null) {
                ranges.add(range);
            }
        }
        for (int x = 0; x < ranges.size(); x++) {
            for (int y = x + 1; y < ranges.size(); y++) {
                if (Math.max(ranges.get(x)[0], ranges.get(y)[0]) > Math.min(ranges.get(x)[1], ranges.get(y)[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Lower and upper bound of a literal multiplicity such as "[1]", "[0..*]" or "[*]"; null otherwise. */
    static long[] range(String multiplicity) {
        if (multiplicity == null || !multiplicity.startsWith("[") || !multiplicity.endsWith("]")) {
            return null;
        }
        String[] bounds = multiplicity.substring(1, multiplicity.length() - 1).split("\\.\\.", -1);
        if (bounds.length > 2) {
            return null;
        }
        Long lower = bound(bounds[0], 0);
        Long upper = bound(bounds[bounds.length - 1], Long.MAX_VALUE);
        if (lower == null || upper == null) {
            return null;
        }
        return new long[] {lower, upper};
    }

    private static Long bound(String text, long star) {
        String t = text.trim();
        if ("*".equals(t)) {
            return star;
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * True when another inherited feature redefines both, as {@code ref self : SuccessionFlow :>> Flow::self,
     * FlowTransferBefore::self;} does: that feature is the more specific one and merges them.
     *
     * @param byRedefined each feature mapped to the inherited features whose redefinition chain contains it
     */
    private static boolean merged(Map<String, List<String>> byRedefined, String g, String h) {
        List<String> overG = byRedefined.getOrDefault(g, List.of());
        List<String> overH = byRedefined.getOrDefault(h, List.of());
        for (String k : overG) {
            if (!k.equals(g) && !k.equals(h) && overH.contains(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when one general contributes one feature and a redefinition of the other: the conflict is that
     * general's own, settled (or reported) where it arose. In the SysML metamodel ConnectionUsage redefines
     * Connector::association but inherits OccurrenceUsage::occurrenceDefinition; a usage typed by
     * InterfaceUsage that also subsets a Connector feature inherits that same pair again.
     */
    private static boolean heldByOneGeneral(Map<String, BitSet> contributors, Map<String, List<String>> byRedefined,
            String g, String h) {
        return redefinedWith(contributors, byRedefined, g, h) || redefinedWith(contributors, byRedefined, h, g);
    }

    private static boolean redefinedWith(Map<String, BitSet> contributors, Map<String, List<String>> byRedefined,
            String redefined, String other) {
        for (String k : byRedefined.getOrDefault(redefined, List.of())) {
            if (!k.equals(redefined) && contributors.get(k).intersects(contributors.get(other))) {
                return true;
            }
        }
        return false;
    }

    /** True when another general specializes general {@code i}, so {@code i} adds nothing of its own. */
    private boolean redundant(List<String> generals, int i) {
        String g = generals.get(i);
        for (int j = 0; j < generals.size(); j++) {
            if (j != i && ancestors(generals.get(j)).contains(g) && !ancestors(g).contains(generals.get(j))) {
                return true;
            }
        }
        return false;
    }

    /** True unless one general contributes both features (the conflict is that general's own). */
    private static boolean fromDifferentGenerals(Map<String, BitSet> contributors, String g, String h) {
        return !contributors.get(g).intersects(contributors.get(h));
    }

    /** The common redefined feature nearest to both: the one whose own chain covers the others. */
    private String mostSpecificCommon(Set<String> a, Set<String> b) {
        String best = null;
        int bestSize = -1;
        for (String f : a) {
            if (b.contains(f)) {
                int size = closure(f).size();
                if (size > bestSize) {
                    best = f;
                    bestSize = size;
                }
            }
        }
        return best;
    }

    private boolean resolved(Conflict c, Set<String> resolvedNames, Set<String> resolvedTargets) {
        Set<String> names = new HashSet<>();
        Set<String> chains = new HashSet<>();
        if (c.name() != null) {
            names.add(c.name());
        }
        if (c.ancestorFeature() != null) {
            chains.add(c.ancestorFeature());
        }
        for (String f : c.features().keySet()) {
            String name = effectiveName(f);
            if (name != null) {
                names.add(name);
            }
            chains.addAll(closure(f));
        }
        return intersects(names, resolvedNames) || intersects(chains, resolvedTargets);
    }

    // ------------------------------------------------------------------------------------------
    // Messages

    private ValidationWarning warning(IndexedElement element, String filePath, List<String> generals, Conflict c,
            List<String> reasons) {
        String d = display(element.qualifiedName);
        List<String> parts = new ArrayList<>();
        Set<String> branches = new LinkedHashSet<>();
        for (Map.Entry<String, BitSet> e : c.features().entrySet()) {
            List<String> via = new ArrayList<>();
            for (int i = e.getValue().nextSetBit(0); i >= 0; i = e.getValue().nextSetBit(i + 1)) {
                via.add("'" + display(generals.get(i)) + "'");
                branches.add("'" + display(generals.get(i)) + "'");
            }
            String detail = describe(e.getKey());
            parts.add("'" + display(e.getKey()) + "' (via " + String.join(", ", via)
                + (detail.isEmpty() ? "" : "; " + detail) + ")");
        }
        String listed = joinWithAnd(parts);
        String branchList = joinWithAnd(new ArrayList<>(branches));
        String name = c.name() == null ? "?" : c.name();
        String example = example(c);
        String all = c.features().size() == 2 ? "both" : "all of them";
        String message;
        String code;
        if (c.ancestorFeature() != null) {
            code = ErrorCodes.DIAMOND_INHERITANCE_CONFLICT;
            String ancestor = display(parentOf(c.ancestorFeature()));
            boolean commonAncestor = isCommonAncestor(parentOf(c.ancestorFeature()), generals, c);
            String origin = "'" + display(c.ancestorFeature()) + "'"
                + (commonAncestor ? " of common ancestor '" + ancestor + "'" : "");
            String verdict = reasons.isEmpty()
                ? "They are duplicates (same type, multiplicity and value), so the model repeats itself."
                : "They contradict (" + String.join(", ", reasons) + ").";
            message = String.format("Diamond inheritance in '%s': %s is redefined along different branches by %s,"
                    + " and '%s' does not redefine '%s', so it inherits %s. %s"
                    + " Refactor: redefine '%s' in '%s' (for example %s) to state which applies, or move the"
                    + " redefinition into a common supertype of %s.",
                d, origin, listed, d, name, all, verdict, name, d, example, branchList);
        } else {
            code = ErrorCodes.MULTIPLE_INHERITANCE_NAME_CONFLICT;
            message = String.format("Multiple inheritance name conflict in '%s': %s each contribute a different"
                    + " feature named '%s' (%s), and neither redefines the other, so '%s' has %s features"
                    + " called '%s'. Refactor: redefine '%s' in '%s' (for example %s), rename one of them, or"
                    + " move '%s' into a common supertype of %s.",
                d, branchList, name, listed, d, c.features().size() == 2 ? "two" : String.valueOf(c.features().size()),
                name, name, d, example, name, branchList);
        }
        return (ValidationWarning) new ValidationWarning.Builder()
            .filePath(filePath)
            .line(element.line)
            .column(element.column)
            .message(message)
            .errorCode(code)
            .build();
    }

    /** A redefinition D could own, e.g. "'attribute :>> mass = ...;'" or "'part :>> engine;'". */
    private String example(Conflict c) {
        String keyword = null;
        boolean valued = false;
        for (String f : c.features().keySet()) {
            IndexedElement r = first(f);
            if (r == null) {
                continue;
            }
            if (keyword == null && r.kind != null && r.kind.endsWith("Usage")) {
                keyword = KEYWORDS.get(r.kind.substring(0, r.kind.length() - "Usage".length()));
            }
            valued |= r.valueText != null;
        }
        String name = c.name() == null ? "?" : sysmlName(c.name());
        return "'" + (keyword == null ? "" : keyword + " ") + ":>> " + name + (valued ? " = ...;'" : ";'");
    }

    /** A name as it is written in SysML: quoted unless it is a basic name ({@code 'max speed'}). */
    private static String sysmlName(String name) {
        return name.matches("[A-Za-z_]\\w*") ? name : "'" + name.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private boolean isCommonAncestor(String ancestor, List<String> generals, Conflict c) {
        for (BitSet via : c.features().values()) {
            for (int i = via.nextSetBit(0); i >= 0; i = via.nextSetBit(i + 1)) {
                String g = generals.get(i);
                if (!g.equals(ancestor) && !ancestors(g).contains(ancestor)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String squash(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "");
    }

    /** Type, multiplicity and value as written, e.g. ": Real [1] = 4"; empty when none is written. */
    private String describe(String feature) {
        IndexedElement r = first(feature);
        if (r == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (r.typeText != null) {
            parts.add(": " + r.typeText);
        }
        if (r.multiplicityText != null) {
            parts.add(r.multiplicityText);
        }
        if (r.valueText != null) {
            parts.add(r.valueText);
        } else if (!values(feature).isEmpty()) {
            List<String> inherited = new ArrayList<>();
            for (Value value : values(feature)) {
                inherited.add(value.text());
            }
            parts.add("inherits " + String.join(" and ", inherited));
        }
        return String.join(" ", parts);
    }

    private static String joinWithAnd(List<String> items) {
        if (items.size() <= 1) {
            return items.isEmpty() ? "" : items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    /** A qualified name for messages, anonymous segments replaced by the name they redefine. */
    private String display(String qualifiedName) {
        IndexedElement r = first(qualifiedName);
        if (r == null) {
            return qualifiedName;
        }
        String own = r.effectiveName() != null ? r.effectiveName() : "<anonymous>";
        String parent = r.parentName;
        return parent == null || parent.isEmpty() ? own : display(parent) + "::" + own;
    }

    // ------------------------------------------------------------------------------------------
    // Inheritance

    /** The resolved general types of an element: typings, specializations, subsettings, redefinitions. */
    private List<String> generals(String qualifiedName) {
        List<String> cached = generalsMemo.get(qualifiedName);
        if (cached != null) {
            return cached;
        }
        if (!generalsInProgress.add(qualifiedName)) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        try {
            for (IndexedElement record : index.recordsOf(qualifiedName)) {
                String owner = record.parentName == null ? "" : record.parentName;
                for (List<String> segs : record.supertypes) {
                    boolean redefinition = record.redefines.contains(segs);
                    Set<String> targets = redefinition || segs.size() != 1 || !record.declaresFeature()
                            || isTyping(record, segs)
                        ? resolveReference(qualifiedName, owner, segs, redefinition)
                        : subsetted(qualifiedName, owner, segs);
                    for (String target : targets) {
                        if (!target.equals(qualifiedName)) {
                            result.add(target);
                        }
                    }
                }
            }
        } finally {
            generalsInProgress.remove(qualifiedName);
        }
        List<String> generals = List.copyOf(result);
        generalsMemo.put(qualifiedName, generals);
        return generals;
    }

    /**
     * The feature a feature subsets by simple name ({@code item tf :> faces}): a member of the owner of that
     * name, which may be an unnamed redefinition ({@code item :>> faces [5..6];}) that hides the inherited
     * feature, else whatever the name resolves to.
     */
    private Set<String> subsetted(String self, String owner, List<String> segs) {
        Set<String> result = new LinkedHashSet<>();
        for (String m : members(owner)) {
            if (!m.equals(self) && segs.get(0).equals(effectiveName(m))) {
                result.add(m);
            }
        }
        return result.isEmpty() ? resolveReference(self, owner, segs, false) : result;
    }

    /** True when the name is one of the element's typings (written after ':'). */
    private static boolean isTyping(IndexedElement record, List<String> segs) {
        if (record.typeText == null) {
            return false;
        }
        String written = String.join("::", segs);
        for (String type : record.typeText.split(",")) {
            String t = type.trim();
            if (ModelIndexBuilder.normalize(t.startsWith("~") ? t.substring(1) : t).equals(written)) {
                return true;
            }
        }
        return false;
    }

    /** Every direct and indirect general of an element. */
    private Set<String> ancestors(String qualifiedName) {
        Set<String> cached = ancestorsMemo.get(qualifiedName);
        if (cached != null) {
            return cached;
        }
        if (!ancestorsInProgress.add(qualifiedName)) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        try {
            for (String general : generals(qualifiedName)) {
                result.add(general);
                result.addAll(ancestors(general));
            }
        } finally {
            ancestorsInProgress.remove(qualifiedName);
        }
        ancestorsMemo.put(qualifiedName, result);
        return result;
    }

    /**
     * The features that are members of a type: its owned features and the non-private features of its
     * generals that none of its owned features redefines (by target or by name).
     */
    private List<String> members(String type) {
        List<String> cached = membersMemo.get(type);
        if (cached != null) {
            return cached;
        }
        if (!membersInProgress.add(type)) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        try {
            Set<String> redefined = new HashSet<>();
            Set<String> redefinedNames = new HashSet<>();
            for (IndexedElement owned : index.ownedElements(type)) {
                if (!owned.declaresFeature() || owned.effectiveName() == null) {
                    continue;
                }
                result.add(owned.qualifiedName);
                for (List<String> segs : owned.redefines) {
                    redefinedNames.add(segs.get(segs.size() - 1));
                }
                for (String r : closure(owned.qualifiedName)) {
                    if (!r.equals(owned.qualifiedName)) {
                        redefined.add(r);
                    }
                }
            }
            for (String general : generals(type)) {
                for (String m : members(general)) {
                    IndexedElement record = first(m);
                    if (record != null && !record.isPrivate && !redefined.contains(m)
                            && !redefinedNames.contains(record.effectiveName())) {
                        result.add(m);
                    }
                }
            }
        } finally {
            membersInProgress.remove(type);
        }
        List<String> members = List.copyOf(result);
        membersMemo.put(type, members);
        return members;
    }

    /**
     * A feature and every feature it redefines, directly or through a chain of redefinitions. A feature
     * whose chain has a redefinition that does not resolve is recorded in {@link #unresolvedChains}.
     */
    private Set<String> closure(String feature) {
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
                    Set<String> targets = resolveReference(feature, owner, segs, true);
                    if (targets.isEmpty()) {
                        unresolvedChains.add(feature);
                    }
                    for (String target : targets) {
                        if (!target.equals(feature)) {
                            result.addAll(closure(target));
                            if (unresolvedChains.contains(target)) {
                                unresolvedChains.add(feature);
                            }
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
     * Resolve a name written in an element's header. A redefinition by simple name names a feature the
     * owner inherits ({@code attribute mass :>> mass;} does not redefine itself), so it is looked up in the
     * owner's generals only; a qualified redefinition ({@code :>> Vehicle::mass}) names the nearest feature
     * of that name in the qualifier, unnamed redefinitions included. Other names resolve as usual, falling
     * back to the generals when they name the element itself. Returns no targets when the name does not
     * resolve.
     */
    private Set<String> resolveReference(String self, String owner, List<String> segs, boolean redefinition) {
        if (segs.isEmpty()) {
            return Set.of();
        }
        String last = segs.get(segs.size() - 1);
        if (redefinition && segs.size() == 1) {
            return dealias(inherited(owner, last, self), 0);
        }
        if (redefinition) {
            ModelIndex.Resolution qualifier = index.resolve(segs.subList(0, segs.size() - 1), owner);
            if (qualifier.getStatus() != ModelIndex.Status.FOUND) {
                return Set.of();
            }
            Set<String> result = new LinkedHashSet<>();
            for (String type : dealias(qualifier.getTargets(), 0)) {
                result.addAll(memberNamed(type, last, self));
            }
            return dealias(result, 0);
        }
        ModelIndex.Resolution r = index.resolve(segs, owner);
        Set<String> targets = r.getStatus() == ModelIndex.Status.FOUND ? r.getTargets() : Set.of();
        if (targets.contains(self) && segs.size() == 1) {
            targets = inherited(owner, segs.get(0), self);
        }
        return dealias(targets, 0);
    }

    /**
     * Features named {@code name} that the owner inherits from its generals: the nearest redefinition on
     * each general (unnamed redefinitions such as {@code :>> f = 1;} included), else whatever the index
     * finds as a member of the general.
     */
    private Set<String> inherited(String owner, String name, String self) {
        Set<String> result = new LinkedHashSet<>();
        for (String general : generals(owner)) {
            result.addAll(memberNamed(general, name, self));
        }
        return result;
    }

    /**
     * The features of a type with the given name: the nearest ones among its members, else whatever the
     * index finds as a member of the type (e.g. a feature hidden by a renaming redefinition).
     */
    private Set<String> memberNamed(String type, String name, String self) {
        Set<String> result = new LinkedHashSet<>();
        for (String m : members(type)) {
            if (!m.equals(self) && name.equals(effectiveName(m))) {
                result.add(m);
            }
        }
        if (result.isEmpty()) {
            ModelIndex.Resolution r = index.resolveMember(type, name);
            if (r.getStatus() == ModelIndex.Status.FOUND) {
                for (String t : r.getTargets()) {
                    if (!t.equals(self)) {
                        result.add(t);
                    }
                }
            }
        }
        return result;
    }

    private Set<String> dealias(Set<String> targets, int depth) {
        Set<String> result = new LinkedHashSet<>();
        for (String t : targets) {
            IndexedElement r = first(t);
            if (r != null && r.aliasTarget != null && depth < MAX_ALIAS_DEPTH) {
                ModelIndex.Resolution target =
                    index.resolve(r.aliasTarget, r.parentName == null ? "" : r.parentName);
                if (target.getStatus() == ModelIndex.Status.FOUND) {
                    result.addAll(dealias(target.getTargets(), depth + 1));
                }
            } else {
                result.add(t);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------

    private IndexedElement first(String qualifiedName) {
        List<IndexedElement> records = index.recordsOf(qualifiedName);
        return records.isEmpty() ? null : records.get(0);
    }

    private String effectiveName(String qualifiedName) {
        IndexedElement r = first(qualifiedName);
        return r == null ? null : r.effectiveName();
    }

    private String parentOf(String qualifiedName) {
        IndexedElement r = first(qualifiedName);
        return r == null || r.parentName == null ? "" : r.parentName;
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        Set<String> small = a.size() <= b.size() ? a : b;
        Set<String> large = small == a ? b : a;
        for (String s : small) {
            if (large.contains(s)) {
                return true;
            }
        }
        return false;
    }
}
