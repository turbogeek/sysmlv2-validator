package com.validator.semantic.index;

import com.validator.parser.SysMLv2Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the name-resolution index of one file from its parse tree.
 *
 * <p>An element is any rule that declares a name (a declaration or usage name, a package, an alias,
 * an enumeration literal) or that has a body without a name (an anonymous element). A feature without
 * a name that redefines another ({@code attribute :>> mass = 5;}) is an anonymous element too. Names
 * written in an element's header (its typing, specializations, redefinitions) are resolved from the
 * owning namespace; names in its body are resolved from the element itself.</p>
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
    /** In a .kerml file KerML keywords are reserved, so they are never names. */
    private final boolean kerml;
    private final boolean collectSites;
    private final Map<String, IndexedElement> elements = new LinkedHashMap<>();
    private final List<ReferenceSite> sites = new ArrayList<>();
    private int anonymousCount;
    /** The last declaration, when it ended without ';' or '}' (its header may continue in the next rule). */
    private IndexedElement unterminated;
    private Token unterminatedStop;

    private ModelIndexBuilder(String fileKey, boolean collectSites) {
        this.fileKey = fileKey;
        this.kerml = fileKey != null && fileKey.endsWith(".kerml");
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
        if (declared != null && misreadAsName(ctx)) {
            declared = declared[1] == null ? null : new String[] {null, declared[1]};
        }
        // An unnamed redefinition ('attribute :>> mass = 5;') is a feature named after what it redefines
        boolean anonymous = declared == null && (hasBody(ctx) || isUnnamedRedefinition(ctx));
        int parameterIndex = recordParameter(ctx, scope, declared);
        if (declared == null && !anonymous) {
            for (int i = 0; i < ctx.getChildCount(); i++) {
                visit(ctx.getChild(i), scope, headerOwnerKind);
            }
            return;
        }

        IndexedElement continued = continuedDeclaration(ctx, scope, declared);
        IndexedElement crossOf = continued == null ? crossFeatureOwner(ctx, scope, declared) : null;
        unterminated = null;
        if (crossOf != null) {
            scope = crossOf.qualifiedName;
        }
        if (continued != null) {
            // The redefinition continues the previous declaration's header: collect it there
            collectHeaderAndBody(ctx, continued, scope);
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
        Token at = nameToken(ctx);
        element.line = at.getLine();
        element.column = at.getCharPositionInLine();
        element.parameter = isParameter(ctx);
        element.direction = directionOf(ctx);
        element.parameterIndex = parameterIndex;
        element.reference = ctx instanceof SysMLv2Parser.OccurrenceUsageContext occurrence
            && occurrence.EVENT() != null && occurrence.OCCURRENCE() == null;
        element.isPrivate = isPrivate(ctx);
        if (ctx instanceof SysMLv2Parser.AliasDeclarationContext alias && alias.qualifiedName() != null) {
            element.aliasTarget = segments(alias.qualifiedName());
        }
        IndexedElement existing = elements.get(element.qualifiedName);
        if (existing == null) {
            elements.put(element.qualifiedName, element);
        } else {
            // A second member with the same name in the same namespace: kept as one record, noted for the
            // name-collision check
            existing.redeclarations.add(new IndexedElement.Declaration(element.line, element.column, element.kind));
            element = existing;
        }
        collectHeaderAndBody(ctx, element, scope);
        Token stop = ctx.getStop();
        if (stop != null && stop.getType() != SysMLv2Parser.SEMICOLON && stop.getType() != SysMLv2Parser.RBRACE
                && element.declaresFeature()) {
            unterminated = element;
            unterminatedStop = stop;
        }
    }

    /**
     * The end feature whose cross feature this declaration is. The grammar can end {@code end inCart1 [0..1]
     * item cart : ShoppingCart[1];} after the end's name, so {@code item cart ...} parses as a separate member;
     * in KerML it is owned by the end feature, not by the end's owner.
     */
    private IndexedElement crossFeatureOwner(ParserRuleContext ctx, String scope, String[] declared) {
        Token start = ctx.getStart();
        if (unterminated == null || declared == null || !"EndUsage".equals(unterminated.kind)
                || !scope.equals(unterminated.parentName) || start == null || start.getInputStream() == null
                || start.getStartIndex() <= unterminatedStop.getStopIndex()) {
            return null;
        }
        String between = start.getInputStream()
            .getText(Interval.of(unterminatedStop.getStopIndex() + 1, start.getStartIndex() - 1));
        return between.isBlank() ? unterminated : null;
    }

    /**
     * Note a directed parameter, named or not, on its owner: parameters redefine by position.
     *
     * @return the parameter's position, or -1 if the declaration is not a directed parameter
     */
    private int recordParameter(ParserRuleContext ctx, String scope, String[] declared) {
        String name = ctx.getClass().getSimpleName();
        if (!name.endsWith("UsageContext") && !name.endsWith("ParameterContext") || directionOf(ctx) == null) {
            return -1;
        }
        IndexedElement owner = elements.get(scope);
        if (owner == null) {
            return -1;
        }
        owner.parameterNames.add(declared == null ? "" : declared[0] != null ? declared[0]
            : declared[1] != null ? declared[1] : "");
        return owner.parameterNames.size() - 1;
    }

    /**
     * The declaration that an unnamed redefinition continues. The grammar can end a declaration that has no
     * ';' or body early ({@code ref constraint self : ConstraintCheck :>> BooleanEvaluation::self;} parses as
     * {@code self : ConstraintCheck} followed by {@code :>> BooleanEvaluation::self;}); the two are one
     * declaration when only whitespace separates them.
     */
    private IndexedElement continuedDeclaration(ParserRuleContext ctx, String scope, String[] declared) {
        Token start = ctx.getStart();
        if (unterminated == null || declared != null || !scope.equals(unterminated.parentName) || start == null
                || start.getType() != SysMLv2Parser.REDEFINES_OP && start.getType() != SysMLv2Parser.REDEFINES) {
            return null;
        }
        if (start.getInputStream() == null || start.getStartIndex() <= unterminatedStop.getStopIndex()) {
            return null;
        }
        String between = start.getInputStream()
            .getText(Interval.of(unterminatedStop.getStopIndex() + 1, start.getStartIndex() - 1));
        return between.isBlank() ? unterminated : null;
    }

    private void collectHeaderAndBody(ParserRuleContext ctx, IndexedElement element, String scope) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (isBody(child)) {
                visit(child, element.qualifiedName, null);
            } else {
                if (child instanceof SysMLv2Parser.QualifiedNameContext q && i > 0
                        && isRedefinesToken(ctx.getChild(i - 1))) {
                    // 'attribute redefines mass = 5;' and 'feature redefines f;' name the feature directly
                    element.supertypes.add(segments(q));
                    element.redefines.add(segments(q));
                }
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

    /**
     * A declared name that the grammar's choice of alternative gives to a keyword or to the first name of
     * a statement:
     * <ul>
     *   <li>in KerML, {@code feature redefines self : T;}, {@code inv { ... }} and {@code step :> s;} are
     *       unnamed features introduced by a keyword, but parse as a feature named after the keyword;</li>
     *   <li>{@code succession a then b;} parses as a succession named {@code a} with nothing else,
     *       followed by a separate {@code then b;}.</li>
     * </ul>
     */
    private boolean misreadAsName(ParserRuleContext ctx) {
        if (kerml && ctx instanceof SysMLv2Parser.ImplicitFeatureDeclarationContext f
                && f.declarationName() != null && f.declarationName().name() != null) {
            SysMLv2Parser.NameContext n = f.declarationName().name();
            return n.kermlOnlyKeyword() != null && n.getStart() == f.getStart();
        }
        if (ctx instanceof SysMLv2Parser.SuccessionUsageContext succession && succession.usageName() != null) {
            return succession.featureRelationships() == null && succession.successionEndpoints() == null
                && succession.successionFlowOf() == null && succession.usageBody() == null
                && succession.SEMICOLON() == null;
        }
        return false;
    }

    /** A member declared 'private', which specializations do not inherit. */
    private static boolean isPrivate(ParserRuleContext ctx) {
        ParserRuleContext owner = ctx.getParent() instanceof SysMLv2Parser.MemberContext m ? m : ctx;
        if (owner.getParent() instanceof SysMLv2Parser.NamespaceBodyElementContext nbe && nbe.visibility() != null) {
            return nbe.visibility().PRIVATE() != null;
        }
        if (ctx instanceof SysMLv2Parser.EnumMemberContext e && e.visibility() != null) {
            return e.visibility().PRIVATE() != null;
        }
        return false;
    }

    /** A feature declared without a name whose header redefines a feature, e.g. {@code :>> mass = 5;}. */
    private static boolean isUnnamedRedefinition(ParserRuleContext ctx) {
        boolean feature = ctx.getParent() instanceof SysMLv2Parser.MemberContext
            || ctx instanceof SysMLv2Parser.ImplicitFeatureDeclarationContext
            || ctx instanceof SysMLv2Parser.AnonymousRedefinesContext
            || ctx instanceof SysMLv2Parser.SimpleFeatureContext
            || ctx instanceof SysMLv2Parser.InParameterContext
            || ctx instanceof SysMLv2Parser.OutParameterContext
            || ctx instanceof SysMLv2Parser.InoutParameterContext
            || ctx instanceof SysMLv2Parser.ReturnFeatureContext;
        if (!feature) {
            return false;
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (isRedefinesToken(child) || child instanceof SysMLv2Parser.RedefinedListContext) {
                return true;
            }
            if (child instanceof SysMLv2Parser.FeatureRelationshipsContext rels) {
                for (SysMLv2Parser.FeatureRelationshipContext rel : rels.featureRelationship()) {
                    if (rel.typeRelationship() != null && rel.typeRelationship().redefinesClause() != null) {
                        return true;
                    }
                }
            }
            if (child instanceof SysMLv2Parser.TypeRelationshipsContext rels) {
                for (SysMLv2Parser.TypeRelationshipContext rel : rels.typeRelationship()) {
                    if (rel.redefinesClause() != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isRedefinesToken(ParseTree node) {
        if (node instanceof TerminalNode t) {
            int type = t.getSymbol().getType();
            return type == SysMLv2Parser.REDEFINES || type == SysMLv2Parser.REDEFINES_OP;
        }
        return false;
    }

    /**
     * Parameters and connector ends: features that redefine the general type's parameters (or ends) by
     * position rather than by name.
     */
    private static boolean isParameter(ParserRuleContext ctx) {
        if (ctx instanceof SysMLv2Parser.InParameterContext
                || ctx instanceof SysMLv2Parser.OutParameterContext
                || ctx instanceof SysMLv2Parser.InoutParameterContext
                || ctx instanceof SysMLv2Parser.ReturnFeatureContext
                || ctx instanceof SysMLv2Parser.SubjectDeclarationContext
                || ctx instanceof SysMLv2Parser.ObjectiveRequirementContext
                || ctx instanceof SysMLv2Parser.ActorUsageContext
                || ctx instanceof SysMLv2Parser.StakeholderUsageContext
                || ctx instanceof SysMLv2Parser.EndUsageContext) {
            return true;
        }
        if (ctx instanceof SysMLv2Parser.SimpleParameterContext p) {
            return p.SUBJECT() != null || p.OBJECTIVE() != null;
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof SysMLv2Parser.DirectionPrefixContext) {
                return true;
            }
        }
        if (ctx.getParent() instanceof SysMLv2Parser.MemberContext member && member.prefixes() != null) {
            for (SysMLv2Parser.FeaturePrefixContext prefix : member.prefixes().featurePrefix()) {
                if (prefix.directionPrefix() != null
                        || prefix.usagePrefix() != null && prefix.usagePrefix().END() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The direction of a directed parameter ("in", "out" or "inout"), or null. */
    private static String directionOf(ParserRuleContext ctx) {
        if (ctx instanceof SysMLv2Parser.InParameterContext) {
            return "in";
        }
        if (ctx instanceof SysMLv2Parser.OutParameterContext) {
            return "out";
        }
        if (ctx instanceof SysMLv2Parser.InoutParameterContext) {
            return "inout";
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof SysMLv2Parser.DirectionPrefixContext d) {
                return d.getText();
            }
        }
        if (ctx.getParent() instanceof SysMLv2Parser.MemberContext member && member.prefixes() != null) {
            for (SysMLv2Parser.FeaturePrefixContext prefix : member.prefixes().featurePrefix()) {
                if (prefix.directionPrefix() != null) {
                    return prefix.directionPrefix().getText();
                }
            }
        }
        return null;
    }

    /** The token that names a declaration (its name or short name), else the declaration's first token. */
    private static Token nameToken(ParserRuleContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof SysMLv2Parser.DeclarationNameContext
                    || child instanceof SysMLv2Parser.UsageNameContext
                    || child instanceof SysMLv2Parser.AliasNameContext
                    || child instanceof SysMLv2Parser.NameContext
                    || child instanceof SysMLv2Parser.QualifiedNameContext
                        && !(i > 0 && isRedefinesToken(ctx.getChild(i - 1)))) {
                return ((ParserRuleContext) child).getStart();
            }
        }
        return ctx.getStart();
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

    /**
     * Typings, specializations, subsettings, redefinitions and references in an element's header, and the
     * text of its typing, multiplicity and value.
     */
    private void collectSupertypes(ParseTree node, IndexedElement element) {
        if (!(node instanceof ParserRuleContext ctx) || isBody(ctx) || declaredNames(ctx) != null) {
            return;
        }
        if (ctx instanceof SysMLv2Parser.ExpressionContext) {
            return;
        }
        if (ctx instanceof SysMLv2Parser.ValueInitContext || ctx instanceof SysMLv2Parser.TupleOrValueInitContext
                || ctx instanceof SysMLv2Parser.ParameterValueInitContext) {
            if (element.valueText == null) {
                element.valueText = valueText(ctx);
            }
            return;
        }
        if (ctx instanceof SysMLv2Parser.MultiplicityClauseContext
                && !(ctx.getParent() instanceof SysMLv2Parser.ConnectionEndContext)) {
            if (element.multiplicityText == null) {
                element.multiplicityText = ctx.getText();
            }
            return;
        }
        if (ctx instanceof SysMLv2Parser.TypingClauseContext typing) {
            StringBuilder text = new StringBuilder(element.typeText == null ? "" : element.typeText + ", ");
            for (int i = 0; i < typing.getChildCount(); i++) {
                ParseTree child = typing.getChild(i);
                if (child instanceof TerminalNode t && t.getSymbol().getType() == SysMLv2Parser.TILDE) {
                    text.append('~');
                } else if (child instanceof SysMLv2Parser.QualifiedNameContext q) {
                    text.append(q.getText());
                } else if (child instanceof TerminalNode t && t.getSymbol().getType() == SysMLv2Parser.COMMA) {
                    text.append(", ");
                }
            }
            element.typeText = text.toString();
        }
        boolean redefinition = ctx instanceof SysMLv2Parser.RedefinesClauseContext
            || ctx instanceof SysMLv2Parser.RedefinedListContext;
        boolean clause = redefinition
            || ctx instanceof SysMLv2Parser.TypingClauseContext
            || ctx instanceof SysMLv2Parser.SpecializesClauseContext
            || ctx instanceof SysMLv2Parser.SubsetsClauseContext
            || ctx instanceof SysMLv2Parser.ReferencesClauseContext
            || ctx instanceof SysMLv2Parser.ConjugatesClauseContext;
        if (clause) {
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);
                List<String> segs = null;
                if (child instanceof SysMLv2Parser.QualifiedNameContext q) {
                    segs = segments(q);
                } else if (child instanceof SysMLv2Parser.FeatureChainContext f) {
                    if (f.DOT().isEmpty()) {
                        segs = new ArrayList<>();
                        for (SysMLv2Parser.NameContext n : f.name()) {
                            segs.add(normalize(n.getText()));
                        }
                    } else {
                        element.opaqueSupertypes = true;
                    }
                }
                if (segs != null) {
                    element.supertypes.add(segs);
                    if (redefinition) {
                        element.redefines.add(segs);
                    }
                }
            }
            return;
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            collectSupertypes(ctx.getChild(i), element);
        }
    }

    private static final int MAX_VALUE_TEXT = 60;

    /**
     * The value as written, whitespace collapsed. A long value is shortened and tagged with a hash of the
     * whole value (ignoring whitespace), so two shortened values are equal only if the values are.
     */
    private static String valueText(ParserRuleContext ctx) {
        Token start = ctx.getStart();
        Token stop = ctx.getStop();
        String text;
        if (start == null || stop == null || start.getInputStream() == null
                || stop.getStopIndex() < start.getStartIndex()) {
            text = ctx.getText();
        } else {
            text = start.getInputStream().getText(Interval.of(start.getStartIndex(), stop.getStopIndex()));
        }
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() <= MAX_VALUE_TEXT) {
            return text;
        }
        String hash = Integer.toHexString(text.replace(" ", "").hashCode());
        return text.substring(0, MAX_VALUE_TEXT - 10) + "... #" + hash;
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
