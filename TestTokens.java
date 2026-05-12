import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import com.validator.parser.*;

public class TestTokens {
    public static void main(String[] args) throws Exception {
        String input = "part vehicle_a:Vehicle { port vehicleToRoadPort redefines vehicleToRoadPort { port wheelToRoadPort1:WheelToRoadPort; } perform ActionTree::providePower redefines providePower; }";
        SysMLv2Lexer lexer = new SysMLv2Lexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SysMLv2Parser parser = new SysMLv2Parser(tokens);
        
        try {
            ParseTree tree = parser.partUsage();
            System.out.println(tree.toStringTree(parser));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
