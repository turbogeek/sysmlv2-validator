package NameCollisionErrorForm {
    doc /*
     * V8: the diagnostics reward the wrong form. When the general binds a value with '=', the correct
     * redefinition is an ERROR, while the pitfall (a second feature with the same name) is only a WARNING.
     * Kept apart from NameCollisionExamples.sysml because carCorrectForm is meant to be rejected.
     */
    private import ScalarValues::*;

    part def VehicleBound { attribute mass : Real = 1000.0; }
    part carCorrectForm : VehicleBound {
        attribute :>> mass = 1500.0 {
            doc /* Pilot: error "Cannot override a binding feature value". */
        }
    }
    part carPitfall : VehicleBound {
        attribute mass = 1500.0 {
            doc /* Pilot: only a warning "Duplicate of inherited member name 'mass' from VehicleBound";
                 * the car now has two masses, 1000.0 and 1500.0. */
        }
    }

    part def VehicleDefaulted { attribute mass : Real default 1000.0; }
    part carFixed : VehicleDefaulted {
        attribute :>> mass = 1500.0 {
            doc /* FIX: give the general a default value, which a redefinition may override. */
        }
    }
}
