/**
 * NEGATIVE TEST: Constraint expression is not boolean
 * EXPECTED ERROR: Constraint must be boolean expression
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    constraint def InvalidConstraint {
        attribute x : Integer;

        assert x + 5;  // ERROR: Expression is Integer, not Boolean
    }
}
