/**
 * NEGATIVE TEST: Redefinition with incompatible type
 * EXPECTED ERROR: Redefined feature has incompatible type
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    part def Base {
        attribute value : Integer;
    }

    part def Derived :> Base {
        attribute :>> value : String;  // ERROR: Cannot redefine Integer as String
    }
}
