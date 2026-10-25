package com.validator.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Writes grammar coverage as report.md, for reading, and report.json, for tools and for comparing runs. A report has
 * one section per corpus (the repository's own files, and the repository's with the official SysML v2 release's
 * when that is available) and lists what the corpus did not exercise, each with the reason from
 * grammar-coverage-notes.properties when there is one.
 */
public final class GrammarCoverageReport {

    /** Test resource with the reasons why rules, alternatives or tokens are not covered. */
    public static final String NOTES_RESOURCE = "/grammar-coverage-notes.properties";

    private final Map<String, String> notes;
    private final Map<String, List<String>> grammarSource;
    private final Map<String, GrammarCoverage> corpora = new LinkedHashMap<>();

    /**
     * @param notes         reasons by key: rule.NAME, alt.NAME#N or token.NAME
     * @param grammarSource the rules' outer alternatives as text, from {@link ParserGrammarSource}; may be empty
     */
    public GrammarCoverageReport(Map<String, String> notes, Map<String, List<String>> grammarSource) {
        this.notes = new TreeMap<>(notes);
        this.grammarSource = grammarSource;
    }

    /** Adds a corpus section; sections appear in the order added. */
    public GrammarCoverageReport add(String corpus, GrammarCoverage coverage) {
        corpora.put(corpus, coverage);
        return this;
    }

