/**
 * NEGATIVE TEST: Succession to non-existent action
 * EXPECTED ERROR: Cannot resolve succession target
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    action def TestAction {
        action step1 {
            out data;
        }

        first step1 then nonExistentStep;  // ERROR: nonExistentStep not defined
    }
}
