package V2x {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle { attribute mass : Real; }
    part myCar : Car {
        attribute mass = 1500.0;
    }
}
