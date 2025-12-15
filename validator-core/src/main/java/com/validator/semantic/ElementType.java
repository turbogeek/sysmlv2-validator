package com.validator.semantic;

/**
 * Types of SysML v2 elements that can be defined as symbols.
 */
public enum ElementType {
    // Namespaces
    PACKAGE,

    // Definitions
    PART_DEFINITION,
    ACTION_DEFINITION,
    STATE_DEFINITION,
    REQUIREMENT_DEFINITION,
    VIEW_DEFINITION,
    VIEWPOINT_DEFINITION,
    CONSTRAINT_DEFINITION,
    ATTRIBUTE_DEFINITION,
    PORT_DEFINITION,
    CONNECTION_DEFINITION,
    INTERFACE_DEFINITION,
    ALLOCATION_DEFINITION,
    ITEM_DEFINITION,
    ENUM_DEFINITION,
    CALC_DEFINITION,
    ANALYSIS_DEFINITION,
    CASE_DEFINITION,
    USE_CASE_DEFINITION,
    VERIFICATION_DEFINITION,
    CONCERN_DEFINITION,
    RENDERING_DEFINITION,
    OCCURRENCE_DEFINITION,
    FLOW_DEFINITION,
    METADATA_DEFINITION,

    // Data types / KerML types
    DATA_TYPE,
    DATATYPE_DEFINITION,
    CLASS,
    CLASS_DEFINITION,
    STRUCT,
    STRUCT_DEFINITION,
    ASSOC_STRUCT,
    BEHAVIOR_DEFINITION,
    FUNCTION_DEFINITION,
    PREDICATE_DEFINITION,
    CLASSIFIER_DEFINITION,
    TYPE_DEFINITION,
    FEATURE_DEFINITION,
    CONNECTOR_DEFINITION,
    METACLASS_DEFINITION,
    INTERACTION_DEFINITION,

    // Usages
    PART_USAGE,
    ATTRIBUTE_USAGE,
    ITEM_USAGE,
    PORT_USAGE,
    ACTION_USAGE,
    STATE_USAGE,
    CONSTRAINT_USAGE,
    REQUIREMENT_USAGE,
    CALC_USAGE,
    CASE_USAGE,
    ANALYSIS_USAGE,
    VERIFICATION_USAGE,
    VIEW_USAGE,
    VIEWPOINT_USAGE,
    RENDERING_USAGE,
    CONNECTION_USAGE,
    INTERFACE_USAGE,
    FLOW_USAGE,
    ALLOCATION_USAGE,

    // Features
    PARAMETER,
    RETURN_FEATURE,

    // Enum values
    ENUMERATION,

    // Other
    ALIAS,
    IMPORT,
    COMMENT,
    DOCUMENTATION;

    /**
     * Check if this is a definition type.
     */
    public boolean isDefinition() {
        return name().endsWith("_DEFINITION")
               || this == DATA_TYPE
               || this == CLASS
               || this == STRUCT
               || this == ASSOC_STRUCT
               || this == ENUMERATION;
    }

    /**
     * Check if this is a usage type.
     */
    public boolean isUsage() {
        return name().endsWith("_USAGE");
    }

    /**
     * Check if this is a namespace type.
     */
    public boolean isNamespace() {
        return this == PACKAGE;
    }
}
