/*
 * Constructs found in the SysML v2 cheat-sheet example models that the validator used to reject.
 * - 'assert not constraint' (AssertConstraintUsage isNegated, SysML v2 8.2.2.20)
 * - 'action stop terminate;' in a state body (TerminateNode 8.2.2.17.6; example in 7.18.3)
 * - transition with trigger, guard and an assign effect (TransitionAssignmentActionUsage, 8.2.2.18.3)
 */
package NegatedAssertTerminateTransitionAssign {
    private import ScalarValues::*;

    item def Tick;
    item def Abort;

    part def Counter {
        attribute n : Integer := 3;

        assert constraint nonNegative { n >= 0 }
        assert not constraint tooBig { n > 100 }

        exhibit state counting {
            entry; then running;

            state running;
            accept Tick if n > 0 do assign n := n - 1 then running;
            accept after 1 [SI::s] if n > 0 do action tickDown assign n := n - 1 then running;
            accept Abort then stop;
            transition countDown first running accept Tick if n > 1 do assign n := n - 2 then running;

            action stop terminate;
        }
    }

    action def Steps {
        action halt terminate;
    }
}
