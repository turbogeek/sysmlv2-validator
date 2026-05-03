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
 * Builds a symbol table by walking the ANTLR parse tree.
 * First pass: collect all definitions (packages, part defs, etc.)
 */
public final class SymbolTableBuilder extends SysMLv2ParserBaseListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SymbolTableBuilder.class);

    private final SymbolTable symbolTable;
    private final String filePath;
    private Visibility currentVisibility = Visibility.PUBLIC;

    private SymbolTableBuilder(String filePath, StandardLibraryManager stdLib) {
        this.symbolTable = new SymbolTable(stdLib);
        this.filePath = filePath != null ? filePath : "<unknown>";
    }

    /**
     * Build a symbol table from a parse tree with a specific standard library manager.
     *
     * @param tree the parse tree
     * @param filePath the source file path (for error reporting)
     * @param stdLib the standard library manager
     * @return the built symbol table
     */
    public static SymbolTable build(ParseTree tree, String filePath, StandardLibraryManager stdLib) {
        Objects.requireNonNull(tree, "Parse tree cannot be null");
        Objects.requireNonNull(stdLib, "StandardLibraryManager cannot be null");
        
        SymbolTableBuilder builder = new SymbolTableBuilder(filePath, stdLib);
        
        ParseTreeWalker walker = new ParseTreeWalker();

        try {
            walker.walk(builder, tree);
            
            // Build the relationship graph for deep namespace resolution
            builder.symbolTable.getRelationshipGraph().buildFromSymbolTable();
        } catch (Exception e) {
            LOGGER.warn("Error building symbol table: {}", e.getMessage());
        }

        LOGGER.info("Symbol table built: {} symbols from {}",
            builder.symbolTable.getAllSymbols().size(), filePath);
        return builder.symbolTable;
    }

    /**
     * Build a symbol table from a parse tree.
     *
     * @param tree the parse tree
     * @param filePath the source file path (for error reporting)
     * @return the built symbol table
     */
    public static SymbolTable build(ParseTree tree, String filePath) {
        StandardLibraryManager stdLib = new StandardLibraryManager();
        stdLib.initializeBuiltins();
        return build(tree, filePath, stdLib);
    }

    // ==================== VISIBILITY ====================

    @Override
    public void enterVisibility(SysMLv2Parser.VisibilityContext ctx) {
        String visText = ctx.getText();
        currentVisibility = Visibility.fromString(visText);
    }

    @Override
    public void exitNamespaceBodyElement(SysMLv2Parser.NamespaceBodyElementContext ctx) {
        // Reset visibility after each namespace body element
        currentVisibility = Visibility.PUBLIC;
    }

    // ==================== PACKAGES ====================

    @Override
    public void enterPackageDeclaration(SysMLv2Parser.PackageDeclarationContext ctx) {
        String packageName = extractQualifiedName(ctx.qualifiedName());
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        symbolTable.enterScope(packageName, ScopeType.PACKAGE);

        Symbol packageSymbol = new Symbol(
            getSimpleName(packageName),
            symbolTable.getCurrentScope().getQualifiedName(),
            ElementType.PACKAGE,
            getLocation(ctx),
            currentVisibility
        );
        packageSymbol.setAstNode(ctx);

        try {
            symbolTable.define(packageSymbol);
            LOGGER.debug("Defined package: {}", packageSymbol.getQualifiedName());
        } catch (IllegalStateException e) {
            LOGGER.warn("Duplicate package: {}", packageName);
        }
    }

    @Override
    public void exitPackageDeclaration(SysMLv2Parser.PackageDeclarationContext ctx) {
        String packageName = extractQualifiedName(ctx.qualifiedName());
        if (packageName != null && !packageName.isEmpty()) {
            symbolTable.exitScope();
        }
    }

    @Override
    public void enterLibraryPackageDeclaration(SysMLv2Parser.LibraryPackageDeclarationContext ctx) {
        String packageName = extractQualifiedName(ctx.qualifiedName());
        if (packageName == null || packageName.isEmpty()) {
            return;
        }

        symbolTable.enterScope(packageName, ScopeType.PACKAGE);

        Symbol packageSymbol = new Symbol(
            getSimpleName(packageName),
            symbolTable.getCurrentScope().getQualifiedName(),
            ElementType.PACKAGE,
            getLocation(ctx),
            currentVisibility
        );
        packageSymbol.setAstNode(ctx);

        try {
            symbolTable.define(packageSymbol);
            LOGGER.debug("Defined library package: {}", packageSymbol.getQualifiedName());
        } catch (IllegalStateException e) {
            LOGGER.warn("Duplicate library package: {}", packageName);
        }
    }

    @Override
    public void exitLibraryPackageDeclaration(SysMLv2Parser.LibraryPackageDeclarationContext ctx) {
        String packageName = extractQualifiedName(ctx.qualifiedName());
        if (packageName != null && !packageName.isEmpty()) {
            symbolTable.exitScope();
        }
    }

    // ==================== IMPORTS ====================

    @Override
    public void enterImportDeclaration(SysMLv2Parser.ImportDeclarationContext ctx) {
        if (ctx.qualifiedNameWithWildcard() == null) {
            return;
        }

        String importPath = ctx.qualifiedNameWithWildcard().getText();
        boolean isWildcard = importPath.endsWith("::*") || importPath.endsWith("*");
        boolean isPublic = !ctx.getText().startsWith("private");

        ImportType type = isWildcard ? ImportType.WILDCARD : ImportType.SPECIFIC;
        ImportStatement importStmt = new ImportStatement(importPath, type, isPublic);

        symbolTable.addImport(importStmt);
        LOGGER.debug("Added import: {}", importPath);
    }

    // ==================== PART DEFINITIONS ====================

    @Override
    public void enterPartDefinition(SysMLv2Parser.PartDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope (qualified name uses current scope)
        Symbol symbol = createSymbol(name, ElementType.PART_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        // Now enter the scope for this definition
        symbolTable.enterScope(name, ScopeType.PART_DEFINITION);
    }

    @Override
    public void exitPartDefinition(SysMLv2Parser.PartDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== ACTION DEFINITIONS ====================

    @Override
    public void enterActionDefinition(SysMLv2Parser.ActionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope (qualified name uses current scope)
        Symbol symbol = createSymbol(name, ElementType.ACTION_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        // Now enter the scope for this definition
        symbolTable.enterScope(name, ScopeType.ACTION_DEFINITION);
    }

    @Override
    public void exitActionDefinition(SysMLv2Parser.ActionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== STATE DEFINITIONS ====================

    @Override
    public void enterStateDefinition(SysMLv2Parser.StateDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.STATE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.STATE_DEFINITION);
    }

    @Override
    public void exitStateDefinition(SysMLv2Parser.StateDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== REQUIREMENT DEFINITIONS ====================

    @Override
    public void enterRequirementDefinition(SysMLv2Parser.RequirementDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.REQUIREMENT_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.REQUIREMENT_DEFINITION);
    }

    @Override
    public void exitRequirementDefinition(SysMLv2Parser.RequirementDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== VIEW DEFINITIONS ====================

    @Override
    public void enterViewDefinition(SysMLv2Parser.ViewDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.VIEW_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.VIEW_DEFINITION);
    }

    @Override
    public void exitViewDefinition(SysMLv2Parser.ViewDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== VIEWPOINT DEFINITIONS ====================

    @Override
    public void enterViewpointDefinition(SysMLv2Parser.ViewpointDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.VIEWPOINT_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.VIEWPOINT_DEFINITION);
    }

    @Override
    public void exitViewpointDefinition(SysMLv2Parser.ViewpointDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== CONSTRAINT DEFINITIONS ====================

    @Override
    public void enterConstraintDefinition(SysMLv2Parser.ConstraintDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.CONSTRAINT_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.CONSTRAINT_DEFINITION);
    }

    @Override
    public void exitConstraintDefinition(SysMLv2Parser.ConstraintDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== ATTRIBUTE DEFINITIONS ====================

    @Override
    public void enterAttributeDefinition(SysMLv2Parser.AttributeDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.ATTRIBUTE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== PORT DEFINITIONS ====================

    @Override
    public void enterPortDefinition(SysMLv2Parser.PortDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.PORT_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== CONNECTION DEFINITIONS ====================

    @Override
    public void enterConnectionDefinition(SysMLv2Parser.ConnectionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.CONNECTION_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.CONNECTION_DEFINITION);
    }

    @Override
    public void exitConnectionDefinition(SysMLv2Parser.ConnectionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== INTERFACE DEFINITIONS ====================

    @Override
    public void enterInterfaceDefinition(SysMLv2Parser.InterfaceDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.INTERFACE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== ALLOCATION DEFINITIONS ====================

    @Override
    public void enterAllocationDefinition(SysMLv2Parser.AllocationDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.ALLOCATION_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.ALLOCATION_DEFINITION);
    }

    @Override
    public void exitAllocationDefinition(SysMLv2Parser.AllocationDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== ITEM DEFINITIONS ====================

    @Override
    public void enterItemDefinition(SysMLv2Parser.ItemDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.ITEM_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== ENUM DEFINITIONS ====================

    @Override
    public void enterEnumDefinition(SysMLv2Parser.EnumDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.ENUM_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.ENUM_DEFINITION);
    }

    @Override
    public void exitEnumDefinition(SysMLv2Parser.EnumDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== CALC DEFINITIONS ====================

    @Override
    public void enterCalcDefinition(SysMLv2Parser.CalcDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.CALC_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.CALC_DEFINITION);
    }

    @Override
    public void exitCalcDefinition(SysMLv2Parser.CalcDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== ANALYSIS DEFINITIONS ====================

    @Override
    public void enterAnalysisDefinition(SysMLv2Parser.AnalysisDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.ANALYSIS_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.ANALYSIS_DEFINITION);
    }

    @Override
    public void exitAnalysisDefinition(SysMLv2Parser.AnalysisDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== CASE/VERIFICATION DEFINITIONS ====================

    @Override
    public void enterCaseDefinition(SysMLv2Parser.CaseDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.CASE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.CASE_DEFINITION);
    }

    @Override
    public void exitCaseDefinition(SysMLv2Parser.CaseDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    @Override
    public void enterVerificationDefinition(SysMLv2Parser.VerificationDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.VERIFICATION_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.VERIFICATION_DEFINITION);
    }

    @Override
    public void exitVerificationDefinition(SysMLv2Parser.VerificationDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== USE CASE DEFINITIONS ====================

    @Override
    public void enterUseCaseDefinition(SysMLv2Parser.UseCaseDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.USE_CASE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== DATATYPE DEFINITIONS ====================

    @Override
    public void enterDatatypeDefinition(SysMLv2Parser.DatatypeDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.DATATYPE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== KERML CLASS DEFINITIONS ====================

    @Override
    public void enterClassDefinition(SysMLv2Parser.ClassDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.CLASS_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.CLASS_DEFINITION);
    }

    @Override
    public void exitClassDefinition(SysMLv2Parser.ClassDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML STRUCT DEFINITIONS ====================

    @Override
    public void enterStructDefinition(SysMLv2Parser.StructDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.STRUCT_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.STRUCT_DEFINITION);
    }

    @Override
    public void exitStructDefinition(SysMLv2Parser.StructDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML BEHAVIOR DEFINITIONS ====================

    @Override
    public void enterBehaviorDefinition(SysMLv2Parser.BehaviorDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.BEHAVIOR_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.BEHAVIOR_DEFINITION);
    }

    @Override
    public void exitBehaviorDefinition(SysMLv2Parser.BehaviorDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML FUNCTION DEFINITIONS ====================

    @Override
    public void enterFunctionDefinition(SysMLv2Parser.FunctionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.FUNCTION_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.FUNCTION_DEFINITION);
    }

    @Override
    public void exitFunctionDefinition(SysMLv2Parser.FunctionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML PREDICATE DEFINITIONS ====================

    @Override
    public void enterPredicateDefinition(SysMLv2Parser.PredicateDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.PREDICATE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.PREDICATE_DEFINITION);
    }

    @Override
    public void exitPredicateDefinition(SysMLv2Parser.PredicateDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML INTERACTION DEFINITIONS ====================

    @Override
    public void enterInteractionDefinition(SysMLv2Parser.InteractionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.INTERACTION_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.INTERACTION_DEFINITION);
    }

    @Override
    public void exitInteractionDefinition(SysMLv2Parser.InteractionDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML METACLASS DEFINITIONS ====================

    @Override
    public void enterMetaclassDefinition(SysMLv2Parser.MetaclassDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.METACLASS_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.METACLASS_DEFINITION);
    }

    @Override
    public void exitMetaclassDefinition(SysMLv2Parser.MetaclassDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML CLASSIFIER DEFINITIONS ====================

    @Override
    public void enterClassifierDefinition(SysMLv2Parser.ClassifierDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.CLASSIFIER_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.CLASSIFIER_DEFINITION);
    }

    @Override
    public void exitClassifierDefinition(SysMLv2Parser.ClassifierDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== KERML TYPE DEFINITIONS ====================

    @Override
    public void enterTypeDefinition(SysMLv2Parser.TypeDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.TYPE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== KERML FEATURE DEFINITIONS ====================

    @Override
    public void enterFeatureDefinition(SysMLv2Parser.FeatureDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.FEATURE_DEFINITION, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== KERML CONNECTOR DEFINITIONS ====================

    @Override
    public void enterConnectorDefinition(SysMLv2Parser.ConnectorDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name == null) {
            return;
        }

        // Create symbol BEFORE entering scope
        Symbol symbol = createSymbol(name, ElementType.CONNECTOR_DEFINITION, ctx);
        defineSymbol(symbol, ctx);

        symbolTable.enterScope(name, ScopeType.CONNECTOR_DEFINITION);
    }

    @Override
    public void exitConnectorDefinition(SysMLv2Parser.ConnectorDefinitionContext ctx) {
        String name = extractDeclarationName(ctx.declarationName());
        if (name != null) {
            symbolTable.exitScope();
        }
    }

    // ==================== USAGES ====================

    @Override
    public void enterPartUsage(SysMLv2Parser.PartUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.PART_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterActionUsage(SysMLv2Parser.ActionUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.ACTION_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterStateUsage(SysMLv2Parser.StateUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.STATE_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterAttributeUsage(SysMLv2Parser.AttributeUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.ATTRIBUTE_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterPortUsage(SysMLv2Parser.PortUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.PORT_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterItemUsage(SysMLv2Parser.ItemUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.ITEM_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterRequirementUsage(SysMLv2Parser.RequirementUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.REQUIREMENT_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterConstraintUsage(SysMLv2Parser.ConstraintUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.CONSTRAINT_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterConnectionUsage(SysMLv2Parser.ConnectionUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.CONNECTION_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterCalcUsage(SysMLv2Parser.CalcUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.CALC_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    @Override
    public void enterViewUsage(SysMLv2Parser.ViewUsageContext ctx) {
        String name = extractUsageName(ctx.usageName());
        if (name == null) {
            return;
        }

        Symbol symbol = createSymbol(name, ElementType.VIEW_USAGE, ctx);
        defineSymbol(symbol, ctx);
    }

    // ==================== HELPER METHODS ====================

    private Symbol createSymbol(String name, ElementType type, ParserRuleContext ctx) {
        String qualifiedName = buildQualifiedName(name);
        return new Symbol(name, qualifiedName, type, getLocation(ctx), currentVisibility);
    }

    private void defineSymbol(Symbol symbol, ParserRuleContext ctx) {
        symbol.setAstNode(ctx);
        try {
            symbolTable.define(symbol);
            LOGGER.debug("Defined {}: {}", symbol.getType(), symbol.getQualifiedName());
        } catch (IllegalStateException e) {
            LOGGER.warn("Duplicate symbol: {} - {}", symbol.getQualifiedName(), e.getMessage());
        }
    }

    private String extractQualifiedName(SysMLv2Parser.QualifiedNameContext ctx) {
        if (ctx == null) {
            return null;
        }
        return ctx.getText();
    }

    private String extractDeclarationName(SysMLv2Parser.DeclarationNameContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.name() != null) {
            return ctx.name().getText();
        }
        if (ctx.shortName() != null) {
            return ctx.shortName().getText();
        }
        return null;
    }

    private String extractUsageName(SysMLv2Parser.UsageNameContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.name() != null) {
            return ctx.name().getText();
        }
        if (ctx.shortName() != null) {
            return ctx.shortName().getText();
        }
        return null;
    }

    private String getSimpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            return qualifiedName;
        }
        if (!qualifiedName.contains("::")) {
            return qualifiedName;
        }
        String[] parts = qualifiedName.split("::");
        return parts[parts.length - 1];
    }

    private String buildQualifiedName(String simpleName) {
        String scopeQualified = symbolTable.getCurrentScope().getQualifiedName();
        if (scopeQualified.isEmpty()) {
            return simpleName;
        }
        return scopeQualified + "::" + simpleName;
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
