package ActorStakeholderTests;

// Test actor and stakeholder usages. SysML v2 has no 'actor def' or 'stakeholder def': actors and
// stakeholders are part usages, typed here by part definitions.

part def User {
    doc /* End user of the system */
}

part def Administrator {
    doc /* System administrator */
}

part def ExternalSystem {
    doc /* External interfacing system */
}

part def Customer {
    doc /* Paying customer */

    concern 'Cost' {
        doc /* Minimize total cost of ownership */
    }

    concern 'Usability' {
        doc /* System must be easy to use */
    }
}

part def Regulator {
    doc /* Regulatory authority */

    concern 'Compliance' {
        doc /* System must meet regulatory requirements */
    }
}

use case def SystemOperation {
    subject system : MainSystem;
    actor user : User;
    actor admin : Administrator;
}

part def MainSystem {
    doc /* Main system being designed */
}

// Requirements from stakeholders
requirement def CustomerRequirement {
    stakeholder customer : Customer;

    doc /* Requirements derived from customer concerns */

    satisfy concern customer.Cost;
    satisfy concern customer.Usability;
}

requirement costReq : CustomerRequirement {
    doc /* Total cost of ownership must be under $10M */
}
