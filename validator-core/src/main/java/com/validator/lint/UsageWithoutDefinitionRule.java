package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserBaseListener;
import com.validator.semantic.Symbol;
import com.validator.semantic.SymbolTable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

/**
 * Lint rule that detects usages without corresponding definitions.
 *
 * <p>In SysML v2, it is syntactically valid to have usages without explicit
 * definitions (e.g., {@code part x;} without a {@code part def}). While this
 * is allowed by the language, it may indicate:
 * <ul>
 *   <li>A missing type that should be defined</li>
 *   <li>A reference to a type that hasn't been imported</li>
 *   <li>An intentional anonymous/untyped usage</li>
 * </ul>
 *
 * <p>This rule generates warnings for:
 * <ul>
 *   <li>Part usages without a type ({@code part x;} instead of {@code part x : SomeType;})</li>
 *   <li>Attribute usages without a type ({@code attribute y;} instead of {@code attribute y : Integer;})</li>
 *   <li>Type references that don't resolve to any definition</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>This rule respects the "missing-types" category in lint configuration.
 */
public class UsageWithoutDefinitionRule implements LintRule {

    private static final String RULE_ID = "usage-without-definition";
    private static final String CATEGORY = "missing-types";
    private static final String DESCRIPTION = "Detects usages without type definitions or with unresolved types";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String[] getErrorCodes() {
        return new String[] {
            ErrorCodes.LINT_MISSING_DEFINITION,
            ErrorCodes.LINT_UNTYPED_ATTRIBUTE,
            ErrorCodes.LINT_UNTYPED_PART
        };
    }

    @Override
    public List<ValidationWarning> analyze(LintContext context) {
        List<ValidationWarning> warnings = new ArrayList<>();

        if (context.getParseTree() == null || context.getSymbolTable() == null) {
            return warnings;
        }

        // Walk the parse tree to find usages and check their types
        UsageAnalyzer analyzer = new UsageAnalyzer(
            context.getSymbolTable(),
            context.getFilePath(),
            warnings
        );

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(analyzer, context.getParseTree());

        return warnings;
    }

    /**
     * Listener that analyzes usages for missing type definitions.
     */
    private static class UsageAnalyzer extends SysMLv2ParserBaseListener {
        private final SymbolTable symbolTable;
        private final String filePath;
        private final List<ValidationWarning> warnings;

        UsageAnalyzer(SymbolTable symbolTable, String filePath, List<ValidationWarning> warnings) {
            this.symbolTable = symbolTable;
            this.filePath = filePath;
            this.warnings = warnings;
        }

        @Override
        public void enterPartUsage(SysMLv2Parser.PartUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "part", ctx);
        }

