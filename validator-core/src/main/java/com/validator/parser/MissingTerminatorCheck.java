package com.validator.parser;

import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
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
 * Finds declarations that end without their ';' or body, which the grammar accepts.
 *
 * <p>In SysML v2 every member of a body ends with ';' or with a body in braces, except a comment,
 * documentation, a textual representation and the result expression at the end of a calculation
 * or constraint. The grammar makes the ';' optional in many rules ({@code usageBody?},
 * {@code SEMICOLON?}), so a dropped ';' was accepted: {@code part sensor : Sensor} followed by
 * {@code part processor : Processor;} parsed as two parts, and {@code port input : ~DataPort}
 * before the '}' of its definition as a complete port. The grammar also relies on the optional
 * ';' to accept OMG syntax it does not model, which it splits into elements that end without
 * one ({@code end x crosses y;} as {@code end x}, {@code crosses} and {@code y;}). So an element
 * without its ';' is reported only where no such split can be the reason: before the '}' of its
 * body or the end of the file, or before an element on a later line that starts with a keyword
 * that only ever starts a declaration ({@link #STARTS_DECLARATION}), unless the grammar is known to
 * split a valid declaration there ({@link #maySplitOneDeclaration}).</p>
 *
 * <p>The check runs on trees without syntax errors only, so that the trees error recovery builds
 * do not add errors of their own. It reports what ANTLR reports for a missing token: "missing ';'
 * at" the token after the element.</p>
 */
final class MissingTerminatorCheck {

    /**
     * Keywords that start a declaration and never continue the one before, also not on the next
     * line. Keywords that can continue one ('connect' after 'connection c', 'then', 'first',
     * 'accept', 'do', 'in' after 'for x') are not here.
     */
    private static final Set<Integer> STARTS_DECLARATION = Set.of(
        SysMLv2Lexer.PART_DEF, SysMLv2Lexer.ACTION_DEF, SysMLv2Lexer.STATE_DEF, SysMLv2Lexer.REQUIREMENT_DEF,
        SysMLv2Lexer.USE_CASE_DEF, SysMLv2Lexer.VIEW_DEF, SysMLv2Lexer.VIEWPOINT_DEF, SysMLv2Lexer.CONSTRAINT_DEF,
        SysMLv2Lexer.ATTRIBUTE_DEF, SysMLv2Lexer.ENUM_DEF, SysMLv2Lexer.CONNECTION_DEF, SysMLv2Lexer.INTERFACE_DEF,
        SysMLv2Lexer.ALLOCATION_DEF, SysMLv2Lexer.PORT_DEF, SysMLv2Lexer.ITEM_DEF, SysMLv2Lexer.CALC_DEF,
        SysMLv2Lexer.ANALYSIS_DEF, SysMLv2Lexer.CASE_DEF, SysMLv2Lexer.VERIFICATION_DEF, SysMLv2Lexer.CONCERN_DEF,
        SysMLv2Lexer.RENDERING_DEF, SysMLv2Lexer.OCCURRENCE_DEF, SysMLv2Lexer.FLOW_DEF, SysMLv2Lexer.METADATA_DEF,
        SysMLv2Lexer.INDIVIDUAL_DEF, SysMLv2Lexer.USE_CASE,
        SysMLv2Lexer.PART, SysMLv2Lexer.ACTION, SysMLv2Lexer.STATE, SysMLv2Lexer.REQUIREMENT, SysMLv2Lexer.VIEW,
        SysMLv2Lexer.VIEWPOINT, SysMLv2Lexer.CONSTRAINT, SysMLv2Lexer.ATTRIBUTE, SysMLv2Lexer.ENUM,
        SysMLv2Lexer.CONNECTION, SysMLv2Lexer.INTERFACE, SysMLv2Lexer.ALLOCATION, SysMLv2Lexer.PORT, SysMLv2Lexer.ITEM,
        SysMLv2Lexer.CALC, SysMLv2Lexer.ANALYSIS, SysMLv2Lexer.VERIFICATION, SysMLv2Lexer.CONCERN,
        SysMLv2Lexer.RENDERING, SysMLv2Lexer.OCCURRENCE,
        SysMLv2Lexer.PACKAGE, SysMLv2Lexer.IMPORT, SysMLv2Lexer.ALIAS, SysMLv2Lexer.PUBLIC, SysMLv2Lexer.PRIVATE,
        SysMLv2Lexer.PROTECTED, SysMLv2Lexer.ABSTRACT, SysMLv2Lexer.VARIATION, SysMLv2Lexer.VARIANT, SysMLv2Lexer.REF,
        SysMLv2Lexer.END, SysMLv2Lexer.IN, SysMLv2Lexer.OUT, SysMLv2Lexer.INOUT, SysMLv2Lexer.DOC,
        SysMLv2Lexer.COMMENT_KW, SysMLv2Lexer.SUBJECT, SysMLv2Lexer.ACTOR, SysMLv2Lexer.STAKEHOLDER,
        SysMLv2Lexer.OBJECTIVE, SysMLv2Lexer.EXHIBIT, SysMLv2Lexer.PERFORM, SysMLv2Lexer.SATISFY, SysMLv2Lexer.INCLUDE,
        SysMLv2Lexer.SUCCESSION, SysMLv2Lexer.TRANSITION, SysMLv2Lexer.ENTRY, SysMLv2Lexer.EXIT,
        SysMLv2Lexer.DEPENDENCY, SysMLv2Lexer.ASSERT, SysMLv2Lexer.ASSUME, SysMLv2Lexer.REQUIRE, SysMLv2Lexer.FRAME,
        SysMLv2Lexer.EXPOSE, SysMLv2Lexer.RETURN, SysMLv2Lexer.DERIVED, SysMLv2Lexer.CONSTANT,
        SysMLv2Lexer.INDIVIDUAL, SysMLv2Lexer.SNAPSHOT, SysMLv2Lexer.TIMESLICE, SysMLv2Lexer.MESSAGE,
        SysMLv2Lexer.VERIFY, SysMLv2Lexer.FLOW, SysMLv2Lexer.METADATA, SysMLv2Lexer.LIBRARY,
        SysMLv2Lexer.STANDARD);

    /** First tokens of the members that end without ';': comments, documentation, metadata annotations. */
    private static final Set<Integer> ENDS_WITHOUT_TERMINATOR = Set.of(
        SysMLv2Lexer.DOC, SysMLv2Lexer.COMMENT_KW, SysMLv2Lexer.REP, SysMLv2Lexer.LANGUAGE, SysMLv2Lexer.LOCALE,
        SysMLv2Lexer.AT_SIGN);

    private MissingTerminatorCheck() {
    }

    /**
     * The members of the bodies of a parse tree that end without ';' or a body where that cannot be
     * the grammar splitting syntax it does not model.
     *
     * @param tree   a parse tree without syntax errors
     * @param tokens the tree's tokens
     * @return one error per such member
     */
    static List<SyntaxError> check(ParseTree tree, TokenStream tokens) {
        List<SyntaxError> errors = new ArrayList<>();
        // Iterative, not recursive: a tree as deep as the parser could build must not overflow here
        Deque<ParseTree> pending = new ArrayDeque<>();
        pending.push(tree);
        while (!pending.isEmpty()) {
            if (!(pending.pop() instanceof ParserRuleContext ctx)) {
                continue;
            }
            // The members of a body are the children between '{' and '}'; at the root, all children
            boolean inBody = ctx instanceof SysMLv2Parser.RootNamespaceContext;
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);
                if (isToken(child, SysMLv2Lexer.LBRACE) || isToken(child, SysMLv2Lexer.RBRACE)) {
                    inBody = isToken(child, SysMLv2Lexer.LBRACE);
                } else if (inBody && child instanceof ParserRuleContext member && lacksTerminator(member, tokens)) {
                    Token next = following(ctx, i, tokens);
                    if (next != null) {
                        errors.add(new SyntaxError(next.getLine(), next.getCharPositionInLine(),
                            "missing ';' at '" + SysMLv2ParserFacade.abbreviate(next.getText()) + "'",
                            next.toString()));
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
     * The token after a member without terminator when the ';' cannot be optional there: the '}'
     * that closes the body, the end of the file after a member at the root, or the first token of
     * the next member when that member is on a later line and starts with a declaration keyword.
     * Otherwise null.
     */
    private static Token following(ParserRuleContext body, int index, TokenStream tokens) {
        Token last = ((ParserRuleContext) body.getChild(index)).getStop();
        if (index + 1 == body.getChildCount()) {
            // The last member at the root: the end of the file follows
            Token eof = SysMLv2ParserFacade.nextOnChannel(tokens, last.getTokenIndex());
            return body instanceof SysMLv2Parser.RootNamespaceContext && eof != null ? eof : null;
        }
        ParseTree next = body.getChild(index + 1);
        if (isToken(next, SysMLv2Lexer.RBRACE)) {
            return ((TerminalNode) next).getSymbol();
        }
        if (next instanceof ParserRuleContext member && member.getStart() != null) {
            Token first = member.getStart();
            boolean declarationFollows =
                first.getLine() > last.getLine() && STARTS_DECLARATION.contains(first.getType());
            return declarationFollows && !maySplitOneDeclaration((ParserRuleContext) body.getChild(index), first)
                ? first : null;
        }
        return null;
    }

    /**
     * True when the grammar may have split one valid declaration into the member and the next one,
     * as it does in the OMG release and the standard library on one line: after a keyword it does
     * not model what follows ('succession flow', 'satisfy concern', 'timeslice part', 'snapshot
     * part'), and in a connection end with a name, a multiplicity and a cross feature ('end inCart
     * [0..1] item cart : ShoppingCart;'). Written over two lines, these must stay valid.
     */
    private static boolean maySplitOneDeclaration(ParserRuleContext member, Token next) {
        String literal = SysMLv2Lexer.VOCABULARY.getLiteralName(member.getStop().getType());
        boolean endsWithKeyword = literal != null && literal.length() > 2
            && literal.substring(1, literal.length() - 1).chars().allMatch(c -> Character.isLetter(c) || c == ' ');
        boolean endBeforeCrossFeature = member.getStart().getType() == SysMLv2Lexer.END
            && next.getType() != SysMLv2Lexer.END;
        return endsWithKeyword || endBeforeCrossFeature;
    }

    /**
     * True when a member ends with neither ';' nor '}', and is not a member that may: a comment,
     * documentation or annotation, or an expression (the result expression of a body, or a
     * misspelled keyword, which {@link MisspelledKeywordCheck} reports).
     */
    private static boolean lacksTerminator(ParserRuleContext member, TokenStream tokens) {
        Token first = member.getStart();
        Token last = member.getStop();
        if (first == null || last == null || last.getTokenIndex() < first.getTokenIndex()) {
            return false;
        }
        if (last.getType() == SysMLv2Lexer.SEMICOLON || last.getType() == SysMLv2Lexer.RBRACE) {
            return false;
        }
        ParserRuleContext node = member;
        while (true) {
            if (node instanceof SysMLv2Parser.ExpressionStatementContext
                    || node instanceof SysMLv2Parser.ConstraintExpressionContext
                    || node instanceof SysMLv2Parser.ExpressionContext) {
                return false;
            }
            if (node.getChildCount() != 1 || !(node.getChild(0) instanceof ParserRuleContext child)) {
                break;
            }
            node = child;
        }
        // Visibility may come first: 'private doc /* ... */'
        Token start = first;
        while (start != null && (start.getType() == SysMLv2Lexer.PUBLIC || start.getType() == SysMLv2Lexer.PRIVATE
                || start.getType() == SysMLv2Lexer.PROTECTED)) {
            start = SysMLv2ParserFacade.nextOnChannel(tokens, start.getTokenIndex());
        }
        return start != null && !ENDS_WITHOUT_TERMINATOR.contains(start.getType());
    }

    private static boolean isToken(ParseTree node, int type) {
        return node instanceof TerminalNode terminal && terminal.getSymbol().getType() == type;
    }
}
