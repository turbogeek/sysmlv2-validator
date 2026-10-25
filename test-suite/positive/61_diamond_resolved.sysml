/*
 * Diamond inheritance, resolved.
 *
 * HybridVehicle specializes both ElectricVehicle and CombustionVehicle, and both of those specialize
 * Vehicle: a diamond. Both branches redefine Vehicle's range and energySource with different values,
 * so HybridVehicle redefines them itself to state the values that apply. It names both branches'
 * features ('range :>> ElectricVehicle::range, CombustionVehicle::range'): a plain ':>> range' would redefine
 * only the first general's range and leave the other inherited. The branch values are defaults, because a
 * bound value ('=') cannot be overridden by a redefinition. Features that one branch
 * redefines (chargePower), or that no branch redefines (mass), are inherited once and need nothing.
 * Both branches also need a service interval: it is declared once, in the common supertype, instead
 * of once per branch.
 *
 * Validates with no errors and no inheritance warnings.
 * Compare 62_diamond_unresolved_warnings.sysml, the same model before refactoring.
 */
package DiamondResolved {
    private import ScalarValues::*;

    part def Vehicle {
        attribute mass : Real;
        attribute range : Real;
        attribute energySource : String;
        attribute chargePower : Real;
        attribute serviceInterval : Real;
    }

    part def ElectricVehicle :> Vehicle {
        attribute :>> range default 400.0;
        attribute :>> energySource default "battery";
        attribute :>> chargePower = 150.0;
    }

    part def CombustionVehicle :> Vehicle {
        attribute :>> range default 700.0;
        attribute :>> energySource default "fuel";
        attribute tankVolume : Real;
    }

    /* Both branches redefine range and energySource: the hybrid states which values apply */
    part def HybridVehicle :> ElectricVehicle, CombustionVehicle {
        attribute range :>> ElectricVehicle::range, CombustionVehicle::range = 1000.0;
        attribute energySource :>> ElectricVehicle::energySource, CombustionVehicle::energySource = "battery and fuel";
    }

    /* A usage typed by both definitions resolves the conflict in its own body */
    part def Fleet {
        part prototype : ElectricVehicle, CombustionVehicle {
            attribute range :>> ElectricVehicle::range, CombustionVehicle::range = 900.0;
            attribute energySource :>> ElectricVehicle::energySource, CombustionVehicle::energySource
                = "battery and fuel";
        }
    }
}
