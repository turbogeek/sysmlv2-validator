/**
 * NEGATIVE TEST: Potential circular import/dependency
 * EXPECTED ERROR: Circular dependency detection
 * CATEGORY: Semantic Errors
 */
package PackageA {
    private import PackageB::*;  // Would create circular dependency if PackageB imports PackageA

    part def PartA {
        part b : PartB;
    }
}
