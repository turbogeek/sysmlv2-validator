package V1 {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle {
        attribute mass : Real;
    }
}
