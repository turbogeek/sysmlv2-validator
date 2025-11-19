/**
 * NEGATIVE TEST: Double 'def' keyword
 * EXPECTED ERROR: Syntax error - duplicate keyword
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def def TestPart {  // ERROR: Double 'def' keyword
        attribute x : Integer;
    }
}
