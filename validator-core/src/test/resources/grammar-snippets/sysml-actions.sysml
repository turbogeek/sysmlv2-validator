// SysML v2 action bodies (SysML v2 8.2.2.17).
// Exercises: assignment, while and loop action nodes ('action a assign ...', 'action a while ... until',
// 'action a loop ... until'), loop, while, for, terminate and if-else-if-else nodes, a decision node with 'if ... then'
// and 'else' successions, and the 'then' forms that the other files do not use: merge, decide, terminate, a while
// action, a while node, perform, assign and if.
package SysmlActions {
    private import ScalarValues::*;

    action def Run {
        in x : Real;
        inout level : Real;
        attribute speed : Integer;
        attribute flag : Boolean;
        part engine { attribute rpm : Integer; }
        action start;
        action finish;
        action step;

        action setSpeed assign speed := 10;
        action repeat while speed < 10 { } until speed >= 10;
        action spin loop { } until speed > 5;
        loop { } until flag;
        while flag { }
        for i : Integer in (1, 2, 3) { }
        terminate;
        if flag { } else if speed > 1 { } else { }

        decide;
        if flag then start;
        else finish;

        then merge joined;
        then decide;
        then terminate;
        then action again while flag { }
        then while flag { }
        then event engine.rpm;
        then perform start;
        then assign engine.rpm := 1;
        then if flag { }
    }
}
