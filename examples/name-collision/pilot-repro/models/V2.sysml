package V2 {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Car :> Vehicle { attribute :>> mass; }
    part myCar : Car {
        attribute mass = 1500.0;
    }
    part yourCar : Car {
        mass = 1500.0;
    }
    part okCar : Car {
        attribute :>> mass = 1500.0;
    }
}
