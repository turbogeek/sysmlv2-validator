/**
 * NEGATIVE TEST: Invalid 'action parallel' syntax
 * EXPECTED ERROR: "Invalid syntax: 'action parallel' is not valid in SysML v2"
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def TestAction {
        action parallel doSomething {  // ERROR: 'action parallel' is invalid
            out result;
        }
    }
}
