import java.io.IOException;

import pikaparser.parser.MetaGrammarSimple;
import pikaparser.parser.Parser;
import pikaparser.parser.ParserInfo;

public class Test {

    public static void main(String[] args) throws IOException {
        //        {
        //            var grammar = Arrays.asList(new OneOrMore(new RuleName("stmt")).addRuleName("prog"), //
        //                    new FirstMatch(new OneOrMore(new CharSet(' ')), new Nothing()).addRuleName("ws"), //
        //                    new OneOrMore(new CharSet('a', 'z')).addRuleName("letter"), //
        //                    new OneOrMore(new RuleName("letter")).addRuleName("word"), //
        //                    new OneOrMore(new CharSet('0', '9')).addRuleName("digit"), //
        //                    new OneOrMore(new RuleName("digit")).addRuleName("int"), //
        //                    new Seq(new RuleName("word"), new RuleName("ws"), new CharSet('='), new RuleName("ws"),
        //                            new RuleName("int"), new RuleName("ws"), new CharSet(';'), new RuleName("ws"))
        //                                    .addRuleName("stmt") //
        //            );
        //
        //            var input = "pqr=123;   xyz = 5992 ;z=3;w=y";
        //
        //            var parser = new Parser(grammar, input);
        //
        //            System.out.println();
        //
        //            ParserInfo.printParseResult(parser);
        //        }

        //        {
        //            String grammarStr;
        //            try (var stream = Test.class.getClassLoader().getResource("testGrammar").openStream()) {
        //                grammarStr = new String(stream.readAllBytes());
        //            }
        //            Parser parser = MetaGrammar.newParser(grammarStr);
        //
        //            ParserInfo.printParseResult(parser);
        //        }

        {
            String grammarStr = "x (x y (a b c))";
            Parser parser = MetaGrammarSimple.newParser(grammarStr);

            ParserInfo.printParseResult(parser);
        }
    }

}
