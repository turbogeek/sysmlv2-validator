package com.validator.parser;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grammar gaps found by parsing the official SysML v2 release (sysml/src and kerml/src/examples). Each test is a
 * minimal form of a construct that the official BNF (SysML-textual-bnf.kebnf, KerML-textual-bnf.kebnf) allows and the
 * validator rejected; the production is named in the test. Where a fix could also accept something the BNF does not,
 * a second test checks that the invalid form is still rejected.
 */
@DisplayName("Grammar gaps found by the official SysML v2 examples")
class OfficialExampleGrammarTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    private SysMLv2ParserFacade.ParseResult parse(String text) {
        return parserFacade.parseString(text, "official-gap.sysml");
    }

    private static void assertParses(SysMLv2ParserFacade.ParseResult result) {
        assertFalse(result.hasErrors(), "Should parse without errors: " + result.getSyntaxErrors());
    }

    /** True if the tree has a node of the given rule, i.e. the construct was read by that rule. */
    private static boolean contains(ParseTree tree, Class<? extends ParserRuleContext> rule) {
        if (rule.isInstance(tree)) {
            return true;
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            if (contains(tree.getChild(i), rule)) {
                return true;
            }
        }
        return false;
    }

    /** Parses with SLL prediction only, failing on the first conflict, as the facade's fast first stage does. */
    private static boolean parsesWithSllAlone(String text) {
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        SysMLv2Parser parser = new SysMLv2Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new BailErrorStrategy());
        try {
            parser.compilationUnit();
            return true;
        } catch (ParseCancellationException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- notes (KerML 8.2.2.2 MULTILINE_NOTE)

    @Test
    @DisplayName("A multiline note '//* ... */' can end mid-line")
    void testMultilineNoteEndsMidLine() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                private import ScalarValues::Real;
                attribute cityScenario : Real[*] = ( //* ... */ );
            }
            """);
        assertParses(result);
        assertTrue(result.getLineComments().isEmpty(), "A multiline note is not a '//' line comment");
    }

    @Test
    @DisplayName("A multiline note spanning lines hides its text; a '//' line comment is still reported")
    void testMultilineNoteSpansLines() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                //*
                // The above is semantically equivalent to:
                ref action 'provide power' (in fuelCmd = fuelCmdPort::fuelCmd) :> performedActions;
                */
                part p; // a line comment
            }
            """);
        assertParses(result);
        assertEquals(1, result.getLineComments().size(), "Only the '//' comment after 'part p;' is a line comment");
        assertEquals(6, result.getLineComments().get(0).getLine());
    }

    // ---------------------------------------------------------------- user-defined keywords (SysML 8.2.2.27)

    @Test
    @DisplayName("ExtendedDefinition and ExtendedUsage: '#scenario def X { #cause 'c' { ... } }'")
    void testUserDefinedKeywords() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                metadata def scenario;
                metadata def cause;
                metadata def system;
                #scenario def DeviceFailure {
                    #cause 'battery old' {
                        :>> probability = 0.01;
                    }
                }
                #system service_registry {
                    #cause :>> serviceDiscovery;
                }
                #system orchestrationDesign;
            }
            """);
        assertParses(result);
        assertTrue(contains(result.getParseTree(), SysMLv2Parser.ExtendedDefinitionContext.class));
    }

    @Test
    @DisplayName("'def' is reserved: 'actor def', 'snapshot def' and a bare 'def' are rejected")
    void testDefWithoutUserKeywordRejected() {
        assertTrue(parse("package P { actor def User { } }").hasErrors(), "There is no 'actor def'");
        assertTrue(parse("package P { snapshot def S :> Vehicle { } }").hasErrors(), "There is no 'snapshot def'");
        assertTrue(parse("package P { def X { } }").hasErrors(), "'def' needs a definition keyword or '#keyword'");
        assertTrue(parse("package P { part p : def; }").hasErrors(), "'def' cannot be a name");
    }

    // ---------------------------------------------------------------- connectors (SysML 8.2.2.13.2, 8.2.2.14.2)

    @Test
    @DisplayName("'connect' with an n-ary ConnectorPart or a body, 'allocate' with named ends and a body")
    void testConnectorParts() {
        assertParses(parse("""
            package P {
                occurrence a; item b; part c; action d;
                connect ( cause1 ::> a, cause2 ::> b, effect1 ::> c, effect2 ::> d );
                connect (a, b, c, d);
                connect b to d {
                    doc /* a binary connection with a body */
                }
                allocation torqueGenAlloc allocate logical ::> a to physical ::> c {
                    allocate a.x to c.y;
                }
            }
            """));
    }

    @Test
    @DisplayName("An n-ary ConnectorPart needs at least two ends")
    void testNaryConnectorWithOneEndRejected() {
        assertTrue(parse("package P { part a; connect (a); }").hasErrors());
    }

    // ---------------------------------------------------------------- metadata (SysML 8.2.2.27)

    @Test
    @DisplayName("Metadata usage 'about' several elements, with or without a body")
    void testMetadataUsageAbout() {
        assertParses(parse("""
            package P {
                metadata def SafetyFeature;
                metadata def Issue { attribute text : ScalarValues::String; }
                part vehicle { part seatBelt; part bumper; }
                metadata SafetyFeature about
                    vehicle::seatBelt,
                    vehicle::bumper;
                metadata issue : Issue about vehicle {
                    text = "A body after the annotated elements";
                }
                @SafetyFeature about vehicle::bumper;
            }
            """));
    }

    // ---------------------------------------------------------------- requirements (SysML 8.2.2.21)

    @Test
    @DisplayName("'assume' / 'require' with a constraint reference, multiplicity or user-defined keywords")
    void testRequirementConstraintUsages() {
        assertParses(parse("""
            package P {
                metadata def goal;
                constraint c1;
                constraint c2;
                concern c3;
                requirement def R2 {
                    assume c1 [0..*];
                    require c2 [0..*];
                    frame c3[0..*];
                }
                requirement deliverPayload {
                    assume #goal constraint payloadMassLimit;
                    require #goal vehicleMassRequirement;
                }
            }
            """));
    }

    @Test
    @DisplayName("Viewpoints and concerns have requirement bodies: 'require constraint { ... }'")
    void testViewpointAndConcernBodies() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                concern def UserConcern {
                    stakeholder user : Person;
                    require constraint {
                        doc /* Device must be readable from 1 meter distance */
                    }
                }
                viewpoint 'system structure perspective' {
                    frame 'system breakdown';
                    require constraint {
                        doc /* shows the part decomposition */
                    }
                }
                concern c {
                    subject s;
                    assume constraint { true }
                }
            }
            """);
        assertParses(result);
    }

    // ---------------------------------------------------------------- views (SysML 8.2.2.26)

    @Test
    @DisplayName("ViewRenderingUsage reference with multiplicity: 'render r [0..*];'")
    void testRenderWithMultiplicity() {
        assertParses(parse("""
            package P {
                view def V;
                rendering r;
                view v : V {
                    render r [0..*];
                }
            }
            """));
    }

    // ---------------------------------------------------------------- states and transitions (SysML 8.2.2.18)

    @Test
    @DisplayName("Target transitions with 'at' / 'when' / 'after' triggers, guards, effects and a body")
    void testTargetTransitions() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                attribute def OverTemp;
                state def S {
                    in controller;
                    in vehicle;
                    first start then normal;
                    state normal;
                    accept at vehicle.maintenanceTime
                        then maintenance;
                    accept when vehicle.temp > vehicle.maxTemperature
                        do send new OverTemp() to controller
                        then degraded;
                    state maintenance;
                    accept after 48 [h]
                        then normal;
                    state degraded;
                    accept VehicleOnSignal
                        if vehicle.brakePedalDepressed
                        do send new OverTemp() to controller
                        then normal;
                }
            }
            """);
        assertParses(result);
        assertTrue(contains(result.getParseTree(), SysMLv2Parser.AcceptTransitionContext.class));
    }

    @Test
    @DisplayName("A transition's target may be followed by a body, and its trigger may be a feature chain")
    void testTransitionBodyAndChainTrigger() {
        assertParses(parse("""
            package P {
                state def Main {
                    state off;
                    state starting;
                    part Remote { action start; }
                    transition 'off-starting'
                        first off
                        accept 'Vehicle Start Signal'
                        if vehicle.brakePedalDepressed
                        do send new StartSignal() to vehicle.controller
                        then starting {
                        doc /* The transition definition is implicit. */
                    }
                    transition startServe
                        first off
                        accept Remote.start
                        do action log : Log { in msg = "Serving"; }
                        then starting;
                }
            }
            """));
    }

    @Test
    @DisplayName("Guarded succession: 'succession S first A1 if x == 0 then A2;'")
    void testGuardedSuccession() {
        assertParses(parse("""
            action def DecisionTest {
                attribute x = 1;
                action A1;
                action A2;
                public succession S first A1
                    if x == 0 then A2;
            }
            """));
    }

    // ---------------------------------------------------------------- use cases (SysML 8.2.2.24)

    @Test
    @DisplayName("Include of a referenced use case with multiplicity and a body")
    void testIncludeReference() {
        assertParses(parse("""
            package P {
                use case 'add fuel' { subject vehicle; }
                use case 'drive vehicle' {
                    subject vehicle;
                    include 'add fuel'[0..*] {
                        subject vehicle;
                        actor fueler = driver;
                    }
                    then include 'add fuel' {
                        subject;
                    }
                }
            }
            """));
    }

    // ---------------------------------------------------------------- imports (SysML 8.2.2.5.1)

    @Test
    @DisplayName("Filtered imports and imports with a relationship body")
    void testFilteredImportAndImportBody() {
        SysMLv2ParserFacade.ParseResult result = parse("""
            package P {
                metadata def Safety { attribute isMandatory : ScalarValues::Boolean; }
                part vehicle { part bumper { @Safety { isMandatory = true; } } }
                package 'Safety Features' {
                    public import vehicle::**[@Safety];
                }
                package 'Mandatory Safety Features' {
                    public import vehicle::**[@Safety and (as Safety).isMandatory];
                }
                private import ScalarValues::Integer {
                    doc
                    /*
                     * The unqualified Integer is signed.
                     */
                }
            }
            """);
        assertParses(result);
    }

    // ---------------------------------------------------------------- flows and successions (SysML 8.2.2.13.4)

    @Test
    @DisplayName("Flows between feature chains, with a body, and a succession flow")
    void testFlowsWithBody() {
        assertParses(parse("""
            package P {
                action 'generate torque' { out engineTorque; }
                action 'amplify torque' { in engineTorque; }
                flow 'generate torque'.engineTorque
                    to 'amplify torque'.engineTorque {
                    doc /* A flow streams items between actions. */
                }
                succession flow 'generate torque'.engineTorque to 'amplify torque'.engineTorque;
                ref flow 'generate torque'.engineTorque to 'amplify torque'.engineTorque;
            }
            """));
    }

    @Test
    @DisplayName("A flow needs its ';' or body (DefinitionBody)")
    void testFlowWithoutBodyRejected() {
        assertTrue(parse("""
            package P {
                part p {
                    flow a.x to b.y
                    part q;
                }
            }
            """).hasErrors(), "Before the fix a bare 'flow' ended the usage and the rest was read as statements");
    }

    // ---------------------------------------------------------------- expressions (KerML 8.2.5.8)

    @Test
    @DisplayName("Extent expression 'all T' as a value and in parentheses; unary '+'")
    void testExtentExpressionAndUnaryPlus() {
        assertParses(parse("""
            package P {
                part def Engine;
                part engineChoice : Engine[*];
                analysis engineTradeStudy {
                    subject : Engine[1..*] = all engineChoice;
                }
                attribute z = (all Engine)#(1);
                attribute p = +1;
            }
            """));
    }

    @Test
    @DisplayName("The non-BNF 'expression transition expression' form is rejected")
    void testExpressionTransitionRejected() {
        assertTrue(parse("package P { state def S { state a; state b; a transition b; } }").hasErrors());
    }

    // ---------------------------------------------------------------- feature declarations (SysML 8.2.2.6)

    @Test
    @DisplayName("Constraint usages with a typing and ';' parse without SLL ambiguity")
    void testConstraintUsageForms() {
        String text = """
            package P {
                constraint def C;
                constraint c : C;
                part q {
                    assert constraint massConstraint : C;
                    assert not c;
                }
            }
            """;
        assertParses(parse(text));
        assertTrue(parsesWithSllAlone(text), "SLL prediction alone should suffice for constraint usages");
    }

    // ---------------------------------------------------------------- SLL prediction (parse speed)

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "part def A { flow p1.torque to p2.torque; }",
        "part def A { succession flow x.p to a1.aa.receiver; }",
        "package P { @Risk about a::b { totalRisk = medium; } }",
        "package P { use case u { include 'add fuel'[0..*] { subject v; } } }",
        "package P { calc def F { return : Real = c->reduce '+' ?? zero; } }",
        "package P { metadata def M specializes A, B; metadata : M; }"
    })
    @DisplayName("Valid forms that used to force a full LL re-parse now parse with SLL prediction alone")
    void testFormsParseWithSllAlone(String text) {
        assertParses(parse(text));
        assertTrue(parsesWithSllAlone(text),
            "SLL prediction should suffice; a conflict here makes the facade re-parse the whole file with LL");
    }
}
