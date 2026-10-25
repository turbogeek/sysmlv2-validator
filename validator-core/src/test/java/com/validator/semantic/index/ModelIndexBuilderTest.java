package com.validator.semantic.index;

import com.validator.parser.SysMLv2ParserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for what {@link ModelIndexBuilder} records for the inheritance check: unnamed redefinitions,
 * redefined features, parameters, positions and the text of typings, multiplicities and values.
 */
@DisplayName("Model index builder: inheritance data")
class ModelIndexBuilderTest {

    private static final SysMLv2ParserFacade PARSER = new SysMLv2ParserFacade();

    @Test
    @DisplayName("Unnamed redefinitions are indexed under the name of the feature they redefine")
    void unnamedRedefinitionsAreIndexed() {
        Map<String, IndexedElement> e = build("""
            package P {
                part def A { attribute f; attribute g; attribute h; }
                part def B :> A {
                    attribute :>> f : Real [1] = 4;
                    :>> g = 5;
                    attribute redefines h = 6;
                }
                part def C { part : A; }
            }
            """);

        List<IndexedElement> owned = ownedBy(e, "P::B");
        assertEquals(3, owned.size(), owned.toString());
        assertEquals(List.of("f", "g", "h"), owned.stream().map(IndexedElement::effectiveName).toList());
        for (IndexedElement r : owned) {
            assertNull(r.name, "unnamed");
            assertTrue(r.qualifiedName.startsWith("P::B::$anon@"), r.qualifiedName);
            assertEquals(List.of(List.of(r.effectiveName())), r.redefines);
            assertTrue(r.supertypes.containsAll(r.redefines), "redefinitions are supertypes too");
        }
        assertEquals("AttributeUsage", owned.get(0).kind);
        assertEquals("Real", owned.get(0).typeText);
        assertEquals("[1]", owned.get(0).multiplicityText);
        assertEquals("= 4", owned.get(0).valueText);
        assertEquals("= 5", owned.get(1).valueText);
        assertEquals("= 6", owned.get(2).valueText);

        assertEquals(List.of(), ownedBy(e, "P::C"), "an unnamed usage that redefines nothing is not indexed");
    }

    @Test
    @DisplayName("Named features record what they redefine, subset and are typed by")
    void namedFeatureRelationships() {
        Map<String, IndexedElement> e = build("""
            package P {
                part def Engine;
                part def Vehicle { part engines : Engine[*]; part engine : Engine; }
                part def Car :> Vehicle {
                    part engine : ~Engine, Engine [0..1] :> engines redefines Vehicle::engine;
                }
            }
            """);

        IndexedElement engine = e.get("P::Car::engine");
        assertEquals(List.of(List.of("Vehicle", "engine")), engine.redefines);
        assertEquals(List.of(List.of("Engine"), List.of("Engine"), List.of("engines"), List.of("Vehicle", "engine")),
            engine.supertypes);
        assertEquals("~Engine, Engine", engine.typeText);
        assertEquals("[0..1]", engine.multiplicityText);
        assertNull(engine.valueText);
        assertEquals("engine", engine.effectiveName());
    }

    @Test
    @DisplayName("Declarations record the line and column of their name")
    void positions() {
        Map<String, IndexedElement> e = build("""
            package P {
                part def Vehicle {
                  attribute mass;
                    attribute :>> mass = 1;
                }
            }
            """);

        assertEquals(2, e.get("P::Vehicle").line);
        assertEquals(13, e.get("P::Vehicle").column);
        assertEquals(3, e.get("P::Vehicle::mass").line);
        assertEquals(16, e.get("P::Vehicle::mass").column);
        IndexedElement redefinition = ownedBy(e, "P::Vehicle").get(1);
        assertEquals(4, redefinition.line);
        assertEquals(8, redefinition.column, "an unnamed feature is placed at its first token");
    }

    @Test
    @DisplayName("Parameters, ends, subjects and directed features are flagged; plain features are not")
    void parametersAreFlagged() {
        Map<String, IndexedElement> e = build("""
            package P {
                action def Move {
                    in speed;
                    out distance;
                    inout fuel;
                    in item cargo;
                    attribute notAParameter;
                }
                calc def Area { return area; }
                requirement def R { subject vehicle; actor driver; }
                connection def Link { end part a; end b; }
                part def Box { in port p; part plain; }
            }
            """);

        for (String parameter : List.of("P::Move::speed", "P::Move::distance", "P::Move::fuel", "P::Move::cargo",
                "P::Area::area", "P::R::vehicle", "P::R::driver", "P::Link::a", "P::Link::b", "P::Box::p")) {
            assertTrue(e.get(parameter).parameter, parameter + " is a parameter");
        }
        for (String feature : List.of("P::Move::notAParameter", "P::Box::plain")) {
            assertFalse(e.get(feature).parameter, feature + " is not a parameter");
        }
    }

    @Test
    @DisplayName("Long values are shortened with a hash of the whole value; whitespace does not matter")
    void longValuesAreShortened() {
        String tail = " + aVeryLongVariableName * anotherVeryLongVariableName + yetAnotherLongName";
        Map<String, IndexedElement> e = build("package P { part def A {"
            + " attribute x = 1" + tail + " + 1;"
            + " attribute y = 1" + tail + " + 2;"
            + " attribute z = 1" + tail.replace(" ", "  ") + "   + 1;"
            + " attribute w = 2 *   3; } }");

        String x = e.get("P::A::x").valueText;
        assertTrue(x.length() < 70, x);
        assertTrue(x.startsWith("= 1 + aVeryLongVariableName"), x);
        assertTrue(x.contains("... #"), x);
        assertNotEquals(x, e.get("P::A::y").valueText, "values that differ only at the end stay different");
        assertEquals(x, e.get("P::A::z").valueText, "the same value with other spacing is the same");
        assertEquals("= 2 * 3", e.get("P::A::w").valueText);
    }

