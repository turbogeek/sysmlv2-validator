/**
 * SysML v2 Metadata Definitions for Lint Configuration
 *
 * This library provides metadata definitions that can be used to enable or
 * disable specific lint warnings at the element, package, or file level.
 *
 * Usage:
 *   import Validation::*;
 *
 *   @SuppressWarning{category = "unused";}
 *   part def MyPart { ... }
 *
 * Categories:
 *   - unused: Unused definitions, imports, attributes, parts
 *   - missing-types: Untyped usages, missing definitions
 *   - naming: Naming convention violations
 *   - documentation: Missing or empty documentation
 *   - complexity: Deep nesting, large packages
 *   - best-practices: Empty definitions, implicit visibility
 *   - constraints: OCL-like constraint validation
 */
package Validation {

    /**
     * Metadata definition for suppressing specific lint warnings.
     *
     * Apply to any element to suppress warnings for that element
     * and optionally its nested elements.
     *
     * Attributes:
     *   category - The category of warnings to suppress (e.g., "unused", "naming")
     *   codes - Specific error codes to suppress (e.g., "LINT001", "LINT005")
     *   reason - Optional documentation for why the suppression is needed
     *   applyToNested - Whether to apply to nested elements (default true)
     *
     * Examples:
     *   @SuppressWarning{category = "unused";}
     *   part def InternalPart;
     *
     *   @SuppressWarning{codes = ("LINT001", "LINT002");}
     *   package LegacyCode { ... }
     *
     *   @SuppressWarning{category = "naming"; reason = "Legacy naming convention";}
     *   part def old_style_name;
     */
    metadata def SuppressWarning {
        attribute category : ScalarValues::String[0..1];
        attribute codes : ScalarValues::String[0..*];
        attribute reason : ScalarValues::String[0..1];
        attribute applyToNested : ScalarValues::Boolean default true;
    }

    /**
     * Metadata definition for enabling lint warnings that are off by default.
     *
     * Apply to enable optional warnings for thorough validation.
     *
     * Attributes:
     *   category - The category of warnings to enable
     *   codes - Specific error codes to enable
     *   severity - Warning severity: "info", "warn", "error"
     *
     * Examples:
     *   @EnableWarning{category = "documentation";}
     *   package PublicAPI { ... }
     *
     *   @EnableWarning{codes = ("LINT012"); severity = "error";}
     *   package StrictModule { ... }
     */
    metadata def EnableWarning {
        attribute category : ScalarValues::String[0..1];
        attribute codes : ScalarValues::String[0..*];
        attribute severity : ScalarValues::String default "warn";
    }

    /**
     * Metadata definition for configuring lint behavior at package level.
     *
     * Apply to a package to set lint configuration for all elements within.
     *
     * Attributes:
     *   maxNestingDepth - Maximum allowed nesting depth (default 5)
     *   maxPackageSize - Maximum elements per package (default 50)
     *   maxParameters - Maximum action parameters (default 7)
     *   requireDocs - Require documentation for public elements
     *   strictNaming - Enforce strict naming conventions
     *
     * Examples:
     *   @LintConfig{maxNestingDepth = 3; requireDocs = true;}
     *   package WellDocumented { ... }
     */
    metadata def LintConfig {
        attribute maxNestingDepth : ScalarValues::Integer default 5;
        attribute maxPackageSize : ScalarValues::Integer default 50;
        attribute maxParameters : ScalarValues::Integer default 7;
        attribute requireDocs : ScalarValues::Boolean default false;
        attribute strictNaming : ScalarValues::Boolean default false;
        attribute enabledCategories : ScalarValues::String[0..*];
        attribute disabledCategories : ScalarValues::String[0..*];
    }

    /**
     * Metadata definition for marking elements as intentionally unused.
     *
     * Use this to indicate that an element is defined for future use
     * or as a template, suppressing unused warnings.
     *
     * Attributes:
     *   reason - Documentation for why the element is intentionally unused
     *   plannedUsage - Description of planned future usage
     *
     * Examples:
     *   @IntentionallyUnused{reason = "Reserved for future vehicle types";}
     *   part def FlyingCar;
     */
    metadata def IntentionallyUnused {
        attribute reason : ScalarValues::String[0..1];
        attribute plannedUsage : ScalarValues::String[0..1];
    }

    /**
     * Metadata definition for marking temporary or work-in-progress elements.
     *
     * Elements marked with this metadata will generate info-level
     * reminders during validation.
     *
     * Attributes:
     *   todo - Description of work remaining
     *   priority - "low", "medium", "high"
     *   assignee - Person responsible for completing the work
     *
     * Examples:
     *   @WorkInProgress{todo = "Add constraint validation"; priority = "high";}
     *   part def IncompleteFeature { ... }
     */
    metadata def WorkInProgress {
        attribute todo : ScalarValues::String;
        attribute priority : ScalarValues::String default "medium";
        attribute assignee : ScalarValues::String[0..1];
    }

    /**
     * Metadata definition for deprecating elements.
     *
     * Deprecated elements will generate warnings when referenced.
     *
     * Attributes:
     *   since - Version or date when element was deprecated
     *   replacement - Suggested replacement element
     *   removalVersion - Version when element will be removed
     *
     * Examples:
     *   @Deprecated{since = "2.0"; replacement = "NewVehicle";}
     *   part def OldVehicle;
     */
    metadata def Deprecated {
        attribute since : ScalarValues::String[0..1];
        attribute replacement : ScalarValues::String[0..1];
        attribute removalVersion : ScalarValues::String[0..1];
    }
}
