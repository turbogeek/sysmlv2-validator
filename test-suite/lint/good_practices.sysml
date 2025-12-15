/*
 * Test file demonstrating GOOD practices (NO lint warnings expected)
 *
 * This file shows the recommended patterns for SysML v2 models that
 * will pass all lint checks. Use this as a reference for best practices.
 *
 * TRAINING NOTES:
 * - Specific imports instead of wildcards
 * - Private imports (default) instead of public
 * - PascalCase for definitions, camelCase for usages
 * - All definitions are used
 * - All imports are used
 */
package GoodPracticesExample {

    /* SPECIFIC IMPORTS - Only import what you need */
    import ISQ::MassValue;
    import ISQ::LengthValue;
    import SI::kg;
    import SI::m;

    /* WELL-NAMED DEFINITIONS - PascalCase */

    part def Vehicle {
        attribute mass : Real;
        attribute length : Real;
        attribute maxSpeed : Real;
    }

    part def PassengerCar :> Vehicle {
        attribute passengerCapacity : Integer;
        attribute trunkVolume : Real;
    }

    part def ElectricVehicle :> Vehicle {
        attribute batteryCapacity : Real;
        attribute range : Real;
    }

    part def Powertrain {
        attribute power : Real;
        attribute efficiency : Real;
    }

    /* WELL-NAMED USAGES - camelCase */

    part def VehicleAssembly {
        part mainVehicle : PassengerCar;
        part electricVariant : ElectricVehicle;
        part frontPowertrain : Powertrain;
        part rearPowertrain : Powertrain;
    }

    /* ACTION DEFINITIONS */

    action def StartVehicle {
        in vehicle : Vehicle;
    }

    action def StopVehicle {
        in vehicle : Vehicle;
    }

    action def OperationalWorkflow {
        action startup : StartVehicle;
        then action shutdown : StopVehicle;
    }

    /* REQUIREMENTS */

    requirement def SafetyRequirement {
        subject vehicle : Vehicle;
    }

    /* VERIFICATION THAT ALL ELEMENTS ARE USED */

    part def TestConfiguration {
        part testAssembly : VehicleAssembly;
        perform testWorkflow : OperationalWorkflow;
    }
}

/*
 * SUMMARY OF GOOD PRACTICES:
 *
 * 1. IMPORTS
 *    - Use specific imports: import ISQ::MassValue;
 *    - Don't use wildcards: avoid import ISQ::*;
 *
 * 2. NAMING
 *    - Definitions: PascalCase (Vehicle, PassengerCar)
 *    - Usages: camelCase (mainVehicle, frontPowertrain)
 *    - Packages: PascalCase (GoodPracticesExample)
 *
 * 3. ORGANIZATION
 *    - Group related definitions together
 *    - Ensure all definitions are used
 */
