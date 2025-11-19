/**
 * NEGATIVE TEST: Reference to non-existent enum value
 * EXPECTED ERROR: Enum value not defined
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    enum def Status {
        enum active;
        enum inactive;
    }

    part def TestPart {
        attribute status : Status = Status::pending;  // ERROR: 'pending' not in Status enum
    }
}
