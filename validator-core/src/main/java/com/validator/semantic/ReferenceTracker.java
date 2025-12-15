package com.validator.semantic;

import com.validator.ast.Location;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Tracks symbol references by walking the parse tree after symbol table is built.
 * This second pass marks symbols as used when they are referenced.
 *
 * <p>Reference types tracked:
 * <ul>
 *   <li>Type references (e.g., {@code part x : SomeType})</li>
 *   <li>Expression references (e.g., variables used in constraints)</li>
 *   <li>Specialization references (e.g., {@code :> ParentType})</li>
 *   <li>Redefinition references (e.g., {@code :>> parentFeature})</li>
 *   <li>Import references (packages and elements imported)</li>
 * </ul>
 */
public final class ReferenceTracker extends SysMLv2ParserBaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceTracker.class);

    private final SymbolTable symbolTable;
    private final String filePath;

    private ReferenceTracker(SymbolTable symbolTable, String filePath) {
        this.symbolTable = Objects.requireNonNull(symbolTable, "Symbol table cannot be null");
        this.filePath = filePath != null ? filePath : "<unknown>";
    }

    /**
     * Track references in a parse tree using an existing symbol table.
     *
     * @param tree the parse tree
     * @param symbolTable the symbol table from first pass
     * @param filePath the source file path
     */
    public static void track(ParseTree tree, SymbolTable symbolTable, String filePath) {
        Objects.requireNonNull(tree, "Parse tree cannot be null");
        Objects.requireNonNull(symbolTable, "Symbol table cannot be null");

        ReferenceTracker tracker = new ReferenceTracker(symbolTable, filePath);
        ParseTreeWalker walker = new ParseTreeWalker();

        try {
            walker.walk(tracker, tree);
        } catch (Exception e) {
            LOGGER.warn("Error tracking references: {}", e.getMessage());
        }

        LOGGER.debug("Reference tracking complete for {}", filePath);
    }

    // ==================== TYPE REFERENCES ====================

    @Override
    public void enterTypingClause(SysMLv2Parser.TypingClauseContext ctx) {
        // Handle type references like : TypeName
        if (ctx.qualifiedName() != null) {
            for (var qn : ctx.qualifiedName()) {
                markTypeReference(qn.getText(), ctx);
            }
        }
    }

    @Override
    public void enterSpecializesClause(SysMLv2Parser.SpecializesClauseContext ctx) {
        // Handle :> specialization references
        if (ctx.qualifiedName() != null) {
            for (var qn : ctx.qualifiedName()) {
                String refName = qn.getText();
                Symbol referenced = resolveReference(refName);
                if (referenced != null) {
                    referenced.addReference(getLocation(ctx));
                    LOGGER.debug("Specialization reference: {}", refName);
                }
            }
        }
    }

    @Override
    public void enterRedefinesClause(SysMLv2Parser.RedefinesClauseContext ctx) {
        // Handle :>> redefinition references
        if (ctx.qualifiedName() != null) {
            for (var qn : ctx.qualifiedName()) {
                String refName = qn.getText();
                Symbol referenced = resolveReference(refName);
                if (referenced != null) {
                    referenced.addReference(getLocation(ctx));
                    LOGGER.debug("Redefinition reference: {}", refName);
                }
            }
        }
    }

    @Override
    public void enterSubsetsClause(SysMLv2Parser.SubsetsClauseContext ctx) {
        // Handle subsets references
        if (ctx.qualifiedName() != null) {
            for (var qn : ctx.qualifiedName()) {
                String refName = qn.getText();
                Symbol referenced = resolveReference(refName);
                if (referenced != null) {
                    referenced.addReference(getLocation(ctx));
                    LOGGER.debug("Subsetting reference: {}", refName);
                }
            }
        }
    }

    // ==================== EXPRESSION REFERENCES ====================

    @Override
    public void enterFeatureChain(SysMLv2Parser.FeatureChainContext ctx) {
        // Handle feature chain expressions like a.b.c
        if (ctx.name() != null) {
            for (var nameCtx : ctx.name()) {
                markExpressionReference(nameCtx.getText(), ctx);
            }
        }
    }

    @Override
    public void enterNameExpression(SysMLv2Parser.NameExpressionContext ctx) {
        // Handle simple name expressions in constraints/calculations
        if (ctx.qualifiedName() != null) {
            String refName = ctx.qualifiedName().getText();
            markExpressionReference(refName, ctx);
        }
    }

    @Override
    public void enterInvocationExpression(SysMLv2Parser.InvocationExpressionContext ctx) {
        // Handle function/calc invocations
        if (ctx.qualifiedName() != null) {
            String refName = ctx.qualifiedName().getText();
            Symbol referenced = resolveReference(refName);
            if (referenced != null) {
                referenced.addReference(getLocation(ctx));
                referenced.markUsedInExpression();
                LOGGER.debug("Invocation reference: {}", refName);
            }
        }
    }

    // ==================== IMPORT REFERENCES ====================

    @Override
    public void enterImportDeclaration(SysMLv2Parser.ImportDeclarationContext ctx) {
        if (ctx.qualifiedNameWithWildcard() == null) {
            return;
        }

        String importPath = ctx.qualifiedNameWithWildcard().getText();
        boolean isWildcard = importPath.endsWith("::*") || importPath.endsWith("*");

        if (isWildcard) {
            // For wildcard imports, mark the package as referenced
            String packagePath = importPath.replace("::*", "").replace("*", "");
            Symbol packageSymbol = symbolTable.resolveQualified(packagePath);
            if (packageSymbol != null) {
                packageSymbol.markUsedInImport();
                packageSymbol.addReference(getLocation(ctx));
                LOGGER.debug("Wildcard import of package: {}", packagePath);
            }
        } else {
            // For specific imports, mark the specific element
            Symbol symbol = symbolTable.resolveQualified(importPath);
            if (symbol != null) {
                symbol.markUsedInImport();
                symbol.addReference(getLocation(ctx));
                LOGGER.debug("Specific import: {}", importPath);
            }
        }
    }

    // ==================== USAGE REFERENCES ====================
    // These track when usages reference definitions via typing

    @Override
    public void enterPartUsage(SysMLv2Parser.PartUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterAttributeUsage(SysMLv2Parser.AttributeUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterActionUsage(SysMLv2Parser.ActionUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterStateUsage(SysMLv2Parser.StateUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterPortUsage(SysMLv2Parser.PortUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterItemUsage(SysMLv2Parser.ItemUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterRequirementUsage(SysMLv2Parser.RequirementUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterConnectionUsage(SysMLv2Parser.ConnectionUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterConstraintUsage(SysMLv2Parser.ConstraintUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterCalcUsage(SysMLv2Parser.CalcUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    @Override
    public void enterViewUsage(SysMLv2Parser.ViewUsageContext ctx) {
        trackUsageTypes(ctx.featureRelationships());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Track type references from featureRelationships.
     */
    private void trackUsageTypes(SysMLv2Parser.FeatureRelationshipsContext featureRels) {
        if (featureRels == null || featureRels.featureRelationship() == null) {
            return;
        }

        for (var featureRel : featureRels.featureRelationship()) {
            if (featureRel.typeRelationship() != null) {
                var typeRel = featureRel.typeRelationship();
                if (typeRel.typingClause() != null) {
                    var typingClause = typeRel.typingClause();
                    if (typingClause.qualifiedName() != null) {
                        for (var qn : typingClause.qualifiedName()) {
                            markTypeReference(qn.getText(), typingClause);
                        }
                    }
                }
            }
        }
    }

    private void markTypeReference(String typeName, ParserRuleContext ctx) {
        if (typeName == null || typeName.isEmpty()) {
            return;
        }

        Symbol referenced = resolveReference(typeName);
        if (referenced != null) {
            referenced.markUsedAsType();
            referenced.addReference(getLocation(ctx));
            LOGGER.debug("Type reference: {} -> {}", typeName, referenced.getQualifiedName());
        }
    }

    private void markExpressionReference(String name, ParserRuleContext ctx) {
        if (name == null || name.isEmpty()) {
            return;
        }

        Symbol referenced = resolveReference(name);
        if (referenced != null) {
            referenced.markUsedInExpression();
            referenced.addReference(getLocation(ctx));
            LOGGER.debug("Expression reference: {} -> {}", name, referenced.getQualifiedName());
        }
    }

    private Symbol resolveReference(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Try qualified name first
        Symbol symbol = symbolTable.resolveQualified(name);
        if (symbol != null) {
            return symbol;
        }

        // Try simple name resolution (walks up scope chain)
        symbol = symbolTable.resolve(name);
        if (symbol != null) {
            return symbol;
        }

        // Handle qualified names that may need partial resolution
        if (name.contains("::")) {
            // Try to resolve each prefix
            String[] parts = name.split("::");
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    prefix.append("::");
                }
                prefix.append(parts[i]);
                symbol = symbolTable.resolveQualified(prefix.toString());
                if (symbol != null && i == parts.length - 1) {
                    return symbol;
                }
            }
        }

        return null;
    }

    private Location getLocation(ParserRuleContext ctx) {
        if (ctx == null || ctx.getStart() == null) {
            return null;
        }
        try {
            return new Location(
                filePath,
                ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
