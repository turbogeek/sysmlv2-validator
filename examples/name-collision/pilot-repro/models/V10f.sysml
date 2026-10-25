package V10f {
    private import ScalarValues::*;
    part def Vehicle { attribute mass : Real; }
    part def Metric :> Vehicle { alias weight for mass; }
    part def Imperial :> Vehicle { alias weight for mass; }
    part def Scale { attribute weight : Real; }
    part def BothAliases :> Metric, Imperial { attribute p = weight; }
    part def MetricFirst :> Metric, Scale { attribute q = weight; }
    part def ScaleFirst :> Scale, Metric { attribute q = weight; }
}
