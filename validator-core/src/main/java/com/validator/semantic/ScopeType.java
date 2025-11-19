package com.validator.semantic;

/**
 * Represents the type of scope in a SysML v2 model.
 */
public enum ScopeType {
    GLOBAL,           // Root/global scope
    PACKAGE,          // Package scope
    PART_DEFINITION,  // Within a part def
    ACTION_DEFINITION, // Within an action def
    STATE_DEFINITION, // Within a state def
    REQUIREMENT_DEFINITION, // Within a requirement def
    USE_CASE_DEFINITION, // Within a use case def
    VIEW_DEFINITION,  // Within a view def
    INTERFACE_DEFINITION, // Within an interface def
    CONSTRAINT,       // Within a constraint
    EXPRESSION        // Within an expression
}
