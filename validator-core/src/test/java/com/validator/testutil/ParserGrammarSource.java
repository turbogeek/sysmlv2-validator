package com.validator.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rules of an ANTLR parser grammar file and the text of each rule's outer alternatives, so that a coverage report
 * can show what an uncovered alternative is and a test can check the ATN's alternative counts against the source.
 *
 * This reads the grammar as text, not with the ANTLR tool: it removes comments, finds each rule header (a name
 * starting with a lowercase letter at the start of a line, then ':'), and splits the rule body at the '|' that are
 * outside parentheses, up to the ';' that ends the rule. Quoted literals, actions and argument brackets are skipped
 * as units, so a '|' or ';' inside them does not split anything. Rule options, 'returns' and 'locals' clauses are not
 * supported; the SysMLv2 parser grammar has none.
 */
public final class ParserGrammarSource {

    /** The parser grammar, relative to the validator-core module directory that Maven runs tests in. */
    public static final Path DEFAULT_GRAMMAR = Path.of("src/main/antlr4/com/validator/parser/SysMLv2Parser.g4");

    private static final Pattern RULE_HEADER = Pattern.compile("(?m)^([a-z][A-Za-z0-9_]*)\\s*:");

    private ParserGrammarSource() {
    }

    /** Reads the grammar file; see {@link #parse}. */
    public static Map<String, List<String>> read(Path grammar) {
        try {
            return parse(Files.readString(grammar, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + grammar, e);
        }
    }

    /**
     * The rules of a parser grammar in source order, each with its outer alternatives, whitespace collapsed. An empty
     * alternative is the empty string.
     */
    public static Map<String, List<String>> parse(String grammar) {
        String text = stripComments(grammar);
        Map<String, List<String>> rules = new LinkedHashMap<>();
        Matcher header = RULE_HEADER.matcher(text);
        int from = 0;
        while (header.find(from)) {
            int end = ruleEnd(text, header.end());
            rules.put(header.group(1), splitAlternatives(text.substring(header.end(), end)));
            from = end + 1;
        }
        return rules;
    }

    /** The index of the ';' that ends the rule body starting at from. */
    private static int ruleEnd(String text, int from) {
        int i = from;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == ';') {
                return i;
            }
            i = skipUnit(text, i);
        }
        throw new IllegalArgumentException("Rule body without ';' at offset " + from);
    }

    private static List<String> splitAlternatives(String body) {
        List<String> alternatives = new ArrayList<>();
        int depth = 0;
        int start = 0;
        int i = 0;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == '|' && depth == 0) {
                alternatives.add(normalize(body.substring(start, i)));
                start = i + 1;
            }
            i = skipUnit(body, i);
        }
        alternatives.add(normalize(body.substring(start)));
        return alternatives;
    }

    /** The index after the unit at i: a quoted literal, an action or argument block, or one character. */
    private static int skipUnit(String text, int i) {
        char c = text.charAt(i);
        if (c == '\'') {
            int j = i + 1;
            while (j < text.length() && text.charAt(j) != '\'') {
                j += text.charAt(j) == '\\' ? 2 : 1;
            }
            return j + 1;
        }
        if (c == '{' || c == '[') {
            char close = c == '{' ? '}' : ']';
            int depth = 0;
            for (int j = i; j < text.length(); j++) {
                char d = text.charAt(j);
                if (d == c) {
                    depth++;
                } else if (d == close && --depth == 0) {
                    return j + 1;
                }
            }
            throw new IllegalArgumentException("Unclosed " + c + " at offset " + i);
        }
        return i + 1;
    }

    /** Removes line and block comments, keeping quoted literals that contain comment markers. */
    static String stripComments(String grammar) {
        StringBuilder out = new StringBuilder(grammar.length());
        int i = 0;
        while (i < grammar.length()) {
            char c = grammar.charAt(i);
            if (c == '\'') {
                int end = skipUnit(grammar, i);
                out.append(grammar, i, Math.min(end, grammar.length()));
                i = end;
            } else if (grammar.startsWith("//", i)) {
                int eol = grammar.indexOf('\n', i);
                i = eol < 0 ? grammar.length() : eol;
            } else if (grammar.startsWith("/*", i)) {
                int close = grammar.indexOf("*/", i + 2);
                i = close < 0 ? grammar.length() : close + 2;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String normalize(String alternative) {
        return alternative.trim().replaceAll("\\s+", " ");
    }

    /** True when the alternative is one token reference (an uppercase name or a quoted literal) and nothing else. */
    public static boolean isSingleToken(String alternative) {
        return alternative.matches("[A-Z][A-Za-z0-9_]*") || alternative.matches("'([^'\\\\]|\\\\.)*'");
    }
}
