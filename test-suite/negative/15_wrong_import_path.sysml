/**
 * NEGATIVE TEST: Wrong import path
 * EXPECTED ERROR: Cannot resolve import path
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    private import NonExistent::Package::Types::*;  // ERROR: Path doesn't exist

    part def TestPart;
}
