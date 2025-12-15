/*
 * Test file for Unused Element Lint Rules (LINT001, LINT002)
 *
 * TRAINING NOTES:
 * - LINT001: Definition declared but never used (never referenced as a type)
 * - LINT002: Import statement that brings in unused symbols
 * - Unused elements add clutter and maintenance burden
 */
package UnusedElementTests {

    /* IMPORT ANALYSIS */

    /* USED: Referenced below */
    import ISQ::MassValue;

    /* LINT002: LengthValue imported but never used */
    import ISQ::LengthValue;

    /* LINT002: TimeValue imported but never used */
    import ISQ::TimeValue;

    /* USED: kg is referenced */
    import SI::kg;

    /* LINT002: m imported but never used */
    import SI::m;

    /* DEFINITION ANALYSIS */

    /* USED: Referenced as type for factory parts */
    part def Vehicle {
        attribute mass : Real;
    }

    /* USED: Specialized by SportsCar */
    part def Car :> Vehicle {
        attribute numDoors : Integer;
    }

    /* LINT001: Defined but never used anywhere */
    part def Truck :> Vehicle {
        attribute payloadCapacity : Real;
    }

    /* LINT001: Defined but never used */
    part def Motorcycle :> Vehicle {
        attribute hasSidecar : Boolean;
    }

    /* USED: Instantiated as mainEngine below */
    part def Engine {
        attribute power : Real;
        attribute displacement : Real;
    }

    /* LINT001: Defined but never used - dead code */
    part def DieselEngine :> Engine {
        attribute turbocharger : Boolean;
    }

    /* LINT001: Empty definition that serves no purpose */
    part def EmptyDefinition;

    /* USAGES - Shows what IS used */

    part def Factory {
        /* Uses Car definition */
        part productionCar : Car;

        /* Uses Vehicle definition */
        part prototypeVehicle : Vehicle;

        /* Uses Engine definition */
        part mainEngine : Engine;

        attribute totalWeight : Real;
    }

    /* Uses Car definition via specialization */
    part def SportsCar :> Car {
        attribute topSpeed : Real;
    }

    /* ACTION UNUSED ANALYSIS */

    /* USED: Performed by MainProcess */
    action def Initialize;

    /* LINT001: Action defined but never performed */
    action def Shutdown;

    /* LINT001: Action defined but never performed */
    action def DiagnosticScan;

    action def MainProcess {
        action step1 : Initialize;
    }
}

/*
 * RECOMMENDATIONS:
 *
 * 1. Remove unused imports:
 *    - Delete: import ISQ::LengthValue;
 *    - Delete: import ISQ::TimeValue;
 *    - Delete: import SI::m;
 *
 * 2. Remove or implement unused definitions:
 *    - Truck: Either use it or delete it
 *    - Motorcycle: Either use it or delete it
 *    - DieselEngine: Either integrate or delete
 *    - EmptyDefinition: Delete
 *
 * 3. Remove or implement unused actions:
 *    - Shutdown: Wire into workflow or delete
 *    - DiagnosticScan: Implement or delete
 */
