package V10e {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def CarRenamed :> Vehicle { attribute weight :>> mass; }   // intended: a second name for mass
    part def CarAliased :> Vehicle { alias weight for mass; }
    part r : CarRenamed { attribute rm = mass; attribute rw = weight; }
    part a : CarAliased { attribute am = mass; attribute aw = weight; }
}
