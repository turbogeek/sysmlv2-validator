package VerificationTests;

private import ScalarValues::*;

// Test verification definitions and verify relationships

requirement def SafetyRequirement {
    subject system : System;
    require constraint { system.speed <= 100.0 }
}

part def System {
    attribute speed : Real;
}

verification def SpeedTest {
    subject systemUnderTest : System;

    objective {
        doc /* Verify speed constraint is satisfied */
    }

    // Test method
    action testSpeed {
        // Verification logic
    }
}

requirement safetyReq : SafetyRequirement {
    subject testSystem;
}

part testSystem : System {
    attribute speed = 85.0;
}

// Verification case
verification speedVerification : SpeedTest {
    subject testSystem;
    objective {
        verify safetyReq;
    }
}
