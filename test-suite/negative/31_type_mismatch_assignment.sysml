/**
 * NEGATIVE TEST: Type mismatch in assignment
 * EXPECTED ERROR: Type mismatch - cannot assign String to Integer
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    part def TestPart {
        attribute count : Integer = "not a number";  // ERROR: Type mismatch
    }
}
