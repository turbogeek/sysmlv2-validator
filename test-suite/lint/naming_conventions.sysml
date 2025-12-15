/*
 * Test file for Naming Convention Lint Rules (LINT005, LINT006, LINT007)
 *
 * TRAINING NOTES:
 * - Definitions (part def, action def, etc.) should use PascalCase
 * - Usages (part, attribute, etc.) should use camelCase
 * - Packages should use PascalCase
 */
package NamingConventionTests {

    /* DEFINITION NAMING - Should be PascalCase (LINT005) */

    /* CORRECT: PascalCase for definitions */
    part def Vehicle {
        attribute mass : Real;
    }

    part def ElectricVehicle :> Vehicle {
        attribute batteryCapacity : Real;
    }

    /* LINT005: lowercase definition name - should be Engine */
    part def engine {
        attribute power : Real;
    }

    /* LINT005: camelCase definition name - should be BrakeSystem */
    part def brakeSystem {
        attribute force : Real;
    }

    /* LINT005: snake_case definition name - should be FuelTank */
    part def fuel_tank {
        attribute capacity : Real;
    }

    /* USAGE NAMING - Should be camelCase (LINT006) */

    part def CorrectUsageNaming {
        /* CORRECT: camelCase for usages */
        part frontEngine : Vehicle;
        part rearAxle : Vehicle;
        attribute maxSpeed : Real;
        attribute fuelCapacity : Real;

        /* LINT006: PascalCase usage name - should be frontBrake */
        part FrontBrake : Vehicle;

        /* LINT006: UPPER_CASE usage name - should be mainEngine */
        part MAIN_ENGINE : Vehicle;

        /* LINT006: snake_case usage name - should be rearSuspension */
        part rear_suspension : Vehicle;
    }

    /* PACKAGE NAMING - Should be PascalCase (LINT007) */

    /* CORRECT: PascalCase for packages */
    package VehicleSystems {
        part def Transmission;
    }

    package ElectricalComponents {
        part def Battery;
    }

    /* LINT007: lowercase package name - should be Utilities */
    package utilities {
        part def Helper;
    }

    /* LINT007: snake_case package name - should be VehicleParts */
    package vehicle_parts {
        part def Wheel;
    }

    /* RECOMMENDED PATTERNS */

    part def WellNamedVehicle {
        part mainPowertrain : Vehicle;
        part auxiliaryPowertrain : Vehicle;
        attribute currentSpeed : Real;
        attribute targetSpeed : Real;
    }

    /* SPECIAL CASES - Acronyms */

    part def ECUController {
        attribute status : Integer;
    }

    part def CANBusInterface {
        attribute baudRate : Integer;
    }

    part def AcronymUsage {
        part ecuModule : ECUController;
        part mainCANBus : CANBusInterface;
    }
}
