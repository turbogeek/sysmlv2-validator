package com.validator.semantic;

/**
 * Represents the type of scope in a SysML v2 model.
 */
public enum ScopeType {
    GLOBAL,           // Root/global scope
    PACKAGE,          // Package scope
    PART_DEFINITION,  // Within a part def
    PART_USAGE,       // Within a part usage
    ACTION_DEFINITION, // Within an action def
    ACTION_USAGE,     // Within an action usage
    STATE_DEFINITION, // Within a state def
    STATE_USAGE,      // Within a state usage
    REQUIREMENT_DEFINITION, // Within a requirement def
    REQUIREMENT_USAGE, // Within a requirement usage
    USE_CASE_DEFINITION, // Within a use case def
    VIEW_DEFINITION,  // Within a view def
    INTERFACE_DEFINITION, // Within an interface def
    CONNECTION_DEFINITION, // Within a connection def
    CONSTRAINT,       // Within a constraint
    EXPRESSION        // Within an expression
}
