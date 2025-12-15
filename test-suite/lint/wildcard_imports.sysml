/*
 * Test file for Wildcard Import Lint Rule (LINT017, LINT018)
 *
 * This file demonstrates various wildcard import patterns that trigger
 * lint warnings. Wildcard imports cause scope pollution by bringing
 * all elements from a package into scope.
 *
 * TRAINING NOTES:
 * - Wildcard imports (::*) should be avoided in favor of specific imports
 * - Recursive wildcards (::**) are even more problematic
 * - Each wildcard import triggers a LINT017 or LINT018 warning
 * - Specific imports make dependencies explicit and improve maintainability
 */
package WildcardImportTests {

    /* PROBLEMATIC PATTERNS - These will trigger lint warnings */

    /* LINT017: Single-level wildcard import */
    import ISQ::*;

    /* LINT017: Another wildcard import */
    import SI::*;

    /* LINT018: Recursive wildcard import */
    import ScalarFunctions::**;

    /* USAGE - Only a few elements are actually used */

    part def Vehicle {
        /* Uses ISQ types from wildcard import */
        attribute mass : Real;
        attribute length : Real;
        attribute weight : Real;
        attribute wheelbase : Real;
    }

    /*
     * RECOMMENDED REFACTORING:
     * Replace the wildcard imports with specific imports:
     *   import ISQ::MassValue;
     *   import ISQ::LengthValue;
     *   import SI::kg;
     *   import SI::m;
     */

    part def Truck :> Vehicle {
        attribute cargoCapacity : Real;
    }
}
