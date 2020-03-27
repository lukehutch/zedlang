import java.io.IOException;

import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class Test {

    public static void main(String[] args) throws IOException {

        //        String input;
        //        try (var stream = Test.class.getClassLoader().getResource("testGrammar").openStream()) {
        //            input = new String(stream.readAllBytes());
        //        }
        //
        //        // String input = " L = P | Q+; Q = L P; P = 'x' | 'y'; ";
        //
        //        // String input = "L = P Q+;";
        //
        //        Parser parser = MetaGrammar.newParser(input);
        //
        //        // Parser parser = MetaGrammarSimple.newParser("x x+");
        //
        //        ParserInfo.printParseResult(parser);

        {
            // String grammarStr = "a (b c (d e f))";
            // String grammarStr = "a + b + c - d - e + f + g - h - i + j - k";
            // String grammarStr = "a + b + c * d - e - ((x + y) + (p + f) + g) + h - i / j / k";
            // String grammarStr = "a + b - c + d - e -  - x - y";
            // String grammarStr = "a=x:yz";
            
            //String grammarStr = "1*2+3*(4+-5)*6";
            String grammarStr = "1+2+3*4+4*5/(3+((4))*--7)+5";

            Parser parser = new Parser(PrecedenceGrammar.grammar);
            parser.parse(grammarStr);
            //            Parser parser = new Parser(new Grammar(Arrays.asList( //
            //                    rule("Rule1", seq(c('a'), c('='), optional(r("Label")), ast("Name", r("Name")))), //
            //                    rule("Label", seq(ast("Label", r("Name")), c(':'))), //
            //                    rule("Name", oneOrMore(new CharSet('a', 'z'))) //
            //            )));
            //            parser.parse(grammarStr);

            ParserInfo.printParseResult(parser, "Expr", false);
        }
    }

}
