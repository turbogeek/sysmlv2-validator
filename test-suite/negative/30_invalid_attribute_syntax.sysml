/**
 * NEGATIVE TEST: Invalid attribute definition syntax
 * EXPECTED ERROR: Syntax error in attribute definition
 * CATEGORY: Syntax Errors
 */
package NegativeTests {
    part def TestPart {
        def attribute speed : Integer;  // ERROR: 'def' should come after 'attribute'
    }
}
