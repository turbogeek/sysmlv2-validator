// SysML v2 state bodies (SysML v2 8.2.2.18).
// Exercises: 'out' and 'inout' parameters of a state, entry, do and exit actions that assign to a name or a feature
// chain or perform a feature chain, an accept transition with 'via' and a 'do send' effect, and 'then state'. The
// accept transition follows the state usage that is its source (StateBodyItem: a TargetTransitionUsageMember comes
// right after a BehaviorUsageMember, not after an entry, do or exit action).
package SysmlStates {
    private import ScalarValues::*;

    attribute def Signal;

    state def Machine {
        out status : Integer;
        inout mode : Integer;
        part controller { attribute level : Integer; action reset; }

        entry assign mode := 1;
        entry assign controller.level := 2;
        entry controller.reset;
        do controller.reset;
        do assign mode := 3;
        do assign controller.level := 4;
        exit assign mode := 0;
        exit controller.reset;

        state off;
        state on;
        accept sig : Signal via controller do send sig to controller then off;
        then state waiting { }
    }
}
