package V6f {
    private import ISQ::*;
    private import SI::*;
    part def Vehicle { attribute mass : MassValue; }
    part def CarTypedByItself :> Vehicle {
        attribute mass : mass;              // intended: Vehicle's mass, typed as a mass quantity
    }
    part def CarTypedByLibraryUsage :> Vehicle {
        attribute mass : ISQ::mass;         // intended: typed by the library quantity 'mass'
    }
    part def CarTypedByUnit :> Vehicle {
        attribute mass : kg;                // intended: a mass in kilograms
    }
    part def CarFixed :> Vehicle { attribute :>> mass : MassValue; }
    part def CarFixedSubsetting :> Vehicle { attribute :>> mass :> ISQ::mass; }

    // Other kinds of usage typed by a usage
    part engine;
    item fuel;
    port p;
    action drive;
    occurrence trip;
    part def Truck {
        part e : engine;
        item f : fuel;
        port q : p;
        action d : drive;
        occurrence o : trip;
        ref r : engine;
    }
    requirement def EngineCheck { subject s : engine; }
}
