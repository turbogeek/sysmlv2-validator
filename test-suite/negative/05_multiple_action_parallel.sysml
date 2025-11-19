/**
 * NEGATIVE TEST: Multiple invalid 'action parallel' uses
 * EXPECTED ERROR: Multiple "Invalid syntax: 'action parallel'" errors
 * CATEGORY: Invalid Action Syntax
 */
package NegativeTests {
    action def ParallelProcessing {
        action parallel task1 {  // ERROR
            out r1;
        }
        action parallel task2 {  // ERROR
            out r2;
        }
        action parallel task3 {  // ERROR
            out r3;
        }
    }
}
