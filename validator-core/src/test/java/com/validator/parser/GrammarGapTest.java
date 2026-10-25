package com.validator.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Valid SysML v2 and KerML that the parser rejects with a syntax error. Grammar coverage (GrammarCoverageTest) measures
 * the rules, alternatives and tokens of SysMLv2Parser.g4, so it cannot see a construct of the OMG BNF that no rule
 * has; its "100% of what valid input can reach" is relative to the grammar. These cases were found by parsing the
 * BNF's productions one at a time; each is valid per SysML-textual-bnf.kebnf or KerML-textual-bnf.kebnf (the
 * production is named with each case). The neighbouring forms that parse today guard the check itself, and the former
 * gaps that the grammar accepts now guard the fixes.
 */
@DisplayName("Valid SysML v2 and KerML the parser rejects")
class GrammarGapTest {

    private SysMLv2ParserFacade parserFacade;

    @BeforeEach
    void setUp() {
        parserFacade = new SysMLv2ParserFacade();
    }

    /**
     * Forms next to the gaps below that parse today, and former gaps that the grammar has a rule for now (unary '+',
     * extents, collect, 'defined by', 'typed by', cross subsetting, filtered imports, 'metadata ... about' and guarded
     * accept transitions), kept here so that they stay valid.
     */
    static Stream<Arguments> acceptedNeighbours() {
        return Stream.of(
            Arguments.of("unary plus (KerML UnaryOperator)", "members.sysml", "package P { attribute a = +1; }"),
            Arguments.of("extent expression (KerML ExtentExpression)", "members.sysml",
                "package P { attribute a = all P; }"),
            Arguments.of("'.{' (KerML CollectExpression)", "members.sysml",
                "package P { attribute a = (1, 2).{ in y; y }; }"),
            Arguments.of("'defined by' (SysML DEFINED_BY)", "members.sysml",
                "package P { part def Q; part p defined by Q; }"),
            Arguments.of("'typed by' (SysML MetadataUsageDeclaration)", "members.sysml",
                "package P { metadata def M { } metadata m typed by M; }"),
            Arguments.of("'typed by' (KerML TYPED_BY)", "members.kerml",
                "package K { classifier T; feature x typed by T; }"),
            Arguments.of("'crosses' (SysML CROSSES)", "members.sysml", "package P { part q; part p crosses q; }"),
            Arguments.of("'=>' (SysML CROSSES)", "members.sysml", "package P { part q; part p => q; }"),
            Arguments.of("filtered import (SysML FilterPackage)", "members.sysml",
                "package P { import Q::*[@M]; }"),
            Arguments.of("'metadata' with 'about' (SysML MetadataUsage)", "members.sysml",
                "package P { metadata def M { } part a; part b; metadata M about a, b; }"),
            Arguments.of("accept with a guard (SysML TargetTransitionUsage)", "members.sysml",
                "package P { state def S { state a; accept e if g then b; state b; } }"),
            Arguments.of("unary minus", "members.sysml", "package P { attribute a = -1; }"),
            Arguments.of("classification", "members.sysml", "package P { attribute a = x istype T; }"),
            Arguments.of("meta cast", "members.sysml", "package P { attribute a = x meta M; }"),
            Arguments.of("sequence", "members.sysml", "package P { attribute a = (1, 2); }"),
            Arguments.of("collect by arrow", "members.sysml", "package P { attribute a = x->collect { in y; y }; }"),
            Arguments.of("typing with ':'", "members.sysml", "package P { part def Q; part p : Q; }"),
            Arguments.of("KerML typing with ':'", "members.kerml", "package K { classifier T; feature x : T; }"),
            Arguments.of("subsetting after a name", "members.sysml", "package P { part q; part p subsets q; }"),
            Arguments.of("redefining usage without a name", "members.sysml", "package P { part p { :>> q; } }"),
            Arguments.of("wildcard import", "members.sysml", "package P { import Q::*; }"),
            Arguments.of("'@' with 'about'", "members.sysml", "package P { metadata def M { } part p { @M about p; } }"),
            Arguments.of("metadata usage typed with ':'", "members.sysml",
                "package P { metadata def M { } metadata m : M; }"),
            Arguments.of("if node with plain bodies", "members.sysml", "package P { action a { if x { } else { } } }"),
            Arguments.of("assert constraint", "members.sysml", "package P { part p { assert constraint c { x > 0 } } }"),
            Arguments.of("transition with a guard", "members.sysml",
                "package P { state def S { state a; transition t first a accept e if g then b; state b; } }"));
    }

    /** Valid forms that are syntax errors today, with the BNF production that makes them valid. */
    static Stream<Arguments> rejectedValidForms() {
        return Stream.of(
            Arguments.of("'@@' (KerML MetaclassificationExpression)", "members.sysml",
                "package P { metadata def M { } attribute x; attribute a = x @@ M; }"),
            Arguments.of("'*' (KerML LiteralInfinity)", "members.sysml", "package P { attribute a = *; }"),
            Arguments.of("trailing comma (KerML SequenceExpressionList)", "members.sysml",
                "package P { attribute a = (1,); }"),
            Arguments.of("subsetting usage without a name (SysML UsageDeclaration)", "members.sysml",
                "package P { part q; part p { :> q; } }"),
            Arguments.of("'subsets' usage without a name (SysML UsageDeclaration)", "members.sysml",
                "package P { part q; part p { subsets q; } }"),
            Arguments.of("'action' bodies of an if node (SysML ActionBodyParameter)", "members.sysml",
                "package P { action a { if x action b { } else action c { } } }"),
            Arguments.of("'assert not constraint' (SysML AssertConstraintUsage)", "members.sysml",
                "package P { part p { assert not constraint c { x > 0 } } }"),
            Arguments.of("guard-only target transition (SysML TargetTransitionUsage)", "members.sysml",
                "package P { state def S { state a; if g then b; state b; } }"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acceptedNeighbours")
    @DisplayName("Neighbouring and fixed forms parse")
    void testAcceptedNeighbours(String description, String fileName, String source) {
        assertParses(fileName, source);
    }

    @Disabled("bug: SysMLv2Parser.g4 has no rule for these valid SysML v2 and KerML forms; see each case's production")
    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedValidForms")
    @DisplayName("Valid forms that the grammar lacks parse")
    void testRejectedValidForms(String description, String fileName, String source) {
        assertParses(fileName, source);
    }

    private void assertParses(String fileName, String source) {
        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(source, fileName);
        assertFalse(result.hasErrors(), "Valid input should parse: " + source + " " + result.getSyntaxErrors());
    }
}
