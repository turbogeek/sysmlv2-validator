package com.validator.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validator.testutil.GrammarCoverage;
import com.validator.testutil.GrammarCoverageReport;
import com.validator.testutil.ParserGrammarSource;
import com.validator.testutil.TestPaths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Grammar coverage of the SysML v2 parser: which rules, outer alternatives of rules and token types the corpus
 * exercises (see {@link GrammarCoverage}), written to target/grammar-coverage/report.md and report.json.
 *
 * Grammar coverage matters because a construct that no test parses can be broken without any test noticing: a
 * keyword that the lexer takes as a name, an alternative that an earlier alternative shadows, a typo that the parser
 * accepts as something else. The corpus is the repository's test-suite and examples and the snippets in
 * src/test/resources/grammar-snippets, each the smallest valid SysML v2 or KerML for something the other files do
 * not exercise; with the OMG SysML-v2-Release checkout ({@link TestPaths#sysmlReleaseRoot()}) a second figure adds
 * its sysml/src models and kerml/src examples. What valid input cannot reach is explained in
 * grammar-coverage-notes.properties. The minimums in grammar-coverage.properties may only go up.
 */
@DisplayName("Grammar coverage")
class GrammarCoverageTest {

    private static final String RATCHET_RESOURCE = "/grammar-coverage.properties";
    private static final Path SNIPPETS = Path.of("src/test/resources/grammar-snippets");
    private static final Path REPORT_DIR = Path.of("target/grammar-coverage");

    private static GrammarCoverage repo;
    private static GrammarCoverage withOfficial;
    private static Map<String, String> notes;
    private static Map<String, List<String>> grammarSource;

    @BeforeAll
    static void measure() {
        Path root = TestPaths.repositoryRoot();
        repo = new GrammarCoverage();
        repo.addTree(root.resolve("test-suite"), "test-suite");
        repo.addTree(root.resolve("examples"), "examples");
        repo.addTree(SNIPPETS, "grammar-snippets");
        notes = GrammarCoverageReport.loadNotes();
        grammarSource = ParserGrammarSource.read(ParserGrammarSource.DEFAULT_GRAMMAR);
        GrammarCoverageReport report = new GrammarCoverageReport(notes, grammarSource);
        report.add("repository", repo);
        Path release = TestPaths.sysmlReleaseRoot().orElse(null);
        if (release != null) {
            withOfficial = repo.copy();
            withOfficial.addTree(release.resolve("sysml/src"), "SysML-v2-Release/sysml/src");
            if (Files.isDirectory(release.resolve("kerml/src"))) {
                withOfficial.addTree(release.resolve("kerml/src"), "SysML-v2-Release/kerml/src");
            }
            report.add("repository + SysML-v2-Release", withOfficial);
        }
        report.write(REPORT_DIR);
        printSummary("repository", repo);
        if (withOfficial != null) {
            printSummary("repository + SysML-v2-Release", withOfficial);
        }
    }

    private static void printSummary(String corpus, GrammarCoverage coverage) {
        GrammarCoverage.Summary s = coverage.summary();
        System.out.printf(Locale.ROOT, "Grammar coverage, %s (%d files, %d with syntax errors left out): rules %.1f%%, "
                + "alternatives %.1f%%, tokens %.1f%%%n", corpus, s.acceptedFiles(), s.rejectedFiles(),
            s.ruleCoverage() * 100, s.alternativeCoverage() * 100, s.tokenCoverage() * 100);
    }

    @Test
    @DisplayName("Every grammar snippet is valid SysML v2 or KerML without syntax errors")
    void testSnippetsParse() throws IOException {
        SysMLv2ParserFacade facade = new SysMLv2ParserFacade();
        List<Path> snippets;
        try (Stream<Path> files = Files.list(SNIPPETS)) {
            snippets = files.filter(p -> p.toString().endsWith(".sysml") || p.toString().endsWith(".kerml"))
                .sorted().toList();
        }
        assertFalse(snippets.isEmpty(), "No snippets in " + SNIPPETS);
        List<String> failures = new ArrayList<>();
        for (Path snippet : snippets) {
            SysMLv2ParserFacade.ParseResult result = facade.parseFile(snippet.toFile());
            if (result.hasErrors()) {
                failures.add(snippet.getFileName() + ": " + result.getSyntaxErrors());
            }
        }
        assertTrue(failures.isEmpty(), "Snippets must parse without syntax errors: " + failures);
        assertTrue(repo.rejectedFiles().stream().noneMatch(f -> f.startsWith("grammar-snippets/")),
            "The coverage counted a snippet as rejected: " + repo.rejectedFiles());
    }

    @Test
    @DisplayName("The ATN's alternative counts agree with the grammar source")
    void testAlternativeCountsMatchSource() {
        assertEquals(GrammarCoverage.RULE_NAMES, List.copyOf(grammarSource.keySet()),
            "The rules read from SysMLv2Parser.g4 should be the parser's rules, in order");
        List<String> mismatches = new ArrayList<>();
        for (String rule : GrammarCoverage.RULE_NAMES) {
            List<String> alternatives = grammarSource.get(rule);
            int counted = repo.alternativeCount(rule);
            boolean tokenSet = counted == 1 && alternatives.stream().allMatch(ParserGrammarSource::isSingleToken);
            if (counted != alternatives.size() && !tokenSet) {
                mismatches.add(rule + ": ATN " + counted + ", source " + alternatives.size());
            }
        }
        assertTrue(mismatches.isEmpty(), "Alternative counts from the ATN disagree with the grammar source, so the "
            + "outer alternative numbers would be misread: " + mismatches);
    }

    @Test
    @DisplayName("Every note names a rule, alternative or token of the grammar")
    void testNotesNameGrammarElements() {
        List<String> unknown = new ArrayList<>();
        for (String key : notes.keySet()) {
            boolean known;
            if (key.startsWith("rule.")) {
                known = GrammarCoverage.RULE_NAMES.contains(key.substring("rule.".length()));
            } else if (key.startsWith("alt.") && key.contains("#")) {
                String rule = key.substring("alt.".length(), key.indexOf('#'));
                int alternative = Integer.parseInt(key.substring(key.indexOf('#') + 1));
                known = GrammarCoverage.RULE_NAMES.contains(rule)
                    && alternative >= 1 && alternative <= repo.alternativeCount(rule);
            } else if (key.startsWith("token.")) {
                known = GrammarCoverage.tokenNames().contains(key.substring("token.".length()));
            } else {
                known = false;
            }
            if (!known) {
                unknown.add(key);
            }
        }
        assertTrue(unknown.isEmpty(), "grammar-coverage-notes.properties names elements the grammar does not have: "
            + unknown);
    }

    @Test
    @DisplayName("The repository corpus meets the coverage minimums")
    void testRepositoryCoverage() {
        assertMeetsRatchet("repo", repo);
    }

    @Test
    @DisplayName("The repository and official corpus meets the coverage minimums")
    void testOfficialCoverage() {
        assumeTrue(withOfficial != null, "SysML-v2-Release not found; set SYSML_RELEASE_PATH");
        assertMeetsRatchet("official", withOfficial);
    }

    @Test
    @DisplayName("The JSON report has the figures of the coverage")
    void testJsonReport() throws IOException {
        JsonNode json = new ObjectMapper().readTree(REPORT_DIR.resolve("report.json").toFile());
        JsonNode section = json.path("corpora").path("repository");
        GrammarCoverage.Summary s = repo.summary();
        assertEquals(s.rulesCovered(), section.path("rules").path("covered").asInt());
        assertEquals(s.alternatives(), section.path("alternatives").path("total").asInt());
        assertEquals(repo.unusedTokens().size(), section.path("unusedTokens").size());
        assertEquals(GrammarCoverage.RULE_NAMES.size(), json.path("parserRules").asInt());
        assertTrue(Files.readString(REPORT_DIR.resolve("report.md"), StandardCharsets.UTF_8)
            .contains("| repository | " + s.acceptedFiles() + " "), "report.md should have the repository row");
    }

    private static void assertMeetsRatchet(String prefix, GrammarCoverage coverage) {
        Properties ratchet = loadRatchet();
        GrammarCoverage.Summary s = coverage.summary();
        List<String> below = new ArrayList<>();
        check(below, prefix + ".rules", s.ruleCoverage(), ratchet);
        check(below, prefix + ".alternatives", s.alternativeCoverage(), ratchet);
        check(below, prefix + ".tokens", s.tokenCoverage(), ratchet);
        assertTrue(below.isEmpty(), "Grammar coverage fell below the minimums in grammar-coverage.properties: "
            + below + "; see " + REPORT_DIR.toAbsolutePath().resolve("report.md"));
    }

    private static void check(List<String> below, String key, double measured, Properties ratchet) {
        String minimum = ratchet.getProperty(key);
        assertTrue(minimum != null, "grammar-coverage.properties has no " + key);
        if (measured < Double.parseDouble(minimum)) {
            below.add(String.format(Locale.ROOT, "%s %.4f < %s", key, measured, minimum));
        }
    }

    private static Properties loadRatchet() {
        Properties p = new Properties();
        try (InputStream in = GrammarCoverageTest.class.getResourceAsStream(RATCHET_RESOURCE)) {
            assertTrue(in != null, RATCHET_RESOURCE + " is missing from the test classpath");
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }
}
