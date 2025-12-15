package 'Requirements Derivation' {
    /* The derivation connection is created to show the connection between requirements at different levels of the specification hierarchy or at the same level but with different levels of abstraction.
     *
     * It links more detailed requirements, often arising from implementation considerations, to their original, higher-level requirements. 
     *
     * A derivation connection asserts that one or more requirements are derived from a single original requirement. */
    package '1 Problem Domain' {
        private import RequirementDerivation::*;
        package '1 Black Box' {
            package '1 Stakeholder Needs' {
                requirement <'SN 1'> 'stakeholder needs' {
                    requirement <'SN 1.1'> 'desired temperature' {
                        doc /* It should be a possibility to easily specify the desired demperature.*/
                    }
                    requirement <'SN 1.2'> 'manual control' {
                        doc /* I should be able to start and stop climate control by myself.*/
                    }
                    requirement <'SN 1.3'> 'heating & cooling' {
                        doc /* The unit must be able to heat and cool. */
                    }
                }
            }
        }
    }
    package '2 Solution Domain' {
        package '1 System Requirements' {
            private import RequirementDerivation::*;
            private import '1 Problem Domain'::'1 Black Box'::'1 Stakeholder Needs'::*;
            private import '1 System Requirements'::*;
            requirement <'SR.1'> 'system requirements for vccs' {
                requirement <'SR.1.1'> 'setting desired temperature' {
                    doc /* The user shall be able to specify the temperature he/she desires in the cabin.*/
                }
                requirement <'SR.1.2'> 'human machine interactions' {
                    doc /* The system shall be able to accept data and commands from the user.*/
                }
                requirement <'SR 1.3'> 'cabin air' {
                    doc /* The system shall be able to draw air out of the cabin and blow comfortable air out.*/
                }
                requirement <'SR 1.4'> heating {
                    doc /* The system shall be able to heat the air, if the user desires to increase the temperature in the cabin.*/
                }
                requirement <'SR 1.5'> cooling {
                    doc /* The system shall be able to cool the air, if the user desires to decrease the temperature in the cabin.*/
                }
            }
            #derivation connection {
                end #original requirement ::> 'stakeholder needs'.'manual control';
                end #derive requirement ::> 'system requirements for vccs'.'human machine interactions';
            }
            #derivation connection {
                end #original requirement ::> 'stakeholder needs'.'desired temperature';
                end #derive requirement ::> 'system requirements for vccs'.'setting desired temperature';
            }
            #derivation connection {
                end #original requirement ::> 'stakeholder needs'.'heating & cooling';
                end #derive requirement ::> 'system requirements for vccs'.'cabin air';
                end #derive requirement ::> 'system requirements for vccs'.heating;
                end #derive requirement ::> 'system requirements for vccs'.cooling;
            }
        }
    }
    view 'requirements derivation' : DS_Views::SymbolicViews::gv;
}
