/**
 * NEGATIVE TEST: 'action decide' for boolean decision
 * EXPECTED ERROR: "Invalid syntax: 'action decide'"
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def BooleanDecision {
        in flag : Boolean;

        action decide branch {  // ERROR - should use state machine
            in condition = flag;
            out selected;
        }
    }
}
