package UnitMismatches {
    import ScalarValues::*;
    import ISQ::*;
    import SI::*;
    
    // Correct usage
    attribute length1: LengthValue = 4.0 [m];
    attribute mass1: MassValue = 10.5 [kg];
    attribute time1: TimeValue = 60 [s];
    
    // INCORRECT: Unit mismatch (CorrectnessRule)
    attribute badLength: LengthValue = 4.0 [kg]; // Should trigger LINT020
    attribute badMass: MassValue = 10.5 [m];    // Should trigger LINT020
    
    // INCORRECT: Missing unit (CompletenessRule)
    attribute lengthNoUnit: LengthValue = 4.0;   // Should trigger LINT021
    
    // INCORRECT: Generic type with unit (CompletenessRule)
    attribute genericWithUnit: Real = 4.0 [m];   // Should trigger LINT022
}
