/**
 * NEGATIVE TEST: Duplicate attribute names
 * EXPECTED ERROR: Duplicate declaration
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    part def TestPart {
        attribute speed : Real;
        attribute speed : Integer;  // ERROR: Duplicate attribute name 'speed'
    }
}
