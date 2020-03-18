import static pikaparser.clause.Clause.c;
import static pikaparser.clause.Clause.first;
import static pikaparser.clause.Clause.oneOrMore;
import static pikaparser.clause.Clause.r;
import static pikaparser.clause.Clause.rule;
import static pikaparser.clause.Clause.seq;

import java.io.IOException;
import java.util.Arrays;

import pikaparser.grammar.Grammar;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class Test2 {

    public static void main(String[] args) throws IOException {

        //        L = P | Q+;
        //        Q = L P;
        //        P = 'x' | 'y';

        //        L = P | Q;
        //        Q = P P;
        //        P = 'x';

        {
            String grammarStr = "xxxxx";

            // Parser parser = MetaGrammarSimple.newParser(grammarStr);
            Parser parser = new Parser(new Grammar(Arrays.asList( //
                    rule("L", first(r("P"), oneOrMore(r("Q")))), //
                    rule("Q", seq(r("L"), r("P"))), //
                    rule("P", first(c('x'), c('y'))) //
            )), grammarStr);

            ParserInfo.printParseResult(parser, "L");
        }
    }

}
