/**
 * NEGATIVE TEST: Wrong parameter type in action call
 * EXPECTED ERROR: Parameter type mismatch
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    action def ProcessData {
        in value : Integer;
        out result : Integer;
    }

    action def Caller {
        action call : ProcessData {
            in value = "string";  // ERROR: Expected Integer, got String
        }
    }
}
