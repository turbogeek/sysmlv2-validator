package com.validator.semantic.index;

import com.validator.ErrorCodes;
import com.validator.SysMLv2ValidatorImpl;
import com.validator.ValidationError;
import com.validator.ValidationResult;
import com.validator.ValidationWarning;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.testutil.TestPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link NameCollisionChecker}: names that make a new element where an existing one was likely
 * meant. Where the reference implementation (SysML v2 Pilot 0.63.0) reports the same case, the test says so;
 * docs/NAME_COLLISION_ANALYSIS.md has the evidence.
 */
@DisplayName("Name collision checker")
class NameCollisionCheckerTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    private static final String VEHICLE = """
        package P {
            attribute def Real;
            part def Vehicle { attribute mass : Real; }
        """;

    // ------------------------------------------------------------------------------------------
    // A new feature with an inherited name

    @Test
    @DisplayName("Re-declaring an inherited feature in a definition is reported, with the redefinition as fix")
    void redeclaredInDefinition() {
        List<ValidationError> found = check(VEHICLE + """
                part def Car :> Vehicle { attribute mass : Real; }
            }
            """);
        ValidationError w = only(found, ErrorCodes.INHERITED_NAME_REDECLARED);
        assertInstanceOf(ValidationWarning.class, w);
        assertEquals(4, w.getLine());
        assertEquals(40, w.getColumn(), "at the name");
        assertTrue(w.getMessage().contains("same name as the inherited 'P::Vehicle::mass'"), w.getMessage());
        assertTrue(w.getMessage().contains("'P::Car' has two features named 'mass'"), w.getMessage());
        assertEquals(List.of("attribute :>> mass : Real;"), w.getSuggestions());
    }

    @Test
    @DisplayName("A value on a re-declared feature goes to the new feature; both declaration forms are reported")
    void redeclaredOnUsage() {
        List<ValidationError> found = check(VEHICLE + """
                part myCar : Vehicle { attribute mass = 1500.0; }
                part yourCar : Vehicle { mass = 1500.0; }
            }
            """);
        assertEquals(2, found.size(), messages(found));
        for (ValidationError w : found) {
            assertEquals(ErrorCodes.INHERITED_NAME_REDECLARED, w.getErrorCode());
            assertTrue(w.getMessage().contains("The value '= 1500.0' goes to the new feature; 'P::Vehicle::mass' keeps"
                + " no value"), w.getMessage());
        }
    }

    @Test
    @DisplayName("Redefinitions of the inherited feature are not reported")
    void redefinitionsAreNotReported() {
        for (String body : List.of("attribute :>> mass : Real;", "attribute mass redefines mass;",
                "attribute mass :>> Vehicle::mass = 5;", "attribute redefines mass = 5;")) {
            assertEquals(List.of(), check(VEHICLE + "part def Car :> Vehicle { " + body + " } }"), body);
        }
    }

    @Test
    @DisplayName("Subsetting the inherited feature under its own name is reported with a ':>>' hint")
    void subsettingIsNotRedefinition() {
        ValidationError w = only(check(VEHICLE + """
                part def Car :> Vehicle { attribute mass :> Vehicle::mass; }
            }
            """), ErrorCodes.INHERITED_NAME_REDECLARED);
        assertTrue(w.getMessage().contains("use ':>>' (redefines), not ':>'"), w.getMessage());
    }

    @Test
    @DisplayName("An owned feature that redefines what the inherited one redefines replaces it (no warning)")
    void replacedThroughACommonRedefinition() {
        // Packets.sysml in the OMG examples: both redefine the package-level feature
        assertEquals(List.of(), check("""
            package P {
                attribute field { }
                part def Data { attribute redefines field; }
                part def Thermal :> Data { attribute field redefines P::field; }
            }
            """));
    }

    @Test
    @DisplayName("Features that refer rather than declare are not re-declarations: events, end cross features")
    void referencesAreNotRedeclarations() {
        assertEquals(List.of(), check("""
            package P {
                occurrence def Msg;
                part def Producer { event occurrence sent : Msg; }
                part p : Producer { event sent; }
                part def Cart;
                connection def Selection { end item cart : Cart[1]; }
                connection def OneSelection :> Selection { end inCart1 [0..1] item cart : Cart[1]; }
            }
            """));
    }

    // ------------------------------------------------------------------------------------------
    // Duplicates in one scope

    @Test
    @DisplayName("Two members with one name in one namespace are reported at both declarations")
    void duplicateMembers() {
        List<ValidationError> found = check("""
            package P {
                attribute def Real;
                part def Vehicle {
                    attribute mass : Real;
                    attribute mass : Real;
                }
            }
            """);
        assertEquals(2, found.size(), messages(found));
        Set<Integer> lines = new TreeSet<>();
        for (ValidationError e : found) {
            assertEquals(ErrorCodes.DUPLICATE_MEMBER_NAME, e.getErrorCode());
            lines.add(e.getLine());
        }
        assertEquals(Set.of(4, 5), lines);
        assertTrue(found.get(0).getMessage().contains("declared 2 times in 'P::Vehicle'"), found.get(0).getMessage());
    }

    @Test
    @DisplayName("A definition and a usage with one name are reported, noting that the specification allows it")
    void definitionAndUsageWithOneName() {
        List<ValidationError> found = check("""
            package P {
                part def Truck {
                    attribute def load;
                    attribute load;
                }
            }
            """);
        assertEquals(2, found.size(), messages(found));
        assertTrue(found.get(0).getMessage()
            .contains("The specification allows a definition and a usage to share a name"), found.get(0).getMessage());
    }

    @Test
    @DisplayName("Reference forms are not duplicate declarations (satisfy, metadata, dependency, then join, lambdas)")
    void referenceFormsAreNotDuplicates() {
        assertEquals(List.of(), check("""
            package P {
                metadata def Safety;
                metadata Safety about P;
                requirement def R;
                requirement r : R;
                part p;
                satisfy r by p;
                attribute z;
                dependency z to p;
                action def Flow {
                    action initialize;
                    action finalize;
                    first initialize;
                    then join finalize;
                }
                state def S { entry action initial; state off; transition initial then off; }
                attribute v;
                attribute both = v->forAll {in ref w; true} and v->forAll {in ref w; true};
            }
            """));
    }

    // ------------------------------------------------------------------------------------------
    // Renamed inherited features and typos

    @Test
    @DisplayName("A new feature named like an ancestor's feature that is inherited renamed is reported")
    void renamedInheritedNameReused() {
        // The reference implementation accepts this model without a diagnostic
        ValidationError w = only(check("""
            package P {
                part def A { part p[*]; }
                part def A1 :> A { part p1 :>> p; }
                part def A2 :> A;
                part def B :> A1, A2 { part p; }
            }
            """), ErrorCodes.RENAMED_INHERITED_NAME_REUSED);
        assertTrue(w.getMessage().contains("inherits renamed as 'p1'"), w.getMessage());
        assertTrue(w.getMessage().contains("use ':>> p1'"), w.getMessage());
    }

    @Test
    @DisplayName("A new feature one edit away from an inherited name is reported as a likely typo")
    void typoOfInheritedName() {
        ValidationError w = only(check(VEHICLE + """
                part myCar : Vehicle { attribute masss = 1500.0; }
            }
            """), ErrorCodes.NAME_SIMILAR_TO_INHERITED);
        assertTrue(w.getMessage().contains("'P::Vehicle::mass', whose name differs by one letter"), w.getMessage());
        assertEquals(List.of("attribute :>> mass = 1500.0;"), w.getSuggestions());

        ValidationError r = only(check(VEHICLE + """
                part myCar : Vehicle { attribute :>> masss = 1500.0; }
            }
            """), ErrorCodes.NAME_SIMILAR_TO_INHERITED);
        assertTrue(r.getMessage().contains("Did you mean ':>> mass'"), r.getMessage());
    }

    @Test
    @DisplayName("Deliberate near names are not typos: numbered names, short names, a name that subsets the other")
    void deliberateNearNamesAreNotTypos() {
        assertEquals(List.of(), check("""
            package P {
                part def Board { port port1; attribute id; attribute fractions[*]; }
                part b : Board { port port2; attribute ids; attribute fraction1 :> fractions; }
            }
            """));
        assertTrue(NameCollisionChecker.oneEditApart("mass", "masss"));
        assertTrue(NameCollisionChecker.oneEditApart("mass", "msas"));
        assertTrue(NameCollisionChecker.oneEditApart("mass", "Mass"));
        assertFalse(NameCollisionChecker.oneEditApart("mass", "moss2"));
    }

    // ------------------------------------------------------------------------------------------
    // Name resolution

    @Test
    @DisplayName("A subsetting by the feature's own name refers to itself")
    void selfReference() {
        ValidationError w = only(check("""
            package P {
                part def Vehicle { attribute mass :> mass; }
            }
            """), ErrorCodes.FEATURE_REFERS_TO_ITSELF);
        assertTrue(w.getMessage().contains("finds the feature being declared"), w.getMessage());
    }

    @Test
    @DisplayName("':>> name' that falls through to an imported library feature is reported")
    void redefinitionFallsThroughToLibrary() {
        ModelIndex library = new ModelIndex();
        library.addAll(index("package ISQ { attribute mass; attribute length; }", "lib.sysml").getElements());

        ValidationError w = only(check(new ModelIndex(library), """
            package P {
                private import ISQ::*;
                part def Vehicle { attribute totalMass :> ISQ::mass; }
                part def Car :> Vehicle { attribute :>> mass = 1500; }
            }
            """), ErrorCodes.REDEFINITION_NOT_INHERITED);
        assertTrue(w.getMessage().contains("to the library feature 'ISQ::mass'"), w.getMessage());

        // A package-level feature of the user's own model is redefined on purpose (variant patterns)
        assertEquals(List.of(), check(new ModelIndex(library), """
            package P {
                part part1;
                part def Sub;
                part s : Sub { part :>> part1; }
            }
            """));
        // A library general may inherit what the index does not model: not reported
        assertEquals(List.of(), check(new ModelIndex(library), """
            package P {
                private import ISQ::*;
                part def Shape;
                part def Wheel { part shape : ISQ::Cylinder { attribute :>> length = 2; } }
            }
            """));
    }

    @Test
    @DisplayName("A usage typed by itself or by a library usage is an error that names the definition to use")
    void typedByUsage() {
        ModelIndex library = new ModelIndex();
        library.addAll(index("""
            package ISQ { attribute def MassValue; attribute mass : MassValue; }
            package MeasurementReferences { attribute def MeasurementUnit; }
            package SI {
                attribute def MassUnit :> MeasurementReferences::MeasurementUnit;
                attribute <kg> kilogram : MassUnit;
            }
            """, "lib.sysml").getElements());
        List<ValidationError> found = check(new ModelIndex(library), """
            package P {
                private import ISQ::*;
                private import SI::*;
                part def Vehicle { attribute mass : MassValue; }
                part def CarTypedByItself :> Vehicle { attribute mass : mass; }
                part def CarTypedByLibraryUsage { attribute mass : ISQ::mass; }
                part def CarTypedByUnit { attribute mass : kg; }
                part def CarFixed :> Vehicle { attribute :>> mass : MassValue; }
                part def CarFixedBySubsetting :> Vehicle { attribute :>> mass :> ISQ::mass; }
                part engine;
                part def Truck { part e : engine; ref r : engine; }
                part def EngineControlUnit;
                part ecu : EngineControlUnit;
                part def Car { part controller : ecu; }
            }
            """);
        assertEquals(List.of(5, 5, 6, 7, 11, 11, 14), found.stream().map(ValidationError::getLine).toList(),
            messages(found));
        // The self-typed 'mass' is also a second mass beside Vehicle::mass, as the reference implementation says
        ValidationError redeclared = found.get(1);
        assertEquals(ErrorCodes.INHERITED_NAME_REDECLARED, redeclared.getErrorCode(), messages(found));
        assertEquals(List.of("attribute :>> mass : MassValue;"), redeclared.getSuggestions());
        found.remove(1);
        for (ValidationError e : found) {
            assertEquals(ErrorCodes.SEMANTIC_TYPED_BY_USAGE, e.getErrorCode(), messages(found));
            assertEquals(ValidationError.Severity.ERROR, e.getSeverity());
        }
        String self = found.get(0).getMessage();
        assertTrue(self.contains("'mass' is typed by itself"), self);
        assertTrue(self.contains("Type it by a definition (': MassValue', the type of 'ISQ::mass')"), self);
        assertTrue(found.get(1).getMessage().contains("typed by 'ISQ::mass', which is an attribute usage."),
            found.get(1).getMessage());
        assertTrue(found.get(1).getMessage().contains("subset the feature (':> ISQ::mass')"),
            found.get(1).getMessage());
        assertTrue(found.get(2).getMessage().contains("'SI::kilogram' is a unit: type the feature by its quantity"
            + " (': MassValue')"), found.get(2).getMessage());
        assertTrue(found.get(3).getMessage().contains("which is a part usage"), found.get(3).getMessage());
        // A part definition named '...Unit' is not a measurement unit
        assertFalse(found.get(5).getMessage().contains("is a unit"), found.get(5).getMessage());

        // KerML allows a feature to type a feature
        ModelIndexBuilder.Result kerml = index("package K { feature f; feature g : f; }", "test.kerml");
        ModelIndex index = new ModelIndex();
        index.addAll(kerml.getElements());
        assertEquals(List.of(), NameCollisionChecker.check(kerml.getElements(), index, "test.kerml"));
    }

    // ------------------------------------------------------------------------------------------
    // Aliases

    @Test
    @DisplayName("An alias of an inherited feature, under its own name or another, is not reported")
    void aliasesOfTheInheritedFeature() {
        assertEquals(List.of(), check(VEHICLE + """
                part def CarSameName :> Vehicle { alias mass for Vehicle::mass; }
                part def CarNewName :> Vehicle { alias weight for mass; }
                part myCar : CarNewName { attribute w = weight; }
                part def CarRenamed :> Vehicle { attribute weight :>> mass; alias mass for weight; }
                part def CarSelfNamed :> Vehicle { alias mass for mass; }
                part def SportsCar :> CarSelfNamed { attribute mass redefines mass = 1.0; }
            }
            """));
    }

    @Test
    @DisplayName("An alias that takes an inherited feature's name is reported, and so is ':>> name' through it")
    void aliasHidesInheritedName() {
        ModelIndex library = new ModelIndex();
        library.addAll(index("package ISQ { attribute mass; }", "lib.sysml").getElements());
        List<ValidationError> found = check(new ModelIndex(library), VEHICLE + """
                part def Car :> Vehicle { alias mass for ISQ::mass; }
                part def SportsCar :> Car { attribute :>> mass; }
                part def CarFixed :> Vehicle { alias isqMass for ISQ::mass; }
            }
            """);
        assertEquals(2, found.size(), messages(found));
        assertEquals(ErrorCodes.ALIAS_NAME_COLLISION, found.get(0).getErrorCode());
        assertEquals(4, found.get(0).getLine());
        assertTrue(found.get(0).getMessage().contains("Alias 'mass' in 'P::Car' stands for 'ISQ::mass', but 'P::Car'"
            + " also inherits 'P::Vehicle::mass'"), found.get(0).getMessage());
        assertEquals(ErrorCodes.ALIAS_NAME_COLLISION, found.get(1).getErrorCode());
        assertEquals(5, found.get(1).getLine());
        assertTrue(found.get(1).getMessage().contains("':>> mass' in 'P::SportsCar' redefines 'ISQ::mass', because"
            + " 'mass' is the alias 'P::Car::mass' for it"), found.get(1).getMessage());
        assertTrue(found.get(1).getMessage().contains("write ':>> Vehicle::mass'"), found.get(1).getMessage());

        // A private alias is not seen by specializations: their ':>> mass' still redefines Vehicle::mass
        found = check(new ModelIndex(library), VEHICLE + """
                part def Car :> Vehicle { private alias mass for ISQ::mass; }
                part def SportsCar :> Car { attribute mass redefines mass = 1500.0; }
            }
            """);
        ValidationError w = only(found, ErrorCodes.ALIAS_NAME_COLLISION);
        assertTrue(w.getMessage().contains("so 'mass' in 'P::Car' now means 'ISQ::mass'. Give"), w.getMessage());
    }

    @Test
    @DisplayName("A new feature with the name or short name of an inherited alias is a re-declaration")
    void featureNamedLikeInheritedAlias() {
        List<ValidationError> found = check("""
            package P {
                attribute def Real;
                part def Vehicle { attribute mass : Real; alias weight for mass; }
                part def Car :> Vehicle { attribute weight : Real; }
                part def CarShortName :> Vehicle { attribute <weight> w : Real; }
                part myCar : Vehicle { attribute weight = 1500.0; }
            }
            """);
        assertEquals(List.of(4, 5, 6), found.stream().map(ValidationError::getLine).toList(), messages(found));
        for (ValidationError w : found) {
            assertEquals(ErrorCodes.INHERITED_NAME_REDECLARED, w.getErrorCode());
            assertTrue(w.getMessage().contains("with the name of the alias 'P::Vehicle::weight', which"),
                w.getMessage());
            assertTrue(w.getMessage().contains("also declare 'alias weight for mass;'"), w.getMessage());
        }
        assertEquals(List.of("attribute :>> mass : Real;"), found.get(0).getSuggestions());
        assertTrue(found.get(2).getMessage().contains("The value '= 1500.0' goes to the new feature, not to"
            + " 'P::Vehicle::mass'"), found.get(2).getMessage());
    }

    @Test
    @DisplayName("A redefinition that drops an inherited alias is reported, unless the alias is declared again")
    void redefinitionDropsAlias() {
        List<ValidationError> found = check("""
            package P {
                attribute def Real;
                part def Vehicle { attribute mass : Real; alias weight for mass; }
                part def CarViaName :> Vehicle { attribute :>> mass = 1500.0; }
                part def CarViaAlias :> Vehicle { attribute :>> weight = 1500.0; }
                part c3 : Vehicle { attribute :>> weight = 1500.0; attribute w = weight; }
                part def CarFixed :> Vehicle { attribute :>> mass : Real; alias weight for mass; }
                part def CarRenamedToAlias :> Vehicle { attribute weight :>> mass; }
                part c4 : CarFixed { attribute w = weight; }
            }
            """);
        assertEquals(List.of(4, 5, 6), found.stream().map(ValidationError::getLine).toList(), messages(found));
        for (ValidationError w : found) {
            assertEquals(ErrorCodes.INHERITED_ALIAS_DROPPED, w.getErrorCode());
            assertTrue(w.getMessage().contains("no longer has 'weight', the alias that 'P::Vehicle' declares for it"),
                w.getMessage());
            assertTrue(w.getMessage().contains("the redefining feature is named 'mass'"), w.getMessage());
            assertEquals(List.of("alias weight for mass;"), w.getSuggestions());
        }
        assertTrue(found.get(0).getMessage().startsWith("':>> mass' redefines 'P::Vehicle::mass'"),
            found.get(0).getMessage());
        assertTrue(found.get(1).getMessage().startsWith("':>> weight' redefines 'P::Vehicle::mass'"),
            found.get(1).getMessage());

        // The alias declared again names the unnamed redefinition, so it is dropped again by a further
        // redefinition and still collides with a new feature of its name
        found = check("""
            package P {
                attribute def Real;
                part def Vehicle { attribute mass : Real; alias weight for mass; }
                part def CarFixed :> Vehicle { attribute :>> mass : Real; alias weight for mass; }
                part def SportsCar :> CarFixed { attribute :>> mass = 1500.0; }
                part def Truck :> CarFixed { attribute weight : Real; }
            }
            """);
        assertEquals(List.of("5 " + ErrorCodes.INHERITED_ALIAS_DROPPED, "6 " + ErrorCodes.INHERITED_NAME_REDECLARED),
            found.stream().map(e -> e.getLine() + " " + e.getErrorCode()).toList(), messages(found));

        // Another general still supplies the name: it now means that member, and no alias is suggested
        found = check("""
            package P {
                attribute def Real;
                part def Vehicle { attribute mass : Real; alias weight for mass; }
                part def Scale { attribute weight : Real; }
                part def Car :> Vehicle, Scale { attribute :>> mass = 1500.0; }
            }
            """);
        ValidationError w = only(found, ErrorCodes.INHERITED_ALIAS_DROPPED);
        assertTrue(w.getMessage().endsWith("'weight' in 'P::Car' now means the feature 'P::Scale::weight'."),
            w.getMessage());
        assertEquals(List.of(), w.getSuggestions());
    }

    @Test
    @DisplayName("An alias and a different feature with one name from two generals are reported, with the winner")
    void aliasFromTwoGenerals() {
        List<ValidationError> found = check("""
            package P {
                attribute def Real;
                part def Vehicle { attribute mass : Real; }
                part def Metric :> Vehicle { alias weight for mass; }
                part def Imperial :> Vehicle { alias weight for mass; }
                part def Scale { attribute weight : Real; }
                part def BothAliases :> Metric, Imperial;
                part def MetricFirst :> Metric, Scale;
                part def ScaleFirst :> Scale, Metric;
                part def Fixed :> Metric, Scale { attribute scaleWeight :>> Scale::weight; }
            }
            """);
        assertEquals(List.of(8, 9), found.stream().map(ValidationError::getLine).toList(), messages(found));
        for (ValidationError w : found) {
            assertEquals(ErrorCodes.ALIAS_NAME_COLLISION, w.getErrorCode());
            assertTrue(w.getMessage().contains("inherits two different members named 'weight': the alias"
                + " 'P::Metric::weight' for 'P::Vehicle::mass' (from 'P::Metric') and the feature 'P::Scale::weight'"
                + " (from 'P::Scale')"), w.getMessage());
        }
        assertTrue(found.get(0).getMessage().contains("listed first, 'P::Metric'"), found.get(0).getMessage());
        assertTrue(found.get(1).getMessage().contains("listed first, 'P::Scale'"), found.get(1).getMessage());
    }

    // ------------------------------------------------------------------------------------------
    // Parameters

    @Test
    @DisplayName("Parameters declared in another order than the general's are reported")
    void parameterPositionMismatch() {
        String transfer = """
            package P {
                item def Account;
                action def Transfer { in source : Account; in target : Account; }
            """;
        List<ValidationError> found = check(transfer + """
                action def Audited :> Transfer { in target : Account; in source : Account; }
            }
            """);
        assertEquals(2, found.size(), messages(found));
        for (ValidationError e : found) {
            assertEquals(ErrorCodes.PARAMETER_POSITION_MISMATCH, e.getErrorCode());
        }
        assertTrue(found.get(0).getMessage().contains("redefines 'P::Transfer::source' (parameter 1 of 'P::Transfer')"),
            found.get(0).getMessage());

        // An explicit redefinition at another position also redefines the parameter there
        ValidationError two = only(check(transfer + """
                action def Refund :> Transfer { in target :>> target; }
            }
            """), ErrorCodes.PARAMETER_REDEFINES_TWO);
        assertTrue(two.getMessage().contains("also redefines 'P::Transfer::source'"), two.getMessage());

        assertEquals(List.of(), check(transfer + """
                action def Same :> Transfer { in source : Account; in target : Account; }
                action def Both :> Transfer { in source :>> source; in target :>> target; }
            }
            """));
    }

    @Test
    @DisplayName("Unnamed parameters count for positions")
    void unnamedParametersCount() {
        // 'Action Decomposition.sysml' in the OMG training: 'in item;' is parameter 1
        assertEquals(List.of(), check("""
            package P {
                item def Image;
                item def Picture;
                action def Shoot { in image : Image; out picture : Picture; }
                action shoot : Shoot { in item; out item picture; }
            }
            """));
    }

    // ------------------------------------------------------------------------------------------
    // Bound values

    @Test
    @DisplayName("Overriding a bound or initial value is an error; overriding a default is not")
    void boundValueOverridden() {
        String model = """
            package P {
                attribute def Real;
                part def Vehicle {
                    attribute mass : Real = 1000.0;
                    attribute m2 : Real := 5.0;
                    attribute m3 : Real default 5.0;
                }
                part def Car :> Vehicle { attribute :>> mass; }
            """;
        List<ValidationError> found = check(model + """
                part c : Car { attribute :>> mass = 1500.0; }
                part v : Vehicle { attribute :>> m2 = 6.0; attribute :>> m3 = 7.0; }
            }
            """);
        assertEquals(2, found.size(), messages(found));
        for (ValidationError e : found) {
            assertEquals(ErrorCodes.SEMANTIC_BOUND_VALUE_OVERRIDDEN, e.getErrorCode());
            assertEquals(ValidationError.Severity.ERROR, e.getSeverity());
        }
        assertTrue(found.get(0).getMessage().contains("whose value is bound ('= 1000.0')"), found.get(0).getMessage());
        assertTrue(found.get(1).getMessage().contains("whose value is initial (':= 5.0')"), found.get(1).getMessage());
    }

    // ------------------------------------------------------------------------------------------
    // Acceptance: the example model for Cameo

    @Test
    @DisplayName("The Cameo example: every pitfall is reported, no fixed form is")
    void cameoExample() throws IOException {
        assumeTrue(TestPaths.sysmlReleaseRoot().isPresent(), "SysML-v2-Release not available");
        File dir = TestPaths.repositoryRoot().resolve("examples/name-collision").toFile();
        List<ValidationResult> results = new SysMLv2ValidatorImpl().validateAll(List.of(
            new File(dir, "NameCollisionExamples.sysml"), new File(dir, "NameCollisionErrorForm.sysml"),
            new File(dir, "MassTypedByMass.sysml")));

        assertEquals(Set.of("77 MULTIPLE_INHERITANCE_NAME_CONFLICT", "14 INHERITED_NAME_REDECLARED",
                "30 INHERITED_NAME_REDECLARED", "36 INHERITED_NAME_REDECLARED", "52 DUPLICATE_MEMBER_NAME",
                "53 DUPLICATE_MEMBER_NAME", "62 DUPLICATE_MEMBER_NAME", "63 DUPLICATE_MEMBER_NAME",
                "94 RENAMED_INHERITED_NAME_REUSED", "110 NAME_SIMILAR_TO_INHERITED", "126 FEATURE_REFERS_TO_ITSELF",
                "142 REDEFINITION_NOT_INHERITED", "163 PARAMETER_POSITION_MISMATCH", "164 PARAMETER_POSITION_MISMATCH",
                "173 PARAMETER_REDEFINES_TWO", "191 INHERITED_NAME_REDECLARED",
                // V10 aliases and V11 the quantity-typed mass (the pilot is silent on 261, and on the renaming at 216)
                "231 ALIAS_NAME_COLLISION", "246 INHERITED_NAME_REDECLARED", "261 INHERITED_ALIAS_DROPPED",
                "280 ALIAS_NAME_COLLISION", "305 INHERITED_NAME_REDECLARED", "314 INHERITED_NAME_REDECLARED"),
            diagnostics(results.get(0)));
        assertEquals(Set.of("11 SEMANTIC_BOUND_VALUE_OVERRIDDEN", "16 INHERITED_NAME_REDECLARED"),
            diagnostics(results.get(1)));
        // As in the reference implementation: an error and the duplicate warning on each pitfall
        assertEquals(Set.of("13 SEMANTIC_TYPED_BY_USAGE", "21 SEMANTIC_TYPED_BY_USAGE", "27 SEMANTIC_TYPED_BY_USAGE",
                "13 INHERITED_NAME_REDECLARED", "21 INHERITED_NAME_REDECLARED", "27 INHERITED_NAME_REDECLARED"),
            diagnostics(results.get(2)));
    }

    // ------------------------------------------------------------------------------------------

    private static Set<String> diagnostics(ValidationResult result) {
        Set<String> found = new TreeSet<>();
        List<ValidationError> all = new ArrayList<>(result.getErrors());
        all.addAll(result.getWarnings());
        for (ValidationError e : all) {
            if (e.getErrorCode() != null && !"LINE_COMMENT_NOT_PERSISTED".equals(e.getErrorCode())) {
                found.add(e.getLine() + " " + e.getErrorCode());
            }
        }
        return found;
    }

    private static List<ValidationError> check(String source) {
        return check(new ModelIndex(), source);
    }

    private static List<ValidationError> check(ModelIndex index, String source) {
        ModelIndexBuilder.Result built = index(source, "test.sysml");
        index.addAll(built.getElements());
        return NameCollisionChecker.check(built.getElements(), index, "test.sysml");
    }

    private static ModelIndexBuilder.Result index(String source, String fileName) {
        SysMLv2ParserFacade.ParseResult parsed = PARSER.parseString(source, fileName);
        List<String> errors = new ArrayList<>();
        for (SysMLv2ParserFacade.SyntaxError e : parsed.getSyntaxErrors()) {
            errors.add(e.getLine() + ":" + e.getCharPositionInLine() + " " + e.getMessage());
        }
        assertEquals(List.of(), errors, "the test model must parse");
        return ModelIndexBuilder.build(parsed.getParseTree(), fileName, true);
    }

    private static ValidationError only(List<ValidationError> found, String code) {
        assertEquals(1, found.size(), messages(found));
        assertEquals(code, found.get(0).getErrorCode(), messages(found));
        return found.get(0);
    }

    private static String messages(List<ValidationError> found) {
        StringBuilder sb = new StringBuilder();
        for (ValidationError e : found) {
            sb.append('\n').append(e.getErrorCode()).append(' ').append(e.getLine()).append(':')
                .append(e.getColumn()).append(' ').append(e.getMessage());
        }
        return sb.toString();
    }
}
