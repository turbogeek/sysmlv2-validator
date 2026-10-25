package com.validator.semantic.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Name resolution over the indexed elements of the libraries and the validated files.
 *
 * <p>Resolution follows SysML v2 / KerML visibility: a simple name is looked up in the current
 * namespace and then in each enclosing namespace up to the root. In each namespace the candidates are
 * its owned members (by name or short name), the members it imports and the members it inherits from
 * its supertypes. A qualified name resolves its first segment that way and each further segment as a
 * member of the previous one (including public imports and inherited members).</p>
 *
 * <p>Resolution is three-valued. When the answer depends on something the index cannot see (a
 * supertype given by a feature chain, a supertype that itself does not resolve, a lookup that depends
 * on itself) the result is {@link Status#UNKNOWN} and the validator reports nothing. Only
 * {@link Status#NOT_FOUND} is an error. Not modelled, so never reported: the implicit library
 * supertypes of untyped elements (Part, Item, Anything, ...), visibility of private members.</p>
 *
 * <p>An import whose target does not exist contributes nothing (it is reported on its own). The
 * target of a namespace's k-th import is resolved with only that namespace's earlier imports
 * ({@code import A::*; import B::*;} may name a member of A as B, as the Dassault libraries do), so
 * an import never depends on itself or on a later sibling.</p>
 *
 * <p>Results are memoized. A result computed while an import target was still being resolved is
 * not memoized, because it may have missed members that import provides.</p>
 *
 * <p>An index can be layered on a parent (the library index) so validated files are added without
 * copying the library.</p>
 */
public final class ModelIndex {

    /** Outcome of a resolution. */
    public enum Status { FOUND, NOT_FOUND, UNKNOWN }

    /** Result of resolving a name. */
    public static final class Resolution {
        private final Status status;
        private final Set<String> targets;
        private final int failedSegment;

        Resolution(Status status, Set<String> targets, int failedSegment) {
            this.status = status;
            this.targets = targets;
            this.failedSegment = failedSegment;
        }

        public Status getStatus() { return status; }
        /** Qualified names of the resolved elements (FOUND only). */
        public Set<String> getTargets() { return targets; }
        /** Index of the first segment that did not resolve (NOT_FOUND only). */
        public int getFailedSegment() { return failedSegment; }

        static Resolution found(Set<String> targets) { return new Resolution(Status.FOUND, targets, -1); }
        static Resolution notFound(int segment) { return new Resolution(Status.NOT_FOUND, Set.of(), segment); }
        static Resolution unknown() { return new Resolution(Status.UNKNOWN, Set.of(), -1); }
    }

    private static final int MAX_DEPTH = 96;

    private final ModelIndex parent;
    private final Map<String, List<IndexedElement>> byName = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> members = new HashMap<>();
    private final Map<String, List<String>> bySimpleName = new HashMap<>();
    private final Map<String, List<IndexedElement>> children = new HashMap<>();

    private final Map<String, Resolution> memberMemo = new HashMap<>();
    private final Map<IndexedElement.ImportRecord, Resolution> importMemo = new IdentityHashMap<>();
    private final Map<List<String>, Map<String, Resolution>> qualifiedMemo = new HashMap<>();

    // State of the resolution in progress (callers are synchronized)
    private final Set<String> lookupsInProgress = new HashSet<>();
    private final Set<IndexedElement.ImportRecord> importsInProgress =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private int importCycleCuts;
    /** Namespace -> number of its imports usable while one of its import targets is resolved. */
    private final Map<String, Integer> importLimits = new HashMap<>();
    private String limitsKey = "";

    public ModelIndex() {
        this(null);
    }

    public ModelIndex(ModelIndex parent) {
        this.parent = parent;
    }

    /** Add the elements of one file. */
    public synchronized void addAll(Collection<IndexedElement> elements) {
        if (elements == null) {
            return;
        }
        memberMemo.clear();
        importMemo.clear();
        qualifiedMemo.clear();
        for (IndexedElement e : elements) {
            byName.computeIfAbsent(e.qualifiedName, k -> new ArrayList<>()).add(e);
            if (e.parentName == null) {
                continue;
            }
            children.computeIfAbsent(e.parentName, k -> new ArrayList<>()).add(e);
            Map<String, Set<String>> owned = members.computeIfAbsent(e.parentName, k -> new HashMap<>());
            if (e.name != null) {
                owned.computeIfAbsent(e.name, k -> new LinkedHashSet<>()).add(e.qualifiedName);
                bySimpleName.computeIfAbsent(e.name, k -> new ArrayList<>()).add(e.qualifiedName);
            }
            if (e.shortName != null) {
                owned.computeIfAbsent(e.shortName, k -> new LinkedHashSet<>()).add(e.qualifiedName);
                bySimpleName.computeIfAbsent(e.shortName, k -> new ArrayList<>()).add(e.qualifiedName);
            }
        }
    }

    /** Number of distinct qualified names in this layer and its parents. */
    public int size() {
        return byName.size() + (parent == null ? 0 : parent.size());
    }

    /**
     * Resolve a (possibly qualified) name from a namespace.
     *
     * @param segments the name, one segment per name, without quotes
     * @param scope qualified name of the namespace the name appears in ("" for the root)
     * @return the resolution
     */
    public synchronized Resolution resolve(List<String> segments, String scope) {
        return resolveQualified(segments, scope, 0);
    }

    /**
     * Resolve the target of an import owned by {@code scope}. The import itself does not take part.
     */
    public synchronized Resolution resolveImportTarget(List<String> segments, String scope) {
        for (IndexedElement record : records(scope)) {
            for (IndexedElement.ImportRecord imp : record.imports) {
                if (imp.target.equals(segments)) {
                    return importTarget(imp, scope, 0);
                }
            }
        }
        return resolveQualified(segments, scope, 0);
    }

    /**
     * Resolve a name as a member of a namespace: owned, publicly imported or inherited, but not found in
     * enclosing namespaces. Used to find the feature that a redefinition names through the generals.
     */
    public synchronized Resolution resolveMember(String namespace, String name) {
        return lookupMember(namespace, name, false, 0);
    }

    /**
     * True when this layer, not only a parent layer, has the element: it belongs to the validated files
     * rather than to a library.
     */
    public synchronized boolean isLocal(String qualifiedName) {
        return byName.containsKey(qualifiedName);
    }

    /** The records of an element (several when a namespace is declared more than once). */
    public synchronized List<IndexedElement> recordsOf(String qualifiedName) {
        return records(qualifiedName);
    }

    /** The elements a namespace owns, named or anonymous, in this layer and its parents. */
    public synchronized List<IndexedElement> ownedElements(String namespace) {
        List<IndexedElement> result = null;
        for (ModelIndex layer = this; layer != null; layer = layer.parent) {
            List<IndexedElement> c = layer.children.get(namespace);
            if (c != null) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.addAll(c);
            }
        }
        return result == null ? List.of() : result;
    }

    // ------------------------------------------------------------------------------------------

    private Resolution resolveQualified(List<String> segments, String scope, int depth) {
        if (segments.isEmpty()) {
            return Resolution.unknown();
        }
        String scopeKey = scope + "\u0003" + limitsKey;
        Map<String, Resolution> byScope = qualifiedMemo.get(segments);
        if (byScope != null) {
            Resolution cached = byScope.get(scopeKey);
            if (cached != null) {
                return cached;
            }
        }
        int cutsBefore = importCycleCuts;
        Resolution result = resolveQualifiedUncached(segments, scope, depth);
        if (importCycleCuts == cutsBefore) {
            qualifiedMemo.computeIfAbsent(segments, k -> new HashMap<>()).put(scopeKey, result);
        }
        return result;
    }

    private Resolution resolveQualifiedUncached(List<String> segments, String scope, int depth) {
        Resolution first = resolveSimple(segments.get(0), scope, depth + 1);
        if (first.status != Status.FOUND) {
            return first.status == Status.NOT_FOUND ? Resolution.notFound(0) : first;
        }
        Set<String> current = first.targets;
        for (int i = 1; i < segments.size(); i++) {
            Set<String> next = new LinkedHashSet<>();
            boolean unknown = false;
            for (String namespace : current) {
                Set<String> actuals = dealias(namespace, depth + 1);
                if (actuals == null) {
                    unknown = true;
                    continue;
                }
                for (String actual : actuals) {
                    Resolution r = lookupMember(actual, segments.get(i), false, depth + 1);
                    if (r.status == Status.FOUND) {
                        next.addAll(r.targets);
                    } else if (r.status == Status.UNKNOWN) {
                        unknown = true;
                    }
                }
            }
            if (next.isEmpty()) {
                return unknown ? Resolution.unknown() : Resolution.notFound(i);
            }
            current = next;
        }
        return Resolution.found(current);
    }

    private Resolution resolveSimple(String name, String scope, int depth) {
        boolean unknown = false;
        String namespace = scope;
        int guard = 0;
        while (namespace != null && guard++ < 256) {
            Resolution r = lookupMember(namespace, name, true, depth + 1);
            if (r.status == Status.FOUND) {
                return r;
            }
            if (r.status == Status.UNKNOWN) {
                unknown = true;
            }
            namespace = namespace.isEmpty() ? null : parentOf(namespace);
        }
        return unknown ? Resolution.unknown() : Resolution.notFound(0);
    }

    /**
     * Members of a namespace with the given name: owned, imported and inherited.
     *
     * @param includePrivateImports whether private imports count (true inside the namespace itself)
     */
    private Resolution lookupMember(String namespace, String name, boolean includePrivateImports, int depth) {
        if (depth > MAX_DEPTH) {
            return Resolution.unknown();
        }
        String key = namespace + "\u0001" + name + "\u0001" + includePrivateImports + "\u0001" + limitsKey;
        Resolution cached = memberMemo.get(key);
        if (cached != null) {
            return cached;
        }
        if (!lookupsInProgress.add(key)) {
            // The lookup depends on itself (an inheritance cycle): nothing can be concluded.
            return Resolution.unknown();
        }
        int cutsBefore = importCycleCuts;
        Resolution result;
        try {
            result = lookupMemberUncached(namespace, name, includePrivateImports, depth);
        } finally {
            lookupsInProgress.remove(key);
        }
        if (importCycleCuts == cutsBefore) {
            memberMemo.put(key, result);
        }
        return result;
    }

    private Resolution lookupMemberUncached(String namespace, String name, boolean includePrivateImports, int depth) {
        Set<String> owned = owned(namespace, name);
        if (!owned.isEmpty()) {
            return Resolution.found(owned);
        }
        boolean unknown = false;
        List<IndexedElement> records = records(namespace);

        Integer limit = importLimits.get(namespace);
        int position = 0;
        for (IndexedElement record : records) {
            for (IndexedElement.ImportRecord imp : record.imports) {
                if (limit != null && position++ >= limit) {
                    continue;
                }
                if (!includePrivateImports && !imp.isPublic) {
                    continue;
                }
                Resolution r = viaImport(namespace, imp, name, depth + 1);
                if (r.status == Status.FOUND) {
                    return r;
                }
                if (r.status == Status.UNKNOWN) {
                    unknown = true;
                }
            }
        }

        for (IndexedElement record : records) {
            if (record.opaqueSupertypes) {
                unknown = true;
            }
            String owner = record.parentName == null ? "" : record.parentName;
            for (List<String> supertype : record.supertypes) {
                Resolution st = resolveQualified(supertype, owner, depth + 1);
                if (st.status != Status.FOUND) {
                    unknown = true;
                    continue;
                }
                for (String type : st.targets) {
                    if (type.equals(namespace)) {
                        continue;
                    }
                    Resolution r = lookupMember(type, name, false, depth + 1);
                    if (r.status == Status.FOUND) {
                        return r;
                    }
                    if (r.status == Status.UNKNOWN) {
                        unknown = true;
                    }
                }
            }
            if (record.aliasTarget != null) {
                Resolution target = resolveQualified(record.aliasTarget, owner, depth + 1);
                if (target.status != Status.FOUND) {
                    unknown = true;
                    continue;
                }
                for (String t : target.targets) {
                    Resolution r = lookupMember(t, name, false, depth + 1);
                    if (r.status == Status.FOUND) {
                        return r;
                    }
                    if (r.status == Status.UNKNOWN) {
                        unknown = true;
                    }
                }
            }
        }
        return unknown ? Resolution.unknown() : Resolution.notFound(0);
    }

    private Resolution viaImport(String namespace, IndexedElement.ImportRecord imp, String name, int depth) {
        if ("ELEMENT".equals(imp.mode)) {
            if (imp.target.isEmpty() || !imp.target.get(imp.target.size() - 1).equals(name)) {
                return Resolution.notFound(0);
            }
            Resolution r = importTarget(imp, namespace, depth + 1);
            return r.status == Status.NOT_FOUND ? Resolution.notFound(0) : r;
        }
        Resolution target = importTarget(imp, namespace, depth + 1);
        if (target.status != Status.FOUND) {
            // An import whose target does not exist is reported on its own; it contributes nothing.
            return target.status == Status.UNKNOWN ? target : Resolution.notFound(0);
        }
        boolean unknown = false;
        for (String t : target.targets) {
            Set<String> actuals = dealias(t, depth + 1);
            if (actuals == null) {
                unknown = true;
                continue;
            }
            for (String actual : actuals) {
                Resolution r = lookupMember(actual, name, false, depth + 1);
                if (r.status == Status.FOUND) {
                    return r;
                }
                if (r.status == Status.UNKNOWN) {
                    unknown = true;
                }
                if ("RECURSIVE".equals(imp.mode)) {
                    Set<String> nested = descendantsNamed(actual, name);
                    if (!nested.isEmpty()) {
                        return Resolution.found(nested);
                    }
                }
            }
        }
        return unknown ? Resolution.unknown() : Resolution.notFound(0);
    }

    /** The target of an import, resolved once, without the import itself. */
    private Resolution importTarget(IndexedElement.ImportRecord imp, String namespace, int depth) {
        Resolution cached = importMemo.get(imp);
        if (cached != null) {
            return cached;
        }
        if (!importsInProgress.add(imp)) {
            // The import is being resolved: it cannot contribute to its own target.
            importCycleCuts++;
            return Resolution.notFound(0);
        }
        int cutsBefore = importCycleCuts;
        Integer previousLimit = importLimits.get(namespace);
        String previousKey = limitsKey;
        int position = importPosition(namespace, imp);
        importLimits.put(namespace, previousLimit == null ? position : Math.min(previousLimit, position));
        limitsKey = limitsKey + "|" + namespace + "#" + importLimits.get(namespace);
        Resolution result;
        try {
            result = resolveQualifiedUncached(imp.target, namespace, depth + 1);
        } finally {
            importsInProgress.remove(imp);
            if (previousLimit == null) {
                importLimits.remove(namespace);
            } else {
                importLimits.put(namespace, previousLimit);
            }
            limitsKey = previousKey;
        }
        if (importCycleCuts == cutsBefore || importsInProgress.isEmpty()) {
            importMemo.put(imp, result);
        }
        return result;
    }

    private int importPosition(String namespace, IndexedElement.ImportRecord target) {
        int position = 0;
        for (IndexedElement record : records(namespace)) {
            for (IndexedElement.ImportRecord imp : record.imports) {
                if (imp == target) {
                    return position;
                }
                position++;
            }
        }
        return position;
    }

    /** The elements an alias stands for (the element itself if not an alias); null if unknown. */
    private Set<String> dealias(String qualifiedName, int depth) {
        for (IndexedElement record : records(qualifiedName)) {
            if (record.aliasTarget != null) {
                Resolution r = resolveQualified(record.aliasTarget,
                    record.parentName == null ? "" : record.parentName, depth + 1);
                return r.status == Status.FOUND ? r.targets : null;
            }
        }
        return Set.of(qualifiedName);
    }

    // ------------------------------------------------------------------------------------------

    private Set<String> owned(String namespace, String name) {
        Set<String> result = null;
        for (ModelIndex layer = this; layer != null; layer = layer.parent) {
            Map<String, Set<String>> m = layer.members.get(namespace);
            if (m != null) {
                Set<String> s = m.get(name);
                if (s != null) {
                    if (result == null) {
                        result = new LinkedHashSet<>();
                    }
                    result.addAll(s);
                }
            }
        }
        return result == null ? Set.of() : result;
    }

    private List<IndexedElement> records(String qualifiedName) {
        List<IndexedElement> result = null;
        for (ModelIndex layer = this; layer != null; layer = layer.parent) {
            List<IndexedElement> r = layer.byName.get(qualifiedName);
            if (r != null) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.addAll(r);
            }
        }
        return result == null ? List.of() : result;
    }

    private Set<String> descendantsNamed(String namespace, String name) {
        Set<String> result = new LinkedHashSet<>();
        String prefix = namespace.isEmpty() ? "" : namespace + "::";
        for (ModelIndex layer = this; layer != null; layer = layer.parent) {
            List<String> candidates = layer.bySimpleName.get(name);
            if (candidates != null) {
                for (String qn : candidates) {
                    if (qn.startsWith(prefix)) {
                        result.add(qn);
                    }
                }
            }
        }
        return result;
    }

    private String parentOf(String qualifiedName) {
        for (IndexedElement record : records(qualifiedName)) {
            return record.parentName == null ? "" : record.parentName;
        }
        int i = qualifiedName.lastIndexOf("::");
        return i < 0 ? "" : qualifiedName.substring(0, i);
    }
}
