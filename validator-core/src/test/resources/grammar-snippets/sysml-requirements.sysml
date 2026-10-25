// SysML v2 requirements, concerns, cases and views (SysML v2 8.2.2.21-8.2.2.26).
// Exercises: in, out and inout parameters of a requirement, return, out and inout parameters of a constraint
// definition, 'frame' and 'require' of a feature chain, 'require constraint' whose body returns, 'constraint def' with
// ';', 'include' of a feature chain, 'render' of a feature chain in a view definition, 'expose' with a filter
// condition in a view usage, and an alias with only a short name. A requirement body itself has no 'return'
// (RequirementBodyItem), but a required constraint's body is a CalculationBody, which has; and a view definition body
// has no 'expose' (ViewDefinitionBodyItem; only the ViewBodyItem of a view usage has Expose).
package SysmlRequirements {
    private import ScalarValues::*;

    part def Vehicle { attribute mass : Real; }
    concern def Safety;
    requirement def MassLimit {
        subject v : Vehicle;
        in limit : Real;
        out margin : Real;
        inout budget : Real;
        frame v.mass;
        require v.mass;
        require constraint withinLimit { return ok : Boolean; }
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
    }
    view overview : Overview {
        expose SysmlRequirements::*[@Safety];
    }

    alias <VA> for Vehicle;
}
