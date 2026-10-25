package com.validator.parser;

import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
import com.validator.suggestions.SpellingSuggestionEngine;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Finds misspelled keywords that the grammar accepts as an expression.
 *
 * <p>A misspelled keyword lexes as a name. At the start of a declaration in a definition or usage
 * body, the grammar takes that name for an expression statement, whose ';' it makes optional, and
 * the rest of the declaration for a second element: {@code atribute mass : Real;} parsed without
 * error as the expression {@code atribute} followed by the feature {@code mass : Real;}, and
 * {@code actin serve { ... }} as {@code actin} followed by the feature {@code serve}. In an
 * enumeration body the grammar makes the ';' of a value optional, so {@code eunm Pending;} parsed
 * as the two values {@code eunm} and {@code Pending}. A body never has a bare name followed by
 * another element in SysML v2: an expression without ';' can only be the result expression at the
 * end of a calculation, constraint or function body.</p>
 *
 * <p>The check also runs on a tree that error recovery built. It reports what ANTLR reports for a missing
 * token, "missing ';' at" the token after the name, with the keywords the name may misspell.</p>
 */
final class MisspelledKeywordCheck {

    /**
     * SysML v2 reserved words (SysML 8.2.2.1.2, RESERVED_KEYWORD). A reserved word that the lexer
     * does not have as a keyword token ('const', 'member' and 'crosses' in KerML files of the
     * standard library and the examples) is a construct the grammar does not cover, not a typo.
     */
    private static final Set<String> SYSML_KEYWORDS = Set.of(
        "about", "abstract", "accept", "action", "actor", "after", "alias", "all", "allocate", "allocation",
        "analysis", "and", "as", "assert", "assign", "assume", "at", "attribute", "bind", "binding", "by", "calc",
        "case", "comment", "concern", "connect", "connection", "constant", "constraint", "crosses", "decide",
        "def", "default", "defined", "dependency", "derived", "do", "doc", "else", "end", "entry", "enum",
        "event", "exhibit", "exit", "expose", "false", "filter", "first", "flow", "for", "fork", "frame", "from",
        "hastype", "if", "implies", "import", "in", "include", "individual", "inout", "interface", "istype",
        "item", "join", "language", "library", "locale", "loop", "merge", "message", "meta", "metadata", "new",
        "nonunique", "not", "null", "objective", "occurrence", "of", "or", "ordered", "out", "package", "parallel",
        "part", "perform", "port", "private", "protected", "public", "redefines", "ref", "references", "render",
        "rendering", "rep", "require", "requirement", "return", "satisfy", "send", "snapshot", "specializes",
        "stakeholder", "standard", "state", "subject", "subsets", "succession", "terminate", "then", "timeslice",
        "to", "transition", "true", "until", "use", "variant", "variation", "verification", "verify", "via",
        "view", "viewpoint", "when", "while", "xor");

    private MisspelledKeywordCheck() {
    }

    /**
     * The bare names followed by another element in the bodies of a parse tree.
     *
     * @param tree          a parse tree
     * @param tokens        the tree's tokens
     * @param keywordEngine suggests the keywords a name may misspell
     * @return one error per such name
     */
    static List<SyntaxError> check(ParseTree tree, TokenStream tokens, SpellingSuggestionEngine keywordEngine) {
        List<SyntaxError> errors = new ArrayList<>();
        // Iterative, not recursive: a tree as deep as the parser could build must not overflow here
        Deque<ParseTree> pending = new ArrayDeque<>();
        pending.push(tree);
        while (!pending.isEmpty()) {
            if (!(pending.pop() instanceof ParserRuleContext ctx)) {
                continue;
            }
            // The elements of a body are the children between '{' and '}'. The last one may be a
            // result expression; a bare name followed by another element is a stray word.
            boolean inBody = false;
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);
                if (isToken(child, SysMLv2Lexer.LBRACE) || isToken(child, SysMLv2Lexer.RBRACE)) {
                    inBody = isToken(child, SysMLv2Lexer.LBRACE);
                } else if (inBody && child instanceof ParserRuleContext element
                        && i + 1 < ctx.getChildCount() && ctx.getChild(i + 1) instanceof ParserRuleContext) {
                    Token name = bareName(element, tokens);
                    if (name != null) {
                        errors.add(missingSemicolon(name, tokens, keywordEngine));
                    }
                }
            }
            for (int i = ctx.getChildCount() - 1; i >= 0; i--) {
                pending.push(ctx.getChild(i));
            }
        }
        return errors;
    }

    /**
     * The name, when the element is an expression statement or an enumeration value made of one
     * name without ';' that stands where a declaration starts (after ';', '{' or '}'); otherwise
     * null.
     */
    private static Token bareName(ParserRuleContext element, TokenStream tokens) {
        ParserRuleContext node = element;
        while (!(node instanceof SysMLv2Parser.ExpressionStatementContext
                || node instanceof SysMLv2Parser.EnumMemberContext)) {
            if (node.getChildCount() != 1 || !(node.getChild(0) instanceof ParserRuleContext child)) {
                return null;
            }
            node = child;
        }
        Token start = node.getStart();
        boolean oneName = start == node.getStop() && start.getType() == SysMLv2Lexer.ID
            && !SYSML_KEYWORDS.contains(start.getText()) && !SysMLv2ParserFacade.KERML_KEYWORDS.contains(start.getText());
        return oneName && SysMLv2ParserFacade.startsDeclaration(
            SysMLv2ParserFacade.previousOnChannel(tokens, start.getTokenIndex())) ? start : null;
    }

    private static SyntaxError missingSemicolon(Token name, TokenStream tokens, SpellingSuggestionEngine keywordEngine) {
        // The next element follows, so there is a next token on the default channel
        Token next = tokens.get(name.getTokenIndex() + 1);
        for (int i = name.getTokenIndex() + 2; next.getChannel() != Token.DEFAULT_CHANNEL; i++) {
            next = tokens.get(i);
        }
        String message = "missing ';' at '" + SysMLv2ParserFacade.abbreviate(next.getText()) + "' after '"
            + SysMLv2ParserFacade.abbreviate(name.getText()) + "', which is a name, not a keyword";
        return new SyntaxError(next.getLine(), next.getCharPositionInLine(), message, next.toString(),
            SysMLv2ParserFacade.keywordSuggestions(keywordEngine, name.getText()));
    }

    private static boolean isToken(ParseTree node, int type) {
        return node instanceof TerminalNode terminal && terminal.getSymbol().getType() == type;
    }
}
