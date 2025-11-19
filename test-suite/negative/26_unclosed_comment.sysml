/**
 * NEGATIVE TEST: Unclosed multi-line comment
 * EXPECTED ERROR: Syntax error - unclosed comment
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    /*  This comment is never closed
    part def TestPart {
        attribute x : Integer;
    }
}
