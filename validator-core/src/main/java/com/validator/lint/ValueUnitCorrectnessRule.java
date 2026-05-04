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
 * Lint rule that detects unit mismatches between type and value.
 *
 * <p>Generates warnings/errors for:
 * <ul>
 *   <li>Attribute with ISQ type (e.g., LengthValue) assigned a value with an incompatible unit (e.g., [kg])</li>
 * </ul>
 */
public class ValueUnitCorrectnessRule implements LintRule {

    private static final String RULE_ID = "unit-correctness";
    private static final String CATEGORY = "units";
    private static final String DESCRIPTION = "Detects unit mismatches between type and value annotation";

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
            ErrorCodes.LINT_UNIT_MISMATCH
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
            if (unit == null) {
                return;
            }

            String elementName = extractUsageName(usageName);
            elementName = elementName != null ? elementName : "<anonymous>";

            for (String typeName : typeNames) {
                if (isDimensionalType(typeName)) {
                    if (!isCompatibleUnit(typeName, unit)) {
                        warnings.add(createWarning(
                            ErrorCodes.LINT_UNIT_MISMATCH,
                            String.format("Value for '%s' of type '%s' has incompatible unit '[%s]'", elementName, typeName, unit),
                            getLocation(ctx),
                            String.format("Change the unit to match the dimension of '%s', or change the type.", typeName)
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

        private boolean isCompatibleUnit(String typeName, String unit) {
            // Strip brackets if they somehow made it here (extractUnit strips them, but just in case)
            unit = unit.replace("[", "").replace("]", "").trim();
            
            // Allow certain prefixes: k, m, c, d, h, da, etc.
            // For simplicity in a static analyzer without full unit algebra, we use heuristics for common units
            switch (typeName) {
                case "MassValue":
                    return unit.matches("(k|m|c)?g|lb|oz|ton");
                case "LengthValue":
                    return unit.matches("(k|m|c|m)?m|in|ft|yd|mi");
                case "TimeValue":
                    return unit.matches("s|ms|min|h|hr|d|day|yr");
                case "TemperatureValue":
                    return unit.matches("K|C|F");
                case "VelocityValue":
                    return unit.matches("m/s|km/h|mph|kn|ft/s");
                case "AccelerationValue":
                    return unit.matches("m/s\\^2|m/s2|g");
                case "ForceValue":
                    return unit.matches("N|kN|lbf");
                case "EnergyValue":
                    return unit.matches("J|kJ|cal|kcal|Wh|kWh");
                case "PowerValue":
                    return unit.matches("W|kW|MW|hp");
                case "PressureValue":
                    return unit.matches("Pa|kPa|MPa|bar|psi|atm");
                case "VolumeValue":
                    return unit.matches("l|L|ml|mL|gal|m\\^3|cm\\^3");
                case "AreaValue":
                    return unit.matches("m\\^2|cm\\^2|km\\^2|sq ft|acre");
                case "AngleValue":
                    return unit.matches("rad|deg");
                case "FrequencyValue":
                    return unit.matches("Hz|kHz|MHz|GHz");
                case "ElectricCurrentValue":
                    return unit.matches("A|mA|kA");
                case "VoltageValue":
                    return unit.matches("V|mV|kV");
                case "ResistanceValue":
                    return unit.matches("ohm|kOhm|MOhm");
                default:
                    // If we don't have explicit checks, be permissive to avoid false positives
                    return true;
            }
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
            // Traverse expression to find sequenceSuffix containing a unit [m]
            if (valueInit.expression() == null) return null;
            return findUnitRecursively(valueInit.expression());
        }

        private String findUnitRecursively(ParserRuleContext ctx) {
            if (ctx == null) return null;
            
            // Check if this context is sequenceSuffix and has LBRACK expression RBRACK
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
