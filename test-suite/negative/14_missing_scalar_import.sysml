/**
 * NEGATIVE TEST: Missing ScalarValues import
 * EXPECTED ERROR: Cannot resolve 'Real' or 'Integer' without import
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    // Missing: private import ScalarValues::*;

    attribute def Temperature {
        attribute value : Real;  // ERROR: Real not imported
    }
}
