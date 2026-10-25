package V1ok {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle {
        attribute :>> mass : Real;
    }
}
