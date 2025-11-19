/**
 * NEGATIVE TEST: Reference to undefined action output
 * EXPECTED ERROR: Cannot resolve action output reference
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    action def TestAction {
        action step1 {
            out result1;
        }

        action step2 {
            in data = step1.undefinedOutput;  // ERROR: undefinedOutput doesn't exist
        }
    }
}
