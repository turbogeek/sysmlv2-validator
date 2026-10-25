package ConstraintTests;

private import ScalarValues::*;

part def BoundedValue {
    attribute value : Real;
    
    // Constraint
    assert constraint {
        value >= 0.0 and value <= 100.0
    }
    
    // Second assertion ('assume' is only valid in a requirement body)
    assert constraint {
        value != 50.0
    }
}

requirement def ValueRequirement {
    subject val : BoundedValue;
    
    require constraint {
        val.value > 10.0
    }
    
    assume constraint {
        val.value < 90.0
    }
}
