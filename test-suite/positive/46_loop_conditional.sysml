package LoopConditionalTests;
private import ScalarValues::*;

// Test loop and conditional action constructs

action def ProcessData {
    in values : Real[*];
    out results : Real[*];

    // Loop over collection
    action processEach for value : Real in values {
        out result : Real;
    }

    // While loop
    action whileLoop {
        attribute count : Integer = 0;

        while count < 10 {
            action increment {
                // Increment count
            }
        }
    }

    // If-then-else conditional
    action conditional {
        in testValue : Real;
        out decision : String;

        if (testValue > 0.0) {
            action positive {
                // Handle positive case
            }
        } else if (testValue < 0.0) {
            action negative {
                // Handle negative case
            }
        } else {
            action zero {
                // Handle zero case
            }
        }
    }
}

action def ControlFlow {
    // Nested conditionals
    action complexDecision {
        in x : Real;
        in y : Real;

        if (x > 0.0) {
            if (y > 0.0) {
                action quadrant1;
            } else {
                action quadrant4;
            }
        } else {
            if (y > 0.0) {
                action quadrant2;
            } else {
                action quadrant3;
            }
        }
    }
}
