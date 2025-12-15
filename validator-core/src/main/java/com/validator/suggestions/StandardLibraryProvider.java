package com.validator.suggestions;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides SysML v2 standard library types and symbols for spelling suggestions.
 * Includes KerML base types, SysML types, ISQ quantities, SI units, and ScalarValues.
 */
public final class StandardLibraryProvider implements SuggestionProvider {

    private static final Set<String> STDLIB_SYMBOLS;

    static {
        Set<String> symbols = new HashSet<>();

        // Primitive types (ScalarValues)
        symbols.add("Boolean");
        symbols.add("Integer");
        symbols.add("Real");
        symbols.add("String");
        symbols.add("Natural");
        symbols.add("Positive");
        symbols.add("UnlimitedNatural");
        symbols.add("Complex");
        symbols.add("Rational");

        // KerML Base Types
        symbols.add("Base");
        symbols.add("Anything");
        symbols.add("Object");
        symbols.add("DataValue");
        symbols.add("Occurrence");
        symbols.add("Link");
        symbols.add("BinaryLink");
        symbols.add("SelfLink");
        symbols.add("Performance");
        symbols.add("Evaluation");

        // SysML Base Types
        symbols.add("Part");
        symbols.add("Action");
        symbols.add("State");
        symbols.add("Requirement");
        symbols.add("Constraint");
        symbols.add("Port");
        symbols.add("Connection");
        symbols.add("Interface");
        symbols.add("Allocation");
        symbols.add("View");
        symbols.add("Viewpoint");
        symbols.add("Rendering");
        symbols.add("Calculation");
        symbols.add("Analysis");
        symbols.add("Case");
        symbols.add("Verification");
        symbols.add("UseCase");
        symbols.add("Concern");

        // ISQ Quantities
        symbols.add("QuantityValue");
        symbols.add("MassValue");
        symbols.add("LengthValue");
        symbols.add("TimeValue");
        symbols.add("TemperatureValue");
        symbols.add("AmountOfSubstanceValue");
        symbols.add("ElectricCurrentValue");
        symbols.add("LuminousIntensityValue");
        symbols.add("VelocityValue");
        symbols.add("AccelerationValue");
        symbols.add("ForceValue");
        symbols.add("PressureValue");
        symbols.add("EnergyValue");
        symbols.add("PowerValue");
        symbols.add("FrequencyValue");
        symbols.add("AreaValue");
        symbols.add("VolumeValue");
        symbols.add("DensityValue");
        symbols.add("AngleValue");
        symbols.add("SolidAngleValue");
        symbols.add("TorqueValue");

        // SI Base Units
        symbols.add("m");
        symbols.add("kg");
        symbols.add("s");
        symbols.add("A");
        symbols.add("K");
        symbols.add("mol");
        symbols.add("cd");

        // SI Derived Units
        symbols.add("N");
        symbols.add("Pa");
        symbols.add("J");
        symbols.add("W");
        symbols.add("Hz");
        symbols.add("C");
        symbols.add("V");
        symbols.add("F");
        symbols.add("ohm");
        symbols.add("S");
        symbols.add("Wb");
        symbols.add("T");
        symbols.add("H");
        symbols.add("lm");
        symbols.add("lx");
        symbols.add("Bq");
        symbols.add("Gy");
        symbols.add("Sv");
        symbols.add("kat");
        symbols.add("rad");
        symbols.add("sr");
        symbols.add("degC");
        symbols.add("L");
        symbols.add("g");
        symbols.add("km");
        symbols.add("mm");
        symbols.add("cm");

        // Collections Library
        symbols.add("Array");
        symbols.add("Set");
        symbols.add("Bag");
        symbols.add("OrderedSet");
        symbols.add("List");
        symbols.add("Map");

        // Analysis Library
        symbols.add("TradeStudy");
        symbols.add("TradeStudyObjective");
        symbols.add("Alternative");
        symbols.add("EvaluationFunction");
        symbols.add("ObjectiveFunction");

        // Common Library Elements
        symbols.add("ScalarValues");
        symbols.add("ISQ");
        symbols.add("SI");
        symbols.add("KerML");
        symbols.add("SysML");
        symbols.add("Quantities");
        symbols.add("Units");
        symbols.add("TradeStudies");
        symbols.add("Actions");
        symbols.add("Parts");
        symbols.add("Attributes");
        symbols.add("Constraints");
        symbols.add("Requirements");

        STDLIB_SYMBOLS = Set.copyOf(symbols);
    }

    @Override
    public Set<String> getCandidates() {
        return STDLIB_SYMBOLS;
    }

    @Override
    public String getSourceName() {
        return "stdlib";
    }

    /**
     * Gets the number of standard library symbols.
     *
     * @return count of symbols
     */
    public static int getSymbolCount() {
        return STDLIB_SYMBOLS.size();
    }
}
