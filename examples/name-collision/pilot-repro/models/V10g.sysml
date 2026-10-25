package V10g {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; alias weight for mass; }
    part def CarFixed :> Vehicle {
        attribute :>> mass : Real;
        alias weight for mass;
        attribute w = weight;
    }
    part myCarFixed : Vehicle {
        attribute :>> mass = 1500.0;
        alias weight for mass;
        attribute w = weight;
    }
    part def CarAliasFixed :> Vehicle { alias isqMass for ISQ::mass; attribute m = mass; }

    // Rename and keep the old name (V10e)
    part def Machine { attribute mass : Real; }
    part def CarRenamedKeepingName :> Machine { attribute weight :>> mass; alias mass for weight; }
    part r : CarRenamedKeepingName { attribute rm = mass; attribute rw = weight; }

    // Two generals with one name (V10f): rename one in the specialization
    part def Metric :> Machine { alias weight for mass; }
    part def Scale { attribute weight : Real; }
    part def WeighedCarFixed :> Metric, Scale { attribute scaleWeight :>> Scale::weight; attribute q = weight; }
}
