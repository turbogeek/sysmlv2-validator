/**
 * NEGATIVE TEST: Invalid 'action if' syntax
 * EXPECTED ERROR: "Invalid syntax: 'action if' is not valid in SysML v2"
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def TestAction {
        in value : Integer;

        action if (value > 10) doSomething {  // ERROR: 'action if' is invalid
            out result;
        }
    }
}
