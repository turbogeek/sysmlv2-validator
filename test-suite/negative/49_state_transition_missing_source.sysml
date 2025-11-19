/**
 * NEGATIVE TEST: State transition without source state
 * EXPECTED ERROR: Transition must have source state
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    state def Machine {
        state idle;
        state active;

        transition then active;  // ERROR: Missing 'first' source state
    }
}
