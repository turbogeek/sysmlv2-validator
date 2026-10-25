package V8 {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real = 1000.0; }
    part car1 : Vehicle { attribute :>> mass = 1500.0; }
    part car2 : Vehicle { attribute mass = 1500.0; }
}
