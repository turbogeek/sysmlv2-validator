/**
 * NEGATIVE TEST: Redefines non-existent feature
 * EXPECTED ERROR: Cannot redefine non-existent feature
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    part def Base {
        attribute x : Integer;
    }

    part def Derived :> Base {
        attribute y :>> nonExistentAttribute;  // ERROR: nonExistentAttribute doesn't exist in Base
    }
}
