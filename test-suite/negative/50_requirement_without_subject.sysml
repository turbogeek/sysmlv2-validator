/**
 * NEGATIVE TEST: Requirement referencing undefined subject
 * EXPECTED ERROR: Cannot resolve requirement subject
 * CATEGORY: Semantic Errors
 */
package NegativeTests {
    requirement def PerformanceReq {
        subject vehicle : UndefinedVehicle;  // ERROR: UndefinedVehicle not defined

        require constraint { vehicle.speed < 100; }
    }
}
