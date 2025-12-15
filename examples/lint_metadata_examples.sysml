/**
 * Examples of using Lint Configuration Metadata
 *
 * This file demonstrates how to use SysML v2 metadata to configure
 * lint warnings at the element, package, and file level.
 */
package LintExamples {
    import Validation::*;

    /* ========================================================================
     * Example 1: Suppressing warnings for a specific element
     * ======================================================================== */

    /**
     * This part def is intentionally defined but not used yet.
     * The @SuppressWarning metadata prevents LINT001 (unused definition).
     */
    @SuppressWarning{category = "unused";}
    part def ReservedForFuture {
        doc /* This definition is a placeholder for future functionality */
    }

    /**
     * Alternative: Use @IntentionallyUnused for clearer intent
     */
    @IntentionallyUnused{
        reason = "Template for plugin extensions";
        plannedUsage = "Will be used when plugin system is implemented";
    }
    part def ExtensionTemplate;

    /* ========================================================================
     * Example 2: Suppressing specific error codes
     * ======================================================================== */

    /**
     * This package uses legacy naming that doesn't follow PascalCase.
     * We suppress only the naming warnings, not other warnings.
     */
    @SuppressWarning{
        codes = ("LINT005", "LINT006");
        reason = "Legacy code - will be renamed in v3.0";
    }
    package legacy_utilities {
        part def old_helper;
        part myPart : old_helper;
    }

    /* ========================================================================
     * Example 3: Package-level lint configuration
     * ======================================================================== */

    /**
     * This is a well-documented public API package.
     * We enable strict documentation requirements.
     */
    @LintConfig{
        requireDocs = true;
        strictNaming = true;
        maxNestingDepth = 3;
    }
    package PublicAPI {
        doc /* Public API for vehicle systems */

        part def Vehicle {
            doc /* A vehicle with engine and wheels */

            attribute mass : Real;
            part engine : Engine;
        }

        part def Engine {
            doc /* Engine providing propulsion */

            attribute power : Real;
        }
    }

    /* ========================================================================
     * Example 4: Marking work in progress
     * ======================================================================== */

    /**
     * This feature is still being developed.
     * The validator will remind about incomplete work.
     */
    @WorkInProgress{
        todo = "Add constraint for maximum speed based on engine power";
        priority = "high";
        assignee = "engineering-team";
    }
    part def PerformanceModel {
        attribute maxSpeed : Real;
        attribute enginePower : Real;

        /* TODO: constraint maxSpeed <= enginePower * 0.5 */
    }

    /* ========================================================================
     * Example 5: Deprecation warnings
     * ======================================================================== */

    /**
     * This old definition is being phased out.
     * References to it will generate deprecation warnings.
     */
    @Deprecated{
        since = "2.0";
        replacement = "ModernVehicle";
        removalVersion = "4.0";
    }
    part def LegacyVehicle {
        attribute speed : Real;
    }

    /**
     * The new replacement definition
     */
    part def ModernVehicle {
        attribute velocity : Real;
        attribute acceleration : Real;
    }

    /* ========================================================================
     * Example 6: Enabling optional warnings
     * ======================================================================== */

    /**
     * This package enables stricter validation than default.
     * Documentation warnings are enabled and treated as errors.
     */
    @EnableWarning{
        category = "documentation";
        severity = "error";
    }
    package StrictDocumentation {
        doc /* This package requires full documentation */

        /**
         * Well documented definition
         */
        part def WellDocumented {
            doc /* This part demonstrates proper documentation */

            attribute value : Real;
        }
    }

    /* ========================================================================
     * Example 7: Combining multiple metadata
     * ======================================================================== */

    /**
     * Multiple metadata can be applied to the same element.
     * This enables some warnings while suppressing others.
     */
    @LintConfig{
        strictNaming = true;
        disabledCategories = ("unused", "complexity");
    }
    @EnableWarning{category = "documentation";}
    package MixedConfig {
        doc /* Package with mixed lint configuration */

        /* Naming must be strict, documentation required,
           but unused warnings are disabled */
        part def ConfiguredElement {
            doc /* Properly documented */
        }
    }

    /* ========================================================================
     * Example 8: File-level suppression (apply to root package)
     * ======================================================================== */

    /**
     * When applied to the root package, affects the entire file.
     * This is equivalent to file-level configuration.
     */
    @SuppressWarning{
        category = "best-practices";
        applyToNested = true;
        reason = "Generated code - do not modify";
    }
    package GeneratedCode {
        /* All best-practice warnings suppressed for generated code */

        part def gen_element_001;
        part def gen_element_002;
        part def gen_element_003;
    }
}
