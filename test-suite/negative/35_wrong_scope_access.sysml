/**
 * NEGATIVE TEST: Accessing element from wrong scope
 * EXPECTED ERROR: Cannot access element outside its scope
 * CATEGORY: Type/Resolution Errors
 */
package PackageA {
    private part def PrivatePart {
        attribute x : Integer;
    }
}

package PackageB {
    part def PublicPart {
        part internal : PackageA::PrivatePart;  // ERROR: Cannot access private element
    }
}
