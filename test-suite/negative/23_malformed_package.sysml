/**
 * NEGATIVE TEST: Malformed package declaration
 * EXPECTED ERROR: Syntax error in package declaration
 * CATEGORY: Syntax Errors
 */
package {  // ERROR: Missing package name
    part def TestPart;
}
