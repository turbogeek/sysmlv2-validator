/**
 * NEGATIVE TEST: Invalid multiplicity range (lower > upper)
 * EXPECTED ERROR: Invalid multiplicity range
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    part def TestPart {
        attribute values : Integer[5..2];  // ERROR: Lower bound 5 > upper bound 2
    }
}
