/**
 * NEGATIVE TEST: Using reserved keyword as identifier
 * EXPECTED ERROR: Syntax error - reserved keyword used as name
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def action {  // ERROR: 'action' is a reserved keyword
        attribute value : Integer;
    }
}
