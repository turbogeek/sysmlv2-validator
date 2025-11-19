/**
 * NEGATIVE TEST: Reference to undefined part
 * EXPECTED ERROR: Cannot resolve part definition
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    part def Vehicle {
        part engine : UndefinedEngine;  // ERROR: UndefinedEngine not defined
        part transmission : UndefinedTransmission;  // ERROR: UndefinedTransmission not defined
    }
}
