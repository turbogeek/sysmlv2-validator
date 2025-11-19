/**
 * NEGATIVE TEST: 'action parallel' with parameters
 * EXPECTED ERROR: "Invalid syntax: 'action parallel'"
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def TestWithParams {
        in data : Integer[5];

        action parallel processData {  // ERROR
            in items = data;
            out results : Integer[5];
        }
    }
}
