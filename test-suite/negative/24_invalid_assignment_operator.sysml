/**
 * NEGATIVE TEST: Invalid assignment operator
 * EXPECTED ERROR: Syntax error - invalid operator
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    action def TestAction {
        in x : Integer;
        out y : Integer;

        attribute result == x + 1;  // ERROR: Should be = or :=, not ==
    }
}
