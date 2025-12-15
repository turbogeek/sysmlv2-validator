package com.validator.semantic;

/**
 * Types of scopes in SysML v2.
 */
public enum ScopeType {
    GLOBAL,               // Top-level global scope
    PACKAGE,              // Package scope
    PART_DEFINITION,      // Inside part def
    PART_USAGE,           // Inside part usage
    ACTION_DEFINITION,    // Inside action def
    ACTION_USAGE,         // Inside action usage
    STATE_DEFINITION,     // Inside state def
    STATE_USAGE,          // Inside state usage
    REQUIREMENT_DEFINITION,
    REQUIREMENT_USAGE,
    CONSTRAINT_DEFINITION,
    CONSTRAINT_USAGE,
    VIEW_DEFINITION,
    VIEW_USAGE,
    VIEWPOINT_DEFINITION,
    INTERFACE_DEFINITION,
    CONNECTION_DEFINITION,
    ALLOCATION_DEFINITION,
    CALC_DEFINITION,
    ANALYSIS_DEFINITION,
    CASE_DEFINITION,
    VERIFICATION_DEFINITION,
    ENUM_DEFINITION,
    BLOCK;                // Generic block scope
}
