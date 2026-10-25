package com.validator.testutil;

import com.validator.parser.SysMLv2Lexer;
import com.validator.parser.SysMLv2Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The counting behind the grammar coverage report: alternative counts from the ATN, what one file records, that
 * files with syntax errors record nothing, and reading alternatives from grammar source.
 */
@DisplayName("Grammar coverage counting")
class GrammarCoverageCounterTest {

    @Test
    @DisplayName("Outer alternatives are counted from the ATN")
    void testAlternativeCounts() {
        GrammarCoverage coverage = new GrammarCoverage();
        assertEquals(1, coverage.alternativeCount("compilationUnit"));
        assertEquals(2, coverage.alternativeCount("packageBody"), "'{ ... }' and ';'");
        assertEquals(3, coverage.alternativeCount("partUsage"));
        assertEquals(20, coverage.alternativeCount("thenStatement"));
        assertEquals(1, coverage.alternativeCount("namespaceBodyElement"),
            "One outer alternative whose inner block chooses the element");
        assertEquals(1, coverage.alternativeCount("visibility"), "Single-token alternatives become one token set");
        for (int r = 0; r < GrammarCoverage.RULE_NAMES.size(); r++) {
            assertFalse(GrammarCoverage.isLeftRecursive(r), "No rule of the SysML v2 grammar is left-recursive");
        }
    }

    @Test
    @DisplayName("A file records its rules, outer alternatives and tokens")
    void testRecordsOneFile() {
        GrammarCoverage coverage = new GrammarCoverage();
        assertTrue(coverage.add("p.sysml", "package P;").isEmpty());

        assertEquals(List.of("p.sysml"), coverage.acceptedFiles());
        assertEquals(1, coverage.ruleHits("packageDeclaration"));
        assertEquals(1, coverage.alternativeHits("packageBody", 2), "';' is the second alternative of packageBody");
        assertEquals(0, coverage.alternativeHits("packageBody", 1));
        assertEquals(0, coverage.ruleHits("partUsage"));
        assertEquals(1, coverage.tokenHits(SysMLv2Lexer.PACKAGE));
        assertEquals(1, coverage.tokenHits(SysMLv2Lexer.SEMICOLON));
        assertTrue(coverage.uncoveredRules().contains("partUsage"));
        assertFalse(coverage.uncoveredRules().contains("packageDeclaration"));
        assertTrue(coverage.uncoveredAlternatives().contains(new GrammarCoverage.Alternative("packageBody", 1, 2)));
        assertTrue(coverage.unusedTokens().contains("PART_DEF"));

        GrammarCoverage.Summary s = coverage.summary();
        assertEquals(1, s.acceptedFiles());
        assertEquals(GrammarCoverage.RULE_NAMES.size(), s.rules());
        assertEquals(SysMLv2Lexer.VOCABULARY.getMaxTokenType(), s.tokens());
        assertEquals(s.rules() - coverage.uncoveredRules().size(), s.rulesCovered());
        assertEquals((double) s.rulesCovered() / s.rules(), s.ruleCoverage(), 1e-9);
    }

    @Test
    @DisplayName("Tokens the lexer skips or hides are counted")
    void testCountsSkippedTokens() {
        GrammarCoverage coverage = new GrammarCoverage();
        assertTrue(coverage.add("c.sysml", "/* block */ package P; // line\n").isEmpty());
        assertEquals(1, coverage.tokenHits(SysMLv2Lexer.BLOCK_COMMENT));
        assertEquals(1, coverage.tokenHits(SysMLv2Lexer.LINE_COMMENT));
        assertTrue(coverage.tokenHits(SysMLv2Lexer.WS) >= 2);
    }

    @Test
    @DisplayName("A file with syntax errors is listed and records nothing")
    void testRejectsFileWithErrors() {
        GrammarCoverage coverage = new GrammarCoverage();
        assertFalse(coverage.add("bad.sysml", "package P { part def }").isEmpty());
        assertEquals(List.of("bad.sysml"), coverage.rejectedFiles());
        assertTrue(coverage.acceptedFiles().isEmpty());
        assertEquals(0, coverage.ruleHits("compilationUnit"));
        assertEquals(0, coverage.tokenHits(SysMLv2Lexer.PACKAGE));
        assertEquals(0, coverage.summary().rulesCovered());
    }

    @Test
    @DisplayName("A copy counts further files without changing the original")
    void testCopyIsIndependent() {
        GrammarCoverage original = new GrammarCoverage();
        original.add("p.sysml", "package P;");
        GrammarCoverage copy = original.copy();
        copy.add("q.sysml", "part def Q;");

        assertEquals(0, original.ruleHits("partDefinition"));
        assertEquals(1, copy.ruleHits("partDefinition"));
        assertEquals(1, copy.ruleHits("packageDeclaration"), "The copy keeps what the original counted");
        assertEquals(List.of("p.sysml"), original.acceptedFiles());
        assertEquals(List.of("p.sysml", "q.sysml"), copy.acceptedFiles());
    }

