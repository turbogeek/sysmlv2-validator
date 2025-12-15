/*
 * Test file for Public Import Lint Rule (LINT019)
 *
 * This file demonstrates public import patterns that trigger lint warnings.
 * Public imports re-export imported elements to anyone who imports this package.
 *
 * TRAINING NOTES:
 * - public import makes imported elements visible to importers
 * - This creates hidden dependencies and can cause scope pollution
 * - Use private imports (default) unless you intentionally want to re-export
 */
package PublicImportTests {

    /* PROBLEMATIC PATTERNS - These will trigger LINT019 warnings */

    /* LINT019: Public wildcard import - MOST PROBLEMATIC */
    public import ISQ::*;

    /* LINT019: Public specific import */
    public import SI::kg;
    public import SI::m;

    /* CORRECT PATTERNS - No warnings */

    /* Private import (default) - only visible within this package */
    import SI::s;

    /* USAGE EXAMPLES */

    part def Engine {
        attribute power : Real;
        attribute runTime : Real;
    }

    part def ElectricMotor :> Engine {
        attribute ratedCurrent : Real;
    }
}

/*
 * IMPACT:
 * When another package imports PublicImportTests:
 *   package UserPackage {
 *       import PublicImportTests::*;
 *       // Now ISQ::* and SI::kg, SI::m are also in scope!
 *       // But NOT SI::s (private)
 *   }
 */

package RecommendedPattern {
    /* When you DO want to re-export as part of public API */
    public import ISQ::MassValue;
    public import SI::kg;

    /* Internal implementation stays private */
    import ISQ::TimeValue;

    part def MeasurementUtils {
        attribute mass : Real;
    }
}
