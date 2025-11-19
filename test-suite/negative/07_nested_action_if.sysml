/**
 * NEGATIVE TEST: Nested invalid 'action if' syntax
 * EXPECTED ERROR: Multiple "Invalid syntax: 'action if'" errors
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def NestedConditional {
        in x : Integer;

        action if (x > 0) positive {  // ERROR
            action if (x > 10) large {  // ERROR
                out result;
            }
        }
    }
}
