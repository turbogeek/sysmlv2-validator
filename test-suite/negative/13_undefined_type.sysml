/**
 * NEGATIVE TEST: Reference to undefined type
 * EXPECTED ERROR: Name resolution error for undefined type
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    part def Vehicle {
        attribute speed : UndefinedSpeedType;  // ERROR: UndefinedSpeedType not defined
    }
}
