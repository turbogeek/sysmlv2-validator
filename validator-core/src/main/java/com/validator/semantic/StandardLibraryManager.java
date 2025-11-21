package com.validator.semantic;

import com.validator.ast.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the SysML v2 standard library.
 * Provides access to built-in types, quantities, units, and kernel elements.
 */
public class StandardLibraryManager {
    private final Map<String, Symbol> standardSymbols;
    private final Map<String, List<Symbol>> packageSymbols;
    private boolean initialized;

    // Standard library package names
    private static final String KERNEL_LIBRARY = "KerML";
    private static final String SYSML_LIBRARY = "SysML";
    private static final String ISQ_LIBRARY = "ISQ";
    private static final String SI_LIBRARY = "SI";

    // Primitive types set for efficient lookup
    private static final Set<String> PRIMITIVE_TYPES = Set.of(
        "Boolean", "Integer", "Real", "String", "Natural"
    );

    public StandardLibraryManager() {
        this.standardSymbols = new LinkedHashMap<>();
        this.packageSymbols = new LinkedHashMap<>();
        this.initialized = false;
    }

    /**
     * Initialize the standard library with built-in types.
     * This creates the core types that are always available.
     */
    public void initializeBuiltins() {
        if (initialized) {
            return;
        }

        initializeKerMLTypes();
        initializeSysMLTypes();
        initializeISQTypes();
        initializeSIUnits();
        initializeCollectionTypes();
        initializeFunctionTypes();
        initializeOtherTypes();

        initialized = true;
    }

    private void initializeKerMLTypes() {
        // Base types from Kernel Library
        addStandardType("Base", ElementType.DATA_TYPE, "KerML");
        addStandardType("Anything", ElementType.DATA_TYPE, "KerML");
        addStandardType("Object", ElementType.DATA_TYPE, "KerML");

        // Primitive data types
        addStandardType("Boolean", ElementType.DATA_TYPE, "KerML");
        addStandardType("Integer", ElementType.DATA_TYPE, "KerML");
        addStandardType("Real", ElementType.DATA_TYPE, "KerML");
        addStandardType("String", ElementType.DATA_TYPE, "KerML");
        addStandardType("Natural", ElementType.DATA_TYPE, "KerML");

        // KerML Core Types
        addStandardType("Feature", ElementType.DATA_TYPE, "KerML");
        addStandardType("Type", ElementType.DATA_TYPE, "KerML");
        addStandardType("Classifier", ElementType.DATA_TYPE, "KerML");
        addStandardType("Class", ElementType.DATA_TYPE, "KerML");
        addStandardType("DataType", ElementType.DATA_TYPE, "KerML");
        addStandardType("Structure", ElementType.DATA_TYPE, "KerML");
        addStandardType("Association", ElementType.DATA_TYPE, "KerML");
        addStandardType("Behavior", ElementType.DATA_TYPE, "KerML");
        addStandardType("Function", ElementType.DATA_TYPE, "KerML");
        addStandardType("Predicate", ElementType.DATA_TYPE, "KerML");
        addStandardType("Interaction", ElementType.DATA_TYPE, "KerML");
        addStandardType("Metaclass", ElementType.DATA_TYPE, "KerML");
    }

