package com.validator.semantic.index;

import com.validator.parser.SysMLv2Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the name-resolution index of one file from its parse tree.
 *
 * <p>An element is any rule that declares a name (a declaration or usage name, a package, an alias,
 * an enumeration literal) or that has a body without a name (an anonymous element). Names written
 * in an element's header (its typing, specializations, redefinitions) are resolved from the owning
 * namespace; names in its body are resolved from the element itself.</p>
 *
 * <p>With {@code collectSites} the builder also records the names that the validator must resolve:
 * import targets, typings, specializations of definitions and metadata annotations.</p>
 */
public final class ModelIndexBuilder {

    /** Elements and reference sites of one file. */
    public static final class Result {
        private final List<IndexedElement> elements;
        private final List<ReferenceSite> sites;

        Result(List<IndexedElement> elements, List<ReferenceSite> sites) {
            this.elements = elements;
            this.sites = sites;
        }

        public List<IndexedElement> getElements() { return elements; }
        public List<ReferenceSite> getSites() { return sites; }
    }

    private final String fileKey;
    private final boolean collectSites;
    private final Map<String, IndexedElement> elements = new LinkedHashMap<>();
    private final List<ReferenceSite> sites = new ArrayList<>();
    private int anonymousCount;

    private ModelIndexBuilder(String fileKey, boolean collectSites) {
        this.fileKey = fileKey;
        this.collectSites = collectSites;
    }

    /**
     * Index a parse tree.
     *
     * @param tree the file's parse tree
     * @param fileKey a name unique to the file, used to keep anonymous elements of different files apart
     * @param collectSites whether to record the names that must resolve
     * @return the file's elements and, if requested, its reference sites
     */
    public static Result build(ParseTree tree, String fileKey, boolean collectSites) {
        ModelIndexBuilder builder = new ModelIndexBuilder(fileKey, collectSites);
        IndexedElement root = new IndexedElement();
        root.qualifiedName = "";
        root.parentName = null;
        root.kind = "Root";
        builder.elements.put("", root);
        builder.visit(tree, "", null);
        // Imports at the root of a file apply to that file only, not to every root namespace:
        // attach them to the file's top-level elements.
        if (!root.imports.isEmpty()) {
            for (IndexedElement e : builder.elements.values()) {
                if ("".equals(e.parentName)) {
                    for (IndexedElement.ImportRecord imp : root.imports) {
                        e.imports.add(new IndexedElement.ImportRecord(imp.target, imp.mode, false));
                    }
                }
            }
            root.imports.clear();
        }
        return new Result(new ArrayList<>(builder.elements.values()), builder.sites);
    }

    // ------------------------------------------------------------------------------------------

