/**
 * NEGATIVE TEST: Circular specialization/inheritance
 * EXPECTED ERROR: Circular specialization detected
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    part def A :> B;  // A specializes B
    part def B :> A;  // ERROR: B specializes A - circular dependency
}
