/**
 * NEGATIVE TEST: Missing semicolon
 * EXPECTED ERROR: Syntax error - missing semicolon
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def TestPart {
        attribute x : Integer  // ERROR: Missing semicolon
        attribute y : Integer;
    }
}
