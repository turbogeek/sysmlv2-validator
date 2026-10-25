package MassTypedByMass {
    doc /*
     * "The car's mass is of type mass": ways of writing it that the language rejects, next to the correct forms.
     * Kept apart from NameCollisionExamples.sysml because the pitfalls are errors.
     * A usage cannot be the type of a usage: 'mass' and 'ISQ::mass' are attribute usages, and so is the unit 'kg'.
     */
    private import ISQ::*;
    private import SI::*;

    part def Vehicle { attribute mass : MassValue; }

    part def CarTypedByItself :> Vehicle {
        attribute mass : mass {
            doc /* PITFALL: 'mass' is looked up in CarTypedByItself first, where it finds this attribute, so the
                 * attribute is typed by itself. It is also a second mass beside Vehicle::mass. Pilot: error "An
                 * attribute must be typed by attribute definitions" and warning "Duplicate of inherited member
                 * name 'mass' from Vehicle". */
        }
    }
    part def CarTypedByLibraryUsage :> Vehicle {
        attribute mass : ISQ::mass {
            doc /* PITFALL: ISQ::mass is the library's mass quantity, an attribute usage, not a definition.
                 * Pilot: the same error and warning. */
        }
    }
    part def CarTypedByUnit :> Vehicle {
        attribute mass : kg {
            doc /* PITFALL: kg is a unit, an attribute usage typed MassUnit. Pilot: the same error and warning. */
        }
    }

    part def CarFixed :> Vehicle {
        attribute :>> mass : MassValue {
            doc /* FIX: redefine Vehicle::mass, typed by the quantity definition MassValue. */
        }
    }
    part def CarFixedBySubsetting :> Vehicle {
        attribute :>> mass :> ISQ::mass {
            doc /* FIX: or redefine it and subset the library quantity, which brings its type, MassValue. */
        }
    }
    part myCar : CarFixed {
        attribute :>> mass = 1500 [kg] {
            doc /* Values are given in a unit. */
        }
    }
}
