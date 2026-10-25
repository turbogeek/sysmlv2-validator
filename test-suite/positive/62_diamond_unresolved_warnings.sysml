/*
 * Diamond inheritance left unresolved: valid SysML v2 (no errors), but the validator warns.
 *
 * HybridVehicle specializes ElectricVehicle and CombustionVehicle, which both specialize Vehicle.
 * Without its own redefinitions HybridVehicle inherits both branches' redefinitions:
 *   - range and energySource: DIAMOND_INHERITANCE_CONFLICT, the redefinitions contradict
 *     (400.0 vs 700.0, "battery" vs "fuel")
 *   - wheelCount: DIAMOND_INHERITANCE_CONFLICT, the redefinitions are duplicates (both 4)
 *   - serviceInterval: MULTIPLE_INHERITANCE_NAME_CONFLICT, each branch declares its own unrelated
 *     feature with that name
 * The usage 'prototype', typed by both definitions, gets the same warnings.
 *
 * Refactoring: redefine range and energySource in HybridVehicle, and move wheelCount = 4 and
 * serviceInterval into Vehicle. 61_diamond_resolved.sysml is the refactored model.
 */
package DiamondUnresolved {
    private import ScalarValues::*;

    part def Vehicle {
        attribute mass : Real;
        attribute range : Real;
        attribute energySource : String;
        attribute wheelCount : Integer;
    }

    part def ElectricVehicle :> Vehicle {
        attribute :>> range = 400.0;
        attribute :>> energySource = "battery";
        attribute :>> wheelCount = 4;
        attribute serviceInterval : Real;
    }

    part def CombustionVehicle :> Vehicle {
        attribute :>> range = 700.0;
        attribute :>> energySource = "fuel";
        attribute :>> wheelCount = 4;
        attribute serviceInterval : Real;
    }

    part def HybridVehicle :> ElectricVehicle, CombustionVehicle;

    part def Fleet {
        part prototype : ElectricVehicle, CombustionVehicle;
    }
}
