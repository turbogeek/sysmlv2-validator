/**
 * NEGATIVE TEST: Combined invalid 'action parallel' and 'action then'
 * EXPECTED ERROR: Multiple invalid action syntax errors
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def MixedInvalidSyntax {
        action parallel start {  // ERROR
            out data;
        }
        action then process {  // ERROR
            in input;
        }
    }
}
