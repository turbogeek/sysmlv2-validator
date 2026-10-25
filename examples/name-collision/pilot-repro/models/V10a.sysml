package V10a {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def CarSameName :> Vehicle { alias mass for Vehicle::mass; }
    part def CarNewName :> Vehicle { alias weight for mass; }
    part myCar : CarNewName {
        attribute m = mass;
        attribute w = weight;
    }
}
