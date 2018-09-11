import java.util.Arrays;

import pikaparser.Parser;
import pikaparser.clause.CharSet;
import pikaparser.clause.FirstMatch;
import pikaparser.clause.Nothing;
import pikaparser.clause.OneOrMore;
import pikaparser.clause.RuleName;
import pikaparser.clause.Seq;

public class Test {

    public static void main(String[] args) {
        var grammar = Arrays.asList(new OneOrMore(new RuleName("stmt")).addRuleName("prog"), //
                new FirstMatch(new OneOrMore(new CharSet(' ')), new Nothing()).addRuleName("ws"), //
                new OneOrMore(new CharSet('a', 'z')).addRuleName("letter"), //
                new OneOrMore(new RuleName("letter")).addRuleName("word"), //
                new OneOrMore(new CharSet('0', '9')).addRuleName("digit"), //
                new OneOrMore(new RuleName("digit")).addRuleName("int"), //
                new Seq(new RuleName("word"), new RuleName("ws"), new CharSet('='), new RuleName("ws"),
                        new RuleName("int"), new RuleName("ws"), new CharSet(';'), new RuleName("ws"))
                                .addRuleName("stmt") //
        );

        var input = "pqr=123;   xyz = 5992 ;z=3;w=y";

        var parser = new Parser(grammar, input);

        System.out.println();

        parser.printParseResult();
    }

}