    /** Reads grammar-coverage-notes.properties from the test classpath. */
    public static Map<String, String> loadNotes() {
        Properties p = new Properties();
        try (InputStream in = GrammarCoverageReport.class.getResourceAsStream(NOTES_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(NOTES_RESOURCE + " is missing from the test classpath");
            }
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + NOTES_RESOURCE, e);
        }
        Map<String, String> notes = new TreeMap<>();
        for (String key : p.stringPropertyNames()) {
            notes.put(key, p.getProperty(key));
        }
        return notes;
    }

    /** The reason recorded for the key, or null. */
    public String note(String key) {
        return notes.get(key);
    }

    /** The source text of a rule's alternative when the ATN and the source agree on the alternatives, else null. */
    public String alternativeText(GrammarCoverage.Alternative alternative) {
        List<String> alternatives = grammarSource.get(alternative.rule());
        if (alternatives == null || alternatives.size() != alternative.of()) {
            return null;
        }
        return alternatives.get(alternative.alternative() - 1);
    }

    /** Writes report.md and report.json into the directory. */
    public void write(Path directory) {
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("report.md"), markdown(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Files.writeString(directory.resolve("report.json"), mapper.writeValueAsString(json()),
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the grammar coverage report to " + directory, e);
        }
    }

    /** The report as a tree of maps and lists, the content of report.json. */
    public Map<String, Object> json() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("parserRules", GrammarCoverage.RULE_NAMES.size());
        root.put("tokenTypes", GrammarCoverage.tokenNames().size());
        List<String> leftRecursive = new ArrayList<>();
        for (int r = 0; r < GrammarCoverage.RULE_NAMES.size(); r++) {
            if (GrammarCoverage.isLeftRecursive(r)) {
                leftRecursive.add(GrammarCoverage.RULE_NAMES.get(r));
            }
        }
        root.put("leftRecursiveRules", leftRecursive);
        Map<String, Object> sections = new LinkedHashMap<>();
        for (Map.Entry<String, GrammarCoverage> e : corpora.entrySet()) {
            sections.put(e.getKey(), section(e.getValue()));
        }
        root.put("corpora", sections);
        return root;
    }

    private Map<String, Object> section(GrammarCoverage coverage) {
        GrammarCoverage.Summary s = coverage.summary();
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("acceptedFiles", s.acceptedFiles());
        section.put("rejectedFiles", coverage.rejectedFiles());
        section.put("rules", figure(s.rulesCovered(), s.rules(), s.ruleCoverage()));
        section.put("alternatives", figure(s.alternativesCovered(), s.alternatives(), s.alternativeCoverage()));
        section.put("tokens", figure(s.tokensCovered(), s.tokens(), s.tokenCoverage()));
        Reachable r = reachable(coverage);
        Map<String, Object> reachable = new LinkedHashMap<>();
        reachable.put("rules", figure(s.rulesCovered(), r.rules(), ratio(s.rulesCovered(), r.rules())));
        reachable.put("alternatives",
            figure(s.alternativesCovered(), r.alternatives(), ratio(s.alternativesCovered(), r.alternatives())));
        reachable.put("tokens", figure(s.tokensCovered(), r.tokens(), ratio(s.tokensCovered(), r.tokens())));
        section.put("reachable", reachable);
        List<Map<String, Object>> rules = new ArrayList<>();
        for (String rule : coverage.uncoveredRules()) {
            rules.add(item("rule", rule, "reason", note("rule." + rule)));
        }
        section.put("uncoveredRules", rules);
        List<Map<String, Object>> alternatives = new ArrayList<>();
        for (GrammarCoverage.Alternative a : coverage.uncoveredAlternatives()) {
            if (coverage.ruleHits(a.rule()) == 0) {
                continue;
            }
            Map<String, Object> item = item("alternative", a.key(), "reason", note("alt." + a.key()));
            item.put("of", a.of());
            item.put("text", alternativeText(a));
            alternatives.add(item);
        }
        section.put("uncoveredAlternativesOfCoveredRules", alternatives);
        List<Map<String, Object>> tokens = new ArrayList<>();
        for (String token : coverage.unusedTokens()) {
            tokens.add(item("token", token, "reason", note("token." + token)));
        }
        section.put("unusedTokens", tokens);
        List<Map<String, Object>> noted = new ArrayList<>();
        for (String key : notedButCovered(coverage)) {
            noted.add(item("note", key, "reason", notes.get(key)));
        }
        section.put("notedButCovered", noted);
        return section;
    }

    /**
     * The number of rules, alternatives and tokens that valid input can reach as far as the notes say: all of them
     * minus the uncovered ones that a note explains. An alternative of an uncovered rule is explained by the rule's
     * note. Covered over reachable is 100% when every gap is either covered by a snippet or explained.
     */
    public record Reachable(int rules, int alternatives, int tokens) {
    }

    /** See {@link Reachable}. */
    public Reachable reachable(GrammarCoverage coverage) {
        GrammarCoverage.Summary s = coverage.summary();
        int rules = s.rules();
        for (String rule : coverage.uncoveredRules()) {
            if (notes.containsKey("rule." + rule)) {
                rules--;
            }
        }
        int alternatives = s.alternatives();
        for (GrammarCoverage.Alternative a : coverage.uncoveredAlternatives()) {
            boolean ruleExplained = coverage.ruleHits(a.rule()) == 0 && notes.containsKey("rule." + a.rule());
            if (ruleExplained || notes.containsKey("alt." + a.key())) {
                alternatives--;
            }
        }
        int tokens = s.tokens();
        for (String token : coverage.unusedTokens()) {
            if (notes.containsKey("token." + token)) {
                tokens--;
            }
        }
        return new Reachable(rules, alternatives, tokens);
    }

    private static double ratio(int covered, int total) {
        return total == 0 ? 1.0 : (double) covered / total;
    }

    /**
     * Notes on elements that the corpus covers after all: the note is out of date, or, for a 'not SysML' note, a
     * corpus file uses something that is not valid SysML v2.
     */
    public List<String> notedButCovered(GrammarCoverage coverage) {
        List<String> covered = new ArrayList<>();
        for (String key : notes.keySet()) {
            if (isCovered(coverage, key)) {
                covered.add(key);
            }
        }
        return covered;
    }

    private static boolean isCovered(GrammarCoverage coverage, String key) {
        try {
            if (key.startsWith("rule.")) {
                return coverage.ruleHits(key.substring("rule.".length())) > 0;
            }
            if (key.startsWith("alt.") && key.contains("#")) {
                String rule = key.substring("alt.".length(), key.indexOf('#'));
                int alternative = Integer.parseInt(key.substring(key.indexOf('#') + 1));
                return alternative <= coverage.alternativeCount(rule) && coverage.alternativeHits(rule, alternative) > 0;
            }
            if (key.startsWith("token.")) {
                int type = GrammarCoverage.tokenNames().indexOf(key.substring("token.".length())) + 1;
                return type > 0 && coverage.tokenHits(type) > 0;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return false;
    }

    private static Map<String, Object> figure(int covered, int total, double ratio) {
        Map<String, Object> figure = new LinkedHashMap<>();
        figure.put("covered", covered);
        figure.put("total", total);
        figure.put("percent", Math.round(ratio * 1000) / 10.0);
        return figure;
    }

    private static Map<String, Object> item(String key, Object value, String key2, Object value2) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put(key, value);
        item.put(key2, value2);
        return item;
    }

    /** The report as Markdown, the content of report.md. */
    public String markdown() {
        StringBuilder md = new StringBuilder("# SysML v2 grammar coverage\n\n");
        md.append("Parser rules, outer alternatives of rules and lexer token types that the corpus exercises. Only ")
            .append("files without syntax errors count. An alternative is listed only when its rule is covered; the ")
            .append("alternatives of an uncovered rule are uncovered with it. Reasons come from ")
            .append("grammar-coverage-notes.properties.\n\n");
        md.append("| Corpus | Files | Rules | Alternatives | Tokens |\n|---|---|---|---|---|\n");
        for (Map.Entry<String, GrammarCoverage> e : corpora.entrySet()) {
            GrammarCoverage.Summary s = e.getValue().summary();
            md.append("| ").append(e.getKey())
                .append(" | ").append(s.acceptedFiles()).append(" (+").append(s.rejectedFiles()).append(" with errors)")
                .append(" | ").append(percent(s.rulesCovered(), s.rules(), s.ruleCoverage()))
                .append(" | ").append(percent(s.alternativesCovered(), s.alternatives(), s.alternativeCoverage()))
                .append(" | ").append(percent(s.tokensCovered(), s.tokens(), s.tokenCoverage()))
                .append(" |\n");
        }
        md.append("\nCovered of what valid input can reach: the totals without the uncovered elements that a note ")
            .append("explains. Below 100% means a gap that needs a snippet or a note. Both tables measure ")
            .append("SysMLv2Parser.g4 itself: valid SysML v2 that no rule of it accepts is not counted anywhere here ")
            .append("(GrammarGapTest lists the forms found so far).\n\n");
        md.append("| Corpus | Rules | Alternatives | Tokens |\n|---|---|---|---|\n");
        for (Map.Entry<String, GrammarCoverage> e : corpora.entrySet()) {
            GrammarCoverage.Summary s = e.getValue().summary();
            Reachable r = reachable(e.getValue());
            md.append("| ").append(e.getKey())
                .append(" | ").append(percent(s.rulesCovered(), r.rules(), ratio(s.rulesCovered(), r.rules())))
                .append(" | ").append(percent(s.alternativesCovered(), r.alternatives(),
                    ratio(s.alternativesCovered(), r.alternatives())))
                .append(" | ").append(percent(s.tokensCovered(), r.tokens(), ratio(s.tokensCovered(), r.tokens())))
                .append(" |\n");
        }
        for (Map.Entry<String, GrammarCoverage> e : corpora.entrySet()) {
            appendSection(md, e.getKey(), e.getValue());
        }
        return md.toString();
    }

    private void appendSection(StringBuilder md, String corpus, GrammarCoverage coverage) {
        md.append("\n## ").append(corpus).append("\n");
        md.append("\n### Uncovered rules\n\n");
        List<String> rules = coverage.uncoveredRules();
        if (rules.isEmpty()) {
            md.append("None.\n");
        }
        for (String rule : rules) {
            md.append("- `").append(rule).append("`").append(reason(note("rule." + rule))).append('\n');
        }
        md.append("\n### Uncovered alternatives of covered rules\n\n");
        int listed = 0;
        for (GrammarCoverage.Alternative a : coverage.uncoveredAlternatives()) {
            if (coverage.ruleHits(a.rule()) == 0) {
                continue;
            }
            listed++;
            String text = alternativeText(a);
            md.append("- `").append(a.key()).append("` of ").append(a.of());
            if (text != null) {
                md.append(": `").append(text.isEmpty() ? "(empty)" : text).append('`');
            }
            md.append(reason(note("alt." + a.key()))).append('\n');
        }
        if (listed == 0) {
            md.append("None.\n");
        }
        md.append("\n### Unused tokens\n\n");
        List<String> tokens = coverage.unusedTokens();
        if (tokens.isEmpty()) {
            md.append("None.\n");
        }
        for (String token : tokens) {
            md.append("- `").append(token).append('`').append(reason(note("token." + token))).append('\n');
        }
        List<String> noted = notedButCovered(coverage);
        md.append("\n### Covered although a note explains why it is not\n\n");
        md.append("The note is out of date, or a corpus file uses something that is not valid SysML v2.\n\n");
        if (noted.isEmpty()) {
            md.append("None.\n");
        }
        for (String key : noted) {
            md.append("- `").append(key).append('`').append(reason(notes.get(key))).append('\n');
        }
        List<String> rejected = coverage.rejectedFiles();
        md.append("\n### Files left out because they have syntax errors (").append(rejected.size()).append(")\n\n");
        if (rejected.isEmpty()) {
            md.append("None.\n");
        }
        for (String file : rejected) {
            md.append("- ").append(file).append('\n');
        }
    }

    private static String reason(String note) {
        return note == null ? "" : " - " + note;
    }

    private static String percent(int covered, int total, double ratio) {
        return String.format(Locale.ROOT, "%d / %d (%.1f%%)", covered, total, ratio * 100);
    }
}
