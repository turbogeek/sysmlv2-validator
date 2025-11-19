/**
 * NEGATIVE TEST: Invalid multiplicity syntax
 * EXPECTED ERROR: Syntax error in multiplicity
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def TestPart {
        attribute values : Integer[1...5];  // ERROR: Should be [1..5] not [1...5]
    }
}
