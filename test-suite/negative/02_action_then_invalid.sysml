/**
 * NEGATIVE TEST: Invalid 'action then' syntax
 * EXPECTED ERROR: "Invalid syntax: 'action then' is not valid in SysML v2"
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def TestAction {
        action step1 {
            out data;
        }
        action then step2 {  // ERROR: 'action then' is invalid
            in input;
        }
    }
}
