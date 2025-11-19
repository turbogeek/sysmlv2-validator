/**
 * NEGATIVE TEST: Missing opening brace for package
 * EXPECTED ERROR: Syntax error - missing opening brace
 * CATEGORY: Syntax Errors
 */
package NegativeTests  // ERROR: Missing opening brace
    part def TestPart {
        attribute x : Integer;
    }
}
