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
            new File(dir, "NameCollisionExamples.sysml"), new File(dir, "NameCollisionErrorForm.sysml")));

        assertEquals(Set.of("77 MULTIPLE_INHERITANCE_NAME_CONFLICT", "14 INHERITED_NAME_REDECLARED",
                "30 INHERITED_NAME_REDECLARED", "36 INHERITED_NAME_REDECLARED", "52 DUPLICATE_MEMBER_NAME",
                "53 DUPLICATE_MEMBER_NAME", "62 DUPLICATE_MEMBER_NAME", "63 DUPLICATE_MEMBER_NAME",
                "94 RENAMED_INHERITED_NAME_REUSED", "110 NAME_SIMILAR_TO_INHERITED", "126 FEATURE_REFERS_TO_ITSELF",
                "142 REDEFINITION_NOT_INHERITED", "163 PARAMETER_POSITION_MISMATCH", "164 PARAMETER_POSITION_MISMATCH",
                "173 PARAMETER_REDEFINES_TWO", "191 INHERITED_NAME_REDECLARED"),
            diagnostics(results.get(0)));
        assertEquals(Set.of("11 SEMANTIC_BOUND_VALUE_OVERRIDDEN", "16 INHERITED_NAME_REDECLARED"),
            diagnostics(results.get(1)));
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
