/**
 * NEGATIVE TEST: Succession pattern creates circular dependency/deadlock
 * EXPECTED ERROR: Circular succession dependency
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    action def CircularActions {
        action a1;
        action a2;
        action a3;

        first a1 then a2;
        first a2 then a3;
        first a3 then a1;  // ERROR: Creates circular succession dependency
    }
}
