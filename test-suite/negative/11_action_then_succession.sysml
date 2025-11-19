/**
 * NEGATIVE TEST: 'action then' instead of proper succession
 * EXPECTED ERROR: "Invalid syntax: 'action then'"
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def SequentialActions {
        action init {
            out initialized;
        }

        action then execute {  // ERROR - should use 'first init then execute'
            in ready;
            out completed;
        }
    }
}
