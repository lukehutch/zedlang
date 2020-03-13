import static pikaparser.parser.MetaGrammarSimple.c;
import static pikaparser.parser.MetaGrammarSimple.oneOrMore;
import static pikaparser.parser.MetaGrammarSimple.optional;
import static pikaparser.parser.MetaGrammarSimple.r;
import static pikaparser.parser.MetaGrammarSimple.rule;
import static pikaparser.parser.MetaGrammarSimple.seq;
import static pikaparser.parser.MetaGrammarSimple.str;

import java.io.IOException;
import java.util.Arrays;

import pikaparser.clause.CharSet;
import pikaparser.parser.Grammar;
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
            String grammarStr = "x=^a^c;";

            // Parser parser = MetaGrammarSimple.newParser(grammarStr);
            Parser parser = new Parser(new Grammar(Arrays.asList( //
                    rule("Rule1", seq(str("x="), oneOrMore(r("Rule2")), c(';'))), //
                    rule("Rule2", seq(optional(c('^')), new CharSet('a', 'z'))) //
                    )), grammarStr);

            ParserInfo.printParseResult(parser);
        }
    }

}
