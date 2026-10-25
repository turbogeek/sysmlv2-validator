package V10b {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle {
        alias mass for ISQ::mass;           // intended: a shorthand for the library quantity
        attribute probe = mass;
    }
    part def SportsCar :> Car { attribute :>> mass; }
}
