package com.validator.testutil;

import com.validator.parser.SysMLv2Lexer;
import com.validator.parser.SysMLv2Parser;
import com.validator.parser.SysMLv2ParserFacade;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContextWithAltNum;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.BasicBlockStartState;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

/**
 * Grammar coverage: which parser rules, which outer alternatives of the rules and which lexer token types a corpus of
 * SysML v2 files exercises. JaCoCo cannot say this, because it excludes the generated parser, and line coverage of
 * generated code would not say it either: one parse method covers every alternative of its rule.
 *
 * Only files without syntax errors count. A file with an error is parsed with error recovery, which enters rules and
 * alternatives that the input does not contain, so it would claim coverage that valid input never showed; such files
 * are listed instead.
 *
 * Rules: a rule is covered when a parse tree of the corpus has a context of the rule.
 *
 * Alternatives: the parser grammar sets contextSuperClass to RuleContextWithAltNum, so every generated context records
 * the outer alternative that the parser chose (ANTLR's enterOuterAlt). The number of outer alternatives of a rule
 * comes from the ATN, see {@link #outerAlternativeCount}. A rule whose alternatives are all single tokens counts as
 * one alternative: ANTLR reduces such a block to a token set, and which token of the set occurred is token coverage.
 *
 * Tokens: every token type that the lexer matched in an accepted file, including the ones it skips (WS,
 * BLOCK_COMMENT) or sends to a hidden channel (LINE_COMMENT). The lexer runs on the raw text, so in a .kerml file a
 * SysML-only keyword counts as that keyword although the parser facade turns it into a name.
 */
public final class GrammarCoverage {

    /** Rule names of the parser, indexed by rule index. */
    public static final List<String> RULE_NAMES = List.of(SysMLv2Parser.ruleNames);

    private static final ATN PARSER_ATN = SysMLv2Parser._ATN;
    private static final Vocabulary VOCABULARY = SysMLv2Lexer.VOCABULARY;

    private final SysMLv2ParserFacade parserFacade = new SysMLv2ParserFacade();
    private final int[] alternativeCounts;
    private final long[] ruleHits;
    private final long[][] alternativeHits;
    private final long[] tokenHits;
    private final List<String> acceptedFiles = new ArrayList<>();
    private final List<String> rejectedFiles = new ArrayList<>();

    /** An empty coverage of the current grammar. */
    public GrammarCoverage() {
        int rules = RULE_NAMES.size();
        alternativeCounts = new int[rules];
        alternativeHits = new long[rules][];
        for (int r = 0; r < rules; r++) {
            alternativeCounts[r] = outerAlternativeCount(PARSER_ATN, r);
            alternativeHits[r] = new long[alternativeCounts[r] + 1];
        }
        ruleHits = new long[rules];
        tokenHits = new long[VOCABULARY.getMaxTokenType() + 1];
    }

    private GrammarCoverage(GrammarCoverage other) {
        alternativeCounts = other.alternativeCounts.clone();
        ruleHits = other.ruleHits.clone();
        alternativeHits = new long[other.alternativeHits.length][];
        for (int r = 0; r < alternativeHits.length; r++) {
            alternativeHits[r] = other.alternativeHits[r].clone();
        }
        tokenHits = other.tokenHits.clone();
        acceptedFiles.addAll(other.acceptedFiles);
        rejectedFiles.addAll(other.rejectedFiles);
    }

    /** A copy that further files can be added to without changing this coverage. */
    public GrammarCoverage copy() {
        return new GrammarCoverage(this);
    }

