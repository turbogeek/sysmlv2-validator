/**
 * NEGATIVE TEST: Undefined variable in constraint
 * EXPECTED ERROR: Cannot resolve variable in constraint
 * CATEGORY: Type/Resolution Errors
 */
package NegativeTests {
    private import ScalarValues::*;

    constraint def SpeedLimit {
        attribute maxSpeed : Real;

        assert currentSpeed <= maxSpeed;  // ERROR: currentSpeed not defined
    }
}
