package V10d {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; alias weight for mass; }
    part def CarViaName :> Vehicle { attribute :>> mass = 1500.0; }
    part def CarViaAlias :> Vehicle { attribute :>> weight = 1500.0; }
    part c1 : CarViaName { attribute w = weight; }
    part c2 : CarViaAlias { attribute w = weight; }
    part c3 : Vehicle {
        attribute :>> weight = 1500.0;
        attribute w = weight;
    }
}
