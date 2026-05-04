package com.validator.lint;

import com.validator.ValidationWarning;
import com.validator.parser.SysMLv2Lexer;
import com.validator.parser.SysMLv2Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public class RuleTester {
    public static void main(String[] args) throws Exception {
        String code = "package UnitMismatches {\n" +
                      "    attribute badLength: LengthValue = 4.0 [kg];\n" +
                      "}";
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(code));
        SysMLv2Parser parser = new SysMLv2Parser(new CommonTokenStream(lexer));
        ParseTree tree = parser.compilationUnit();

        LintContext ctx = new LintContext(tree, new com.validator.semantic.SymbolTable(), "dummy.sysml", code, new LintConfig(), null);
        
        ValueUnitCorrectnessRule rule = new ValueUnitCorrectnessRule();
        List<ValidationWarning> warnings = rule.analyze(ctx);
        
        System.out.println("Warnings found: " + warnings.size());
        for (ValidationWarning w : warnings) {
            System.out.println(w.getMessage());
        }
    }
}
