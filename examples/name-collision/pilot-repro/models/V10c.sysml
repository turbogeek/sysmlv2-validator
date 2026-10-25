package V10c {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; alias weight for mass; }
    part def Car :> Vehicle { attribute weight : Real; }          // intended: Vehicle's mass, by its alias
    part def CarShortName :> Vehicle { attribute <weight> w : Real; }
}
