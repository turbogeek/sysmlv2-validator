package com.validator.suggestions;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides Cameo/Catia Magic custom library symbols for spelling suggestions.
 * Includes common stereotypes, profiles, and library elements from Cameo SysML Toolkit.
 */
public final class CameoLibraryProvider implements SuggestionProvider {

    // Cameo SysML profile stereotypes and elements
    private static final Set<String> CAMEO_SYMBOLS;

    static {
        Set<String> symbols = new HashSet<>();

        // Cameo Systems Modeler stereotypes
        symbols.add("Block");
        symbols.add("ValueType");
        symbols.add("ConstraintBlock");
        symbols.add("FlowPort");
        symbols.add("ProxyPort");
        symbols.add("FullPort");
        symbols.add("InterfaceBlock");
        symbols.add("BoundReference");
        symbols.add("ParticipantProperty");
        symbols.add("ConnectorProperty");
        symbols.add("DistributedProperty");
        symbols.add("EndPathMultiplicity");
        symbols.add("PropertySpecificType");
        symbols.add("NestedConnectorEnd");
        symbols.add("DirectedRelationshipPropertyPath");
        symbols.add("AdjunctProperty");
        symbols.add("ClassifierBehaviorProperty");

        // Cameo Requirement stereotypes
        symbols.add("DeriveReqt");
        symbols.add("Copy");
        symbols.add("Satisfy");
        symbols.add("Verify");
        symbols.add("Refine");
        symbols.add("Trace");
        symbols.add("TestCase");
        symbols.add("RequirementRelated");
        symbols.add("AbstractRequirement");
        symbols.add("ExtendedRequirement");
        symbols.add("FunctionalRequirement");
        symbols.add("InterfaceRequirement");
        symbols.add("PerformanceRequirement");
        symbols.add("PhysicalRequirement");
        symbols.add("DesignConstraint");

        // Cameo Activity stereotypes
        symbols.add("Rate");
        symbols.add("Continuous");
        symbols.add("Discrete");
        symbols.add("ControlOperator");
        symbols.add("NoBuffer");
        symbols.add("Overwrite");
        symbols.add("Optional");
        symbols.add("Probability");

        // Cameo Parametric stereotypes
        symbols.add("ConstraintProperty");
        symbols.add("ConstraintParameter");

        // Cameo Allocation stereotypes
        symbols.add("Allocate");
        symbols.add("AllocateActivityPartition");

        // Cameo State Machine stereotypes
        symbols.add("StateMachine");
        symbols.add("Region");
        symbols.add("Pseudostate");
        symbols.add("FinalState");
        symbols.add("InitialPseudostate");
        symbols.add("ChoicePseudostate");
        symbols.add("JunctionPseudostate");
        symbols.add("ForkPseudostate");
        symbols.add("JoinPseudostate");
        symbols.add("DeepHistoryPseudostate");
        symbols.add("ShallowHistoryPseudostate");
        symbols.add("EntryPointPseudostate");
        symbols.add("ExitPointPseudostate");
        symbols.add("TerminatePseudostate");

        // Cameo Use Case stereotypes
        symbols.add("Actor");
        symbols.add("UseCase");
        symbols.add("Include");
        symbols.add("Extend");

        // Cameo Package stereotypes
        symbols.add("Model");
        symbols.add("Profile");
        symbols.add("ModelLibrary");
        symbols.add("Viewpoint");

        // Cameo Diagram types
        symbols.add("BlockDefinitionDiagram");
        symbols.add("InternalBlockDiagram");
        symbols.add("ParametricDiagram");
        symbols.add("RequirementDiagram");
        symbols.add("ActivityDiagram");
        symbols.add("SequenceDiagram");
        symbols.add("StateMachineDiagram");
        symbols.add("UseCaseDiagram");
        symbols.add("PackageDiagram");

        // Common Cameo value types
        symbols.add("RealValueType");
        symbols.add("IntegerValueType");
        symbols.add("BooleanValueType");
        symbols.add("StringValueType");
        symbols.add("ComplexValueType");
        symbols.add("NumberValueType");

        // Cameo Units (common additions)
        symbols.add("Unit");
        symbols.add("QuantityKind");
        symbols.add("DimensionlessUnit");
        symbols.add("SimpleUnit");
        symbols.add("DerivedUnit");
        symbols.add("ConversionBasedUnit");
        symbols.add("PrefixedUnit");
        symbols.add("UnitFactor");

        // Cameo QUDV (Quantities, Units, Dimensions, Values)
        symbols.add("QuantityKindFactor");
        symbols.add("SystemOfQuantities");
        symbols.add("SystemOfUnits");
        symbols.add("Prefix");
        symbols.add("Scale");
        symbols.add("LinearConversionUnit");
        symbols.add("AffineConversionUnit");

        // Cameo Simulation Profile
        symbols.add("SimulationConfig");
        symbols.add("ExecutionListener");
        symbols.add("NumberFormat");
        symbols.add("MockupUI");
        symbols.add("FMU");
        symbols.add("FMUExport");
        symbols.add("Clock");
        symbols.add("AnimationConfig");

        // Cameo Safety and Reliability Profile elements
        symbols.add("Hazard");
        symbols.add("HazardousElement");
        symbols.add("SafetyMechanism");
        symbols.add("SafetyRequirement");
        symbols.add("SafetyCase");
        symbols.add("Argument");
        symbols.add("Claim");
        symbols.add("Evidence");
        symbols.add("AssumptionElement");

        // MBSE common terms
        symbols.add("Subsystem");
        symbols.add("Assembly");
        symbols.add("Signal");
        symbols.add("Message");
        symbols.add("InOut");
        symbols.add("Return");

        // Physical architecture elements
        symbols.add("Hardware");
        symbols.add("Software");
        symbols.add("Firmware");
        symbols.add("Mechanical");
        symbols.add("Electrical");
        symbols.add("Electronic");
        symbols.add("Hydraulic");
        symbols.add("Pneumatic");
        symbols.add("Thermal");
        symbols.add("Optical");

        // Common modeling elements
        symbols.add("Mode");
        symbols.add("Variant");
        symbols.add("Option");
        symbols.add("Configuration");
        symbols.add("Baseline");
        symbols.add("Release");
        symbols.add("Revision");

        // Analysis elements
        symbols.add("Optimization");
        symbols.add("Inspection");
        symbols.add("Audit");

        CAMEO_SYMBOLS = Set.copyOf(symbols);
    }

    @Override
    public Set<String> getCandidates() {
        return CAMEO_SYMBOLS;
    }

    @Override
    public String getSourceName() {
        return "cameo";
    }

    /**
     * Gets the number of Cameo library symbols.
     *
     * @return count of symbols
     */
    public static int getSymbolCount() {
        return CAMEO_SYMBOLS.size();
    }
}
