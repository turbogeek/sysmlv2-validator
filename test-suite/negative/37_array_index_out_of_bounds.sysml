/**
 * NEGATIVE TEST: Array index out of bounds
 * EXPECTED ERROR: Index out of bounds
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    action def TestAction {
        in data : Integer[3];

        action process {
            in item = data#(5);  // ERROR: Index 5 out of bounds for array of size 3
        }
    }
}
