package com.validator.parser;

import com.validator.parser.SysMLv2ParserFacade.SyntaxError;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds imports written without a visibility, which the grammar accepts.
 *
 * <p>SysML v2 8.2.2.5.1 and KerML 8.2.3.4.2 both define
 * {@code Import = visibility = VisibilityIndicator 'import' ( isImportAll ?= 'all' )? ImportDeclaration
 * RelationshipBody}, where {@code VisibilityIndicator} is {@code 'public' | 'private' | 'protected'}, and
 * SysML v2 7.5.3 says "The visibility of an import is always shown explicitly by placing the keyword
 * private, protected, or public before the import declaration." So {@code import ScalarValues::*;}
 * is not valid; it must be {@code private import ScalarValues::*;} (or public or protected).</p>
 *
 * <p>The grammar puts an optional visibility in front of every member of a body and again inside
 * {@code importDeclaration}, so a bare import parses. This check reports it the way ANTLR reports a
 * missing token, at the 'import' keyword. The imports inside a filter package ({@code [...]} after an
 * import or expose) and {@code expose} itself take no visibility and are not affected.</p>
 */
final class ImportVisibilityCheck {

    private ImportVisibilityCheck() {
    }

    /**
     * The imports in a parse tree that have no visibility.
     *
     * @param tree a parse tree
     * @return one error per bare import, at its 'import' keyword
     */
    static List<SyntaxError> check(ParseTree tree) {
        List<SyntaxError> errors = new ArrayList<>();
        ParseTreeWalker.DEFAULT.walk(new SysMLv2ParserBaseListener() {
            @Override
            public void enterImportDeclaration(SysMLv2Parser.ImportDeclarationContext ctx) {
                if (ctx.visibility() != null) {
                    return;
                }
                if (ctx.getParent() instanceof SysMLv2Parser.NamespaceBodyElementContext element
                        && element.visibility() != null) {
                    return;
                }
                Token keyword = ctx.IMPORT().getSymbol();
                errors.add(new SyntaxError(keyword.getLine(), keyword.getCharPositionInLine(),
                    "missing visibility ('public', 'private' or 'protected') at 'import'",
                    keyword.toString()));
            }
        }, tree);
        return errors;
    }
}
