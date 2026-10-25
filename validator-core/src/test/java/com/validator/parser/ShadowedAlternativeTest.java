package com.validator.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Valid SysML v2 and KerML members must parse as the members they are. Grammar coverage (GrammarCoverageTest) found rule
 * alternatives that no valid input reaches because an earlier alternative of the same rule accepts the keyword alone:
 * 'constraint c;' parses as an anonymous 'constraint' followed by a separate feature 'c;', without a syntax error.
 * The validator then sees two elements where the model has one, named differently. Each case below is one member,
 * or a fixed number of members, in the body of the element around it; the body must have exactly that many elements.
 */
@DisplayName("Members parse as one element")
class ShadowedAlternativeTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    /** A SysML v2 case: members inside open/close, which parse as the expected number of elements. */
    private static Arguments sysml(String open, String members, String close, int expected) {
        return Arguments.of("members.sysml", open, members, close, expected);
    }

    /** A KerML case, parsed as a .kerml file. */
    private static Arguments kerml(String open, String members, String close, int expected) {
        return Arguments.of("members.kerml", open, members, close, expected);
    }

    /** Forms that parse as the expected elements today; they guard the cases below against a broken check. */
    static Stream<Arguments> parsedAsWritten() {
        return Stream.of(
            sysml("", "metadata def Tag { }", "", 1),
            sysml("part def V {", "constraint c { }", "}", 1),
            sysml("part def V {", "assert constraint c { }", "}", 1),
            sysml("part def V {", "concern c { }", "}", 1),
            sysml("part def V {", "metadata Tag { }", "}", 1),
            sysml("part def V {", "event occurrence e;", "}", 1),
            sysml("requirement def R {", "subject v : V;", "}", 1),
            sysml("requirement def R {", "verify requirement r : Q;", "}", 1),
            sysml("connection def C {", "end [0..1] item cart : Cart[1];", "}", 1),
            sysml("enum def E {", "enum red;", "}", 1),
            sysml("state def S {", "state off; transition t first off accept Sig then on;", "}", 2),
            kerml("behavior B {", "succession s first a then b;", "}", 1));
    }

    /**
     * Valid SysML v2 and KerML that the parser splits into more elements than it has. In each rule the first
     * alternative ends with an optional body that cannot be ';' (or an optional name and nothing else), so it matches
     * the keyword alone and the rest of the member parses as another element; the alternative written for the member
     * is never chosen. KerML's 'succession a then b;' (SuccessionDeclaration, whose 'first' is optional) parses as
     * 'succession a' with 'a' as its name and a separate 'then b;'.
     */
    static Stream<Arguments> splitByShadowedAlternative() {
        return Stream.of(
            sysml("", "metadata def Tag;", "", 1),
            sysml("part def V {", "constraint c;", "}", 1),
            sysml("part def V {", "assert constraint c;", "}", 1),
            sysml("part def V {", "concern c;", "}", 1),
            sysml("part def V {", "metadata Tag;", "}", 1),
            sysml("part def V {", "event a.b;", "}", 1),
            sysml("requirement def R {", "subject v : V { }", "}", 1),
            sysml("requirement def R {", "verify r.req;", "}", 1),
            sysml("connection def C {", "end inCart [0..1] item cart : Cart[1];", "}", 1),
            sysml("enum def E {", "enum red { }", "}", 1),
            sysml("state def S {", "state off; accept Sig then on;", "}", 2),
            kerml("behavior B {", "succession a then b;", "}", 1));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("parsedAsWritten")
    @DisplayName("Unambiguous forms parse as the elements written")
    void testParsedAsWritten(String fileName, String open, String members, String close, int expected) {
        assertBodyElements(fileName, open, members, close, expected);
    }

    @Disabled("bug: SysMLv2Parser.g4 shadows the ';' alternatives of metadataDefinition, constraintUsage, concernUsage, "
        + "metadataUsage, occurrenceUsage (event), subjectDeclaration, verifyRequirement, endUsage, enumMember and "
        + "stateBodyElement (acceptTransition), and successionUsage takes a KerML succession's first end as its name; "
        + "found by grammar coverage, see GrammarCoverageTest")
    @ParameterizedTest(name = "{2}")
    @MethodSource("splitByShadowedAlternative")
    @DisplayName("Members that need the shadowed alternatives parse as the elements written")
    void testShadowedAlternativeForms(String fileName, String open, String members, String close, int expected) {
        assertBodyElements(fileName, open, members, close, expected);
    }

    /**
     * Parses the members inside open/close inside a package and checks that the innermost body around them, the
     * package body when open is empty, has the expected number of elements and that together they are the members'
     * text. The file name's extension selects SysML v2 or KerML keywords.
     */
    private void assertBodyElements(String fileName, String open, String members, String close, int expected) {
        String source = "package P { " + open + " " + members + " " + close + " }";
        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(source, fileName);
        assertFalse(result.hasErrors(), "Valid SysML v2 should parse: " + result.getSyntaxErrors());

        ParserRuleContext packageBody = findFirst(result.getParseTree(), "packageBody");
        assertNotNull(packageBody, "No package body in " + source);
        ParserRuleContext body = packageBody;
        if (!open.isEmpty()) {
            ParserRuleContext wrapper = (ParserRuleContext) children(packageBody).get(0);
            body = findFirst(wrapper, null);
            assertNotNull(body, "No body in " + open);
        }
        List<ParseTree> elements = children(body);
        StringBuilder text = new StringBuilder();
        List<String> kinds = new ArrayList<>();
        for (ParseTree element : elements) {
            text.append(element.getText());
            kinds.add(describe(element));
        }
        assertEquals(expected, elements.size(), "'" + members + "' should parse as " + expected
            + " element(s), got " + kinds);
        assertEquals(members.replaceAll("\\s", ""), text.toString().replaceAll("\\s", ""));
    }

    /** The rule elements of a body: its children without the brace tokens. */
    private static List<ParseTree> children(ParserRuleContext body) {
        List<ParseTree> result = new ArrayList<>();
        for (int i = 0; i < body.getChildCount(); i++) {
            if (body.getChild(i) instanceof ParserRuleContext) {
                result.add(body.getChild(i));
            }
        }
        return result;
    }

    /** Breadth-first, the first context of the named rule, or with null the first whose rule name ends in Body. */
    private static ParserRuleContext findFirst(ParseTree root, String rule) {
        Deque<ParseTree> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            ParseTree node = pending.removeFirst();
            if (node instanceof ParserRuleContext ctx && ctx != root) {
                String name = SysMLv2Parser.ruleNames[ctx.getRuleIndex()];
                if (rule != null ? name.equals(rule) : name.endsWith("Body")) {
                    return ctx;
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                pending.add(node.getChild(i));
            }
        }
        return null;
    }

    /** The innermost rule that an element of a body is: 'member > constraintUsage' says constraintUsage. */
    private static String describe(ParseTree element) {
        ParseTree node = element;
        while (node instanceof ParserRuleContext ctx && ctx.getChildCount() > 0
                && ctx.getChild(0) instanceof ParserRuleContext && ctx.getChildCount() == 1) {
            node = ctx.getChild(0);
        }
        if (node instanceof ParserRuleContext ctx && ctx.getChild(0) instanceof ParserRuleContext first
                && SysMLv2Parser.ruleNames[first.getRuleIndex()].equals("prefixes")) {
            node = ctx.getChild(1);
        }
        return node instanceof ParserRuleContext ctx ? SysMLv2Parser.ruleNames[ctx.getRuleIndex()] : node.getText();
    }
}
