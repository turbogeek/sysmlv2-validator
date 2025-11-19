/**
 * NEGATIVE TEST: Reference to undefined state
 * EXPECTED ERROR: Cannot resolve state reference
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    state def StateMachine {
        state active;

        transition first active then inactive;  // ERROR: 'inactive' state not defined
    }
}
