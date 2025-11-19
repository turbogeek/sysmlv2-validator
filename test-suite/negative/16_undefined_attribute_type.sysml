/**
 * NEGATIVE TEST: Undefined attribute type in part
 * EXPECTED ERROR: Cannot resolve attribute type
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    part def Engine {
        attribute power : PowerRating;  // ERROR: PowerRating not defined
        attribute efficiency : EfficiencyMetric;  // ERROR: EfficiencyMetric not defined
    }
}
