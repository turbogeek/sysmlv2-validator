package com.validator.robustness;

import com.validator.parser.SysMLv2Lexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Predicate;

/**
 * Makes reproducible typos and other small mutations of SysML v2 source for the robustness harness.
 *
 * <p>Every mutant is identified by its seed file, its operator and the offset (in chars) where the source was
 * changed, plus a short detail such as the inserted character. The random choices come from a
 * {@link SplittableRandom} seeded from the seed file's name and the operator, so a mutant does not change when
 * files are added to the corpus, and the same mutant is made on every machine.</p>
 *
 * <p>Operators that touch code, not comments or strings, work on the lexer's tokens of the seed, so that "drop a
 * ';'" drops a real semicolon and "misspell a keyword" misspells a real keyword.</p>
 */
final class TypoMutator {

    /** What the validator must say about a mutant, beyond the invariants that hold for every input. */
    enum Expectation {
        /** Nothing beyond the general invariants: the typo may or may not make the source invalid. */
        ANY,
        /** The mutant is invalid SysML whatever the seed was, so at least one ERROR must be reported. */
        ERROR,
        /** Only layout changed: the same syntax errors as the seed. */
        SAME_AS_SEED,
        /** The mutant is valid SysML whatever the seed was: no syntax error. */
        NO_SYNTAX_ERROR
    }

    /** The mutation operators. Whole-file operators ignore the seed and run once per harness run. */
    enum Operator {
        DELETE_CHAR, INSERT_CHAR, TRANSPOSE_CHARS, DELETE_TOKEN, DUPLICATE_TOKEN, MISSPELL_KEYWORD,
        DROP_SEMICOLON, DROP_CLOSING_BRACE, EXTRA_CLOSING_BRACE, UNTERMINATED_STRING, UNTERMINATED_COMMENT,
        SMART_QUOTES, NO_BREAK_SPACE, TAB_FOR_SPACE, ZERO_WIDTH_SPACE, NON_ASCII_IDENTIFIER, CRLF_LINE_ENDINGS,
        CR_LINE_ENDINGS, UTF8_BOM, LONG_IDENTIFIER, LONG_IDENTIFIER_AT_ERROR, LONG_LINE, TRUNCATE,
        EMPTY_FILE(true), WHITESPACE_ONLY(true), DEEP_PACKAGE_NESTING(true), DEEP_PAREN_NESTING(true),
        DEEP_UNBALANCED_BRACES(true);

        private final boolean wholeFile;

        Operator() {
            this(false);
        }

        Operator(boolean wholeFile) {
            this.wholeFile = wholeFile;
        }

        boolean isWholeFile() {
            return wholeFile;
        }
    }

    /**
     * One mutant.
     *
     * @param seed            the seed file's name relative to its corpus, or "synthetic"
     * @param operator        the mutation
     * @param offset          the char offset in the seed where the source was changed
     * @param detail          what was changed, enough to tell two mutants at one offset apart
     * @param source          the mutated source
     * @param expectation     what the validator must report
     * @param sameLines       for SAME_AS_SEED: whether the seed's errors must also be on the same lines
     * @param intendedKeyword for MISSPELL_KEYWORD: the keyword that was misspelled
     * @param misspelling     for MISSPELL_KEYWORD: what it was misspelled as
     */
    record Mutant(String seed, Operator operator, int offset, String detail, String source, Expectation expectation,
                  boolean sameLines, String intendedKeyword, String misspelling) {

        /** The mutant's identity: seed file, operator and offset. */
        String id() {
            return seed + " " + operator + "@" + offset + (detail.isEmpty() ? "" : " " + detail);
        }
    }

    /** Length of the identifiers of LONG_IDENTIFIER and LONG_IDENTIFIER_AT_ERROR. */
    static final int LONG_IDENTIFIER_LENGTH = 100_000;
    /** Nesting depth of DEEP_PACKAGE_NESTING and DEEP_PAREN_NESTING. */
    static final int DEEP_NESTING = 5_000;
    /** Number of unclosed braces of DEEP_UNBALANCED_BRACES. */
    static final int DEEP_UNBALANCED = 1_500;

