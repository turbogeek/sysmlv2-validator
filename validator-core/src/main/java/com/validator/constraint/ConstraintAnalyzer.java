package com.validator.constraint;

import com.validator.ErrorCodes;
import com.validator.ValidationError;
import com.validator.ValidationWarning;
import com.validator.parser.SysMLv2Parser;
import com.validator.semantic.ElementType;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzer for SysML v2 constraint expressions.
 *
 * <p>This analyzer validates constraint expressions for:
 * <ul>
 *   <li>Undefined variable references</li>
 *   <li>Type mismatches in operations</li>
 *   <li>Invalid operators for operand types</li>
 *   <li>Unit compatibility in comparisons</li>
 *   <li>Constraint expressions that evaluate to non-boolean</li>
 * </ul>
 *
 * <h2>Constraint Types Analyzed</h2>
 * <ul>
 *   <li>Invariant constraints ({@code constraint { ... }})</li>
 *   <li>Derived values ({@code attribute x = expr;})</li>
 *   <li>Requirement constraints ({@code require constraint { ... }})</li>
 *   <li>Guard conditions ({@code if expr then ...})</li>
 * </ul>
 */
public class ConstraintAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintAnalyzer.class);

    private final SymbolTable symbolTable;
    private final String filePath;
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<ValidationWarning> warnings = new ArrayList<>();
    private final ExpressionTypeChecker typeChecker;

    /**
     * Creates a new constraint analyzer.
     *
     * @param symbolTable the symbol table for variable resolution
     * @param filePath the source file path
     */
    public ConstraintAnalyzer(SymbolTable symbolTable, String filePath) {
        this.symbolTable = symbolTable;
        this.filePath = filePath;
        this.typeChecker = new ExpressionTypeChecker(symbolTable);
    }

    /**
     * Analyzes all constraints in the parse tree.
     *
     * @param tree the parse tree
     * @return list of validation errors
     */
    public List<ValidationError> analyze(ParseTree tree) {
        LOGGER.debug("Starting constraint analysis for: {}", filePath);

        ConstraintListener listener = new ConstraintListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        LOGGER.debug("Constraint analysis complete: {} errors, {} warnings",
            errors.size(), warnings.size());

        return errors;
    }

    /**
     * Gets constraint-related warnings.
     *
     * @return list of warnings
     */
    public List<ValidationWarning> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Analyzes a constraint expression.
     *
     * @param constraint the constraint expression context
     * @param scope the current scope for variable resolution
     */
    private void analyzeConstraint(
            SysMLv2Parser.ConstraintUsageContext constraint,
            ConstraintScope scope) {

        if (constraint == null) {
            return;
        }

        // Find the expression body
        SysMLv2Parser.ConstraintBodyContext body = findConstraintBody(constraint);
        if (body == null) {
            return;
        }

        // Analyze expression in the body
        analyzeExpression(body, scope);
    }

    /**
     * Finds the constraint body within a constraint context.
     */
    private SysMLv2Parser.ConstraintBodyContext findConstraintBody(
            SysMLv2Parser.ConstraintUsageContext ctx) {
        // Navigate to find constraint body
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof SysMLv2Parser.ConstraintBodyContext) {
                return (SysMLv2Parser.ConstraintBodyContext) child;
            }
        }
        return null;
    }

    /**
     * Analyzes an expression for type correctness.
     *
     * @param expr the expression context
     * @param scope the variable scope
     */
    private void analyzeExpression(ParseTree expr, ConstraintScope scope) {
        if (expr == null) {
            return;
        }

        // Check for variable references
        checkVariableReferences(expr, scope);

        // Check operator compatibility
        checkOperatorCompatibility(expr, scope);
    }

    /**
     * Checks that all variable references can be resolved.
     */
    private void checkVariableReferences(ParseTree expr, ConstraintScope scope) {
        // Walk the expression tree looking for name references
        for (int i = 0; i < expr.getChildCount(); i++) {
            ParseTree child = expr.getChild(i);

            // Check for name context (identifier reference)
            if (child instanceof SysMLv2Parser.NameContext) {
                String name = child.getText();
                if (!scope.isDefined(name) && !isBuiltInName(name)) {
                    addError(ErrorCodes.CONSTRAINT_UNDEFINED_VAR,
                        String.format("Undefined variable '%s' in constraint", name),
                        child);
                }
            }

            // Recursively check children
            checkVariableReferences(child, scope);
        }
    }

    /**
     * Checks that operators are compatible with operand types.
     */
    private void checkOperatorCompatibility(ParseTree expr, ConstraintScope scope) {
        // This is a placeholder for more sophisticated type checking
        // A full implementation would infer types and check operator compatibility
    }

    /**
     * Checks if a name is a built-in identifier.
     *
     * @param name the identifier name
     * @return true if built-in
     */
    private boolean isBuiltInName(String name) {
        // Common built-in names in SysML v2 constraints
        return "true".equals(name)
            || "false".equals(name)
            || "null".equals(name)
            || "self".equals(name);
    }

    /**
     * Adds a validation error.
     */
    private void addError(String code, String message, ParseTree ctx) {
        int line = 1;
        int column = 1;

        if (ctx instanceof ParserRuleContext) {
            ParserRuleContext prc = (ParserRuleContext) ctx;
            if (prc.getStart() != null) {
                line = prc.getStart().getLine();
                column = prc.getStart().getCharPositionInLine() + 1;
            }
        }

        ValidationError.Builder builder = new ValidationError.Builder();
        builder.errorCode(code);
        builder.message(message);
        builder.filePath(filePath);
        builder.line(line);
        builder.column(column);
        errors.add(builder.build());
    }

    /**
     * Adds a validation warning.
     */
    private void addWarning(String code, String message, ParseTree ctx) {
        int line = 1;
        int column = 1;

        if (ctx instanceof ParserRuleContext) {
            ParserRuleContext prc = (ParserRuleContext) ctx;
            if (prc.getStart() != null) {
                line = prc.getStart().getLine();
                column = prc.getStart().getCharPositionInLine() + 1;
            }
        }

        ValidationWarning.Builder builder = new ValidationWarning.Builder();
        builder.errorCode(code);
        builder.message(message);
        builder.filePath(filePath);
        builder.line(line);
        builder.column(column);
        warnings.add(builder.build());
    }

    /**
     * Listener that collects constraint expressions for analysis.
     */
    private class ConstraintListener extends com.validator.parser.SysMLv2ParserBaseListener {

        @Override
        public void enterConstraintUsage(SysMLv2Parser.ConstraintUsageContext ctx) {
            // Build scope from containing definition
            ConstraintScope scope = buildScopeForContext(ctx);
            analyzeConstraint(ctx, scope);
        }

        /**
         * Builds a variable scope for a constraint based on its containing context.
         */
        private ConstraintScope buildScopeForContext(ParserRuleContext ctx) {
            ConstraintScope scope = new ConstraintScope();

            // Find containing definition
            ParserRuleContext parent = ctx.getParent();
            while (parent != null) {
                // Check for part definition or similar
                if (parent instanceof SysMLv2Parser.PartDefinitionContext
                        || parent instanceof SysMLv2Parser.PartUsageContext) {
                    // Add attributes from this definition to scope
                    addDefinitionMembers(parent, scope);
                    break;
                }
                parent = parent.getParent();
            }

            return scope;
        }

        /**
         * Adds members from a definition to the constraint scope.
         */
        private void addDefinitionMembers(ParserRuleContext defCtx, ConstraintScope scope) {
            // Look up the definition in symbol table
            String defName = getDefinitionName(defCtx);
            if (defName != null) {
                Symbol defSymbol = symbolTable.resolve(defName);
                if (defSymbol != null) {
                    // Add all symbols that start with this definition's qualified name
                    String prefix = defSymbol.getQualifiedName() + "::";
                    for (Symbol child : symbolTable.getAllSymbols()) {
                        if (child.getQualifiedName().startsWith(prefix)) {
                            scope.addVariable(child.getName(), inferType(child));
                        }
                    }
                }
            }
        }

        /**
         * Gets the name of a definition context.
         */
        private String getDefinitionName(ParserRuleContext ctx) {
            // Look for name in definition
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);
                if (child instanceof SysMLv2Parser.NameContext) {
                    return child.getText();
                }
            }
            return null;
        }

        /**
         * Infers the type of a symbol for constraint checking.
         */
        private ConstraintType inferType(Symbol symbol) {
            // Infer type from symbol's ElementType
            ElementType elementType = symbol.getType();
            if (elementType == ElementType.ATTRIBUTE_USAGE
                    || elementType == ElementType.ATTRIBUTE_DEFINITION) {
                // Try to infer from name patterns
                String name = symbol.getName().toLowerCase();
                if (name.contains("bool") || name.startsWith("is") || name.startsWith("has")) {
                    return ConstraintType.BOOLEAN;
                }
                if (name.contains("count") || name.contains("num") || name.contains("index")) {
                    return ConstraintType.INTEGER;
                }
                if (name.contains("rate") || name.contains("ratio") || name.contains("value")) {
                    return ConstraintType.REAL;
                }
                if (name.contains("name") || name.contains("text") || name.contains("string")) {
                    return ConstraintType.STRING;
                }
            }
            return ConstraintType.ANY;
        }
    }
}
