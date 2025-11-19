/**
 * NEGATIVE TEST: Missing required parameter
 * EXPECTED ERROR: Required parameter not provided
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    action def RequiresInput {
        in required : Integer;
        out result : Integer;
    }

    action def Caller {
        action call : RequiresInput {
            // ERROR: Missing required 'in required' parameter
            out result;
        }
    }
}
