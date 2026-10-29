package com.validator.semantic.index;

import com.validator.parser.SysMLv2Lexer;
import com.validator.parser.SysMLv2ParserFacade;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Replaces private wildcard imports ({@code private import ISQ::*;}, {@code private import SI::**;}) with
 * element imports of exactly the names the file uses through them ({@code private import ISQ::MassValue;}).
 *
 * <p>A wildcard import makes every member of the imported namespace visible, and through public imports
 * (ISQ, SI and others re-export further packages) many more. That is costly for a tool that loads the
 * imports into its context, and it lets a name bind to an unintended element when the intended one is
 * missing. The minimizer resolves every name written in the files with {@link ModelIndex}, finds the
 * import that supplies it ({@link ModelIndex#importUse}), and keeps only those members.</p>
 *
 * <p>Public wildcard imports are left unchanged, because other models may rely on what they re-export.
 * A private wildcard import that supplies no name is removed. Element imports are left unchanged.</p>
 *
 * <p>The rewrite is checked: the files are parsed and indexed again, and every name must resolve to the
 * same elements as before. Names whose resolution was unknown before must not become unresolved.</p>
 */
public final class ImportMinimizer {

    private static final Pattern PLAIN_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** Outcome of a minimization over a set of files. */
    public static final class Result {
        private final Map<File, String> rewritten = new LinkedHashMap<>();
        private final List<String> changes = new ArrayList<>();
        private final List<String> mismatches = new ArrayList<>();
        private int wildcardsBefore;
        private int wildcardsReplaced;
        private int wildcardsRemoved;
        private int elementImportsAdded;
        private int publicWildcardsKept;

        /** New text of each file that changed. */
        public Map<File, String> getRewritten() { return rewritten; }
        /** One line per replaced or removed import. */
        public List<String> getChanges() { return changes; }
        /** Names that would resolve differently after the rewrite (must be empty). */
        public List<String> getMismatches() { return mismatches; }
        public int getWildcardsBefore() { return wildcardsBefore; }
        public int getWildcardsReplaced() { return wildcardsReplaced; }
        public int getWildcardsRemoved() { return wildcardsRemoved; }
        public int getElementImportsAdded() { return elementImportsAdded; }
        public int getPublicWildcardsKept() { return publicWildcardsKept; }
    }

    private ImportMinimizer() {
    }

    /**
     * Plan and check the minimization of the given files.
     *
     * @param files the model files, validated together (names resolve across them)
     * @param libraryIndex the index of the configured libraries
     * @param parser the parser
     * @return the rewritten texts, the changes and any resolution mismatch
     * @throws IOException if a file cannot be read
     */
    public static Result minimize(List<File> files, ModelIndex libraryIndex, SysMLv2ParserFacade parser)
            throws IOException {
        return minimize(files, libraryIndex, parser, false);
    }

    /**
     * Plan and check the minimization of the given files.
     *
     * @param files the model files, validated together (names resolve across them)
     * @param libraryIndex the index of the configured libraries
     * @param parser the parser
     * @param owningNamespace import each element from the namespace that owns it ({@code ISQBase::MassValue})
     *        rather than through the imported namespace's public re-exports ({@code ISQ::MassValue}), so a
     *        tool does not have to load the re-exporting packages to resolve it
     * @return the rewritten texts, the changes and any resolution mismatch
     * @throws IOException if a file cannot be read
     */
    public static Result minimize(List<File> files, ModelIndex libraryIndex, SysMLv2ParserFacade parser,
            boolean owningNamespace) throws IOException {
        Result result = new Result();
        Map<File, String> texts = new LinkedHashMap<>();
        for (File f : files) {
            texts.put(f, Files.readString(f.toPath(), StandardCharsets.UTF_8));
        }

        Indexed before = index(texts, libraryIndex, parser);
        Map<File, List<String>> resolutionsBefore = resolutions(before);

        for (File f : files) {
            ModelIndexBuilder.Result built = before.built.get(f);
            if (built == null) {
                continue;
            }
            String text = texts.get(f);
            String rewritten = rewrite(f, text, built, before.index, result, owningNamespace);
            if (!rewritten.equals(text)) {
                result.rewritten.put(f, rewritten);
            }
        }

        if (!result.rewritten.isEmpty()) {
            Map<File, String> after = new LinkedHashMap<>(texts);
            after.putAll(result.rewritten);
            Indexed check = index(after, libraryIndex, parser);
            Map<File, List<String>> resolutionsAfter = resolutions(check);
            for (File f : files) {
                List<String> a = resolutionsBefore.getOrDefault(f, List.of());
                List<String> b = resolutionsAfter.getOrDefault(f, List.of());
                if (a.size() != b.size()) {
                    result.mismatches.add(f.getName() + ": " + a.size() + " names before, " + b.size() + " after");
                    continue;
                }
                for (int i = 0; i < a.size(); i++) {
                    if (!compatible(a.get(i), b.get(i))) {
                        result.mismatches.add(f.getName() + ": " + a.get(i) + "  ->  " + b.get(i));
                    }
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------

    private static final class Indexed {
        final ModelIndex index;
        final Map<File, ModelIndexBuilder.Result> built = new LinkedHashMap<>();

        Indexed(ModelIndex index) {
            this.index = index;
        }
    }

    private static Indexed index(Map<File, String> texts, ModelIndex libraryIndex, SysMLv2ParserFacade parser) {
        Indexed indexed = new Indexed(new ModelIndex(libraryIndex));
        for (Map.Entry<File, String> e : texts.entrySet()) {
            File f = e.getKey();
            SysMLv2ParserFacade.ParseResult parsed = parser.parseString(e.getValue(), f.getName());
            if (parsed.getParseTree() == null || !parsed.getSyntaxErrors().isEmpty()) {
                continue;
            }
            ModelIndexBuilder.Result built = ModelIndexBuilder.build(parsed.getParseTree(), f.getAbsolutePath(), true);
            indexed.index.addAll(built.getElements());
            indexed.built.put(f, built);
        }
        return indexed;
    }

    /** "line:column name @scope -> STATUS targets" for every name written in each file. */
    private static Map<File, List<String>> resolutions(Indexed indexed) {
        Map<File, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<File, ModelIndexBuilder.Result> e : indexed.built.entrySet()) {
            List<String> list = new ArrayList<>();
            for (ModelIndexBuilder.NameUse use : e.getValue().getUses()) {
                if (use.isGlobal()) {
                    continue;
                }
                ModelIndex.Resolution r = indexed.index.resolve(use.getSegments(), use.getScope());
                list.add(String.join("::", use.getSegments()) + " @" + use.getScope() + " -> " + r.getStatus()
                    + (r.getStatus() == ModelIndex.Status.FOUND ? " " + new TreeSet<>(r.getTargets()) : ""));
            }
            out.put(e.getKey(), list);
        }
        return out;
    }

    /** Same name and scope; same resolution, except that an unknown resolution may become found. */
    private static boolean compatible(String before, String after) {
        if (before.equals(after)) {
            return true;
        }
        int a = before.indexOf(" -> ");
        int b = after.indexOf(" -> ");
        return a > 0 && b > 0 && before.substring(0, a).equals(after.substring(0, b))
            && before.endsWith(" -> UNKNOWN") && after.substring(b).startsWith(" -> FOUND");
    }

    private static String rewrite(File file, String text, ModelIndexBuilder.Result built, ModelIndex index,
            Result result, boolean owningNamespace) {
        Map<IndexedElement.ImportRecord, int[]> spans = built.getImportSpans();
        Map<IndexedElement.ImportRecord, Set<String>> needed = new IdentityHashMap<>();
        for (IndexedElement.ImportRecord rec : spans.keySet()) {
            needed.put(rec, new TreeSet<>());
        }

        for (ModelIndexBuilder.NameUse use : built.getUses()) {
            if (use.isGlobal()) {
                continue;
            }
            String name = use.getSegments().get(0);
            ModelIndex.ImportUse via = index.importUse(name, use.getScope());
            if (via == null || !needed.containsKey(via.getRecord())) {
                continue;
            }
            IndexedElement.ImportRecord rec = via.getRecord();
            if ("ELEMENT".equals(rec.mode)) {
                continue;
            }
            needed.get(rec).add(elementPath(rec, name, via, index, owningNamespace));
        }

        // Rewrite from the end of the file so earlier spans stay valid
        List<IndexedElement.ImportRecord> order = new ArrayList<>(spans.keySet());
        order.sort((x, y) -> Integer.compare(spans.get(y)[0], spans.get(x)[0]));
        StringBuilder out = new StringBuilder(text);
        Set<int[]> done = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IndexedElement.ImportRecord rec : order) {
            if ("ELEMENT".equals(rec.mode)) {
                continue;
            }
            int[] span = spans.get(rec);
            if (!done.add(span)) {
                continue;
            }
            result.wildcardsBefore++;
            if (rec.isPublic) {
                result.publicWildcardsKept++;
                continue;
            }
            int lineStart = text.lastIndexOf('\n', span[0] - 1) + 1;
            String indent = text.substring(lineStart, span[0]);
            boolean ownLine = indent.isBlank();
            String original = text.substring(span[0], span[1] + 1);
            int line = lineNumber(text, span[0]);
            Set<String> paths = needed.get(rec);
            if (paths.isEmpty()) {
                result.wildcardsRemoved++;
                result.changes.add(file.getName() + ":" + line + " removed " + original + " (no name used)");
                int end = span[1] + 1;
                if (ownLine) {
                    int nl = text.indexOf('\n', end);
                    String rest = nl < 0 ? text.substring(end) : text.substring(end, nl);
                    if (rest.isBlank()) {
                        out.delete(lineStart, nl < 0 ? text.length() : nl + 1);
                        continue;
                    }
                }
                out.delete(span[0], end);
                continue;
            }
            List<String> lines = new ArrayList<>();
            for (String p : paths) {
                lines.add("private import " + p + ";");
            }
            String sep = ownLine ? "\n" + indent : " ";
            out.replace(span[0], span[1] + 1, String.join(sep, lines));
            result.wildcardsReplaced++;
            result.elementImportsAdded += lines.size();
            result.changes.add(file.getName() + ":" + line + " " + original + " -> " + paths.size() + " element import(s): "
                + String.join(", ", paths));
        }
        return out.toString();
    }

    /** The qualified name to import for a name supplied by a wildcard import. */
    private static String elementPath(IndexedElement.ImportRecord rec, String name, ModelIndex.ImportUse via,
            ModelIndex index, boolean owningNamespace) {
        if (owningNamespace && via.getTargets().size() == 1) {
            String qn = via.getTargets().iterator().next();
            List<String> segs = splitQualified(qn);
            // Only when the element is owned under its own name (not reached through an alias)
            if (!qn.contains("$anon") && segs.get(segs.size() - 1).equals(name)) {
                return quoteAll(segs);
            }
        }
        List<String> segs = new ArrayList<>(rec.target);
        if ("RECURSIVE".equals(rec.mode)) {
            // A recursive import can supply a nested member; import it by its full qualified name unless it
            // is a direct member of the imported namespace.
            Set<String> targets = index.resolveImportTarget(rec.target, via.getNamespace()).getTargets();
            boolean direct = false;
            for (String t : targets) {
                if (index.resolveMember(t, name).getStatus() == ModelIndex.Status.FOUND) {
                    direct = true;
                    break;
                }
            }
            if (!direct) {
                for (String qn : new TreeSet<>(via.getTargets())) {
                    if (!qn.contains("$anon")) {
                        return quoteAll(splitQualified(qn));
                    }
                }
            }
        }
        segs.add(name);
        return quoteAll(segs);
    }

    private static List<String> splitQualified(String qn) {
        List<String> segs = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < qn.length(); i++) {
            if (i + 1 < qn.length() && qn.charAt(i) == ':' && qn.charAt(i + 1) == ':' && depth == 0) {
                segs.add(qn.substring(start, i));
                start = i + 2;
                i++;
            }
        }
        segs.add(qn.substring(start));
        return segs;
    }

    private static String quoteAll(List<String> segs) {
        List<String> out = new ArrayList<>();
        for (String s : segs) {
            out.add(quote(s));
        }
        return String.join("::", out);
    }

    /** Quote a name that is not a plain identifier or that the lexer treats as a keyword. */
    static String quote(String name) {
        if (PLAIN_NAME.matcher(name).matches() && !isKeyword(name)) {
            return name;
        }
        return "'" + name.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static boolean isKeyword(String name) {
        for (int t = 1; t <= SysMLv2Lexer.VOCABULARY.getMaxTokenType(); t++) {
            String literal = SysMLv2Lexer.VOCABULARY.getLiteralName(t);
            if (literal != null && literal.equals("'" + name + "'")) {
                return true;
            }
        }
        return false;
    }

    private static int lineNumber(String text, int index) {
        int line = 1;
        for (int i = 0; i < index && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
