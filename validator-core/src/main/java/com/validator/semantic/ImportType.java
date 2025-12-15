package com.validator.semantic;

/**
 * Types of import statements in SysML v2.
 */
public enum ImportType {
    /**
     * Wildcard import: import Package::*.
     */
    WILDCARD,

    /**
     * Specific element import: import Package::Element.
     */
    SPECIFIC,

    /**
     * Filtered import: import Package::{Element1, Element2}.
     */
    FILTERED,

    /**
     * Alias import: import Package::Element as Alias.
     */
    ALIAS;
}
