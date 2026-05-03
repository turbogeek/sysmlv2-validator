library package <DS_Validation> DassaultSystemesValidation {
    package CoreValidationComponents {
        part def Suite :> DS_Styles::CoreStylesComponents::Policy {
            part [*] :>> rule {
                part condition : DS_Styles::CoreStylesComponents::Predicates::Condition;
                part effect [1..*] : Effect;
                attribute severity : SeverityLevel;
            }
            abstract part templateRule :> rule [*] {
                attribute :>> severity = severity::error;
                part :>> effect : DS_Validation::CoreValidationComponents::Failure {
                    attribute :>> 'message';
                }
                part :>> condition : DS_Validation::CoreValidationComponents::Condition {
                    ref item :>> subjectType;
                    calc :>> test {
                        true
                    }
                }
            }
        }
        part def Effect;
        part def Failure :> Effect {
            attribute 'message' : ScalarValues::String;
        }
        part def Condition :> DS_Styles::CoreStylesComponents::Predicates::Condition {
            ref item subjectType [0..*] : KerML::Kernel::Metaclass;
            calc test {
                in element : Metaobjects::Metaobject;
                return : ScalarValues::Boolean;
            }
        }
        part def SuitesConfiguration {
            part activeSuites [0..*] : Suite;
            part passiveSuites [0..*] : Suite;
        }
        attribute def SeverityDetails {
            attribute color : DS_Styles::CoreStylesComponents::KerMLStyles::Color;
            attribute icon;
            attribute priority;
        }
        enum def SeverityColors :> DS_Styles::CoreStylesComponents::KerMLStyles::Color {
            red;
            yellow;
            blue;
        }
        enum def SeverityLevel :> SeverityDetails {
            fatal {
                attribute :>> color default SeverityColors::red;
                attribute :>> priority default 1;
            }
            error {
                attribute :>> color default SeverityColors::red;
                attribute :>> priority default 2;
            }
            warning {
                attribute :>> color default SeverityColors::yellow;
                attribute :>> priority default 3;
            }
            info {
                attribute :>> color default SeverityColors::blue;
                attribute :>> priority default 4;
            }
            debug {
                attribute :>> color default SeverityColors::blue;
                attribute :>> priority default 5;
            }
        }
    }
    package Suites {
        part def SysMLConstraintsSuite :> CoreValidationComponents::Suite;
        part def DassaultSystemesValidationSuite :> CoreValidationComponents::Suite;
    }
}