    private void initializeSysMLTypes() {
        // SysML base types
        addStandardType("Part", ElementType.PART_DEFINITION, "SysML");
        addStandardType("Action", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("Requirement", ElementType.REQUIREMENT_DEFINITION, "SysML");
        addStandardType("State", ElementType.STATE_DEFINITION, "SysML");
        addStandardType("Port", ElementType.PORT_DEFINITION, "SysML");
        addStandardType("Interface", ElementType.INTERFACE_DEFINITION, "SysML");
        addStandardType("Connection", ElementType.CONNECTION_DEFINITION, "SysML");
        addStandardType("Item", ElementType.ITEM_DEFINITION, "SysML");

        // SysML Actions library elements
        addStandardType("AcceptAction", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("SendAction", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("AssignAction", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("PerformAction", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("DecisionNode", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("MergeNode", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("ForkNode", ElementType.ACTION_DEFINITION, "SysML");
        addStandardType("JoinNode", ElementType.ACTION_DEFINITION, "SysML");

        // SysML Constraints library elements
        addStandardType("Constraint", ElementType.DATA_TYPE, "SysML");
        addStandardType("ConstraintCheck", ElementType.DATA_TYPE, "SysML");

        // SysML Calculations library elements
        addStandardType("Calculation", ElementType.CALC_DEFINITION, "SysML");
        addStandardType("CalcDef", ElementType.CALC_DEFINITION, "SysML");
    }

    private void initializeISQTypes() {
        // ISQ quantities (International System of Quantities)
        addStandardType("QuantityValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("LengthValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("MassValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("TimeValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("TemperatureValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("VelocityValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("AccelerationValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("ForceValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("EnergyValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("PowerValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("PressureValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("VolumeValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("ElectricCurrentValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("ElectricPotentialValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("ResistanceValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("CapacitanceValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("InductanceValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("FrequencyValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("AreaValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("DensityValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("TorqueValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("AngleValue", ElementType.DATA_TYPE, "ISQ");
        addStandardType("SolidAngleValue", ElementType.DATA_TYPE, "ISQ");
    }

    private void initializeSIUnits() {
        // SI units (International System of Units) - Base units
        addStandardType("m", ElementType.DATA_TYPE, "SI");  // meter
        addStandardType("kg", ElementType.DATA_TYPE, "SI"); // kilogram
        addStandardType("s", ElementType.DATA_TYPE, "SI");  // second
        addStandardType("K", ElementType.DATA_TYPE, "SI");  // kelvin
        addStandardType("A", ElementType.DATA_TYPE, "SI");  // ampere
        addStandardType("mol", ElementType.DATA_TYPE, "SI"); // mole
        addStandardType("cd", ElementType.DATA_TYPE, "SI"); // candela
        // Derived SI units
        addStandardType("N", ElementType.DATA_TYPE, "SI");  // newton
        addStandardType("Pa", ElementType.DATA_TYPE, "SI"); // pascal
        addStandardType("J", ElementType.DATA_TYPE, "SI");  // joule
        addStandardType("W", ElementType.DATA_TYPE, "SI");  // watt
        addStandardType("Hz", ElementType.DATA_TYPE, "SI"); // hertz
        addStandardType("V", ElementType.DATA_TYPE, "SI");  // volt
        addStandardType("C", ElementType.DATA_TYPE, "SI");  // coulomb
        addStandardType("F", ElementType.DATA_TYPE, "SI");  // farad
        addStandardType("Ohm", ElementType.DATA_TYPE, "SI"); // ohm
        addStandardType("S", ElementType.DATA_TYPE, "SI");  // siemens
        addStandardType("Wb", ElementType.DATA_TYPE, "SI"); // weber
        addStandardType("T", ElementType.DATA_TYPE, "SI");  // tesla
        addStandardType("H", ElementType.DATA_TYPE, "SI");  // henry
        addStandardType("lm", ElementType.DATA_TYPE, "SI"); // lumen
        addStandardType("lx", ElementType.DATA_TYPE, "SI"); // lux
        addStandardType("Bq", ElementType.DATA_TYPE, "SI"); // becquerel
        addStandardType("Gy", ElementType.DATA_TYPE, "SI"); // gray
        addStandardType("Sv", ElementType.DATA_TYPE, "SI"); // sievert
    }

    private void initializeCollectionTypes() {
        // Collections library
        addStandardType("Array", ElementType.DATA_TYPE, "Collections");
        addStandardType("List", ElementType.DATA_TYPE, "Collections");
        addStandardType("Set", ElementType.DATA_TYPE, "Collections");
        addStandardType("OrderedSet", ElementType.DATA_TYPE, "Collections");
        addStandardType("Bag", ElementType.DATA_TYPE, "Collections");
        addStandardType("Map", ElementType.DATA_TYPE, "Collections");
        addStandardType("Sequence", ElementType.DATA_TYPE, "Collections");
        // Scalar values
        addStandardType("ScalarValue", ElementType.DATA_TYPE, "ScalarValues");
        addStandardType("NumericalValue", ElementType.DATA_TYPE, "ScalarValues");
        addStandardType("Positive", ElementType.DATA_TYPE, "ScalarValues");
        addStandardType("NonNegative", ElementType.DATA_TYPE, "ScalarValues");
    }

    private void initializeFunctionTypes() {
        // Sequence functions (commonly used collection operations)
        addStandardType("size", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("isEmpty", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("notEmpty", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("includes", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("excludes", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("head", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("tail", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("first", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("last", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("union", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("intersection", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("including", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("excluding", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("select", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("reject", ElementType.DATA_TYPE, "SequenceFunctions");
        addStandardType("collect", ElementType.DATA_TYPE, "SequenceFunctions");
        // Base functions
        addStandardType("BaseFunctions", ElementType.DATA_TYPE, "BaseFunctions");
        addStandardType("sum", ElementType.DATA_TYPE, "BaseFunctions");
        addStandardType("product", ElementType.DATA_TYPE, "BaseFunctions");
    }

    private void initializeOtherTypes() {
        // Occurrences
        addStandardType("Occurrence", ElementType.DATA_TYPE, "Occurrences");
        addStandardType("Event", ElementType.DATA_TYPE, "Occurrences");
        addStandardType("Life", ElementType.DATA_TYPE, "Occurrences");
        addStandardType("Snapshot", ElementType.DATA_TYPE, "Occurrences");
        addStandardType("TimesliceOf", ElementType.DATA_TYPE, "Occurrences");
        addStandardType("Transfer", ElementType.DATA_TYPE, "Occurrences");

        // Links
        addStandardType("Link", ElementType.DATA_TYPE, "Links");
        addStandardType("BinaryLink", ElementType.DATA_TYPE, "Links");

        // Performances
        addStandardType("Performance", ElementType.DATA_TYPE, "Performances");

        // Metadata
        addStandardType("Metaobject", ElementType.DATA_TYPE, "Metadata");
        addStandardType("SemanticMetadata", ElementType.DATA_TYPE, "Metadata");
    }

    /**
     * Add a standard library type.
     */
    private void addStandardType(String name, ElementType type, String packageName) {
        String qualifiedName = packageName + "::" + name;
        Symbol symbol = new Symbol(name, qualifiedName, type,
            new Location("<stdlib>", 1, 0), Visibility.PUBLIC);

        standardSymbols.put(qualifiedName, symbol);
        standardSymbols.put(name, symbol); // Also allow unqualified lookup

        // Add to package index
        packageSymbols.computeIfAbsent(packageName, k -> new ArrayList<>()).add(symbol);
    }

    /**
     * Resolve a symbol from the standard library.
     *
     * @param qualifiedName Qualified name (e.g., "ISQ::MassValue") or simple name (e.g., "Real")
     * @return Symbol if found, null otherwise
     */
    public Symbol resolveSymbol(String qualifiedName) {
        if (!initialized) {
            initializeBuiltins();
        }
        return standardSymbols.get(qualifiedName);
    }

    /**
     * Get all symbols in a specific package.
     *
     * @param packageName Package name (e.g., "ISQ", "SI", "KerML")
     * @return List of symbols in the package
     */
    public List<Symbol> getSymbolsInPackage(String packageName) {
        if (!initialized) {
            initializeBuiltins();
        }
        return packageSymbols.getOrDefault(packageName, Collections.emptyList());
    }

    /**
     * Check if a type is a primitive type.
     */
    public boolean isPrimitiveType(String typeName) {
        return PRIMITIVE_TYPES.contains(typeName);
    }

    /**
     * Check if a type is an ISQ quantity type.
     */
    public boolean isQuantityType(String typeName) {
        return typeName.endsWith("Value")
               && resolveSymbol("ISQ::" + typeName) != null;
    }

    /**
     * Check if a name represents a unit.
     */
    public boolean isUnit(String unitName) {
        return resolveSymbol("SI::" + unitName) != null;
    }

    /**
     * Load standard library from file system (optional).
     * This would load the actual SysML v2 standard library files.
     *
     * @param libraryPath Path to sysml.library directory
     */
    public void loadFromFileSystem(Path libraryPath) throws IOException {
        if (!Files.exists(libraryPath)) {
            throw new IOException("Standard library path does not exist: " + libraryPath);
        }

        // Find all .sysml and .kerml files in the library
        List<Path> libraryFiles = Files.walk(libraryPath)
            .filter(p -> p.toString().endsWith(".sysml") || p.toString().endsWith(".kerml"))
            .collect(Collectors.toList());

        // TODO: Parse each library file and add symbols to standardSymbols
        // For now, we rely on initializeBuiltins() for core types
        // Full implementation would:
        // 1. Parse each library file
        // 2. Build symbol table for library
        // 3. Merge into standardSymbols

        System.out.println("Standard library loading from filesystem not yet implemented.");
        System.out.println("Found " + libraryFiles.size() + " library files at: " + libraryPath);
        System.out.println("Using built-in type definitions instead.");
    }

    /**
     * Get statistics about the standard library.
     */
    public StandardLibraryStats getStats() {
        if (!initialized) {
            initializeBuiltins();
        }

        Map<ElementType, Integer> typeCounts = standardSymbols.values().stream()
            .collect(Collectors.groupingBy(
                Symbol::getType,
                () -> new EnumMap<>(ElementType.class),
                Collectors.summingInt(symbol -> 1)
            ));

        return new StandardLibraryStats(
            standardSymbols.size(),
            packageSymbols.size(),
            typeCounts
        );
    }

    /**
     * Get all standard library symbols.
     */
    public Collection<Symbol> getAllSymbols() {
        if (!initialized) {
            initializeBuiltins();
        }
        return Collections.unmodifiableCollection(standardSymbols.values());
    }

    /**
     * Standard library statistics.
     */
    public static class StandardLibraryStats {
        private final int symbolCount;
        private final int packageCount;
        private final Map<ElementType, Integer> symbolsByType;

        public StandardLibraryStats(int symbolCount, int packageCount, Map<ElementType, Integer> symbolsByType) {
            this.symbolCount = symbolCount;
            this.packageCount = packageCount;
            this.symbolsByType = new EnumMap<>(symbolsByType);
        }

        public int getSymbolCount() {
            return symbolCount;
        }
        public int getPackageCount() {
            return packageCount;
        }
        public Map<ElementType, Integer> getSymbolsByType() {
            return Collections.unmodifiableMap(symbolsByType);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Standard Library: %d symbols across %d packages%n", symbolCount, packageCount));
            sb.append("By Type:%n");
            symbolsByType.entrySet().stream()
                .sorted(Map.Entry.<ElementType, Integer>comparingByValue().reversed())
                .forEach(entry -> sb.append(String.format("  %s: %d%n", entry.getKey(), entry.getValue())));
            return sb.toString();
        }
    }
}