    /**
     * The number of outer alternatives of a parser rule, from the ATN.
     *
     * The rule's start state has one epsilon transition. When the rule has several outer alternatives, it leads to the
     * rule block's BasicBlockStartState, whose transitions are the alternatives in order and whose block end leads
     * straight to the rule's stop state. Otherwise the rule has one alternative. Two shapes look like a rule block
     * but are not: an optional block that is the whole rule ('r : x? ;') has a bypass transition straight to its
     * block end, which an alternative never has (an empty alternative is a pair of basic states); and a rule whose
     * whole body is one parenthesized block ('r : (a | b) ;') has the same ATN as 'r : a | b ;' although ANTLR
     * records alternative 1 for it, which GrammarCoverageTest rules out by comparing these counts with the grammar
     * source.
     *
     * Left-recursive rules are counted as one alternative. ANTLR rewrites 'e : e op e | primary ;' into one outer
     * alternative, a block of the primary alternatives followed by a loop over the operator alternatives, and records
     * alternative 1 for every context of the rule, so the outer alternative number cannot tell the original
     * alternatives apart. Such a rule is reported as covered or not as a whole; report.json lists them as
     * leftRecursiveRules. The SysMLv2 grammar has none today, which GrammarCoverageCounterTest checks, so that adding
     * one is noticed and its alternatives can be counted from its operator loop if that is wanted.
     */
    public static int outerAlternativeCount(ATN atn, int ruleIndex) {
        RuleStartState start = atn.ruleToStartState[ruleIndex];
        if (start.isLeftRecursiveRule || start.getNumberOfTransitions() != 1) {
            return 1;
        }
        ATNState first = start.transition(0).target;
        if (!(first instanceof BasicBlockStartState block)) {
            return 1;
        }
        ATNState end = block.endState;
        if (end.getNumberOfTransitions() != 1 || end.transition(0).target != atn.ruleToStopState[ruleIndex]) {
            return 1;
        }
        for (Transition t : block.getTransitions()) {
            if (t.target == end) {
                return 1;
            }
        }
        return block.getNumberOfTransitions();
    }

    /** True when ANTLR rewrote the rule as a left-recursive rule. */
    public static boolean isLeftRecursive(int ruleIndex) {
        return PARSER_ATN.ruleToStartState[ruleIndex].isLeftRecursiveRule;
    }

    /**
     * Parses one file with the validator's parser facade and, when it has no syntax errors, records the rules,
     * alternatives and tokens it exercised.
     *
     * @param name   the file name; its extension selects SysML or KerML keywords as in the validator
     * @param source the file's text
     * @return the syntax errors, empty when the file was counted
     */
    public List<SysMLv2ParserFacade.SyntaxError> add(String name, String source) {
        SysMLv2ParserFacade.ParseResult result = parserFacade.parseString(source, name);
        if (result.hasErrors()) {
            rejectedFiles.add(name);
            return result.getSyntaxErrors();
        }
        acceptedFiles.add(name);
        recordTree(result.getParseTree());
        recordTokens(source);
        return List.of();
    }

