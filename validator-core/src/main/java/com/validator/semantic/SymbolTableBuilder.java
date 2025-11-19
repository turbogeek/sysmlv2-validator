package com.validator.semantic;

import com.validator.ast.Location;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

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
        this.fileName = fileName;
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
    public Void visitPackageDeclaration(SysMLv2Parser.PackageDeclarationContext ctx) {
        String packageName = getIdentifier(ctx.name);
        if (packageName != null) {
            try {
                symbolTable.enterScope(packageName, ScopeType.PACKAGE);
                Location location = getLocation(ctx);
                Symbol symbol = new Symbol(packageName, packageName, ElementType.PACKAGE, location);
                symbol.setAstNode(ctx);
                symbolTable.define(symbol);

                // Visit children
                visitChildren(ctx);

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
        String partName = getIdentifier(ctx.name);
        if (partName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + partName;

                Symbol symbol = new Symbol(partName, qualifiedName, ElementType.PART_DEFINITION, location);
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
        String actionName = getIdentifier(ctx.name);
        if (actionName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + actionName;

                Symbol symbol = new Symbol(actionName, qualifiedName, ElementType.ACTION_DEFINITION, location);
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
        String stateName = getIdentifier(ctx.name);
        if (stateName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + stateName;

                Symbol symbol = new Symbol(stateName, qualifiedName, ElementType.STATE_DEFINITION, location);
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
        String reqName = getIdentifier(ctx.name);
        if (reqName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + reqName;

                Symbol symbol = new Symbol(reqName, qualifiedName, ElementType.REQUIREMENT_DEFINITION, location);
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
        String attrName = getIdentifier(ctx.name);
        if (attrName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + attrName;

                Symbol symbol = new Symbol(attrName, qualifiedName, ElementType.ATTRIBUTE_DEFINITION, location);
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
    public Void visitPortDefinition(SysMLv2Parser.PortDefinitionContext ctx) {
        String portName = getIdentifier(ctx.name);
        if (portName != null) {
            try {
                Location location = getLocation(ctx);
                String qualifiedName = symbolTable.getCurrentScope().getQualifiedName() + "::" + portName;

                Symbol symbol = new Symbol(portName, qualifiedName, ElementType.PORT_DEFINITION, location);
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
    public Void visitImportStatement(SysMLv2Parser.ImportStatementContext ctx) {
        try {
            boolean isPublic = ctx.PUBLIC() != null;
            String importPath = getImportPath(ctx);

            ImportType importType = determineImportType(ctx);
            String alias = getImportAlias(ctx);

            ImportStatement importStmt = new ImportStatement(importPath, importType, isPublic, alias);
            symbolTable.addImport(importStmt);
        } catch (Exception e) {
            errors.add(String.format("Error processing import at %s: %s",
                getLocation(ctx), e.getMessage()));
        }
        return null;
    }

    // Helper methods

    private String getIdentifier(ParserRuleContext ctx) {
        if (ctx == null) return null;
        // This is simplified - actual implementation would extract from identifier rule
        return ctx.getText();
    }

    private Location getLocation(ParserRuleContext ctx) {
        if (ctx == null) return new Location(fileName, 0, 0);
        return new Location(fileName, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    }

    private String getImportPath(SysMLv2Parser.ImportStatementContext ctx) {
        // Extract qualified name from import statement
        // Simplified - actual implementation would parse the import path
        return ctx.getText().replaceAll("^(public |private )?import ", "").replaceAll(";$", "");
    }

    private ImportType determineImportType(SysMLv2Parser.ImportStatementContext ctx) {
        String text = ctx.getText();
        if (text.contains("::*")) return ImportType.WILDCARD;
        if (text.contains(" as ")) return ImportType.ALIAS;
        if (text.contains("{")) return ImportType.FILTERED;
        return ImportType.SPECIFIC;
    }

    private String getImportAlias(SysMLv2Parser.ImportStatementContext ctx) {
        String text = ctx.getText();
        if (text.contains(" as ")) {
            String[] parts = text.split(" as ");
            if (parts.length > 1) {
                return parts[1].replaceAll(";$", "").trim();
            }
        }
        return null;
    }
}
