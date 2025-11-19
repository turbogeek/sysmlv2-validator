/**
 * NEGATIVE TEST: Invalid 'action decide' syntax
 * EXPECTED ERROR: "Invalid syntax: 'action decide' is not valid in SysML v2"
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def TestAction {
        in condition : Boolean;

        action decide choice {  // ERROR: 'action decide' is invalid
            in cond = condition;
        }
    }
}
