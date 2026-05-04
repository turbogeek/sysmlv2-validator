package com.validator.lint;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.ast.Location;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

/**
 * Lint rule that detects unit completeness issues.
 *
 * <p>Generates warnings for:
 * <ul>
 *   <li>Attribute with ISQ type (e.g., LengthValue) assigned a value without a unit</li>
 *   <li>Attribute with generic type (e.g., Real) assigned a value with a unit</li>
 * </ul>
 */
public class ValueUnitCompletenessRule implements LintRule {

    private static final String RULE_ID = "unit-completeness";
    private static final String CATEGORY = "units";
    private static final String DESCRIPTION = "Detects missing units or improper generic typing for values with units";

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
            ErrorCodes.LINT_MISSING_UNIT,
            ErrorCodes.LINT_GENERIC_TYPE_WITH_UNIT
        };
    }

    @Override
    public List<ValidationWarning> analyze(LintContext context) {
        List<ValidationWarning> warnings = new ArrayList<>();

        if (context.getParseTree() == null) {
            return warnings;
        }

        ValueUnitAnalyzer analyzer = new ValueUnitAnalyzer(context.getFilePath(), warnings);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(analyzer, context.getParseTree());

        return warnings;
    }

    private static class ValueUnitAnalyzer extends SysMLv2ParserBaseListener {
        private final String filePath;
        private final List<ValidationWarning> warnings;

        ValueUnitAnalyzer(String filePath, List<ValidationWarning> warnings) {
            this.filePath = filePath;
            this.warnings = warnings;
        }

        @Override
        public void enterAttributeUsage(SysMLv2Parser.AttributeUsageContext ctx) {
            checkValueUnit(ctx.featureRelationships(), ctx.usageName(), ctx.valueInit(), ctx);
        }

        private void checkValueUnit(SysMLv2Parser.FeatureRelationshipsContext featureRels,
                                     SysMLv2Parser.UsageNameContext usageName,
                                     SysMLv2Parser.ValueInitContext valueInit,
                                     ParserRuleContext ctx) {
            
            if (valueInit == null && featureRels != null) {
                for (var featureRel : featureRels.featureRelationship()) {
                    if (featureRel.valueInit() != null) {
                        valueInit = featureRel.valueInit();
                        break;
                    }
                }
            }
            if (valueInit == null) {
                return;
            }

            List<String> typeNames = extractTypeNames(featureRels);
            if (typeNames.isEmpty()) {
                return;
            }

            String unit = extractUnit(valueInit);
            String elementName = extractUsageName(usageName);
            elementName = elementName != null ? elementName : "<anonymous>";

            for (String typeName : typeNames) {
                if (isDimensionalType(typeName)) {
                    if (unit == null) {
                        warnings.add(createWarning(
                            ErrorCodes.LINT_MISSING_UNIT,
                            String.format("Value for '%s' of type '%s' is missing a unit annotation", elementName, typeName),
                            getLocation(ctx),
                            String.format("Add a unit annotation, e.g., [%s_UNIT], to the value.", typeName.replace("Value", "").toLowerCase())
                        ));
                    }
                } else if (isGenericType(typeName)) {
                    if (unit != null) {
                        warnings.add(createWarning(
                            ErrorCodes.LINT_GENERIC_TYPE_WITH_UNIT,
                            String.format("Value for '%s' has unit '[%s]' but is typed generically as '%s'", elementName, unit, typeName),
                            getLocation(ctx),
                            "Use a specific dimensional type (e.g., LengthValue, MassValue) for unit safety."
                        ));
                    }
                }
            }
        }

        private boolean isDimensionalType(String typeName) {
            switch (typeName) {
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

        private boolean isGenericType(String typeName) {
            return "Real".equals(typeName) || "Integer".equals(typeName) || "NumericalValue".equals(typeName) || "ScalarValue".equals(typeName);
        }

        private List<String> extractTypeNames(SysMLv2Parser.FeatureRelationshipsContext featureRels) {
            List<String> typeNames = new ArrayList<>();
            if (featureRels == null || featureRels.featureRelationship() == null) {
                return typeNames;
            }
            for (var featureRel : featureRels.featureRelationship()) {
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

        private String extractUnit(SysMLv2Parser.ValueInitContext valueInit) {
            if (valueInit.expression() == null) return null;
            return findUnitRecursively(valueInit.expression());
        }

        private String findUnitRecursively(ParserRuleContext ctx) {
            if (ctx == null) return null;
            
            if (ctx instanceof SysMLv2Parser.SequenceSuffixContext) {
                SysMLv2Parser.SequenceSuffixContext seqSuffix = (SysMLv2Parser.SequenceSuffixContext) ctx;
                if (seqSuffix.LBRACK() != null && seqSuffix.RBRACK() != null && seqSuffix.expression() != null) {
                    return seqSuffix.expression().getText();
                }
            }
            
            for (int i = 0; i < ctx.getChildCount(); i++) {
                if (ctx.getChild(i) instanceof ParserRuleContext) {
                    String unit = findUnitRecursively((ParserRuleContext) ctx.getChild(i));
                    if (unit != null) {
                        return unit;
                    }
                }
            }
            return null;
        }

        private String extractUsageName(SysMLv2Parser.UsageNameContext ctx) {
            if (ctx == null) return null;
            if (ctx.name() != null) return ctx.name().getText();
            if (ctx.shortName() != null) return ctx.shortName().getText();
            return null;
        }

        private Location getLocation(ParserRuleContext ctx) {
            if (ctx == null || ctx.getStart() == null) return null;
            return new Location(filePath, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
        }

        private ValidationWarning createWarning(String code, String message, Location location, String suggestion) {
            ValidationWarning.Builder builder = new ValidationWarning.Builder();
            builder.errorCode(code);
            builder.message(message);
            builder.specReference("SysMLv2 Specification, Section 7.13.4");

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
