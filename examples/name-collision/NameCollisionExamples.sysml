package NameCollisionExamples {
    doc /*
     * Name-collision pitfalls in SysML v2, each next to its fix.
     * In every sub-package, elements named ...Pitfall show the mistake and elements named ...Fixed show
     * the correct form. The doc comment of each pitfall states what the reference implementation
     * (SysML v2 Pilot Implementation 0.62.0 and 0.63.0) reports for it. See docs/NAME_COLLISION_ANALYSIS.md.
     */
    private import ScalarValues::*;

    package V1_RedeclareInDefinition {
        doc /* Re-declaring an inherited feature instead of redefining it. */
        part def Vehicle { attribute mass : Real; }
        part def CarPitfall :> Vehicle {
            attribute mass : Real {
                doc /* PITFALL: a second, unrelated feature named mass; Vehicle::mass is still inherited.
                     * Pilot: warning "Duplicate of inherited member name 'mass' from Vehicle". */
            }
        }
        part def CarFixed :> Vehicle {
            attribute :>> mass : Real {
                doc /* FIX: redefine the inherited mass. */
            }
        }
    }

    package V2_ValueOnUsage {
        doc /* Giving an inherited feature a value on a usage. */
        part def Vehicle { attribute mass : Real; }
        part myCarPitfall : Vehicle {
            attribute mass = 1500.0 {
                doc /* PITFALL: a new attribute; the inherited mass stays without a value.
                     * Pilot: warning "Duplicate of inherited member name 'mass' from Vehicle". */
            }
        }
        part yourCarPitfall : Vehicle {
            mass = 1500.0 {
                doc /* PITFALL: the keyword-less form also declares a new (reference) feature.
                     * Pilot: warning "Duplicate of inherited member name 'mass' from Vehicle". */
            }
        }
        part myCarFixed : Vehicle {
            attribute :>> mass = 1500.0 {
                doc /* FIX: redefine the inherited mass and bind its value. */
            }
        }
    }

    package V3_DuplicateInOneScope {
        doc /* Two members with the same name in one namespace. */
        part def VehiclePitfall {
            doc /* PITFALL: copy/paste duplicate. Pilot: warning "Duplicate of other owned member name" on both. */
            attribute mass : Real;
            attribute mass : Real;
        }
        part def VehicleFixed {
            attribute mass : Real;
            attribute payloadMass : Real;
        }
        part def TruckPitfall {
            doc /* PITFALL: a definition and a usage both named load. The specification allows this
                 * (different metaclasses); the pilot warns on both and resolves ':> load' to the usage. */
            attribute def load;
            attribute load : Real;
            attribute payload :> load;
        }
        part def TruckFixed {
            attribute def Load;
            attribute load : Load;
            attribute payload :> load;
        }
    }

    package V4_MultipleInheritance {
        doc /* The same name inherited from two general types. */
        part def Powered { attribute rating : Real; }
        part def Insured { attribute rating : Real; }
        part def TruckPitfall :> Powered, Insured {
            doc /* PITFALL: two inherited features named rating.
                 * Pilot: warning "Duplicate of inherited member name 'rating' from Insured, Powered";
                 * 'rating' below resolves to Powered::rating only because Powered is listed first. */
            assert constraint { rating > 0.0 }
        }
        part def TruckFixed :> Powered, Insured {
            doc /* FIX: redefine each inherited rating under a distinct name. */
            attribute powerRating :>> Powered::rating;
            attribute insuranceRating :>> Insured::rating;
            assert constraint { powerRating > 0.0 }
        }

        part def A { part p[*]; }
        part def A1 :> A { part p1 :>> p; }
        part def A2 :> A;
        part def DiamondPitfall :> A1, A2 {
            part p {
                doc /* PITFALL: A1 renamed A::p to p1, so this p collides with nothing and is a new,
                     * unrelated feature. Pilot: no diagnostic. */
            }
        }
        part def DiamondFixed :> A1, A2 {
            part :>> p1 {
                doc /* FIX: refer to the inherited feature by the name it has along the renaming path. */
            }
        }
    }

    package V5_Typo {
        doc /* A misspelled name silently declares a new feature. */
        part def Vehicle { attribute mass : Real; }
        part myCarPitfall : Vehicle {
            attribute masss = 1500.0 {
                doc /* PITFALL: typo; a new feature masss, and mass stays without a value. Pilot: no diagnostic. */
            }
        }
        part myCarFixed : Vehicle {
            attribute :>> mass = 1500.0 {
                doc /* FIX: with ':>>' a typo becomes an unresolved-name error instead of a silent new feature. */
            }
        }
    }

    package V6_NameResolution {
        doc /* A name resolves to a different element than intended. */
        package OwnedHidesImported {
            private import ISQ::*;
            part def VehiclePitfall {
                attribute mass :> mass {
                    doc /* PITFALL: ':> mass' resolves to this feature itself, not to ISQ::mass; it subsets
                         * itself. Pilot: no diagnostic. */
                }
            }
            part def VehicleFixed {
                attribute mass :> ISQ::mass {
                    doc /* FIX: qualify the library name. */
                }
            }
        }
        package RedefinitionFallsThroughToImport {
            private import ISQ::*;
            private import SI::*;
            part def Vehicle { attribute totalMass :> ISQ::mass; }
            part def CarPitfall :> Vehicle {
                attribute :>> mass = 1500 [kg] {
                    doc /* PITFALL: Vehicle has no feature named mass, so ':>> mass' redefines the imported
                         * library quantity ISQ::mass; totalMass stays without a value. Pilot: no diagnostic. */
                }
            }
            part def CarFixed :> Vehicle {
                attribute :>> totalMass = 1500 [kg];
            }
        }
    }

    package V7_ParametersRedefineByPosition {
        doc /* Parameters redefine the general's parameters by position, not by name. */
        item def Account;
        action def Transfer {
            in source : Account;
            in target : Account;
        }
        action def AuditedTransferPitfall :> Transfer {
            doc /* PITFALL: target is first, so it redefines Transfer::source, and source redefines
                 * Transfer::target: the two are silently swapped. Pilot: no diagnostic. */
            in target : Account;
            in source : Account;
        }
        action def AuditedTransferFixed :> Transfer {
            in source : Account;
            in target : Account;
        }
        action def RefundPitfall :> Transfer {
            doc /* PITFALL: the explicit ':>> target' is at position 1, so target redefines both
                 * Transfer::target and Transfer::source, equating them. Pilot: no diagnostic. */
            in target :>> target;
        }
        action def RefundFixed :> Transfer {
            in source :>> source;
            in target :>> target;
        }
    }

    package V9_SpecificationExample {
        doc /* SysML v2.1 Beta 2, Part 1, 7.13.4, pages 72-73, as published (pitfall) and corrected (fixed). */
        private import NumericalFunctions::*;
        private import SequenceFunctions::*;
        item def TestRecordAsPublished {
            attribute scores[1..*] : Integer;
            derived attribute averageScore[1] : Rational = sum(scores)/size(scores);
            attribute cutoff : Integer default = 0.75 * averageScore;
        }
        item def TestWithCutoffPitfall :> TestRecordAsPublished {
            attribute cutoff : Rational default = 0.75 * averageScore {
                doc /* PITFALL: a second cutoff. Pilot: warning "Duplicate of inherited member name 'cutoff'
                     * from TestRecordAsPublished". */
            }
        }
        item def TestRecord {
            attribute scores[1..*] : Integer;
            derived attribute averageScore[1] : Rational = sum(scores)/size(scores);
            attribute cutoff : Rational;
        }
        item def TestWithCutoffFixed :> TestRecord {
            attribute :>> cutoff default = 0.75 * averageScore {
                doc /* FIX: redefine cutoff (typed Rational in the general, which the expression produces). */
            }
        }
    }

    package V10_Aliases {
        doc /* An alias gives an existing feature another name without creating a new feature, so it is the
             * way to add a name to an inherited feature. But an alias carries no value, type or multiplicity,
             * it competes with inherited names, and redefining a feature drops the aliases that the general
             * declares for it. */
        package AliasOrRename {
            part def Vehicle { attribute mass : Real; }
            part def CarPitfall :> Vehicle {
                attribute curbMass :>> mass {
                    doc /* PITFALL: meant as a second name for mass, but a named redefinition renames it: 'mass'
                         * no longer resolves in CarPitfall or its specializations. Pilot: an error only where
                         * 'mass' is used ("Couldn't resolve reference"). */
                }
            }
            part def CarFixed :> Vehicle {
                alias curbMass for mass {
                    doc /* FIX: an alias adds the name; 'mass' and 'curbMass' both mean Vehicle::mass. */
                }
            }
        }
        package AliasHidesInheritedName {
            part def Vehicle { attribute mass : Real; }
            part def CarPitfall :> Vehicle {
                alias mass for ISQ::mass {
                    doc /* PITFALL: owned members are found first, so 'mass' here and in specializations means
                         * ISQ::mass, and ':>> mass' in a specialization redefines the library quantity.
                         * Pilot: warning "Duplicate of inherited member name 'mass' from Vehicle". */
                }
            }
            part def CarFixed :> Vehicle {
                alias isqMass for ISQ::mass {
                    doc /* FIX: an alias name that no inherited member has. */
                }
            }
        }
        package FeatureNamedLikeInheritedAlias {
            part def Vehicle { attribute mass : Real; alias weight for mass; }
            part myCarPitfall : Vehicle {
                attribute weight = 1500.0 {
                    doc /* PITFALL: a new feature, not Vehicle::mass under its alias. Pilot: warning
                         * "Duplicate of inherited member name 'weight' from Vehicle". */
                }
            }
            part myCarFixed : Vehicle {
                attribute :>> mass = 1500.0;
                alias weight for mass {
                    doc /* FIX: redefine the feature, and declare the alias again (see RedefinitionDropsAlias). */
                }
            }
        }
        package RedefinitionDropsAlias {
            part def Vehicle { attribute mass : Real; alias weight for mass; }
            part myCarPitfall : Vehicle {
                attribute :>> weight = 1500.0 {
                    doc /* PITFALL: this redefines Vehicle::mass through the alias, and the redefining feature is
                         * named 'mass'. A redefinition drops every inherited membership of the feature it
                         * redefines, aliases included, so 'weight' no longer resolves in myCarPitfall. Pilot: an
                         * error only where 'weight' is used ("Couldn't resolve reference"). */
                }
            }
            part myCarFixed : Vehicle {
                attribute :>> mass = 1500.0;
                alias weight for mass {
                    doc /* FIX: declare the alias again after the redefinition; it now names myCarFixed::mass. */
                }
                attribute reportedWeight = weight;
            }
        }
        package AliasFromTwoGenerals {
            part def Vehicle { attribute mass : Real; }
            part def Metric :> Vehicle { alias weight for mass; }
            part def Scale { attribute weight : Real; }
            part def WeighedCarPitfall :> Metric, Scale {
                doc /* PITFALL: inherits the alias weight (for Vehicle::mass) and the feature Scale::weight.
                     * 'weight' resolves to the one from the general listed first, here Vehicle::mass. Pilot:
                     * warning "Duplicate of inherited member name 'weight' from Metric, Scale". */
            }
            part def WeighedCarFixed :> Metric, Scale {
                attribute scaleWeight :>> Scale::weight {
                    doc /* FIX: rename one of them in the specialization. */
                }
            }
        }
    }

    package V11_QuantityTypedMass {
        doc /* The car's mass as a mass quantity (ISQ::MassValue, values in kg). The pitfalls are those of V1
             * and V2. Typing the attribute by 'mass' itself, by ISQ::mass or by kg is an error; see
             * MassTypedByMass.sysml. */
        private import ISQ::*;
        private import SI::*;
        part def Vehicle {
            attribute mass :> ISQ::mass {
                doc /* Subsets the library quantity ISQ::mass, so it is typed MassValue. */
            }
        }
        part def CarPitfall :> Vehicle {
            attribute mass : MassValue {
                doc /* PITFALL: a second mass beside Vehicle::mass. Pilot: warning "Duplicate of inherited member
                     * name 'mass' from Vehicle". */
            }
        }
        part def CarFixed :> Vehicle {
            attribute :>> mass : MassValue;
        }
        part myCarPitfall : Vehicle {
            attribute mass = 1500 [kg] {
                doc /* PITFALL: a new attribute; Vehicle::mass stays without a value. Pilot: warning "Duplicate of
                     * inherited member name 'mass' from Vehicle". */
            }
        }
        part myCarFixed : Vehicle {
            attribute :>> mass = 1500 [kg];
        }
    }
}