    /**
     * Adds every .sysml and .kerml file under a directory, in path order so that the file lists are stable.
     *
     * @param root  the directory
     * @param label the prefix of the file names recorded, so that a report shows which corpus a file came from
     */
    public void addTree(Path root, String label) {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".sysml") || p.toString().endsWith(".kerml"))
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list " + root, e);
        }
        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            try {
                add(label + "/" + relative, Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + file, e);
            }
        }
    }

    private void recordTree(ParseTree root) {
        Deque<ParseTree> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            ParseTree node = pending.pop();
            if (node instanceof ErrorNode) {
                continue;
            }
            if (node instanceof ParserRuleContext ctx) {
                int rule = ctx.getRuleIndex();
                ruleHits[rule]++;
                int alt = ctx instanceof RuleContextWithAltNum withAlt ? withAlt.getAltNumber() : 0;
                if (alt < 1 || alt > alternativeCounts[rule]) {
                    throw new IllegalStateException("Rule " + RULE_NAMES.get(rule) + " recorded alternative " + alt
                        + " but the ATN has " + alternativeCounts[rule] + "; is contextSuperClass still "
                        + "RuleContextWithAltNum in SysMLv2Parser.g4?");
                }
                alternativeHits[rule][alt]++;
            }
            for (int i = node.getChildCount() - 1; i >= 0; i--) {
                pending.push(node.getChild(i));
            }
        }
    }

    private void recordTokens(String source) {
        Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.setInterpreter(new RecordingLexerSimulator(lexer, lexer.getInterpreter()));
        Token token;
        do {
            token = lexer.nextToken();
        } while (token.getType() != Token.EOF);
    }

    /**
     * Records the type of every token the lexer matches. The lexer does not emit the tokens it skips, so they are
     * recorded where the simulator returns their type, before the lexer drops them.
     */
    private final class RecordingLexerSimulator extends LexerATNSimulator {
        RecordingLexerSimulator(Lexer lexer, LexerATNSimulator original) {
            super(lexer, original.atn, original.decisionToDFA, new PredictionContextCache());
        }

        @Override
        public int match(CharStream input, int mode) {
            int type = super.match(input, mode);
            if (type > 0 && type < tokenHits.length) {
                tokenHits[type]++;
            }
            return type;
        }
    }

    /** Files counted, in the order they were added. */
    public List<String> acceptedFiles() {
        return List.copyOf(acceptedFiles);
    }

    /** Files left out because they have syntax errors. */
    public List<String> rejectedFiles() {
        return List.copyOf(rejectedFiles);
    }

    /** How often contexts of the rule occurred. */
    public long ruleHits(String rule) {
        return ruleHits[ruleIndex(rule)];
    }

    /** How often the rule's outer alternative (1-based) occurred. */
    public long alternativeHits(String rule, int alternative) {
        return alternativeHits[ruleIndex(rule)][alternative];
    }

    /** The number of outer alternatives of the rule, see {@link #outerAlternativeCount}. */
    public int alternativeCount(String rule) {
        return alternativeCounts[ruleIndex(rule)];
    }

    /** How often the lexer matched the token type. */
    public long tokenHits(int tokenType) {
        return tokenHits[tokenType];
    }

    private static int ruleIndex(String rule) {
        int index = RULE_NAMES.indexOf(rule);
        if (index < 0) {
            throw new IllegalArgumentException("No parser rule " + rule);
        }
        return index;
    }

    /** Parser rules that no accepted file entered. */
    public List<String> uncoveredRules() {
        List<String> result = new ArrayList<>();
        for (int r = 0; r < ruleHits.length; r++) {
            if (ruleHits[r] == 0) {
                result.add(RULE_NAMES.get(r));
            }
        }
        return result;
    }

    /** Outer alternatives that no accepted file chose, as rule name and 1-based alternative number. */
    public List<Alternative> uncoveredAlternatives() {
        List<Alternative> result = new ArrayList<>();
        for (int r = 0; r < alternativeHits.length; r++) {
            for (int a = 1; a <= alternativeCounts[r]; a++) {
                if (alternativeHits[r][a] == 0) {
                    result.add(new Alternative(RULE_NAMES.get(r), a, alternativeCounts[r]));
                }
            }
        }
        return result;
    }

    /** Token types that the lexer never matched in an accepted file, by symbolic name. */
    public List<String> unusedTokens() {
        List<String> result = new ArrayList<>();
        for (int t = 1; t < tokenHits.length; t++) {
            if (tokenHits[t] == 0) {
                result.add(tokenName(t));
            }
        }
        return result;
    }

    /** The token type's symbolic name, such as PART_DEF. */
    public static String tokenName(int tokenType) {
        String name = VOCABULARY.getSymbolicName(tokenType);
        return name != null ? name : VOCABULARY.getDisplayName(tokenType);
    }

    /** All token types of the lexer, by symbolic name. */
    public static List<String> tokenNames() {
        List<String> names = new ArrayList<>();
        for (int t = 1; t <= VOCABULARY.getMaxTokenType(); t++) {
            names.add(tokenName(t));
        }
        return names;
    }

    /** The totals: covered over all rules, alternatives and token types. */
    public Summary summary() {
        int rulesCovered = (int) Arrays.stream(ruleHits).filter(h -> h > 0).count();
        int alternatives = 0;
        int alternativesCovered = 0;
        for (int r = 0; r < alternativeHits.length; r++) {
            for (int a = 1; a <= alternativeCounts[r]; a++) {
                alternatives++;
                if (alternativeHits[r][a] > 0) {
                    alternativesCovered++;
                }
            }
        }
        int tokens = tokenHits.length - 1;
        int tokensCovered = 0;
        for (int t = 1; t < tokenHits.length; t++) {
            if (tokenHits[t] > 0) {
                tokensCovered++;
            }
        }
        return new Summary(acceptedFiles.size(), rejectedFiles.size(), rulesCovered, ruleHits.length,
            alternativesCovered, alternatives, tokensCovered, tokens);
    }

    /** One outer alternative of a parser rule; alternative is 1-based and of is the rule's number of alternatives. */
    public record Alternative(String rule, int alternative, int of) {
        /** The key used in the notes and the reports: rule#alternative. */
        public String key() {
            return rule + "#" + alternative;
        }
    }

    /** Covered over total for rules, outer alternatives and token types, and the files counted and left out. */
    public record Summary(int acceptedFiles, int rejectedFiles, int rulesCovered, int rules,
                          int alternativesCovered, int alternatives, int tokensCovered, int tokens) {
        public double ruleCoverage() {
            return ratio(rulesCovered, rules);
        }

        public double alternativeCoverage() {
            return ratio(alternativesCovered, alternatives);
        }

        public double tokenCoverage() {
            return ratio(tokensCovered, tokens);
        }

        private static double ratio(int covered, int total) {
            return total == 0 ? 1.0 : (double) covered / total;
        }
    }
}
