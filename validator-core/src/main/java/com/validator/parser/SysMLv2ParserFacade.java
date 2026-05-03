package com.validator.parser;

import com.validator.suggestions.SpellingSuggestionEngine;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Facade for parsing SysML v2 files using ANTLR 4.
 * Provides error collection and parse tree generation.
 */
public class SysMLv2ParserFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SysMLv2ParserFacade.class);

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
        private final List<SyntaxError> errors = new ArrayList<>();
        private final SpellingSuggestionEngine suggestionEngine;

        CollectingErrorListener() {
            this.suggestionEngine = new SpellingSuggestionEngine();
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

            errors.add(new SyntaxError(line, charPositionInLine, msg, symbol, suggestions));
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
        CharStream input = CharStreams.fromString(sourceCode);
        SysMLv2Lexer lexer = new SysMLv2Lexer(input);

        // Create error listener
        CollectingErrorListener errorListener = new CollectingErrorListener();

        // Remove default error listeners and add custom one
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        // Create token stream
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Create parser
        SysMLv2Parser parser = new SysMLv2Parser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        // Parse
        long startTime = System.currentTimeMillis();
        ParseTree tree = parser.compilationUnit();
        long parseTime = System.currentTimeMillis() - startTime;

        // Collect line comments from channel 1
        List<LineComment> lineComments = collectLineComments(tokens);

        LOGGER.debug("Parsing completed in {}ms with {} errors, {} line comments",
            parseTime, errorListener.getErrors().size(), lineComments.size());

        return new ParseResult(tree, errorListener.getErrors(), lineComments);
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