    private static final String NON_ASCII_LETTERS = "\u00e9\u00fc\u00df\u03a9\u0436\u4e2d";
    private static final String SMART_OPEN = "\u201c";
    private static final String SMART_CLOSE = "\u201d";

    /** A token of the seed with char (not code point) offsets; end is exclusive. */
    record Tok(int type, int channel, int start, int end, String text) {
    }

    private final String seedName;
    private final String source;
    private final List<Tok> code = new ArrayList<>();
    private final List<Tok> lineComments = new ArrayList<>();
    private final long baseSeed;

    /**
     * Prepares mutations of one seed.
     *
     * @param seedName the seed's name, part of every mutant's identity and of its random seed
     * @param source   the seed's text
     * @param baseSeed the harness's base random seed
     */
    TypoMutator(String seedName, String source, long baseSeed) {
        this.seedName = seedName;
        this.source = source;
        this.baseSeed = baseSeed;
        int[] charIndex = charIndexOfCodePoints(source);
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        for (Token t : tokens.getTokens()) {
            if (t.getType() == Token.EOF) {
                continue;
            }
            Tok tok = new Tok(t.getType(), t.getChannel(), charIndex[t.getStartIndex()],
                charIndex[t.getStopIndex() + 1], t.getText());
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                code.add(tok);
            } else if (t.getType() == SysMLv2Lexer.LINE_COMMENT) {
                lineComments.add(tok);
            }
        }
    }

    /** Char index of every code point index (ANTLR's token offsets count code points), plus one past the end. */
    private static int[] charIndexOfCodePoints(String text) {
        int[] index = new int[text.codePointCount(0, text.length()) + 1];
        int cp = 0;
        for (int i = 0; i < text.length(); i += Character.charCount(text.codePointAt(i))) {
            index[cp++] = i;
        }
        index[cp] = text.length();
        return index;
    }

    /** A stable seed for one operator of one seed file (String.hashCode is specified, so it is the same everywhere). */
    private SplittableRandom random(Operator op, int round) {
        long h = baseSeed;
        h = h * 1_000_003L + seedName.hashCode();
        h = h * 1_000_003L + op.name().hashCode();
        h = h * 1_000_003L + round;
        return new SplittableRandom(h);
    }

    /**
     * Makes up to {@code count} distinct mutants of the seed with one operator. Fewer are returned when the seed has
     * fewer places the operator applies to (no strings for UNTERMINATED_STRING, say).
     */
    List<Mutant> mutate(Operator op, int count) {
        List<Mutant> mutants = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int round = 0; round < count * 4 && mutants.size() < count; round++) {
            Mutant m = apply(op, random(op, round));
            if (m != null && seen.add(m.id())) {
                mutants.add(m);
            }
        }
        return mutants;
    }

    private Mutant apply(Operator op, SplittableRandom rnd) {
        return switch (op) {
            case DELETE_CHAR -> deleteChar(rnd);
            case INSERT_CHAR -> insertChar(rnd);
            case TRANSPOSE_CHARS -> transposeChars(rnd);
            case DELETE_TOKEN -> pick(rnd, code, t -> true, t -> replace(op, t.start, t.end, "", quote(t.text),
                Expectation.ANY));
            case DUPLICATE_TOKEN -> pick(rnd, code, t -> true, t -> replace(op, t.end, t.end, " " + t.text,
                quote(t.text), Expectation.ANY));
            case MISSPELL_KEYWORD -> misspellKeyword(rnd);
            // Not a guaranteed error: the grammar makes most terminating semicolons optional
            case DROP_SEMICOLON -> pick(rnd, code, t -> t.type == SysMLv2Lexer.SEMICOLON,
                t -> replace(op, t.start, t.end, "", "", Expectation.ANY));
            case DROP_CLOSING_BRACE -> pick(rnd, code, t -> t.type == SysMLv2Lexer.RBRACE,
                t -> replace(op, t.start, t.end, "", "", Expectation.ERROR));
            case EXTRA_CLOSING_BRACE -> {
                int at = tokenBoundary(rnd);
                yield replace(op, at, at, "}", "", Expectation.ERROR);
            }
            case UNTERMINATED_STRING -> unterminatedString(rnd);
            case UNTERMINATED_COMMENT -> unterminatedComment(rnd);
            case SMART_QUOTES -> smartQuotes(rnd);
            case NO_BREAK_SPACE -> spaceBetweenTokens(rnd, op, "\u00a0", Expectation.ERROR);
            case TAB_FOR_SPACE -> spaceBetweenTokens(rnd, op, "\t", Expectation.SAME_AS_SEED);
            case ZERO_WIDTH_SPACE -> {
                int at = tokenBoundary(rnd);
                yield replace(op, at, at, "\u200b", "", Expectation.ERROR);
            }
            case NON_ASCII_IDENTIFIER -> pick(rnd, code, t -> t.type == SysMLv2Lexer.ID, t -> {
                int at = t.start + rnd.nextInt(t.text.length());
                String letter = String.valueOf(NON_ASCII_LETTERS.charAt(rnd.nextInt(NON_ASCII_LETTERS.length())));
                return replace(op, at, at + 1, letter, quote(t.text) + "->" + letter, Expectation.ERROR);
            });
            case CRLF_LINE_ENDINGS -> wholeSource(op, source.replace("\r\n", "\n").replace("\n", "\r\n"), true);
            case CR_LINE_ENDINGS -> wholeSource(op, source.replace("\r\n", "\n").replace('\n', '\r'), true);
            case UTF8_BOM -> wholeSource(op, "\ufeff" + source, true);
            case LONG_IDENTIFIER -> pick(rnd, code, t -> t.type == SysMLv2Lexer.ID,
                t -> replace(op, t.start, t.end, longIdentifier(t.text), quote(t.text), Expectation.SAME_AS_SEED));
            case LONG_IDENTIFIER_AT_ERROR -> {
                int at = tokenBoundary(rnd);
                yield replace(op, at, at, longIdentifier("Typo") + " ", "", Expectation.ANY);
            }
            case LONG_LINE -> longLine();
            case TRUNCATE -> truncate(rnd);
            case EMPTY_FILE -> synthetic(op, "", Expectation.NO_SYNTAX_ERROR);
            case WHITESPACE_ONLY -> synthetic(op, "  \n\t\r\n   \n", Expectation.NO_SYNTAX_ERROR);
            // Valid, but deeper than the parser's stack goes on a default thread: parsed, or reported as too deep
            case DEEP_PACKAGE_NESTING -> synthetic(op, deepPackages(DEEP_NESTING), Expectation.ANY);
            case DEEP_PAREN_NESTING -> synthetic(op, deepParentheses(DEEP_NESTING), Expectation.ANY);
            // One error per brace, and error recovery slows down with the depth: 5,000 braces took 9 s (five entry
            // points), so fewer; the 5,000-level stack overflow is checked by the valid nestings above
            case DEEP_UNBALANCED_BRACES -> synthetic(op, "package Deep {\n" + "{".repeat(DEEP_UNBALANCED) + "\n",
                Expectation.ERROR);
        };
    }

    // ---------------------------------------------------------------------------------------------- operators

    private Mutant deleteChar(SplittableRandom rnd) {
        if (source.isEmpty()) {
            return null;
        }
        int at = codePointStart(rnd.nextInt(source.length()));
        int end = at + Character.charCount(source.codePointAt(at));
        return replace(Operator.DELETE_CHAR, at, end, "", quote(source.substring(at, end)), Expectation.ANY);
    }

    private Mutant insertChar(SplittableRandom rnd) {
        int at = codePointStart(rnd.nextInt(source.length() + 1));
        String c = String.valueOf((char) (0x20 + rnd.nextInt(0x7f - 0x20)));
        return replace(Operator.INSERT_CHAR, at, at, c, quote(c), Expectation.ANY);
    }

    private Mutant transposeChars(SplittableRandom rnd) {
        if (source.length() < 2) {
            return null;
        }
        int at = rnd.nextInt(source.length() - 1);
        char a = source.charAt(at);
        char b = source.charAt(at + 1);
        if (a == b || Character.isSurrogate(a) || Character.isSurrogate(b)) {
            return null;
        }
        return replace(Operator.TRANSPOSE_CHARS, at, at + 2, "" + b + a, quote("" + a + b), Expectation.ANY);
    }

    /**
     * Misspells a keyword that starts a declaration (after ';', '{', '}' or at the start of the file) and is
     * followed by a name or by 'def'. The misspelled word lexes as a name, so the declaration becomes two names in a
     * row, which SysML never allows: a guaranteed error. The keyword is misspelled at edit distance 1: one letter
     * dropped, two adjacent letters swapped or one letter doubled.
     */
    private Mutant misspellKeyword(SplittableRandom rnd) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            Tok t = code.get(i);
            String word = firstWord(t.text);
            boolean keyword = t.type != SysMLv2Lexer.ID && word.length() >= 3 && word.chars().allMatch(Character::isLetter);
            boolean atStart = i == 0 || Set.of(SysMLv2Lexer.SEMICOLON, SysMLv2Lexer.LBRACE, SysMLv2Lexer.RBRACE)
                .contains(code.get(i - 1).type);
            boolean nameNext = i + 1 < code.size()
                && (code.get(i + 1).type == SysMLv2Lexer.ID || code.get(i + 1).type == SysMLv2Lexer.QUOTED_ID);
            if (keyword && atStart && nameNext) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Tok t = code.get(candidates.get(rnd.nextInt(candidates.size())));
        String word = firstWord(t.text);
        String typo = misspell(word, rnd);
        if (typo == null) {
            return null;
        }
        Mutant m = replace(Operator.MISSPELL_KEYWORD, t.start, t.start + word.length(), typo,
            word + "->" + typo, Expectation.ERROR);
        return new Mutant(m.seed, m.operator, m.offset, m.detail, m.source, m.expectation, false, word, typo);
    }

    /** A misspelling of a keyword at edit distance 1 that lexes as a name, or null. */
    static String misspell(String word, SplittableRandom rnd) {
        int kind = rnd.nextInt(3);
        int i = rnd.nextInt(word.length() - 1);
        String typo = switch (kind) {
            case 0 -> word.substring(0, i + 1) + word.substring(i + 2);                          // atribute
            case 1 -> word.substring(0, i) + word.charAt(i + 1) + word.charAt(i) + word.substring(i + 2); // prat
            default -> word.substring(0, i + 1) + word.charAt(i) + word.substring(i + 1);       // partt
        };
        return !typo.equals(word) && lexesAsName(typo) ? typo : null;
    }

    /** True when the text is one ID token: not a keyword. */
    static boolean lexesAsName(String text) {
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        List<? extends Token> tokens = lexer.getAllTokens();
        return tokens.size() == 1 && tokens.get(0).getType() == SysMLv2Lexer.ID;
    }

    /**
     * Removes the closing quote of a string, or inserts a quote before a token. Either way a string is opened with
     * no closing quote on the rest of its line, which the lexer must reject.
     */
    private Mutant unterminatedString(SplittableRandom rnd) {
        List<Tok> strings = code.stream().filter(t -> t.type == SysMLv2Lexer.STRING).toList();
        if (!strings.isEmpty() && rnd.nextBoolean()) {
            Tok t = strings.get(rnd.nextInt(strings.size()));
            boolean unclosed = restOfLine(t.end).indexOf('"') < 0;
            return replace(Operator.UNTERMINATED_STRING, t.end - 1, t.end, "", "closing quote of " + quote(t.text),
                unclosed ? Expectation.ERROR : Expectation.ANY);
        }
        int at = tokenBoundary(rnd);
        boolean unclosed = restOfLine(at).indexOf('"') < 0;
        return replace(Operator.UNTERMINATED_STRING, at, at, "\"", "", unclosed ? Expectation.ERROR : Expectation.ANY);
    }

    /**
     * Opens a block comment that is never closed: '/*' inserted before a token after the last '*' + '/' of the seed,
     * or the last comment's closing '*' + '/' removed.
     */
    private Mutant unterminatedComment(SplittableRandom rnd) {
        int lastClose = source.lastIndexOf("*/");
        if (lastClose >= 0 && rnd.nextBoolean()) {
            return replace(Operator.UNTERMINATED_COMMENT, lastClose, lastClose + 2, "", "removed closing */",
                Expectation.ERROR);
        }
        List<Integer> boundaries = new ArrayList<>();
        for (Tok t : code) {
            if (t.start >= lastClose + 2) {
                boundaries.add(t.start);
            }
        }
        boundaries.add(source.length());
        int at = boundaries.get(rnd.nextInt(boundaries.size()));
        return replace(Operator.UNTERMINATED_COMMENT, at, at, "/*", "", Expectation.ERROR);
    }

    /** Replaces the quotes of a string with typographic quotes, or inserts one before a token. */
    private Mutant smartQuotes(SplittableRandom rnd) {
        List<Tok> strings = code.stream().filter(t -> t.type == SysMLv2Lexer.STRING).toList();
        if (!strings.isEmpty()) {
            Tok t = strings.get(rnd.nextInt(strings.size()));
            String curly = SMART_OPEN + t.text.substring(1, t.text.length() - 1) + SMART_CLOSE;
            return replace(Operator.SMART_QUOTES, t.start, t.end, curly, quote(t.text), Expectation.ERROR);
        }
        int at = tokenBoundary(rnd);
        return replace(Operator.SMART_QUOTES, at, at, SMART_OPEN, "", Expectation.ERROR);
    }

    /**
     * Replaces a space in code (between two tokens, or inside a compound keyword such as 'part def') with another
     * kind of space. Spaces in comments and strings are left alone, since any character is allowed there.
     */
    private Mutant spaceBetweenTokens(SplittableRandom rnd, Operator op, String replacement, Expectation expectation) {
        List<Integer> spaces = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            Tok t = code.get(i);
            if (t.type != SysMLv2Lexer.STRING && t.type != SysMLv2Lexer.QUOTED_ID) {
                addSpaces(spaces, t.start, t.end);
            }
            if (i + 1 < code.size()) {
                int gapEnd = code.get(i + 1).start;
                if (source.substring(t.end, gapEnd).chars().allMatch(c -> c == ' ' || c == '\t' || c == '\r'
                        || c == '\n')) {
                    addSpaces(spaces, t.end, gapEnd);
                }
            }
        }
        if (spaces.isEmpty()) {
            return null;
        }
        int at = spaces.get(rnd.nextInt(spaces.size()));
        return replace(op, at, at + 1, replacement, "", expectation);
    }

    private void addSpaces(List<Integer> spaces, int from, int to) {
        for (int i = from; i < to; i++) {
            if (source.charAt(i) == ' ') {
                spaces.add(i);
            }
        }
    }

    /** The seed on one line: line comments are blanked out, then every line break becomes a space. */
    private Mutant longLine() {
        StringBuilder text = new StringBuilder(source);
        for (Tok comment : lineComments) {
            for (int i = comment.start; i < comment.end; i++) {
                text.setCharAt(i, ' ');
            }
        }
        String oneLine = text.toString().replace("\r\n", "  ").replace('\n', ' ').replace('\r', ' ');
        return new Mutant(seedName, Operator.LONG_LINE, 0, "", oneLine, Expectation.SAME_AS_SEED, false, null, null);
    }

    /** Cuts the seed off. A guaranteed error when the cut leaves a '{' of the seed without its '}'. */
    private Mutant truncate(SplittableRandom rnd) {
        int at = codePointStart(rnd.nextInt(source.length() + 1));
        int open = 0;
        for (Tok t : code) {
            if (t.end <= at) {
                open += t.type == SysMLv2Lexer.LBRACE ? 1 : t.type == SysMLv2Lexer.RBRACE ? -1 : 0;
            }
        }
        return new Mutant(seedName, Operator.TRUNCATE, at, "", source.substring(0, at),
            open > 0 ? Expectation.ERROR : Expectation.ANY, false, null, null);
    }

    // ---------------------------------------------------------------------------------------------- whole files

    private Mutant synthetic(Operator op, String text, Expectation expectation) {
        return new Mutant("synthetic", op, 0, "", text, expectation, false, null, null);
    }

    /** {@code depth} packages, each nested in the one before. */
    static String deepPackages(int depth) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            text.append("package P").append(i).append(" {\n");
        }
        text.append("part def Innermost;\n");
        text.append("}\n".repeat(depth));
        return text.toString();
    }

    /** An attribute whose value is a literal in {@code depth} pairs of parentheses. */
    static String deepParentheses(int depth) {
        return "package DeepExpression {\n    attribute x = " + "(".repeat(depth) + "1" + ")".repeat(depth) + ";\n}\n";
    }

    private Mutant wholeSource(Operator op, String text, boolean sameLines) {
        return new Mutant(seedName, op, 0, "", text, Expectation.SAME_AS_SEED, sameLines, null, null);
    }

    // ---------------------------------------------------------------------------------------------- helpers

    private Mutant replace(Operator op, int from, int to, String replacement, String detail, Expectation expectation) {
        String mutated = source.substring(0, from) + replacement + source.substring(to);
        return new Mutant(seedName, op, from, detail, mutated, expectation, true, null, null);
    }

    private static <T> Mutant pick(SplittableRandom rnd, List<T> items, Predicate<T> filter,
                                   java.util.function.Function<T, Mutant> mutation) {
        List<T> matching = items.stream().filter(filter).toList();
        return matching.isEmpty() ? null : mutation.apply(matching.get(rnd.nextInt(matching.size())));
    }

    /** The start of a random code token, or the end of the source. */
    private int tokenBoundary(SplittableRandom rnd) {
        int i = rnd.nextInt(code.size() + 1);
        return i < code.size() ? code.get(i).start : source.length();
    }

    private int codePointStart(int at) {
        return at > 0 && at < source.length() && Character.isLowSurrogate(source.charAt(at)) ? at - 1 : at;
    }

    private String restOfLine(int from) {
        int end = from;
        while (end < source.length() && source.charAt(end) != '\n' && source.charAt(end) != '\r') {
            end++;
        }
        return source.substring(from, end);
    }

    private static String firstWord(String text) {
        int i = 0;
        while (i < text.length() && !Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return text.substring(0, i);
    }

    private static String longIdentifier(String stem) {
        StringBuilder id = new StringBuilder(LONG_IDENTIFIER_LENGTH);
        id.append(stem.isEmpty() || !Character.isLetter(stem.charAt(0)) ? "x" : stem);
        while (id.length() < LONG_IDENTIFIER_LENGTH) {
            id.append((char) ('a' + id.length() % 26));
        }
        return id.toString();
    }

    private static String quote(String text) {
        String shown = text.length() > 20 ? text.substring(0, 20) + "..." : text;
        return "'" + shown.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "'";
    }
}
