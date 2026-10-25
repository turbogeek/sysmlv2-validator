package com.validator.semantic.index;

import com.validator.ErrorCodes;
import com.validator.ValidationWarning;
import com.validator.parser.SysMLv2ParserFacade;
import com.validator.testutil.TestPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link InheritanceChecker}: diamonds whose branches redefine a common feature differently, and
 * generals that contribute different features with the same name.
 */
@DisplayName("Inheritance checker (diamonds and name conflicts)")
class InheritanceCheckerTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    /** A diamond whose branches redefine A::f with different values. */
    private static final String DIAMOND = """
        package P {
            attribute def Real;
            part def A { attribute f : Real; }
            part def B :> A { attribute :>> f = 4; }
            part def C :> A { attribute :>> f = 6; }
        """;

    // ------------------------------------------------------------------------------------------
    // Diamonds

    @Test
    @DisplayName("Unresolved conflicting redefinitions in a diamond are reported on D")
    void unresolvedDiamondIsReported() {
        List<ValidationWarning> warnings = check(DIAMOND + """
                part def D :> B, C;
            }
            """);

        assertEquals(1, warnings.size(), messages(warnings));
        ValidationWarning w = warnings.get(0);
        assertEquals(ErrorCodes.DIAMOND_INHERITANCE_CONFLICT, w.getErrorCode());
        assertEquals(ValidationWarning.Severity.WARNING, w.getSeverity());
        assertEquals(6, w.getLine(), "reported at D's declaration");
        assertEquals(13, w.getColumn(), "at D's name");
        String m = w.getMessage();
        assertTrue(m.contains("'P::D'"), m);
        assertTrue(m.contains("'P::A::f' of common ancestor 'P::A'"), m);
        assertTrue(m.contains("'P::B::f' (via 'P::B'; = 4)"), m);
        assertTrue(m.contains("'P::C::f' (via 'P::C'; = 6)"), m);
        assertTrue(m.contains("contradict (different value)"), m);
        assertTrue(m.contains("so it inherits both"), m);
        assertTrue(m.contains("redefine 'f' in 'P::D' (for example 'attribute :>> f = ...;')"), m);
        assertTrue(m.contains("common supertype of 'P::B' and 'P::C'"), m);
    }

    @Test
    @DisplayName("A diamond that D resolves by redefining f by name gives no warning")
    void diamondResolvedByNameIsNotReported() {
        assertEquals(List.of(), check(DIAMOND + """
                part def D :> B, C { attribute :>> f = 5; }
            }
            """));
        assertEquals(List.of(), check(DIAMOND + """
                part def D :> B, C { attribute redefines f = 5; }
            }
            """));
        assertEquals(List.of(), check(DIAMOND + """
                part def D :> B, C { attribute f :>> f = 5; }
            }
            """));
    }

    @Test
    @DisplayName("Redefining a branch redefinition or the ancestor feature by qualified name resolves it")
    void diamondResolvedByQualifiedRedefinition() {
        assertEquals(List.of(), check(DIAMOND + """
                part def D :> B, C { attribute value :>> B::f; }
            }
            """));
        assertEquals(List.of(), check(DIAMOND + """
                part def D :> B, C { attribute value :>> A::f = 1; }
            }
            """));
    }

    @Test
    @DisplayName("Identical redefinitions along both branches are reported as duplicates")
    void duplicateRedefinitionsAreReportedAsDuplicates() {
        List<ValidationWarning> warnings = check("""
            package P {
                attribute def Real;
                part def A { attribute f : Real; }
                part def B :> A { attribute :>> f = 4; }
                part def C :> A { attribute :>> f   =   4; }
                part def D :> B, C;
            }
            """);

        assertEquals(1, warnings.size(), messages(warnings));
        assertEquals(ErrorCodes.DIAMOND_INHERITANCE_CONFLICT, warnings.get(0).getErrorCode());
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("duplicates (same type, multiplicity and value)"), m);
        assertTrue(!m.contains("contradict"), m);
    }

    @Test
    @DisplayName("Redefinitions that differ in type and multiplicity say so")
    void differencesInTypeAndMultiplicityAreNamed() {
        List<ValidationWarning> warnings = check("""
            package P {
                attribute def Real;
                attribute def Integer;
                part def A { attribute f; }
                part def B :> A { attribute :>> f : Real[1]; }
                part def C :> A { attribute :>> f : Integer[2]; }
                part def D :> B, C;
            }
            """);

        assertEquals(1, warnings.size(), messages(warnings));
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("different type, different multiplicity"), m);
        assertTrue(m.contains("(via 'P::B'; : Real [1])"), m);
        assertTrue(m.contains("(via 'P::C'; : Integer [2])"), m);
    }

    @Test
    @DisplayName("A deeper diamond (ancestor two levels above one branch) is found")
    void deeperDiamondIsReported() {
        List<ValidationWarning> warnings = check("""
            package P {
                part def A { attribute f; }
                part def A1 :> A;
                part def A2 :> A1;
                part def B :> A2 { attribute :>> f = 1; }
                part def C :> A { attribute :>> f = 2; }
                part def D :> B, C;
            }
            """);

        assertEquals(1, warnings.size(), messages(warnings));
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("'P::A::f' of common ancestor 'P::A'"), m);
        assertTrue(m.contains("'P::B::f' (via 'P::B'; = 1)"), m);
    }

    @Test
    @DisplayName("The nearest redefinition on a branch is used, and the most specific common feature is named")
    void nearestRedefinitionOnEachBranch() {
        List<ValidationWarning> warnings = check("""
            package P {
                part def A0 { attribute f; }
                part def A :> A0 { attribute :>> f = 0; }
                part def B0 :> A { attribute :>> f = 1; }
                part def B :> B0 { attribute :>> f = 2; }
                part def C :> A { attribute :>> f = 3; }
                part def D :> B, C;
            }
            """);

        assertEquals(1, warnings.size(), messages(warnings));
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("'P::A::f' of common ancestor 'P::A'"), m);
        assertTrue(m.contains("'P::B::f' (via 'P::B'; = 2)"), m);
        assertTrue(!m.contains("P::B0::f"), "B0's redefinition is hidden by B's: " + m);
    }

    @Test
    @DisplayName("A usage typed by two conflicting definitions is reported; one that redefines f is not")
    void usageWithMultipleTypings() {
        List<ValidationWarning> warnings = check(DIAMOND + """
                part def Holder { part d : B, C; }
            }
            """);
        assertEquals(1, warnings.size(), messages(warnings));
        assertTrue(warnings.get(0).getMessage().contains("'P::Holder::d'"), warnings.get(0).getMessage());

        assertEquals(List.of(), check(DIAMOND + """
                part def Holder { part d : B, C { attribute :>> f = 1; } }
            }
            """));
    }

    @Test
    @DisplayName("A redefining usage inherits from the redefined feature and from its new type")
    void redefinedFeatureAndNewTypeConflict() {
        String model = """
            package P {
                part def Engine { attribute cylinders; }
                part def V8 :> Engine { attribute :>> cylinders = 8; }
                part def Vehicle { part engine : Engine { attribute :>> cylinders = 4; } }
                part def Car :> Vehicle { part :>> engine : V8; }
                part def Truck :> Vehicle { part :>> engine : V8 { attribute :>> cylinders = 8; } }
            }
            """;
        List<ValidationWarning> warnings = check(model);
        assertEquals(1, warnings.size(), messages(warnings));
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("'P::Car::engine'"), m);
        assertTrue(m.contains("'P::Vehicle::engine::cylinders' (via 'P::Vehicle::engine'; = 4)"), m);
        assertTrue(m.contains("'P::V8::cylinders' (via 'P::V8'; = 8)"), m);
    }

    @Test
    @DisplayName("A qualified redefinition of an unnamed redefinition is the more specific one (SpatialItems pattern)")
    void qualifiedRedefinitionOfUnnamedRedefinition() {
        // As in SpatialItems: componentParts redefines SpatialItem's own ':>> localClock' by qualified name
        assertEquals(List.of(), check("""
            package P {
                part def Occurrence { attribute localClock; }
                part def SpatialItem :> Occurrence {
                    attribute :>> localClock = 1;
                    part componentParts : SpatialItem[*] {
                        attribute :>> SpatialItem::localClock = 2;
                    }
                }
                part def Engine :> SpatialItem;
                part car : SpatialItem {
                    part powerSource : Engine :> componentParts;
                }
            }
            """));
    }

    @Test
    @DisplayName("Diamonds through library types are found (ISQ mass and length values)")
    void diamondThroughLibraryTypes() throws IOException {
        Optional<Path> release = TestPaths.sysmlReleaseRoot();
        assumeTrue(release.isPresent(), "SysML-v2-Release not available");
        Path quantities = release.get().resolve("sysml.library/Domain Libraries/Quantities and Units");
        ModelIndex library = new ModelIndex();
        for (String file : List.of("Quantities.sysml", "ISQBase.sysml")) {
            library.addAll(index(Files.readString(quantities.resolve(file)), file).getElements());
        }

        List<ValidationWarning> warnings = check(new ModelIndex(library), """
            package UserModel {
                private import ISQBase::*;
                attribute def Confused :> MassValue, LengthValue;
                attribute def Settled :> MassValue, LengthValue {
                    attribute :>> num;
                    attribute :>> mRef;
                }
            }
            """);

        assertEquals(2, warnings.size(), messages(warnings));
        String mRef = find(warnings, "mRef");
        assertTrue(mRef.contains("'UserModel::Confused'"), mRef);
        assertTrue(mRef.contains("'Quantities::ScalarQuantityValue::mRef' of common ancestor"
            + " 'Quantities::ScalarQuantityValue'"), mRef);
        assertTrue(mRef.contains("(via 'ISQBase::MassValue'; : MassUnit [1])"), mRef);
        assertTrue(mRef.contains("contradict (different type)"), mRef);
        String num = find(warnings, "'num'");
        assertTrue(num.contains("duplicates"), num);
        assertTrue(num.contains("'Quantities::TensorQuantityValue::num'"), num);
    }

    // ------------------------------------------------------------------------------------------
    // Name conflicts

    @Test
    @DisplayName("Two unrelated features with the same name are a name conflict")
    void nameConflictIsReported() {
        List<ValidationWarning> warnings = check("""
            package P {
                attribute def Real;
                part def B { attribute mass : Real; }
                part def C { attribute mass : Real = 2; }
                part def D :> B, C;
            }
            """);

        assertEquals(1, warnings.size(), messages(warnings));
        ValidationWarning w = warnings.get(0);
        assertEquals(ErrorCodes.MULTIPLE_INHERITANCE_NAME_CONFLICT, w.getErrorCode());
        String m = w.getMessage();
        assertTrue(m.contains("'P::D'"), m);
        assertTrue(m.contains("'P::B' and 'P::C' each contribute a different feature named 'mass'"), m);
        assertTrue(m.contains("'P::B::mass' (via 'P::B'; : Real)"), m);
        assertTrue(m.contains("'P::C::mass' (via 'P::C'; : Real = 2)"), m);
        assertTrue(m.contains("(for example 'attribute :>> mass = ...;'), rename one of them"), m);
    }

    @Test
    @DisplayName("A name conflict that D resolves by redefining the name gives no warning")
    void nameConflictResolved() {
        String base = """
            package P {
                part def B { attribute mass; }
                part def C { attribute mass; }
            """;
        assertEquals(List.of(), check(base + "part def D :> B, C { attribute mass :>> B::mass, C::mass; } }"));
        assertEquals(List.of(), check(base + "part def D :> B, C { attribute :>> mass; } }"));
    }

    @Test
    @DisplayName("A name conflict among parameters is not reported (parameters redefine by position)")
    void parametersAreNotCompared() {
        assertEquals(List.of(), check("""
            package P {
                attribute def Real;
                action def Move { in speed : Real; out done; }
                action def Fly { in speed : Real; out done; }
                action def Hover :> Move, Fly;
                requirement def R1 { subject v; }
                requirement def R2 { subject v; }
                requirement def R3 :> R1, R2;
            }
            """));
    }

    // ------------------------------------------------------------------------------------------
    // No warning

    @Test
    @DisplayName("A feature inherited along two paths without redefinition is one feature")
    void sameFeatureAlongTwoPathsIsNotAConflict() {
        assertEquals(List.of(), check("""
            package P {
                part def A { attribute f; part p; }
                part def B :> A;
                part def C :> A;
                part def D :> B, C;
            }
            """));
    }

    @Test
    @DisplayName("Redundant specialization (D :> B, A with B :> A) is not a conflict")
    void redundantSpecializationIsNotAConflict() {
        assertEquals(List.of(), check("""
            package P {
                part def A { attribute f; }
                part def B :> A { attribute :>> f = 1; }
                part def D :> B, A;
                part def E :> A, B;
            }
            """));
    }

    @Test
    @DisplayName("A third general whose redefinition merges both branches resolves the diamond")
    void mergingRedefinitionResolvesTheDiamond() {
        assertEquals(List.of(), check(DIAMOND + """
                part def Merged :> B, C { attribute :>> f = 5; }
                part def D :> B, C, Merged;
                part def E :> B, C { ref self2 :>> B::f, C::f; }
            }
            """));
    }

    @Test
    @DisplayName("Subsetting a feature by name finds the owner's unnamed redefinition of it (ShapeItems pattern)")
    void subsettingFindsTheOwnersRedefinition() {
        // As in ShapeItems: Polyhedron merges the faces' dimension; Prism's 'top :> faces' is one of those faces
        assertEquals(List.of(), check("""
            package P {
                part def Item { attribute dim; }
                part def Curve :> Item { attribute :>> dim = 1; }
                part def Surface :> Item { attribute :>> dim = 2; }
                part def Polygon :> Curve;
                part def Shell { part faces : Surface[*]; }
                part def Polyhedron :> Shell {
                    part :>> faces : Polygon[*] { attribute :>> Polygon::dim, faces::dim; }
                }
                part def Prism :> Polyhedron {
                    part :>> faces [5..6];
                    part top : Polygon :> faces;
                }
            }
            """));
    }

    @Test
    @DisplayName("A conflict settled in one general is not reported again where that general is combined")
    void conflictSettledInOneGeneral() {
        // As in the SysML metamodel: ConnectionUsage redefines association but keeps occurrenceDefinition
        assertEquals(List.of(), check("""
            package P {
                part def Feature { attribute type; }
                part def Usage :> Feature { attribute definition :>> type; }
                part def Connector :> Feature { attribute association :>> type; }
                part def ConnectionUsage :> Connector, Usage { attribute connectionDefinition :>> association; }
                part def Holder {
                    part connectors : Connector[*];
                    part connection : ConnectionUsage :> connectors;
                }
            }
            """));
    }

    @Test
    @DisplayName("A general that another general already specializes adds nothing (D :> M, C with M :> C)")
    void redundantGeneralIsIgnored() {
        assertEquals(List.of(), check(DIAMOND + """
                part def M :> B, C { attribute g :>> B::f = 3; }
                part def D :> M, C;
            }
            """));
    }

    @Test
    @DisplayName("Redefinitions without values of their own contradict through the values they inherit")
    void inheritedValuesAreCompared() {
        List<ValidationWarning> warnings = check("""
            package P {
                part def Item { attribute dim; }
                part def Curve :> Item { attribute :>> dim = 1; }
                part def Surface :> Item { attribute :>> dim = 2; }
                part def StructuredCurve :> Curve { attribute :>> dim; }
                part def StructuredSurface :> Surface { attribute :>> dim; }
                part def Face :> StructuredCurve, StructuredSurface;
            }
            """);
        assertEquals(1, warnings.size(), messages(warnings));
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("contradict (different value)"), m);
        assertTrue(m.contains("'P::StructuredCurve::dim' (via 'P::StructuredCurve'; inherits = 1)"), m);

        assertEquals(List.of(), check("""
            package P {
                part def A { attribute f; }
                part def B :> A { attribute :>> f; }
                part def C :> A { attribute :>> f; }
                part def D :> B, C;
            }
            """), "two redefinitions that add nothing do not repeat anything");
    }

    @Test
    @DisplayName("Only one branch redefining f is not a conflict")
    void singleBranchRedefinitionIsNotAConflict() {
        assertEquals(List.of(), check("""
            package P {
                part def A { attribute f; }
                part def B :> A { attribute :>> f = 1; }
                part def C :> A;
                part def D :> B, C;
            }
            """));
    }

    @Test
    @DisplayName("Implicit library base types are not modelled, so plain definitions never form a diamond")
    void implicitBaseTypesAreIgnored() {
        assertEquals(List.of(), check("""
            package P {
                part def B { attribute x; }
                part def C { attribute y; }
                part def D :> B, C;
                part d : B, C;
            }
            """));
    }

    @Test
    @DisplayName("A conflict that one general holds by itself is reported on that general only")
    void inheritedConflictIsReportedOnce() {
        List<ValidationWarning> warnings = check(DIAMOND + """
                part def D :> B, C;
                part def E :> D;
                part def X { attribute other; }
                part def F :> D, X;
            }
            """);
        assertEquals(1, warnings.size(), messages(warnings));
        assertTrue(warnings.get(0).getMessage().startsWith("Diamond inheritance in 'P::D'"), messages(warnings));
    }

    @Test
    @DisplayName("Only elements of the checked file are reported, not library elements")
    void libraryElementsAreNotChecked() {
        ModelIndex library = new ModelIndex();
        library.addAll(index(DIAMOND + "part def LibD :> B, C; }", "Lib.sysml").getElements());

        assertEquals(List.of(), check(new ModelIndex(library), """
            package User {
                part def Uses :> P::LibD;
            }
            """));
        List<ValidationWarning> warnings = check(new ModelIndex(library), """
            package User {
                part def Mine :> P::B, P::C;
            }
            """);
        assertEquals(1, warnings.size(), messages(warnings));
        assertTrue(warnings.get(0).getMessage().contains("'User::Mine'"), messages(warnings));
    }

    @Test
    @DisplayName("Unresolved generals and inheritance cycles are skipped without errors")
    void unresolvedAndCyclicGeneralsAreSkipped() {
        assertEquals(List.of(), check("""
            package P {
                part def B :> Missing { attribute :>> f = 1; }
                part def C :> AlsoMissing { attribute :>> f = 2; }
                part def D :> B, C;
                part def X :> Y { attribute :>> g = 1; }
                part def Y :> X { attribute :>> g = 2; }
                part def Z :> X, Y;
            }
            """));
    }

    @Test
    @DisplayName("A KerML classifier with two conflicting generals is reported")
    void kermlClassifiers() {
        List<ValidationWarning> warnings = check(new ModelIndex(), """
            package K {
                classifier A { feature f; }
                classifier B specializes A { feature redefines f = 1; inv { true } }
                classifier C specializes A { feature redefines f = 2; inv { true } }
                classifier D specializes B, C;
            }
            """, "test.kerml");
        assertEquals(1, warnings.size(), messages(warnings));
        assertEquals(ErrorCodes.DIAMOND_INHERITANCE_CONFLICT, warnings.get(0).getErrorCode());
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("'K::B::f' (via 'K::B'; = 1)"), "the unnamed redefinition is named after f: " + m);
    }

    // ------------------------------------------------------------------------------------------
    // Contradiction, duplicate or compatible refinement

    @Test
    @DisplayName("Redefinitions that only refine compatibly (one adds a value, a subtype or a bound) are not reported")
    void compatibleRefinementsAreNotReported() {
        assertEquals(List.of(), check("""
            package P {
                attribute def Number;
                attribute def Integer :> Number;
                part def A { attribute f : Number; attribute g; attribute h; }
                part def B :> A { attribute :>> f : Number; attribute :>> g = 4; attribute :>> h [0..1]; }
                part def C :> A { attribute :>> f : Integer; attribute :>> g; attribute :>> h [1]; }
                part def D :> B, C;
            }
            """));
    }

    @Test
    @DisplayName("Only literal values are compared: expressions may agree, literals with units may not")
    void onlyLiteralValuesContradict() {
        assertEquals(List.of(), check("""
            package P {
                part def A { attribute f; attribute size; }
                part def B :> A { attribute :>> f = size * 2; }
                part def C :> A { attribute :>> f = 3; }
                part def D :> B, C;
            }
            """), "'size * 2' and '3' cannot be shown to differ");

        List<ValidationWarning> warnings = check("""
            package P {
                part def A { attribute f; }
                part def B :> A { attribute :>> f = 5 [kg]; }
                part def C :> A { attribute :>> f = 6 [kg]; }
                part def D :> B, C;
            }
            """);
        assertEquals(1, warnings.size(), messages(warnings));
        assertTrue(warnings.get(0).getMessage().contains("contradict (different value)"), messages(warnings));
        assertTrue(InheritanceChecker.isLiteralValue("= \"FR4 PCB\""));
        assertTrue(InheritanceChecker.isLiteralValue("default = -1.5e3"));
        assertTrue(InheritanceChecker.isLiteralValue(":= true"));
        assertTrue(!InheritanceChecker.isLiteralValue("= mRef.dimensions"));
    }

    @Test
    @DisplayName("Multiplicities without a common value contradict; unrelated types contradict")
    void disjointMultiplicitiesAndUnrelatedTypesContradict() {
        List<ValidationWarning> warnings = check("""
            package P {
                attribute def Mass;
                attribute def Length;
                part def A { attribute f; attribute g; }
                part def B :> A { attribute :>> f [0..1]; attribute :>> g : Mass; }
                part def C :> A { attribute :>> f [2..*]; attribute :>> g : Length; }
                part def D :> B, C;
            }
            """);
        assertEquals(2, warnings.size(), messages(warnings));
        assertTrue(find(warnings, "'P::A::f'").contains("contradict (different multiplicity)"), messages(warnings));
        assertTrue(find(warnings, "'P::A::g'").contains("contradict (different type)"), messages(warnings));
    }

    @Test
    @DisplayName("Redefinitions whose bodies redefine a nested feature differently contradict")
    void nestedContradictionsAreFound() {
        String base = """
            package P {
                part def Engine { attribute cylinders; attribute mass; }
                part def Vehicle { part engine : Engine; }
            """;
        List<ValidationWarning> warnings = check(base + """
                part def Sporty :> Vehicle { part :>> engine { attribute :>> cylinders = 8; } }
                part def Frugal :> Vehicle { part :>> engine { attribute :>> cylinders = 3; } }
                part def Hybrid :> Sporty, Frugal;
            }
            """);
        assertEquals(1, warnings.size(), messages(warnings));
        String m = warnings.get(0).getMessage();
        assertTrue(m.contains("'P::Vehicle::engine' of common ancestor 'P::Vehicle'"), m);
        assertTrue(m.contains("contradict (conflicting nested feature 'cylinders')"), m);
        assertTrue(m.contains("'part :>> engine;'"), m);

        assertEquals(List.of(), check(base + """
                part def Sporty :> Vehicle { part :>> engine { attribute :>> cylinders = 8; } }
                part def Heavy :> Vehicle { part :>> engine { attribute :>> mass = 300; } }
                part def Both :> Sporty, Heavy;
            }
            """), "bodies that redefine different nested features are compatible");
    }

    @Test
    @DisplayName("Private features are not inherited, so they cannot conflict")
    void privateFeaturesAreNotInherited() {
        assertEquals(List.of(), check("""
            package P {
                part def B { private attribute factor; }
                part def C { private attribute factor; }
                part def D :> B, C;
            }
            """));
    }


    // ------------------------------------------------------------------------------------------

    private static List<ValidationWarning> check(String source) {
        return check(new ModelIndex(), source);
    }

    private static List<ValidationWarning> check(ModelIndex index, String source) {
        return check(index, source, "test.sysml");
    }

    private static List<ValidationWarning> check(ModelIndex index, String source, String fileName) {
        ModelIndexBuilder.Result built = index(source, fileName);
        index.addAll(built.getElements());
        return InheritanceChecker.check(built.getElements(), index, fileName);
    }

    private static ModelIndexBuilder.Result index(String source, String fileName) {
        SysMLv2ParserFacade.ParseResult parsed = PARSER.parseString(source, fileName);
        assertEquals(List.of(), syntaxErrors(parsed), "the test model must parse");
        return ModelIndexBuilder.build(parsed.getParseTree(), fileName, true);
    }

    private static List<String> syntaxErrors(SysMLv2ParserFacade.ParseResult parsed) {
        List<String> errors = new ArrayList<>();
        for (SysMLv2ParserFacade.SyntaxError e : parsed.getSyntaxErrors()) {
            errors.add(e.getLine() + ":" + e.getCharPositionInLine() + " " + e.getMessage());
        }
        return errors;
    }

    private static String messages(List<ValidationWarning> warnings) {
        StringBuilder sb = new StringBuilder();
        for (ValidationWarning w : warnings) {
            sb.append('\n').append(w.getErrorCode()).append(' ').append(w.getLine()).append(':')
                .append(w.getColumn()).append(' ').append(w.getMessage());
        }
        return sb.toString();
    }

    private static String find(List<ValidationWarning> warnings, String text) {
        for (ValidationWarning w : warnings) {
            if (w.getMessage().contains(text)) {
                return w.getMessage();
            }
        }
        throw new AssertionError("No warning mentions " + text + ":" + messages(warnings));
    }
}
