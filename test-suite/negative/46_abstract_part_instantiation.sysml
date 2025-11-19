/**
 * NEGATIVE TEST: Cannot instantiate abstract part
 * EXPECTED ERROR: Cannot create instance of abstract definition
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    abstract part def AbstractBase {
        attribute x : Integer;
    }

    part concrete : AbstractBase;  // ERROR: Cannot instantiate abstract definition
}
