package V6b {
    private import ISQ::*;
    private import SI::*;
    part def Vehicle { attribute totalMass :> ISQ::mass; }
    part def Car :> Vehicle {
        attribute :>> mass = 1500 [kg];
    }
}
