package com.validator.semantic;

import com.validator.ast.Location;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserBaseVisitor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a symbol table from a SysML v2 parse tree.
 * This is a first-pass visitor that collects all definitions and their scopes.
 */
public class SymbolTableBuilder extends SysMLv2ParserBaseVisitor<Void> {
    private final SymbolTable symbolTable;
    private final String fileName;
    private final List<String> errors;

    // Track current visibility for nested elements
    private Visibility currentVisibility = Visibility.PUBLIC;

    public SymbolTableBuilder(String fileName) {
        this.symbolTable = new SymbolTable();
        this.fileName = (fileName != null) ? fileName : "<unknown>";
        this.errors = new ArrayList<>();
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Symbol table is intentionally returned for further processing")
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Build symbol table from parse tree.
     */
    public static SymbolTable build(ParseTree tree, String fileName) {
        SymbolTableBuilder builder = new SymbolTableBuilder(fileName);
        builder.visit(tree);
        return builder.getSymbolTable();
    }

    // Track file-scoped package (package X; - no braces)
    private boolean inFileScopedPackage = false;

    @Override
    public Void visitCompilationUnit(SysMLv2Parser.CompilationUnitContext ctx) {
        // Visit root namespace
        if (ctx.rootNamespace() != null) {
            visit(ctx.rootNamespace());
        }
        // Exit file-scoped package at end of file if needed
        if (inFileScopedPackage) {
            symbolTable.exitScope();
            inFileScopedPackage = false;
        }
        return null;
    }

    @Override
    public Void visitRootNamespace(SysMLv2Parser.RootNamespaceContext ctx) {
        // Process all namespace body elements
        for (SysMLv2Parser.NamespaceBodyElementContext elem : ctx.namespaceBodyElement()) {
            visit(elem);
        }
        return null;
    }

    @Override
    public Void visitNamespaceBodyElement(SysMLv2Parser.NamespaceBodyElementContext ctx) {
        // Capture visibility for the member
        if (ctx.visibility() != null) {
            currentVisibility = extractVisibility(ctx.visibility());
        } else {
            currentVisibility = Visibility.PUBLIC;
        }
        // Visit children (the actual member/import/package)
        return visitChildren(ctx);
    }

    @Override
    public Void visitPackageDeclaration(SysMLv2Parser.PackageDeclarationContext ctx) {
        String packageName = getIdentifier(ctx.qualifiedName());
        if (packageName != null) {
            try {
                symbolTable.enterScope(packageName, ScopeType.PACKAGE);
                Location location = getLocation(ctx);
                Symbol symbol = new Symbol(packageName, packageName, ElementType.PACKAGE, location);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                // Check if this is a file-scoped package (packageBody is just semicolon)
                boolean isFileScopedPackage = ctx.packageBody() != null
                    && ctx.packageBody().SEMICOLON() != null
                    && ctx.packageBody().LBRACE() == null;

                if (isFileScopedPackage) {
                    // Keep scope open - subsequent elements are in this package
                    inFileScopedPackage = true;
                } else if (ctx.packageBody() != null) {
                    // Visit packageBody with braces
                    visit(ctx.packageBody());
                    symbolTable.exitScope();
                } else {
                    symbolTable.exitScope();
                }
            } catch (Exception e) {
                errors.add(String.format("Error processing package '%s' at %s: %s",
                    packageName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitPackageBody(SysMLv2Parser.PackageBodyContext ctx) {
        // Visit all namespace body elements
        for (SysMLv2Parser.NamespaceBodyElementContext elem : ctx.namespaceBodyElement()) {
            visit(elem);
        }
        return null;
    }

    // ============================================================================
    // SysML Definitions
    // ============================================================================

    @Override
    public Void visitPartDefinition(SysMLv2Parser.PartDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.PART_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                // Enter part scope for nested elements
                symbolTable.enterScope(name, ScopeType.PART_DEFINITION);
                if (ctx.definitionBody() != null) {
                    visit(ctx.definitionBody());
                }
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing part definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitActionDefinition(SysMLv2Parser.ActionDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.ACTION_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                symbolTable.enterScope(name, ScopeType.ACTION_DEFINITION);
                if (ctx.definitionBody() != null) {
                    visit(ctx.definitionBody());
                }
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing action definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitStateDefinition(SysMLv2Parser.StateDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.STATE_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                symbolTable.enterScope(name, ScopeType.STATE_DEFINITION);
                if (ctx.stateDefinitionBody() != null) {
                    visit(ctx.stateDefinitionBody());
                }
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing state definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitRequirementDefinition(SysMLv2Parser.RequirementDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.REQUIREMENT_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                symbolTable.enterScope(name, ScopeType.REQUIREMENT_DEFINITION);
                if (ctx.requirementBody() != null) {
                    visit(ctx.requirementBody());
                }
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing requirement definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitAttributeDefinition(SysMLv2Parser.AttributeDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.ATTRIBUTE_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing attribute definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitPortDefinition(SysMLv2Parser.PortDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.PORT_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing port definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitConnectionDefinition(SysMLv2Parser.ConnectionDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.CONNECTION_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                symbolTable.enterScope(name, ScopeType.CONNECTION_DEFINITION);
                if (ctx.definitionBody() != null) {
                    visit(ctx.definitionBody());
                }
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing connection definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitViewDefinition(SysMLv2Parser.ViewDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.VIEW_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing view definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitConstraintDefinition(SysMLv2Parser.ConstraintDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.CONSTRAINT_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing constraint definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitEnumDefinition(SysMLv2Parser.EnumDefinitionContext ctx) {
        String name = getDeclarationName(ctx.declarationName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.ENUMERATION_DEFINITION, location,
                    currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing enum definition '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    // ============================================================================
    // SysML Usages
    // ============================================================================

    @Override
    public Void visitPartUsage(SysMLv2Parser.PartUsageContext ctx) {
        String name = getUsageName(ctx.usageName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.PART_USAGE, location);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing part usage '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitAttributeUsage(SysMLv2Parser.AttributeUsageContext ctx) {
        String name = getUsageName(ctx.usageName());
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, ElementType.ATTRIBUTE_USAGE, location);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing attribute usage '%s' at %s: %s",
                    name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    // ============================================================================
    // Imports
    // ============================================================================

    @Override
    public Void visitImportDeclaration(SysMLv2Parser.ImportDeclarationContext ctx) {
        try {
            boolean isPublic = ctx.visibility() != null
                && ctx.visibility().getText() != null
                && ctx.visibility().getText().equals("public");

            // Extract import path from qualifiedNameWithWildcard
            String importPath = null;
            if (ctx.qualifiedNameWithWildcard() != null) {
                importPath = ctx.qualifiedNameWithWildcard().getText();
            }

            if (importPath != null) {
                ImportType importType = determineImportType(importPath);
                ImportStatement importStmt = new ImportStatement(importPath, importType, isPublic, null);
                symbolTable.addImport(importStmt);
            }
        } catch (Exception e) {
            errors.add(String.format("Error processing import at %s: %s",
                getLocation(ctx), e.getMessage()));
        }
        return null;
    }

    // ============================================================================
    // KerML Definitions
    // ============================================================================

    @Override
    public Void visitDatatypeDefinition(SysMLv2Parser.DatatypeDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.DATATYPE_DEFINITION, "datatype");
    }

    @Override
    public Void visitClassDefinition(SysMLv2Parser.ClassDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.CLASS_DEFINITION, "class");
    }

    @Override
    public Void visitStructDefinition(SysMLv2Parser.StructDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.STRUCT_DEFINITION, "struct");
    }

    @Override
    public Void visitAssocDefinition(SysMLv2Parser.AssocDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.ASSOCIATION_DEFINITION, "assoc");
    }

    @Override
    public Void visitBehaviorDefinition(SysMLv2Parser.BehaviorDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.BEHAVIOR_DEFINITION, "behavior");
    }

    @Override
    public Void visitFunctionDefinition(SysMLv2Parser.FunctionDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.FUNCTION_DEFINITION, "function");
    }

    @Override
    public Void visitPredicateDefinition(SysMLv2Parser.PredicateDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.PREDICATE_DEFINITION, "predicate");
    }

    @Override
    public Void visitInteractionDefinition(SysMLv2Parser.InteractionDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.INTERACTION_DEFINITION, "interaction");
    }

    @Override
    public Void visitMetaclassDefinition(SysMLv2Parser.MetaclassDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.METACLASS_DEFINITION, "metaclass");
    }

    @Override
    public Void visitClassifierDefinition(SysMLv2Parser.ClassifierDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.CLASSIFIER_DEFINITION, "classifier");
    }

    @Override
    public Void visitTypeDefinition(SysMLv2Parser.TypeDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.TYPE_DEFINITION, "type");
    }

    @Override
    public Void visitFeatureDefinition(SysMLv2Parser.FeatureDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.FEATURE_DEFINITION, "feature");
    }

    @Override
    public Void visitConnectorDefinition(SysMLv2Parser.ConnectorDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.CONNECTOR_DEFINITION, "connector");
    }

    @Override
    public Void visitBindingConnectorDefinition(SysMLv2Parser.BindingConnectorDefinitionContext ctx) {
        return visitKerMLDefinition(ctx, ctx.declarationName(), ElementType.BINDING_DEFINITION, "binding");
    }

    /**
     * Generic visitor for KerML definition nodes.
     */
    private Void visitKerMLDefinition(ParserRuleContext ctx,
                                      SysMLv2Parser.DeclarationNameContext nameCtx,
                                      ElementType elementType, String elementKind) {
        String name = getDeclarationName(nameCtx);
        if (name != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + name;

                Symbol symbol = new Symbol(name, qualifiedName, elementType, location, currentVisibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                // Enter scope for nested definitions
                symbolTable.enterScope(name, ScopeType.DEFINITION);
                visitChildren(ctx);
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing %s definition '%s' at %s: %s",
                    elementKind, name, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    // ============================================================================
    // Helper methods
    // ============================================================================

    private Visibility extractVisibility(SysMLv2Parser.VisibilityContext ctx) {
        if (ctx == null) {
            return Visibility.PUBLIC;
        }
        String text = ctx.getText();
        if ("public".equals(text)) {
            return Visibility.PUBLIC;
        } else if ("private".equals(text)) {
            return Visibility.PRIVATE;
        } else if ("protected".equals(text)) {
            return Visibility.PROTECTED;
        }
        return Visibility.PUBLIC;
    }

    private String getDeclarationName(SysMLv2Parser.DeclarationNameContext ctx) {
        if (ctx == null) {
            return null;
        }
        // DeclarationName can be: name shortName? | shortName
        if (ctx.name() != null) {
            return getNameText(ctx.name());
        } else if (ctx.shortName() != null) {
            return ctx.shortName().getText();
        }
        return ctx.getText();
    }

    private String getUsageName(SysMLv2Parser.UsageNameContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.name() != null) {
            return getNameText(ctx.name());
        } else if (ctx.shortName() != null) {
            return ctx.shortName().getText();
        }
        return ctx.getText();
    }

    private String getNameText(SysMLv2Parser.NameContext ctx) {
        if (ctx == null) {
            return null;
        }
        // Name can be ID or QUOTED_ID
        String text = ctx.getText();
        // Remove quotes if present
        if (text.startsWith("'") && text.endsWith("'")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private String getIdentifier(ParserRuleContext ctx) {
        if (ctx == null) {
            return null;
        }
        return ctx.getText();
    }

    private Location getLocation(ParserRuleContext ctx) {
        if (ctx == null) {
            return new Location(fileName, 1, 0);
        }
        return new Location(fileName, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    }

    private ImportType determineImportType(String text) {
        if (text.contains("::*") || text.contains("::**")) {
            return ImportType.WILDCARD;
        }
        if (text.contains(" as ")) {
            return ImportType.ALIAS;
        }
        if (text.contains("{")) {
            return ImportType.FILTERED;
        }
        return ImportType.SPECIFIC;
    }
}
