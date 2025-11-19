/**
 * NEGATIVE TEST: Missing colon before type
 * EXPECTED ERROR: Syntax error - missing type separator
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def TestPart {
        attribute speed Integer;  // ERROR: Missing : before type
    }
}
