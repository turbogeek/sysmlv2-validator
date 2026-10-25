// SysML v2 requirements, concerns, cases and views (SysML v2 8.2.2.21-8.2.2.26).
// Exercises: return, in, out and inout parameters of a requirement and of a constraint definition, 'frame' and
// 'require' of a feature chain, 'constraint def' with ';', 'include' of a feature chain, 'render' of a feature chain,
// 'expose' with a filter condition, and an alias with only a short name.
package SysmlRequirements {
    private import ScalarValues::*;

    part def Vehicle { attribute mass : Real; }
    concern def Safety;
    requirement def MassLimit {
        subject v : Vehicle;
        return verdict : Boolean;
        in limit : Real;
        out margin : Real;
        inout budget : Real;
        frame v.mass;
        require v.mass;
    }
    constraint def Check;
    constraint def Bounded {
        out result : Real;
        inout scale : Real;
        return area : Real;
    }
    part car : Vehicle;
    use case def Drive;
    use case def Trip {
        include car.mass;
    }

    view def Overview {
        render car.mass;
        expose SysmlRequirements::*[@Safety];
    }

    alias <VA> for Vehicle;
}