    @Test
    @DisplayName("The ATN's left-recursion flag is what decides one alternative")
    void testLeftRecursiveFlag() {
        int rule = GrammarCoverage.RULE_NAMES.indexOf("packageBody");
        var start = SysMLv2Parser._ATN.ruleToStartState[rule];
        start.isLeftRecursiveRule = true;
        try {
            assertEquals(1, GrammarCoverage.outerAlternativeCount(SysMLv2Parser._ATN, rule),
                "A left-recursive rule records alternative 1 for every context, so it counts as one");
        } finally {
            start.isLeftRecursiveRule = false;
        }
        assertEquals(2, GrammarCoverage.outerAlternativeCount(SysMLv2Parser._ATN, rule));
    }

    @Test
    @DisplayName("Grammar source is split into outer alternatives outside literals, actions and comments")
    void testParsesGrammarSource() {
        Map<String, List<String>> rules = ParserGrammarSource.parse("""
            parser grammar T;
            options { tokenVocab = L; }
            // a comment with | and ;
            r : a | b c /* | ; */ | ;
            s
                : ( x | y ) z? '|' ';'
                | {act(); | } w
                ;
            """);
        assertEquals(List.of("r", "s"), List.copyOf(rules.keySet()));
        assertEquals(List.of("a", "b c", ""), rules.get("r"));
        assertEquals(List.of("( x | y ) z? '|' ';'", "{act(); | } w"), rules.get("s"));
    }

    @Test
    @DisplayName("Single-token alternatives are recognized")
    void testSingleToken() {
        assertTrue(ParserGrammarSource.isSingleToken("PUBLIC"));
        assertTrue(ParserGrammarSource.isSingleToken("'+'"));
        assertFalse(ParserGrammarSource.isSingleToken("name"));
        assertFalse(ParserGrammarSource.isSingleToken("PUBLIC PRIVATE"));
        assertFalse(ParserGrammarSource.isSingleToken(""));
    }

    @Test
    @DisplayName("The report shows an uncovered alternative's source text and its note")
    void testReportItems() {
        GrammarCoverage coverage = new GrammarCoverage();
        coverage.add("p.sysml", "package P;");
        GrammarCoverageReport report = new GrammarCoverageReport(Map.of("alt.packageBody#1", "a reason"),
            ParserGrammarSource.read(ParserGrammarSource.DEFAULT_GRAMMAR));
        report.add("one file", coverage);

        assertEquals("LBRACE namespaceBodyElement* RBRACE",
            report.alternativeText(new GrammarCoverage.Alternative("packageBody", 1, 2)));
        String markdown = report.markdown();
        assertTrue(markdown.contains("- `packageBody#1` of 2: `LBRACE namespaceBodyElement* RBRACE` - a reason"),
            markdown);
        assertFalse(markdown.contains("`partUsage#1`"), "Alternatives of uncovered rules are not listed one by one");
        assertTrue(markdown.contains("- `partUsage`"), "The uncovered rule is listed");
    }

    @Test
    @DisplayName("Notes take what they explain out of the reachable totals")
    void testReachable() {
        GrammarCoverage coverage = new GrammarCoverage();
        coverage.add("p.sysml", "package P;");
        GrammarCoverageReport report = new GrammarCoverageReport(Map.of(
            "rule.partUsage", "explained", "alt.packageBody#1", "explained", "alt.packageBody#2", "covered anyway",
            "token.PART_DEF", "explained"), Map.of());
        GrammarCoverage.Summary s = coverage.summary();

        GrammarCoverageReport.Reachable reachable = report.reachable(coverage);
        assertEquals(s.rules() - 1, reachable.rules());
        assertEquals(s.alternatives() - 1 - coverage.alternativeCount("partUsage"), reachable.alternatives(),
            "packageBody#1 and the alternatives of the explained rule partUsage");
        assertEquals(s.tokens() - 1, reachable.tokens());
        report.add("one file", coverage);
        assertTrue(report.markdown().contains("Covered of what valid input can reach"));
    }

    @Test
    @DisplayName("Notes on elements the corpus covers are listed as out of date")
    void testNotedButCovered() {
        GrammarCoverage coverage = new GrammarCoverage();
        coverage.add("p.sysml", "package P;");
        GrammarCoverageReport report = new GrammarCoverageReport(Map.of(
            "rule.packageDeclaration", "covered", "rule.partUsage", "not covered",
            "alt.packageBody#2", "covered", "alt.packageBody#1", "not covered",
            "token.PACKAGE", "covered", "token.PART_DEF", "not covered", "rule.noSuchRule", "unknown"), Map.of());
        report.add("one file", coverage);

        assertEquals(List.of("alt.packageBody#2", "rule.packageDeclaration", "token.PACKAGE"),
            report.notedButCovered(coverage));
        assertTrue(report.markdown().contains("- `token.PACKAGE` - covered"));
        assertNull(report.alternativeText(new GrammarCoverage.Alternative("packageBody", 1, 2)),
            "Without grammar source there is no alternative text");
    }
}