        @Override
        public void enterAttributeUsage(SysMLv2Parser.AttributeUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "attribute", ctx);
        }

        @Override
        public void enterPortUsage(SysMLv2Parser.PortUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "port", ctx);
        }

        @Override
        public void enterItemUsage(SysMLv2Parser.ItemUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "item", ctx);
        }

        @Override
        public void enterActionUsage(SysMLv2Parser.ActionUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "action", ctx);
        }

        @Override
        public void enterStateUsage(SysMLv2Parser.StateUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "state", ctx);
        }

        @Override
        public void enterRequirementUsage(SysMLv2Parser.RequirementUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "requirement", ctx);
        }

        @Override
        public void enterConstraintUsage(SysMLv2Parser.ConstraintUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "constraint", ctx);
        }

        @Override
        public void enterConnectionUsage(SysMLv2Parser.ConnectionUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "connection", ctx);
        }

        @Override
        public void enterCalcUsage(SysMLv2Parser.CalcUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "calc", ctx);
        }

        @Override
        public void enterViewUsage(SysMLv2Parser.ViewUsageContext ctx) {
            checkUsageType(ctx.featureRelationships(), ctx.usageName(), "view", ctx);
        }

        private void checkUsageType(SysMLv2Parser.FeatureRelationshipsContext featureRels,
                                     SysMLv2Parser.UsageNameContext usageName,
                                     String usageKind,
                                     ParserRuleContext ctx) {
            String elementName = extractUsageName(usageName);

            // Extract type names from featureRelationships if present
            List<String> typeNames = extractTypeNames(featureRels);

            // Case 1: No type specified at all
            if (typeNames.isEmpty()) {
                // Untyped usage
                String errorCode = usageKind.equals("attribute")
                    ? ErrorCodes.LINT_UNTYPED_ATTRIBUTE
                    : ErrorCodes.LINT_UNTYPED_PART;

                warnings.add(createWarning(
                    errorCode,
                    String.format("Untyped %s '%s' has no type definition",
                        usageKind, elementName != null ? elementName : "<anonymous>"),
                    getLocation(ctx),
                    String.format("Add a type with ': TypeName' or define a %s def", usageKind)
                ));
                return;
            }

            // Case 2: Type specified but may not resolve
            for (String typeName : typeNames) {
                if (!typeExists(typeName)) {
                    warnings.add(createWarning(
                        ErrorCodes.LINT_MISSING_DEFINITION,
                        String.format("Type '%s' used in %s '%s' has no definition",
                            typeName, usageKind, elementName != null ? elementName : "<anonymous>"),
                        getLocation(ctx),
                        String.format("Define '%s' with '%s def %s' or import it",
                            typeName, usageKind, typeName)
                    ));
                }
            }
        }

        /**
         * Extract type names from featureRelationships.
         * Looks for typingClause which has format ": TypeName" or ": ~TypeName"
         */
        private List<String> extractTypeNames(SysMLv2Parser.FeatureRelationshipsContext featureRels) {
            List<String> typeNames = new ArrayList<>();

            if (featureRels == null || featureRels.featureRelationship() == null) {
                return typeNames;
            }

            for (var featureRel : featureRels.featureRelationship()) {
                // Check typeRelationship which contains typingClause
                if (featureRel.typeRelationship() != null) {
                    var typeRel = featureRel.typeRelationship();
                    if (typeRel.typingClause() != null) {
                        var typingClause = typeRel.typingClause();
                        if (typingClause.qualifiedName() != null) {
                            for (var qn : typingClause.qualifiedName()) {
                                typeNames.add(qn.getText());
                            }
                        }
                    }
                }
            }

            return typeNames;
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

        private boolean typeExists(String typeName) {
            if (typeName == null || typeName.isEmpty()) {
                return false;
            }

            // Check if it's a standard library type
            if (isStandardLibraryType(typeName)) {
                return true;
            }

            // Check symbol table
            Symbol symbol = symbolTable.resolveQualified(typeName);
            if (symbol != null) {
                return true;
            }

            // Try simple name resolution
            symbol = symbolTable.resolve(typeName);
            if (symbol != null) {
                return true;
            }

            // Check for common built-in types
            if (isBuiltinType(typeName)) {
                return true;
            }

            return false;
        }

        private boolean isStandardLibraryType(String typeName) {
            // Common standard library types
            return typeName.startsWith("ISQ::") ||
                   typeName.startsWith("SI::") ||
                   typeName.startsWith("USCustomary::") ||
                   typeName.startsWith("ScalarValues::") ||
                   typeName.startsWith("Base::") ||
                   typeName.startsWith("Quantities::") ||
                   typeName.startsWith("KerML::");
        }

        private boolean isBuiltinType(String typeName) {
            // Built-in primitive types
            switch (typeName) {
                case "Boolean":
                case "Integer":
                case "Natural":
                case "Positive":
                case "Real":
                case "String":
                case "ScalarValue":
                case "NumericalValue":
                case "Complex":
                case "Rational":
                case "MassValue":
                case "LengthValue":
                case "TimeValue":
                case "VelocityValue":
                case "AccelerationValue":
                case "ForceValue":
                case "TorqueValue":
                case "EnergyValue":
                case "PowerValue":
                case "PressureValue":
                case "TemperatureValue":
                case "DensityValue":
                case "VolumeValue":
                case "AreaValue":
                case "AngleValue":
                case "FrequencyValue":
                case "ElectricCurrentValue":
                case "VoltageValue":
                case "ResistanceValue":
                case "CapacitanceValue":
                case "InductanceValue":
                    return true;
                default:
                    return false;
            }
        }

        private Location getLocation(ParserRuleContext ctx) {
            if (ctx == null || ctx.getStart() == null) {
                return null;
            }
            return new Location(
                filePath,
                ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine()
            );
        }

        private ValidationWarning createWarning(String code, String message, Location location, String suggestion) {
            ValidationWarning.Builder builder = new ValidationWarning.Builder();
            builder.errorCode(code);
            builder.message(message);

            if (location != null) {
                builder.filePath(location.getFileName());
                builder.line(location.getLine());
                builder.column(location.getColumn());
            }

            if (suggestion != null) {
                builder.addSuggestion(suggestion);
            }

            return builder.build();
        }
    }
}
