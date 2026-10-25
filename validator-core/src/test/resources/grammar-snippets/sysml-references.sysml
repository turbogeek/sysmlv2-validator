// SysML v2 members that refer to an existing feature by a qualified name or a feature chain instead of declaring one
// (SysML v2 8.2.2.9.2-8.2.2.9.3, 8.2.2.18.1, 8.2.2.18.3, 8.2.2.21.1, 8.2.2.24, 8.2.2.25, 8.2.2.26.1).
// Exercises: an event occurrence of a qualified name (occurrenceUsage#10) and 'then event' of a feature chain
// (thenStatement#15), each in a body where no statement can follow a bare 'event' (in action and definition bodies the
// parser splits them, see ShadowedAlternativeTest), an accept transition
// whose payload is typed by a qualified name (acceptTransition#2), 'assume' and 'require' of an existing constraint
// (assumeConstraint#2, requireConstraint#3), 'verify requirement' with a declaration (verifyRequirement#2), 'include'
// of a use case from the global namespace (includeUsage#2) and 'render' of a rendering with a multiplicity
// (renderStatement#4).
package SysmlReferences {
    private import ScalarValues::*;

    package Signals {
        attribute def Start;
        occurrence arrival;
    }
    event Signals::arrival;

    state def Machine {
        part sensor {
            event occurrence tripped;
        }
        state off;
        accept Signals::Start then on;
        state on;
        then event sensor.tripped;
    }

    part def Vehicle {
        attribute mass : Real;
    }
    constraint massLimit {
        in mass : Real;
        mass <= 2000.0
    }
    requirement def MassRequirement {
        subject vehicle : Vehicle;
        assume massLimit;
        require $::SysmlReferences::massLimit;
    }
    verification def MassTest {
        objective {
            verify requirement massCheck : MassRequirement;
        }
    }

    use case refuel;
    use case def Trip {
        include $::SysmlReferences::refuel;
    }

    rendering sketch;
    view def Overview {
        render sketch [0..*];
    }
}
