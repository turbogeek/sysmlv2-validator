/**
 * NEGATIVE TEST: Invalid identifier (starts with number)
 * EXPECTED ERROR: Syntax error - invalid identifier
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def 123InvalidName {  // ERROR: Identifier cannot start with number
        attribute value : Integer;
    }
}
