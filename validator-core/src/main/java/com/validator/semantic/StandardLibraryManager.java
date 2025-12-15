package com.validator.semantic;

import com.validator.ast.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the SysML v2 standard library symbols including
 * primitive types, KerML base types, SysML types, ISQ quantities, and SI units.
 */
public final class StandardLibraryManager {

    private static final String STDLIB_LOCATION = "<stdlib>";
    private static final Location STDLIB_LOC = new Location(STDLIB_LOCATION, 1, 0);

    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private final Map<String, Symbol> unqualifiedLookup = new LinkedHashMap<>();
    private final Map<String, List<Symbol>> packageSymbols = new LinkedHashMap<>();

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
        "Boolean", "Integer", "Real", "String", "Natural", "Positive",
        "UnlimitedNatural", "Complex", "Rational"
    );

    private static final Set<String> QUANTITY_TYPES = Set.of(
        "QuantityValue", "MassValue", "LengthValue", "TimeValue",
        "TemperatureValue", "AmountOfSubstanceValue", "ElectricCurrentValue",
        "LuminousIntensityValue", "VelocityValue", "AccelerationValue",
        "ForceValue", "PressureValue", "EnergyValue", "PowerValue",
        "FrequencyValue", "AreaValue", "VolumeValue", "DensityValue",
        "AngleValue", "SolidAngleValue", "TorqueValue"
    );

    private static final Set<String> SI_UNITS = Set.of(
        "m", "kg", "s", "A", "K", "mol", "cd",
        "N", "Pa", "J", "W", "Hz", "C", "V", "F", "ohm",
        "S", "Wb", "T", "H", "lm", "lx", "Bq", "Gy", "Sv", "kat",
        "rad", "sr", "degC", "L", "g", "km", "mm", "cm"
    );

    /**
     * Initialize the standard library with built-in symbols.
     */
    public void initializeBuiltins() {
        initializePrimitiveTypes();
        initializeKerMLTypes();
        initializeSysMLTypes();
        initializeISQTypes();
        initializeSIUnits();
        initializeScalarValues();
        initializeTradeStudies();
    }

    private void initializePrimitiveTypes() {
        for (String type : PRIMITIVE_TYPES) {
            registerSymbol(type, "ScalarValues::" + type, ElementType.DATA_TYPE);
            // Also available unqualified
            if (!unqualifiedLookup.containsKey(type)) {
                unqualifiedLookup.put(type, symbols.get("ScalarValues::" + type));
            }
        }
    }

    private void initializeKerMLTypes() {
        String[] kermlTypes = {
            "Base", "Anything", "Object", "DataValue", "Occurrence",
            "Links", "Link", "BinaryLink", "SelfLink",
            "Performances", "Performance", "Evaluation",
            "TransfersBefore", "TransfersAfter", "HappensBefore", "HappensAfter"
        };
        for (String type : kermlTypes) {
            registerSymbol(type, "KerML::" + type, ElementType.CLASS_DEFINITION);
        }
    }

    private void initializeSysMLTypes() {
        String[][] sysmlTypes = {
            {"Part", "PART_DEFINITION"},
            {"Action", "ACTION_DEFINITION"},
            {"Requirement", "REQUIREMENT_DEFINITION"},
            {"State", "STATE_DEFINITION"},
            {"Port", "PORT_DEFINITION"},
            {"Connection", "CONNECTION_DEFINITION"},
            {"Interface", "INTERFACE_DEFINITION"},
            {"Constraint", "CONSTRAINT_DEFINITION"},
            {"Attribute", "ATTRIBUTE_DEFINITION"},
            {"Item", "ITEM_DEFINITION"},
            {"View", "VIEW_DEFINITION"},
            {"Viewpoint", "VIEWPOINT_DEFINITION"},
            {"Allocation", "ALLOCATION_DEFINITION"},
            {"Analysis", "ANALYSIS_DEFINITION"},
            {"Calculation", "CALC_DEFINITION"},
            {"Case", "CASE_DEFINITION"},
            {"Verification", "VERIFICATION_DEFINITION"},
            {"UseCase", "USE_CASE_DEFINITION"},
            {"Concern", "CONCERN_DEFINITION"},
            {"Rendering", "RENDERING_DEFINITION"}
        };
        for (String[] typePair : sysmlTypes) {
            registerSymbol(typePair[0], "SysML::" + typePair[0],
                ElementType.valueOf(typePair[1]));
        }

        // Register nested SysML elements
        registerSymbol("Actions", "SysML::Actions", ElementType.PACKAGE);
        registerSymbol("start", "SysML::Actions::Action::start", ElementType.ACTION_USAGE);
        registerSymbol("done", "SysML::Actions::Action::done", ElementType.ACTION_USAGE);
    }

    private void initializeISQTypes() {
        for (String type : QUANTITY_TYPES) {
            registerSymbol(type, "ISQ::" + type, ElementType.ATTRIBUTE_DEFINITION);
        }
    }

    private void initializeSIUnits() {
        for (String unit : SI_UNITS) {
            registerSymbol(unit, "SI::" + unit, ElementType.ATTRIBUTE_USAGE);
        }
    }

    private void initializeScalarValues() {
        // ScalarValues package contains primitive type definitions
        registerSymbol("ScalarValues", "ScalarValues", ElementType.PACKAGE);
        // Primitive types already registered in initializePrimitiveTypes
    }

    private void initializeTradeStudies() {
        // TradeStudies analysis library
        registerSymbol("TradeStudies", "TradeStudies", ElementType.PACKAGE);
        registerSymbol("TradeStudy", "TradeStudies::TradeStudy", ElementType.ANALYSIS_DEFINITION);
        registerSymbol("TradeStudyObjective", "TradeStudies::TradeStudyObjective",
            ElementType.CALC_DEFINITION);
        registerSymbol("Alternative", "TradeStudies::Alternative", ElementType.PART_DEFINITION);
        registerSymbol("EvaluationFunction", "TradeStudies::EvaluationFunction",
            ElementType.CALC_DEFINITION);
        registerSymbol("ObjectiveFunction", "TradeStudies::ObjectiveFunction",
            ElementType.CALC_DEFINITION);
    }

    private void registerSymbol(String name, String qualifiedName, ElementType type) {
        Symbol symbol = new Symbol(name, qualifiedName, type, STDLIB_LOC, Visibility.PUBLIC);
        symbols.put(qualifiedName, symbol);

        // Add to unqualified lookup (first match wins)
        if (!unqualifiedLookup.containsKey(name)) {
            unqualifiedLookup.put(name, symbol);
        }

        // Add to package lookup
        String packageName = extractPackageName(qualifiedName);
        packageSymbols.computeIfAbsent(packageName, k -> new ArrayList<>()).add(symbol);
    }

    private String extractPackageName(String qualifiedName) {
        int lastSep = qualifiedName.lastIndexOf("::");
        if (lastSep > 0) {
            String path = qualifiedName.substring(0, lastSep);
            // Return the top-level package
            int firstSep = path.indexOf("::");
            if (firstSep > 0) {
                return path.substring(0, firstSep);
            }
            return path;
        }
        return qualifiedName;
    }

    /**
     * Resolve a symbol by name (qualified or unqualified).
     *
     * @param name the symbol name
     * @return the symbol if found, null otherwise
     */
    public Symbol resolveSymbol(String name) {
        if (name == null) {
            return null;
        }
        // Try qualified first
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        // Try unqualified
        return unqualifiedLookup.get(name);
    }

    /**
     * Get all symbols in a package.
     *
     * @param packageName the package name
     * @return list of symbols in the package
     */
    public List<Symbol> getSymbolsInPackage(String packageName) {
        List<Symbol> result = packageSymbols.get(packageName);
        return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
    }

    /**
     * Check if a name is a primitive type.
     *
     * @param name the type name
     * @return true if it's a primitive type
     */
    public boolean isPrimitiveType(String name) {
        return PRIMITIVE_TYPES.contains(name);
    }

    /**
     * Check if a name is a quantity type.
     *
     * @param name the type name
     * @return true if it's a quantity type
     */
    public boolean isQuantityType(String name) {
        return QUANTITY_TYPES.contains(name);
    }

    /**
     * Check if a name is a SI unit.
     *
     * @param name the unit name
     * @return true if it's a SI unit
     */
    public boolean isUnit(String name) {
        return SI_UNITS.contains(name);
    }

    /**
     * Get all symbols in the standard library.
     *
     * @return collection of all symbols
     */
    public Collection<Symbol> getAllSymbols() {
        return Collections.unmodifiableCollection(symbols.values());
    }

    /**
     * Get statistics about the standard library.
     *
     * @return the statistics
     */
    public StandardLibraryStats getStats() {
        return new StandardLibraryStats();
    }

    /**
     * Statistics about the standard library.
     */
    public class StandardLibraryStats {

        /**
         * Get the total symbol count.
         */
        public int getSymbolCount() {
            return symbols.size();
        }

        /**
         * Get the package count.
         */
        public int getPackageCount() {
            return packageSymbols.size();
        }

        /**
         * Get symbols grouped by element type.
         */
        public Map<ElementType, Integer> getSymbolsByType() {
            Map<ElementType, Integer> result = new EnumMap<>(ElementType.class);
            for (Symbol symbol : symbols.values()) {
                result.merge(symbol.getType(), 1, Integer::sum);
            }
            return result;
        }

        @Override
        public String toString() {
            return String.format(
                "StandardLibraryStats{symbolCount=%d, packageCount=%d, typeBreakdown=%s}",
                getSymbolCount(), getPackageCount(), getSymbolsByType());
        }
    }
}
