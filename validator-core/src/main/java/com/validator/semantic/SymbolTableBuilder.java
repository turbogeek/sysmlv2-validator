package com.validator.semantic;

import com.validator.ast.Location;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserBaseVisitor;
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

    public SymbolTableBuilder(String fileName) {
        this.symbolTable = new SymbolTable();
        this.fileName = (fileName != null) ? fileName : "<unknown>";
        this.errors = new ArrayList<>();
    }

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

    @Override
    public Void visitCompilationUnit(SysMLv2Parser.CompilationUnitContext ctx) {
        // Process children in order
        // Track if we have a file-scoped package (package X; without braces)
        boolean hasFileScopedPackage = false;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);

            if (child instanceof SysMLv2Parser.PackageDeclarationContext) {
                SysMLv2Parser.PackageDeclarationContext pkgCtx =
                    (SysMLv2Parser.PackageDeclarationContext) child;

                // Check if this is file-scoped (semicolon) or block-scoped (braces)
                boolean isFileScopedPkg = pkgCtx.packageBody() == null;

                if (isFileScopedPkg) {
                    // Enter scope, visit package, but DON'T exit scope yet
                    visitPackageDeclarationFileScoped(pkgCtx);
                    hasFileScopedPackage = true;
                } else {
                    // Visit package with braces - it manages its own scope
                    visit(pkgCtx);
                }
            } else {
                // Visit other children (imports, elements)
                visit(child);
            }
        }

        // Exit file-scoped package at end of file
        if (hasFileScopedPackage) {
            symbolTable.exitScope();
        }

        return null;
    }

    private void visitPackageDeclarationFileScoped(SysMLv2Parser.PackageDeclarationContext ctx) {
        String packageName = getIdentifier(ctx.qualifiedName());
        if (packageName != null) {
            try {
                symbolTable.enterScope(packageName, ScopeType.PACKAGE);
                Location location = getLocation(ctx);
                Symbol symbol = new Symbol(packageName, packageName, ElementType.PACKAGE, location);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
                // DON'T exit scope - it remains open for rest of file
            } catch (Exception e) {
                errors.add(String.format("Error processing package '%s' at %s: %s",
                    packageName, getLocation(ctx), e.getMessage()));
            }
        }
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

                // Visit packageBody if present (when using braces syntax)
                if (ctx.packageBody() != null) {
                    visit(ctx.packageBody());
                }

                // Exit scope after processing this package (only for block-scoped packages)
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing package '%s' at %s: %s",
                    packageName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitPartDefinition(SysMLv2Parser.PartDefinitionContext ctx) {
        String partName = getIdentifier(ctx.name());
        if (partName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + partName;
                Visibility visibility = extractVisibility(ctx.visibility());

                Symbol symbol = new Symbol(partName, qualifiedName, ElementType.PART_DEFINITION, location, visibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                // Enter part scope for nested elements
                symbolTable.enterScope(partName, ScopeType.PART_DEFINITION);

                // Visit children (attributes, ports, nested parts)
                visitChildren(ctx);

                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing part definition '%s' at %s: %s",
                    partName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitActionDefinition(SysMLv2Parser.ActionDefinitionContext ctx) {
        String actionName = getIdentifier(ctx.name());
        if (actionName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + actionName;
                Visibility visibility = extractVisibility(ctx.visibility());

                Symbol symbol = new Symbol(actionName, qualifiedName,
                    ElementType.ACTION_DEFINITION, location, visibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                symbolTable.enterScope(actionName, ScopeType.ACTION_DEFINITION);
                visitChildren(ctx);
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing action definition '%s' at %s: %s",
                    actionName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitStateDefinition(SysMLv2Parser.StateDefinitionContext ctx) {
        String stateName = getIdentifier(ctx.name());
        if (stateName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + stateName;
                Visibility visibility = extractVisibility(ctx.visibility());

                Symbol symbol = new Symbol(stateName, qualifiedName,
                    ElementType.STATE_DEFINITION, location, visibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                symbolTable.enterScope(stateName, ScopeType.STATE_DEFINITION);
                visitChildren(ctx);
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing state definition '%s' at %s: %s",
                    stateName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitRequirementDefinition(SysMLv2Parser.RequirementDefinitionContext ctx) {
        String reqName = getIdentifier(ctx.name());
        if (reqName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + reqName;
                Visibility visibility = extractVisibility(ctx.visibility());

                Symbol symbol = new Symbol(reqName, qualifiedName,
                    ElementType.REQUIREMENT_DEFINITION, location, visibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                symbolTable.enterScope(reqName, ScopeType.REQUIREMENT_DEFINITION);
                visitChildren(ctx);
                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing requirement definition '%s' at %s: %s",
                    reqName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitAttributeDefinition(SysMLv2Parser.AttributeDefinitionContext ctx) {
        String attrName = getIdentifier(ctx.name());
        if (attrName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + attrName;
                Visibility visibility = extractVisibility(ctx.visibility());

                Symbol symbol = new Symbol(attrName, qualifiedName,
                    ElementType.ATTRIBUTE_DEFINITION, location, visibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing attribute definition '%s' at %s: %s",
                    attrName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitAttributeUsage(SysMLv2Parser.AttributeUsageContext ctx) {
        String attrName = getIdentifier(ctx.name());
        if (attrName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + attrName;

                Symbol symbol = new Symbol(attrName, qualifiedName,
                    ElementType.ATTRIBUTE_USAGE, location);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing attribute usage '%s' at %s: %s",
                    attrName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitPortDefinition(SysMLv2Parser.PortDefinitionContext ctx) {
        String portName = getIdentifier(ctx.name());
        if (portName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + portName;
                Visibility visibility = extractVisibility(ctx.visibility());

                Symbol symbol = new Symbol(portName, qualifiedName, ElementType.PORT_DEFINITION, location, visibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);
            } catch (Exception e) {
                errors.add(String.format("Error processing port definition '%s' at %s: %s",
                    portName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitConnectionDefinition(SysMLv2Parser.ConnectionDefinitionContext ctx) {
        String connectionName = getIdentifier(ctx.name());
        if (connectionName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + connectionName;
                Visibility visibility = extractVisibility(ctx.visibility());

                Symbol symbol = new Symbol(connectionName, qualifiedName,
                    ElementType.CONNECTION_DEFINITION, location, visibility);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                // Enter connection scope for nested elements
                symbolTable.enterScope(connectionName, ScopeType.CONNECTION_DEFINITION);

                // Visit children
                visitChildren(ctx);

                symbolTable.exitScope();
            } catch (Exception e) {
                errors.add(String.format("Error processing connection definition '%s' at %s: %s",
                    connectionName, getLocation(ctx), e.getMessage()));
            }
        }
        return null;
    }

    @Override
    public Void visitImportDeclaration(SysMLv2Parser.ImportDeclarationContext ctx) {
        try {
            // Check if visibility is PUBLIC by checking for the PUBLIC token
            boolean isPublic = ctx.visibility() != null
                && ctx.visibility().getText() != null
                && ctx.visibility().getText().equals("public");
            String importPath = extractImportPath(ctx.qualifiedName());

            ImportType importType = determineImportType(ctx.getText());
            String alias = extractImportAlias(ctx.getText());

            ImportStatement importStmt = new ImportStatement(importPath, importType, isPublic, alias);
            symbolTable.addImport(importStmt);
        } catch (Exception e) {
            errors.add(String.format("Error processing import at %s: %s",
                getLocation(ctx), e.getMessage()));
        }
        return null;
    }

    @Override
    public Void visitImportStatement(SysMLv2Parser.ImportStatementContext ctx) {
        try {
            // Check if visibility is PUBLIC by checking for the PUBLIC token
            boolean isPublic = ctx.visibility() != null
                && ctx.visibility().getText() != null
                && ctx.visibility().getText().equals("public");
            String importPath = extractImportPath(ctx.qualifiedName());

            ImportType importType = determineImportType(ctx.getText());
            String alias = extractImportAlias(ctx.getText());

            ImportStatement importStmt = new ImportStatement(importPath, importType, isPublic, alias);
            symbolTable.addImport(importStmt);
        } catch (Exception e) {
            errors.add(String.format("Error processing import at %s: %s",
                getLocation(ctx), e.getMessage()));
        }
        return null;
    }

    // Helper methods

    private Visibility extractVisibility(SysMLv2Parser.VisibilityContext ctx) {
        if (ctx == null) {
            return Visibility.PUBLIC; // Default visibility
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

    private String getIdentifier(ParserRuleContext ctx) {
        if (ctx == null) {
            return null;
        }
        // This is simplified - actual implementation would extract from identifier rule
        return ctx.getText();
    }

    private Location getLocation(ParserRuleContext ctx) {
        if (ctx == null) {
            return new Location(fileName, 1, 0);
        }
        return new Location(fileName, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    }

    private String extractImportPath(SysMLv2Parser.QualifiedNameContext ctx) {
        if (ctx == null) {
            return null;
        }
        return ctx.getText();
    }

    private ImportType determineImportType(String text) {
        if (text.contains("::*")) {
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

    private String extractImportAlias(String text) {
        if (text.contains(" as ")) {
            String[] parts = text.split(" as ");
            if (parts.length > 1) {
                return parts[1].replaceAll(";$", "").trim();
            }
        }
        return null;
    }
}
