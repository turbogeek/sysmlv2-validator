// SysML v2 definitions and usages that the other corpus files do not use (SysML v2 8.2.2.6-8.2.2.27).
// Exercises: 'case def' and 'case', 'enum def' without a body, an enumerated value with only a value (enumMember#5)
// and a body that redefines (enumMemberBodyElement#1), 'enum' usages with ';' and with a body, 'rendering' usages,
// the 'constant' prefix, 'references', 'assert' of a named constraint with 'not' and a body and without, an 'inout' parameter in
// an action usage body, and an anonymous redefinition in a constraint definition.
package SysmlUsages {
    private import ScalarValues::*;

    case def Inspection;
    enum def Colors;
    attribute def Coded {
        attribute code : Integer;
    }
    enum def Codes :> Coded {
        enum low;
        = 2;
        high { :>> code = 3; }
    }
    constraint def Positive {
        attribute x : Real;
    }
    constraint def Strict :> Positive {
        :>> x = 1.0;
    }

    part def Vehicle {
        enum shade : Colors;
        enum tint : Colors { }
        case check : Inspection;
        rendering sketch;
        constant attribute wheels : Integer = 4;
        attribute count references wheels;
        constraint fast : Positive { }
        assert not fast { true }
        assert fast;
        action drive {
            inout fuel : Real;
        }
    }
}