    private void visit(ParseTree node, String scope, String headerOwnerKind) {
        if (!(node instanceof ParserRuleContext ctx)) {
            return;
        }
        if (ctx instanceof SysMLv2Parser.ImportDeclarationContext imp) {
            recordImport(imp, scope);
            return;
        }
        if (collectSites && recordSite(ctx, scope, headerOwnerKind)) {
            return;
        }
        if (ctx instanceof SysMLv2Parser.DocumentationContext || ctx instanceof SysMLv2Parser.CommentContext) {
            return;
        }

        String[] declared = declaredNames(ctx);
        boolean anonymous = declared == null && hasBody(ctx);
        if (declared == null && !anonymous) {
            for (int i = 0; i < ctx.getChildCount(); i++) {
                visit(ctx.getChild(i), scope, headerOwnerKind);
            }
            return;
        }

        IndexedElement element = new IndexedElement();
        String segment = anonymous || declared[0] == null && declared[1] == null
            ? "$anon@" + fileKey + "#" + (++anonymousCount)
            : (declared[0] != null ? declared[0] : declared[1]);
        element.name = anonymous ? null : declared[0];
        element.shortName = anonymous ? null : declared[1];
        element.parentName = scope;
        element.qualifiedName = scope.isEmpty() ? segment : scope + "::" + segment;
        element.kind = kindOf(ctx);
        if (ctx instanceof SysMLv2Parser.AliasDeclarationContext alias && alias.qualifiedName() != null) {
            element.aliasTarget = segments(alias.qualifiedName());
        }
        IndexedElement existing = elements.get(element.qualifiedName);
        if (existing == null) {
            elements.put(element.qualifiedName, element);
        } else {
            element = existing;
        }

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (isBody(child)) {
                visit(child, element.qualifiedName, null);
            } else {
                collectSupertypes(child, element);
                visit(child, scope, element.kind);
            }
        }
    }

    /** Declared name and short name, or null if the rule declares no name. */
    private static String[] declaredNames(ParserRuleContext ctx) {
        if (ctx instanceof SysMLv2Parser.PackageDeclarationContext p) {
            return new String[] {lastName(p.qualifiedName()), shortName(p.shortName())};
        }
        if (ctx instanceof SysMLv2Parser.LibraryPackageDeclarationContext p) {
            return new String[] {lastName(p.qualifiedName()), shortName(p.shortName())};
        }
        if (ctx instanceof SysMLv2Parser.NamespaceDeclarationContext p) {
            return new String[] {lastName(p.qualifiedName()), shortName(p.shortName())};
        }
        if (ctx instanceof SysMLv2Parser.AliasDeclarationContext a) {
            SysMLv2Parser.AliasNameContext an = a.aliasName();
            return an == null ? null : new String[] {an.name() == null ? null : normalize(an.name().getText()),
                shortName(an.shortName())};
        }
        if (ctx instanceof SysMLv2Parser.EnumMemberContext e) {
            return e.name() == null ? null : new String[] {normalize(e.name().getText()), null};
        }
        if (ctx instanceof SysMLv2Parser.SimpleParameterContext p && p.ID() != null) {
            return new String[] {p.ID().getText(), null};
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof SysMLv2Parser.DeclarationNameContext d) {
                return new String[] {d.name() == null ? null : normalize(d.name().getText()), shortName(d.shortName())};
            }
            if (child instanceof SysMLv2Parser.UsageNameContext u) {
                return new String[] {u.name() == null ? null : normalize(u.name().getText()), shortName(u.shortName())};
            }
        }
        return null;
    }

    private static boolean hasBody(ParserRuleContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof ParserRuleContext c && c.getClass().getSimpleName().endsWith("BodyContext")
                    && !(c instanceof SysMLv2Parser.DocumentationBodyContext)
                    && c.getChildCount() > 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBody(ParseTree child) {
        if (!(child instanceof ParserRuleContext c)) {
            return false;
        }
        String n = c.getClass().getSimpleName();
        return (n.endsWith("BodyContext") && !(c instanceof SysMLv2Parser.DocumentationBodyContext))
            || n.endsWith("BodyElementContext")
            || c instanceof SysMLv2Parser.StatementContext
            || c instanceof SysMLv2Parser.NamespaceBodyElementContext;
    }

    private static String kindOf(ParserRuleContext ctx) {
        String n = ctx.getClass().getSimpleName();
        return n.endsWith("Context") ? n.substring(0, n.length() - "Context".length()) : n;
    }

    /** Typings, specializations, subsettings, redefinitions and references in an element's header. */
    private void collectSupertypes(ParseTree node, IndexedElement element) {
        if (!(node instanceof ParserRuleContext ctx) || isBody(ctx) || declaredNames(ctx) != null) {
            return;
        }
        if (ctx instanceof SysMLv2Parser.ExpressionContext) {
            return;
        }
        boolean clause = ctx instanceof SysMLv2Parser.TypingClauseContext
            || ctx instanceof SysMLv2Parser.SpecializesClauseContext
            || ctx instanceof SysMLv2Parser.RedefinesClauseContext
            || ctx instanceof SysMLv2Parser.SubsetsClauseContext
            || ctx instanceof SysMLv2Parser.ReferencesClauseContext
            || ctx instanceof SysMLv2Parser.ConjugatesClauseContext
            || ctx instanceof SysMLv2Parser.RedefinedListContext;
        if (clause) {
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);
                if (child instanceof SysMLv2Parser.QualifiedNameContext q) {
                    element.supertypes.add(segments(q));
                } else if (child instanceof SysMLv2Parser.FeatureChainContext f) {
                    if (f.DOT().isEmpty()) {
                        List<String> segs = new ArrayList<>();
                        for (SysMLv2Parser.NameContext n : f.name()) {
                            segs.add(normalize(n.getText()));
                        }
                        element.supertypes.add(segs);
                    } else {
                        element.opaqueSupertypes = true;
                    }
                }
            }
            return;
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            collectSupertypes(ctx.getChild(i), element);
        }
    }

    private void recordImport(SysMLv2Parser.ImportDeclarationContext imp, String scope) {
        SysMLv2Parser.QualifiedNameWithWildcardContext target = imp.qualifiedNameWithWildcard();
        if (target == null || target.qualifiedName() == null) {
            return;
        }
        List<String> segs = segments(target.qualifiedName());
        String mode = "ELEMENT";
        if (target.DOUBLE_STAR() != null || target.getChildCount() > 3) {
            mode = "RECURSIVE";
        } else if (target.STAR() != null) {
            mode = "MEMBERS";
        }
        boolean isPrivate = imp.visibility() != null && imp.visibility().PRIVATE() != null;
        if (imp.getParent() instanceof SysMLv2Parser.NamespaceBodyElementContext nbe && nbe.visibility() != null) {
            isPrivate = nbe.visibility().PRIVATE() != null;
        }
        IndexedElement owner = elements.get(scope);
        if (owner != null) {
            owner.imports.add(new IndexedElement.ImportRecord(segs, mode, !isPrivate));
        }
        if (collectSites) {
            Token start = target.getStart();
            sites.add(new ReferenceSite(ReferenceSite.Kind.IMPORT, segs, target.getText(), scope,
                start.getLine(), start.getCharPositionInLine()));
        }
    }

    /** Records reference sites; returns true when the node is fully handled. */
    private boolean recordSite(ParserRuleContext ctx, String scope, String headerOwnerKind) {
        if (ctx instanceof SysMLv2Parser.TypingClauseContext typing) {
            for (SysMLv2Parser.QualifiedNameContext q : typing.qualifiedName()) {
                addSite(ReferenceSite.Kind.TYPE, q, scope);
            }
            return true;
        }
        if (ctx instanceof SysMLv2Parser.SpecializesClauseContext spec && isClassifierKind(headerOwnerKind)) {
            for (SysMLv2Parser.QualifiedNameContext q : spec.qualifiedName()) {
                addSite(ReferenceSite.Kind.SPECIALIZATION, q, scope);
            }
            for (SysMLv2Parser.FeatureChainContext f : spec.featureChain()) {
                if (f.DOT().isEmpty()) {
                    List<String> segs = new ArrayList<>();
                    for (SysMLv2Parser.NameContext n : f.name()) {
                        segs.add(normalize(n.getText()));
                    }
                    Token start = f.getStart();
                    sites.add(new ReferenceSite(ReferenceSite.Kind.SPECIALIZATION, segs, f.getText(), scope,
                        start.getLine(), start.getCharPositionInLine()));
                }
            }
            return true;
        }
        if (ctx instanceof SysMLv2Parser.MetadataAnnotationContext meta && !meta.qualifiedName().isEmpty()) {
            addSite(ReferenceSite.Kind.METADATA, meta.qualifiedName(0), scope);
            return false;
        }
        if (ctx instanceof SysMLv2Parser.HashAnnotationContext hash && hash.qualifiedName() != null) {
            addSite(ReferenceSite.Kind.METADATA, hash.qualifiedName(), scope);
            return true;
        }
        return false;
    }

    /** Definitions (classifiers), whose ':>' names a definition; feature-like KerML rules excluded. */
    private static boolean isClassifierKind(String kind) {
        return kind != null && kind.endsWith("Definition")
            && !kind.equals("FeatureDefinition")
            && !kind.equals("ConnectorDefinition")
            && !kind.equals("BindingConnectorDefinition");
    }

    private void addSite(ReferenceSite.Kind kind, SysMLv2Parser.QualifiedNameContext q, String scope) {
        Token start = q.getStart();
        sites.add(new ReferenceSite(kind, segments(q), q.getText(), scope, start.getLine(), start.getCharPositionInLine()));
    }

    // ------------------------------------------------------------------------------------------

    static List<String> segments(SysMLv2Parser.QualifiedNameContext q) {
        List<String> segs = new ArrayList<>();
        for (SysMLv2Parser.NameContext n : q.name()) {
            segs.add(normalize(n.getText()));
        }
        return segs;
    }

    private static String lastName(SysMLv2Parser.QualifiedNameContext q) {
        if (q == null || q.name().isEmpty()) {
            return null;
        }
        return normalize(q.name(q.name().size() - 1).getText());
    }

    private static String shortName(SysMLv2Parser.ShortNameContext s) {
        return s == null || s.name() == null ? null : normalize(s.name().getText());
    }

    /** Strip the quotes of an unrestricted name: 'sim config' and sim config are the same name. */
    public static String normalize(String name) {
        if (name != null && name.length() >= 2 && name.startsWith("'") && name.endsWith("'")) {
            return name.substring(1, name.length() - 1).replace("\\'", "'").replace("\\\\", "\\");
        }
        return name;
    }
}