    @Test
    @DisplayName("In KerML, 'feature redefines x' and 'inv { }' are unnamed, not features named after the keyword")
    void kermlKeywordsAreNotNames() {
        String source = """
            package K {
                classifier A {
                    feature self : A;
                    feature redefines self : A;
                    inv { true }
                }
            }
            """;
        Map<String, IndexedElement> kerml = build(source, "lib.kerml");
        assertFalse(kerml.containsKey("K::A::feature"), kerml.keySet().toString());
        assertFalse(kerml.containsKey("K::A::inv"), kerml.keySet().toString());
        List<IndexedElement> owned = ownedBy(kerml, "K::A");
        assertEquals(3, owned.size(), owned.toString());
        assertEquals("self", owned.get(1).effectiveName(), "named after what it redefines");
        assertEquals(List.of(List.of("self")), owned.get(1).redefines);
        assertNull(owned.get(2).effectiveName(), "an invariant has no name");

        // In SysML these words are not reserved: a feature may be called 'feature'
        assertTrue(build(source.replace("inv { true }", ""), "model.sysml").containsKey("K::A::feature"));
    }

    @Test
    @DisplayName("'succession a then b;' does not declare a member named a")
    void successionSourceIsNotADeclaration() {
        Map<String, IndexedElement> e = build("""
            package P {
                action def A { action done; succession self then done; succession s first done then done; }
            }
            """);
        assertFalse(e.containsKey("P::A::self"), e.keySet().toString());
        assertTrue(e.containsKey("P::A::s"), "a named succession with endpoints keeps its name");
    }

    @Test
    @DisplayName("A redefinition that the grammar splits off an unterminated declaration is joined back to it")
    void splitDeclarationIsJoined() {
        // As in the standard library's Constraints package: the constraint usage ends before ':>>'
        Map<String, IndexedElement> e = build("""
            package P {
                constraint def BooleanEvaluation;
                constraint def ConstraintCheck :> BooleanEvaluation {
                    ref constraint self: ConstraintCheck :>> BooleanEvaluation::self;
                    attribute x;
                    :>> x = 1;
                }
            }
            """);

        IndexedElement self = e.get("P::ConstraintCheck::self");
        assertEquals(List.of(List.of("BooleanEvaluation", "self")), self.redefines);
        assertTrue(self.supertypes.contains(List.of("ConstraintCheck")), self.supertypes.toString());
        List<IndexedElement> owned = ownedBy(e, "P::ConstraintCheck");
        assertEquals(List.of("self", "x", "x"), owned.stream().map(IndexedElement::effectiveName).toList(),
            "a terminated declaration is not joined with the redefinition that follows it");
    }

    @Test
    @DisplayName("Private members are flagged")
    void privateMembersAreFlagged() {
        Map<String, IndexedElement> e = build("""
            package P {
                part def A { private attribute hidden; protected attribute shared; attribute open; }
                private part def Internal;
            }
            """);
        assertTrue(e.get("P::A::hidden").isPrivate);
        assertFalse(e.get("P::A::shared").isPrivate, "protected members are inherited");
        assertFalse(e.get("P::A::open").isPrivate);
        assertTrue(e.get("P::Internal").isPrivate);
    }

    @Test
    @DisplayName("Indexing unnamed redefinitions does not change name resolution")
    void resolutionIsUnchanged() {
        ModelIndex index = new ModelIndex();
        Map<String, IndexedElement> e = build("""
            package P {
                part def A { attribute f; }
                part def B :> A { attribute :>> f = 1; }
            }
            """);
        index.addAll(e.values());

        ModelIndex.Resolution r = index.resolve(List.of("B", "f"), "P");
        assertEquals(ModelIndex.Status.FOUND, r.getStatus());
        assertEquals(java.util.Set.of("P::A::f"), r.getTargets(), "unnamed features are not members by name");
        assertEquals(ModelIndex.Status.FOUND, index.resolveMember("P::B", "f").getStatus());
        assertEquals(ModelIndex.Status.NOT_FOUND, index.resolveMember("P::B", "P").getStatus(),
            "a member lookup does not search enclosing namespaces");
        assertEquals(1, index.ownedElements("P::B").size());
        assertEquals("P::B", index.recordsOf("P::B").get(0).qualifiedName);
    }

    // ------------------------------------------------------------------------------------------

    private static Map<String, IndexedElement> build(String source) {
        return build(source, "test.sysml");
    }

    private static Map<String, IndexedElement> build(String source, String fileName) {
        SysMLv2ParserFacade.ParseResult parsed = PARSER.parseString(source, fileName);
        List<String> errors = new ArrayList<>();
        for (SysMLv2ParserFacade.SyntaxError err : parsed.getSyntaxErrors()) {
            errors.add(err.getLine() + ":" + err.getCharPositionInLine() + " " + err.getMessage());
        }
        assertEquals(List.of(), errors, "the test model must parse");
        Map<String, IndexedElement> result = new LinkedHashMap<>();
        for (IndexedElement element : ModelIndexBuilder.build(parsed.getParseTree(), fileName, false).getElements()) {
            result.put(element.qualifiedName, element);
        }
        return result;
    }

    private static List<IndexedElement> ownedBy(Map<String, IndexedElement> elements, String parent) {
        return elements.values().stream().filter(x -> parent.equals(x.parentName)).toList();
    }
}
