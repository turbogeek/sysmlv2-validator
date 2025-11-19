package com.validator.semantic;

/**
 * Represents the type of import statement.
 */
public enum ImportType {
    WILDCARD,    // import Package::*
    SPECIFIC,    // import Package::Element
    FILTERED,    // import Package::{Element1, Element2}
    ALIAS        // import Package::Element as Alias
}
