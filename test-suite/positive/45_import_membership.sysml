package ImportMembershipTests;

// Test various import and membership patterns

// Public import
public import CommonTypes::*;

// Private import
private import UtilityFunctions::calculateHash;

// Filtered import (selective membership imports; SysML has no brace-list import)
private import SensorLibrary::TemperatureSensor;
private import SensorLibrary::PressureSensor;

package CommonTypes {
    attribute def Timestamp;
    attribute def Identifier;
}

package UtilityFunctions {
    attribute def calculateHash;
}

package SensorLibrary {
    part def TemperatureSensor;
    part def PressureSensor;
    part def HumiditySensor;
}

// Namespace membership
package SystemComponents {
    public part def PublicComponent {
        doc /* Visible outside package */
    }

    private part def InternalComponent {
        doc /* Only visible within package */
    }

    part def System {
        // Can use both public and private within package
        part publicPart : PublicComponent;
        part internalPart : InternalComponent;
    }
}

// Alias import
alias Real for ScalarValues::RealValue;

package ScalarValues {
    attribute def RealValue;
}
