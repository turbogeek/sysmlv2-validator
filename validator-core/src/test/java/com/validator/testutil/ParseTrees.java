package com.validator.testutil;

import com.validator.parser.SysMLv2Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks on parse trees for grammar tests.
 *
 * <p>A valid file can parse without syntax errors and still be misread: when an alternative that can end early wins,
 * the rest of the construct is read as a separate expression statement. 'subject v : V { ... }' became 'subject v : V'
 * plus a '{ ... }' expression, and 'event a.b[1] :>> e;' became a bare 'event', the expression 'a.b[1]' and an
 * anonymous redefinition. Such stray statements have no ';' and are followed by another element of the same body.</p>
 */
public final class ParseTrees {

    private ParseTrees() {
    }

    /**
     * Expression statements without ';' that another element of the same body follows, as "line N: text". A result
     * expression is the last element of its body ('calc def F { ... x * 2 }'), so it is not counted.
     */
    public static List<String> strayStatements(ParseTree tree) {
        List<String> stray = new ArrayList<>();
        collect(tree, stray);
        return stray;
    }

    private static void collect(ParseTree tree, List<String> stray) {
        if (tree instanceof SysMLv2Parser.ExpressionStatementContext statement && statement.SEMICOLON() == null) {
            // climb to the body element that holds the statement, then look at its next sibling
            ParseTree element = statement;
            while (element.getParent() != null && element.getParent().getChildCount() == 1) {
                element = element.getParent();
            }
            ParseTree body = element.getParent();
            for (int i = 0; body != null && i < body.getChildCount() - 1; i++) {
                if (body.getChild(i) == element && body.getChild(i + 1) instanceof ParserRuleContext) {
                    stray.add("line " + statement.getStart().getLine() + ": " + statement.getText());
                }
            }
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            collect(tree.getChild(i), stray);
        }
    }
}
