import static pikaparser.clause.Clause.ast;
import static pikaparser.clause.Clause.c;
import static pikaparser.clause.Clause.oneOrMore;
import static pikaparser.clause.Clause.optional;
import static pikaparser.clause.Clause.r;
import static pikaparser.clause.Clause.rule;
import static pikaparser.clause.Clause.seq;

import java.io.IOException;
import java.util.Arrays;

import pikaparser.clause.CharSet;
import pikaparser.grammar.Grammar;
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
            String grammarStr = "a=x:yz";

            // Parser parser = MetaGrammarSimple.newParser(grammarStr);
            Parser parser = new Parser(new Grammar(Arrays.asList( //
                    rule("Rule1", seq(c('a'), c('='), optional(r("Label")), ast("Name", r("Name")))), //
                    rule("Label", seq(ast("Label", r("Name")), c(':'))), //
                    rule("Name", oneOrMore(new CharSet('a', 'z'))) //
            )), grammarStr);

            ParserInfo.printParseResult(parser, "Rule1");
        }
    }

}
