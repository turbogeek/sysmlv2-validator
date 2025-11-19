/**
 * NEGATIVE TEST: Missing closing brace
 * EXPECTED ERROR: Syntax error - unclosed brace
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def TestPart {
        attribute x : Integer;
    // ERROR: Missing closing brace for part def
}
