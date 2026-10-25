package com.validator.parser;

import com.validator.suggestions.SpellingSuggestionEngine;
import com.validator.suggestions.SuggestionResult;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facade for parsing SysML v2 files using ANTLR 4.
 * Provides error collection and parse tree generation.
 */
public class SysMLv2ParserFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SysMLv2ParserFacade.class);

    /**
     * Deepest nesting of grammar rules in a tree handed on to the tree walkers. They recurse once or
     * more per rule; on a 1 MB stack the model index ran out at 2,900 rules deep (170 nested
     * parentheses, 17 rules each) and the semantic checks and symbol table at 6,000 (2,000 nested
     * packages). The deepest file of the OMG release, the CATIA libraries, test-suite and examples
     * is 136 rules deep.
     */
    static final int MAX_TREE_DEPTH = 1_000;

    /**
     * Bracket nesting taken as the cause of a nesting error. The deepest file of the corpora above
     * nests brackets 10 deep.
     */
    private static final int DEEP_BRACKETS = 50;

    /**
     * Represents a line comment found in the source.
     * Note: Line comments (//) are NOT persisted to the model in SysML v2.
     */
    public static class LineComment {
        private final int line;
        private final int column;
        private final String text;

        public LineComment(int line, int column, String text) {
            this.line = line;
            this.column = column;
            this.text = text;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Parse result containing parse tree and any syntax errors.
     */
    public static class ParseResult {
        private final ParseTree parseTree;
        private final List<SyntaxError> syntaxErrors;
        private final List<LineComment> lineComments;
        private final boolean success;

        public ParseResult(ParseTree parseTree, List<SyntaxError> syntaxErrors,
                          List<LineComment> lineComments) {
            this.parseTree = parseTree;
            this.syntaxErrors = new ArrayList<>(syntaxErrors);
            this.lineComments = new ArrayList<>(lineComments);
            this.success = syntaxErrors.isEmpty();
        }

        public ParseTree getParseTree() {
            return parseTree;
        }

        public List<SyntaxError> getSyntaxErrors() {
            return new ArrayList<>(syntaxErrors);
        }

        public List<LineComment> getLineComments() {
            return new ArrayList<>(lineComments);
        }

        public boolean hasLineComments() {
            return !lineComments.isEmpty();
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean hasErrors() {
            return !syntaxErrors.isEmpty();
        }
    }

    /**
     * Represents a syntax error from the parser.
     */
    public static class SyntaxError {
        private final int line;
        private final int charPositionInLine;
        private final String message;
        private final String offendingSymbol;
        private final List<String> suggestions;

        public SyntaxError(int line, int charPositionInLine, String message, String offendingSymbol) {
            this(line, charPositionInLine, message, offendingSymbol, List.of());
        }

        public SyntaxError(int line, int charPositionInLine, String message,
                          String offendingSymbol, List<String> suggestions) {
            this.line = line;
            this.charPositionInLine = charPositionInLine;
            this.message = message;
            this.offendingSymbol = offendingSymbol;
            this.suggestions = new ArrayList<>(suggestions);
        }

        public int getLine() {
            return line;
        }

        public int getCharPositionInLine() {
            return charPositionInLine;
        }

        public String getMessage() {
            return message;
        }

        public String getOffendingSymbol() {
            return offendingSymbol;
        }

        public List<String> getSuggestions() {
            return new ArrayList<>(suggestions);
        }

        public boolean hasSuggestions() {
            return !suggestions.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Line %d:%d - %s (at '%s')",
                line, charPositionInLine, message, offendingSymbol));
            if (!suggestions.isEmpty()) {
                sb.append(". Did you mean: ").append(String.join(", ", suggestions)).append("?");
            }
            return sb.toString();
        }
    }

    /**
     * Custom error listener to collect syntax errors with spelling suggestions.
     */
    private static class CollectingErrorListener extends BaseErrorListener {
        /** Longest quotation of the input kept whole in an error message. */
        private static final int MAX_QUOTED_INPUT = 60;
        private static final Pattern QUOTED_INPUT_START = Pattern.compile("(input |at: |at )'");
        /** Tokens searched back from an error for the start of its declaration. */
        private static final int MAX_DECLARATION_LOOKBACK = 64;

        private final List<SyntaxError> errors = new ArrayList<>();
        private final SpellingSuggestionEngine suggestionEngine;
        private final SpellingSuggestionEngine keywordEngine;

        CollectingErrorListener() {
            this.suggestionEngine = new SpellingSuggestionEngine();
            this.keywordEngine = SpellingSuggestionEngine.keywordsOnly();
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                              Object offendingSymbol,
                              int line,
                              int charPositionInLine,
                              String msg,
                              RecognitionException e) {
            String symbol = offendingSymbol != null ? offendingSymbol.toString() : "unknown";

            // Extract the actual token text for suggestion lookup
            String tokenText = extractTokenText(symbol);
            List<String> suggestions = List.of();

            // Only suggest for identifier-like tokens (not operators, punctuation, etc.)
            if (isIdentifierLike(tokenText)) {
                suggestions = suggestionEngine.suggestForError(tokenText);
            }

            // A misspelled keyword ('prat def X', 'atribute x : Real') lexes as a name and the error
            // is reported at a later token, so offer the keywords for that name first
            Token misspelled = recognizer instanceof Parser parser && offendingSymbol instanceof Token offending
                ? misspelledKeyword(parser.getInputStream(), offending) : null;
            if (misspelled != null) {
                List<String> merged = new ArrayList<>(keywordSuggestions(keywordEngine, misspelled.getText()));
                suggestions.stream().filter(s -> !merged.contains(s)).forEach(merged::add);
                suggestions = merged;
            }

            errors.add(new SyntaxError(line, charPositionInLine, abbreviateQuotedInput(msg), symbol, suggestions));
        }

        /**
         * ANTLR quotes all the input a failed decision looked at, and the lexer the whole rest of
         * an unterminated string: a 100,000-character name made a 200,000-character message, and
         * 5,000 unclosed braces made messages that grew with every error. Keep the start and the
         * end of a long quoted input. The input is the first quotation after "input " or "at: "
         * (or "at " in a missing-token message) and ends before " expecting" or at the end of the
         * message; it may itself contain quotes, so the expected-token list is left as it is.
         */
        private static String abbreviateQuotedInput(String msg) {
            Matcher start = QUOTED_INPUT_START.matcher(msg);
            if (!start.find()) {
                return msg;
            }
            int open = start.end();
            int close = msg.indexOf("' expecting", open);
            if (close < 0) {
                close = msg.endsWith("'") ? msg.length() - 1 : -1;
            }
            if (close - open <= MAX_QUOTED_INPUT) {
                return msg;
            }
            return msg.substring(0, open + 40) + "..." + msg.substring(close - 17);
        }

        /**
         * The first token of the declaration the error is in (the declaration starts after the
         * nearest ';', '{' or '}'), when it is a name followed by another name: two names in a row
         * never start a declaration, so the first is most likely a misspelled keyword. The search
         * goes back a bounded number of tokens, so that errors in a long run of tokens without a
         * declaration boundary stay cheap.
         */
        private static Token misspelledKeyword(TokenStream tokens, Token offending) {
            if (offending.getTokenIndex() < 0) {
                return null;
            }
            Token first = offending;
            int steps = 0;
            for (Token t = previousOnChannel(tokens, offending.getTokenIndex()); !startsDeclaration(t);
                    t = previousOnChannel(tokens, t.getTokenIndex())) {
                if (++steps > MAX_DECLARATION_LOOKBACK) {
                    return null;
                }
                first = t;
            }
            if (first.getType() != SysMLv2Lexer.ID) {
                return null;
            }
            Token second = nextOnChannel(tokens, first.getTokenIndex());
            return second != null && (second.getType() == SysMLv2Lexer.ID || second.getType() == SysMLv2Lexer.QUOTED_ID)
                ? first : null;
        }

        /**
         * Extracts the actual token text from ANTLR's symbol representation.
         * ANTLR tokens look like "[@5,10:15='identifier',<IDENTIFIER>,1:10]"
         */
        private String extractTokenText(String symbol) {
            if (symbol == null || symbol.isEmpty()) {
                return "";
            }
            // Try to extract text between '=' and ','
            int eqPos = symbol.indexOf('=');
            if (eqPos >= 0) {
                int endPos = symbol.indexOf(',', eqPos);
                if (endPos > eqPos) {
                    String text = symbol.substring(eqPos + 1, endPos);
                    // Remove quotes if present (with length check to avoid index issues)
                    if (text.length() > 2 && text.startsWith("'") && text.endsWith("'")) {
                        return text.substring(1, text.length() - 1);
                    }
                    return text;
                }
            }
            // Fallback: return original
            return symbol;
        }

        /**
         * Checks if the token looks like an identifier (for suggestion purposes).
         */
        private boolean isIdentifierLike(String token) {
            if (token == null || token.isEmpty()) {
                return false;
            }
            // Skip very short tokens and common syntax characters
            if (token.length() < 2) {
                return false;
            }
            // Check for alphanumeric start (identifiers typically start with letter)
            char first = token.charAt(0);
            return Character.isLetter(first) || first == '_';
        }

        public List<SyntaxError> getErrors() {
            return errors;
        }
    }

    /** True when a declaration can start after this token: ';', '{', '}' or the start of the file (null). */
    static boolean startsDeclaration(Token before) {
        return before == null || before.getType() == SysMLv2Lexer.SEMICOLON
            || before.getType() == SysMLv2Lexer.LBRACE || before.getType() == SysMLv2Lexer.RBRACE;
    }

    /** The token before the given index on the default channel, or null at the start. */
    static Token previousOnChannel(TokenStream tokens, int index) {
        for (int i = index - 1; i >= 0; i--) {
            Token t = tokens.get(i);
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                return t;
            }
        }
        return null;
    }

    /** The token after the given index on the default channel (or EOF), or null past the end. */
    static Token nextOnChannel(TokenStream tokens, int index) {
        for (int i = index + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getChannel() == Token.DEFAULT_CHANNEL || t.getType() == Token.EOF) {
                return t;
            }
        }
        return null;
    }

    /** Keywords at most two edits from a word that stands where a keyword is expected. */
    static List<String> keywordSuggestions(SpellingSuggestionEngine keywordEngine, String word) {
        if (word.length() < 2) {
            return List.of();
        }
        return keywordEngine.suggest(word).stream()
            .filter(s -> s.getDistance() <= 2)
            .map(SuggestionResult::getSuggestion)
            .toList();
    }

    /**
     * Parse a SysML v2 file.
     *
     * @param file the file to parse
     * @return parse result with tree and errors
     * @throws IOException if file cannot be read
     */
    public ParseResult parseFile(File file) throws IOException {
        LOGGER.debug("Parsing file: {}", file.getAbsolutePath());
        String content = Files.readString(file.toPath());
        return parseString(content, file.getName());
    }

    /**
     * Parse a SysML v2 string.
     *
     * @param sourceCode the source code to parse
     * @param fileName optional file name for error reporting
     * @return parse result with tree and errors
     */
    public ParseResult parseString(String sourceCode, String fileName) {
        LOGGER.debug("Parsing source: {} ({} characters)", fileName, sourceCode.length());

        // Create lexer
        CharStream input = CharStreams.fromString(normalizeLayout(sourceCode));
        SysMLv2Lexer lexer = new SysMLv2Lexer(input);

        // Create error listener
        CollectingErrorListener errorListener = new CollectingErrorListener();

        // Remove default error listeners and add custom one
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        // Create token stream
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        if (fileName != null && fileName.endsWith(".kerml")) {
            demoteSysMLOnlyKeywords(tokens);
        }

        long startTime = System.currentTimeMillis();
        SysMLv2Parser parser = new SysMLv2Parser(tokens);
        parser.removeErrorListeners();
        ParseTree tree;
        try {
            tree = parse(parser, tokens, errorListener, fileName);
        } catch (StackOverflowError e) {
            // The parser is recursive descent: nesting deep enough (about 400 parentheses or 1,500
            // packages on a 1 MB thread stack) exhausts the stack. Report it instead of letting the
            // Error escape; there is no tree.
            errorListener.getErrors().add(nestingTooDeep(tokens, parser.getCurrentToken()));
            tree = null;
        }
        ParserRuleContext tooDeep = tree != null ? nestedTooDeep(tree) : null;
        if (tooDeep != null) {
            // The walkers that get the tree (index, semantic checks, symbol table, refactoring) are
            // recursive too, and some run out of stack on trees the parser could still build
            errorListener.getErrors().add(nestingTooDeep(tokens, tooDeep.getStart()));
            tree = null;
        }
        if (tree != null) {
            // Misspelled keywords the grammar accepted; one error per position
            List<SyntaxError> errors = errorListener.getErrors();
            for (SyntaxError misspelled : MisspelledKeywordCheck.check(tree, tokens, errorListener.keywordEngine)) {
                if (errors.stream().noneMatch(e -> e.getLine() == misspelled.getLine()
                        && e.getCharPositionInLine() == misspelled.getCharPositionInLine())) {
                    errors.add(misspelled);
                }
            }
        }
        long parseTime = System.currentTimeMillis() - startTime;

        // Collect line comments from channel 1
        List<LineComment> lineComments = collectLineComments(tokens);

        LOGGER.debug("Parsing completed in {}ms with {} errors, {} line comments",
            parseTime, errorListener.getErrors().size(), lineComments.size());

        return new ParseResult(tree, errorListener.getErrors(), lineComments);
    }

    /**
     * Two-stage parse (ANTLR 4 recommended practice): try the fast SLL prediction mode with a
     * bail-out error strategy; only if that fails, re-parse with full LL prediction and normal
     * error recovery. Valid input gets the same tree; invalid input gets the same errors.
     */
    private static ParseTree parse(SysMLv2Parser parser, CommonTokenStream tokens,
                                   CollectingErrorListener errorListener, String fileName) {
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new BailErrorStrategy());
        try {
            return parser.compilationUnit();
        } catch (ParseCancellationException e) {
            LOGGER.debug("SLL parse of {} failed; re-parsing with full LL prediction", fileName);
        }
        tokens.seek(0);
        parser.reset();
        parser.addErrorListener(errorListener);
        parser.setErrorHandler(new DefaultErrorStrategy());
        parser.getInterpreter().setPredictionMode(PredictionMode.LL);
        return parser.compilationUnit();
    }

    /**
     * Layout that the lexer would get wrong, replaced one character for one so that offsets into
     * the source do not change. A UTF-8 byte order mark at the start (some editors write one and
     * Files.readString keeps it) is not whitespace to the lexer, which reported a token
     * recognition error on every such file; it becomes a space. A lone CR (old Mac line ends) is
     * skipped as whitespace but ANTLR counts only LF as a line break, so every error and warning
     * in such a file was reported on line 1; it becomes a LF.
     */
    static String normalizeLayout(String source) {
        boolean bom = !source.isEmpty() && source.charAt(0) == '\uFEFF';
        int cr = source.indexOf('\r');
        if (!bom && cr < 0) {
            return source;
        }
        char[] chars = source.toCharArray();
        if (bom) {
            chars[0] = ' ';
        }
        for (int i = Math.max(cr, 0); i < chars.length; i++) {
            if (chars[i] == '\r' && (i + 1 == chars.length || chars[i + 1] != '\n')) {
                chars[i] = '\n';
            }
        }
        return new String(chars);
    }

    /**
     * The first rule, in source order, nested more than {@link #MAX_TREE_DEPTH} rules deep, or null.
     * Iterative, so that it cannot run out of stack itself.
     */
    private static ParserRuleContext nestedTooDeep(ParseTree tree) {
        Deque<ParseTree> pending = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        pending.push(tree);
        depths.push(1);
        while (!pending.isEmpty()) {
            ParseTree node = pending.pop();
            int depth = depths.pop();
            if (!(node instanceof ParserRuleContext ctx)) {
                continue;
            }
            if (depth > MAX_TREE_DEPTH) {
                return ctx;
            }
            for (int i = ctx.getChildCount() - 1; i >= 0; i--) {
                pending.push(ctx.getChild(i));
                depths.push(depth + 1);
            }
        }
        return null;
    }

    /**
     * The error for a source nested too deep: the parser ran out of stack, or its tree is deeper
     * than {@link #MAX_TREE_DEPTH}. Which of the two happens, and where the stack ran out, depend
     * on the thread's stack size and on the JIT, so the error is placed by the source alone: where
     * the brackets first reach their deepest nesting when they are nested {@link #DEEP_BRACKETS}
     * deep or more; otherwise, as for {@code not not ... true}, at the start of the declaration
     * the nesting is in.
     */
    private static SyntaxError nestingTooDeep(CommonTokenStream tokens, Token inside) {
        tokens.fill();
        int depth = 0;
        int deepest = 0;
        Token deepestBracket = null;
        for (Token t : tokens.getTokens()) {
            int type = t.getType();
            if (type == SysMLv2Lexer.LBRACE || type == SysMLv2Lexer.LPAREN || type == SysMLv2Lexer.LBRACK) {
                if (++depth > deepest) {
                    deepest = depth;
                    deepestBracket = t;
                }
            } else if (type == SysMLv2Lexer.RBRACE || type == SysMLv2Lexer.RPAREN || type == SysMLv2Lexer.RBRACK) {
                depth--;
            }
        }
        if (deepest >= DEEP_BRACKETS) {
            return new SyntaxError(deepestBracket.getLine(), deepestBracket.getCharPositionInLine(),
                "nesting too deep to parse: brackets nested " + deepest + " deep", deepestBracket.toString());
        }
        Token start = inside;
        for (Token t = previousOnChannel(tokens, inside.getTokenIndex()); !startsDeclaration(t);
                t = previousOnChannel(tokens, t.getTokenIndex())) {
            start = t;
        }
        return new SyntaxError(start.getLine(), start.getCharPositionInLine(),
            "nesting too deep to parse at '" + abbreviate(start.getText()) + "'", start.toString());
    }

    static String abbreviate(String text) {
        return text.length() > 40 ? text.substring(0, 40) + "..." : text;
    }

    /**
     * KerML reserved words (KerML 8.2.2.1.2). The lexer is shared with SysML, whose additional
     * reserved words ('part', 'state', 'entry', 'frame', 'accept', ...) are ordinary names in a
     * .kerml file; the standard library's kernel files rely on that.
     */
    static final Set<String> KERML_KEYWORDS = Set.of(
        "about", "abstract", "alias", "all", "and", "as", "assoc", "behavior", "binding", "bool", "by",
        "chains", "class", "classifier", "comment", "composite", "conjugate", "conjugates", "conjugation",
        "connector", "const", "crosses", "datatype", "default", "dependency", "derived", "differences",
        "disjoining", "disjoint", "doc", "else", "end", "expr", "false", "feature", "featured", "featuring",
        "filter", "first", "flow", "for", "from", "function", "hastype", "if", "implies", "import", "in",
        "inout", "interaction", "intersects", "inv", "inverse", "inverting", "istype", "language", "library",
        "locale", "loop", "member", "meta", "metaclass", "metadata", "multiplicity", "namespace", "new",
        "nonunique", "not", "null", "of", "or", "ordered", "out", "package", "portion", "predicate", "private",
        "protected", "public", "redefines", "redefinition", "references", "rep", "return", "specialization",
        "specializes", "standard", "step", "struct", "subclassifier", "subset", "subsets", "subtype",
        "succession", "then", "to", "true", "type", "typed", "typing", "unions", "var", "xor");

    /**
     * In a .kerml file, turn keyword tokens that KerML does not reserve into identifiers.
     */
    private static void demoteSysMLOnlyKeywords(CommonTokenStream tokens) {
        tokens.fill();
        Vocabulary vocabulary = SysMLv2Lexer.VOCABULARY;
        for (Token token : tokens.getTokens()) {
            if (token.getType() == Token.EOF || token.getType() == SysMLv2Lexer.ID) {
                continue;
            }
            String literal = vocabulary.getLiteralName(token.getType());
            if (literal == null || literal.length() < 3) {
                continue;
            }
            String word = literal.substring(1, literal.length() - 1);
            if (word.chars().allMatch(Character::isLetter) && !KERML_KEYWORDS.contains(word)
                    && token instanceof CommonToken commonToken) {
                commonToken.setType(SysMLv2Lexer.ID);
            }
        }
    }

    /**
     * Collect line comments from the hidden channel.
     * Line comments are on channel 1 (not the default channel 0).
     */
    private List<LineComment> collectLineComments(CommonTokenStream tokens) {
        List<LineComment> comments = new ArrayList<>();

        // Fill the token stream to ensure all tokens are available
        tokens.fill();

        // Get all tokens from channel 1 (LINE_COMMENT channel)
        for (Token token : tokens.getTokens()) {
            if (token.getChannel() == 1 && token.getType() == SysMLv2Lexer.LINE_COMMENT) {
                comments.add(new LineComment(
                    token.getLine(),
                    token.getCharPositionInLine(),
                    token.getText()
                ));
            }
        }

        return comments;
    }

    /**
     * Get a string representation of the parse tree.
     *
     * @param tree the parse tree
     * @param parser the parser (for rule names)
     * @return tree string
     */
    public String getTreeString(ParseTree tree, SysMLv2Parser parser) {
        return tree.toStringTree(parser);
    }

    /**
     * Extracts the original text for a parse tree node from the character stream.
     * This preserves whitespace which is normally lost because the lexer skips it.
     *
     * @param ctx the parse tree node
     * @return the original text including skipped whitespace
     */
    public static String getOriginalText(ParseTree ctx) {
        if (ctx instanceof org.antlr.v4.runtime.ParserRuleContext) {
            org.antlr.v4.runtime.ParserRuleContext ruleCtx = (org.antlr.v4.runtime.ParserRuleContext) ctx;
            Token start = ruleCtx.getStart();
            Token stop = ruleCtx.getStop();
            
            if (start != null && stop != null) {
                int startIndex = start.getStartIndex();
                int stopIndex = stop.getStopIndex();
                CharStream input = start.getInputStream();
                
                if (startIndex >= 0 && stopIndex >= startIndex && input != null) {
                    return input.getText(org.antlr.v4.runtime.misc.Interval.of(startIndex, stopIndex));
                }
            }
        } else if (ctx instanceof org.antlr.v4.runtime.tree.TerminalNode) {
            Token symbol = ((org.antlr.v4.runtime.tree.TerminalNode) ctx).getSymbol();
            if (symbol != null) {
                return symbol.getText();
            }
        }
        
        return ctx.getText();
    }

    /**
     * Check if a file is a SysML v2 file based on extension.
     *
     * @param file the file to check
     * @return true if .sysml extension
     */
    public static boolean isSysMLFile(File file) {
        return file.getName().toLowerCase().endsWith(".sysml");
    }

    /**
     * Check if a file is a KerML file based on extension.
     *
     * @param file the file to check
     * @return true if .kerml extension
     */
    public static boolean isKerMLFile(File file) {
        return file.getName().toLowerCase().endsWith(".kerml");
    }

    /**
     * Check if a file is a KPAR library archive based on extension.
     *
     * @param file the file to check
     * @return true if .kpar extension
     */
    public static boolean isKparFile(File file) {
        return file.getName().toLowerCase().endsWith(".kpar");
    }

    /**
     * Check if a file is any supported SysML v2 file type.
     *
     * @param file the file to check
     * @return true if supported file type (.sysml, .kerml, .kpar)
     */
    public static boolean isSupportedFile(File file) {
        return isSysMLFile(file) || isKerMLFile(file) || isKparFile(file);
    }
}
