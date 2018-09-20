package pikaparser.parser;

import java.util.Arrays;

import pikaparser.clause.CharSeq;
import pikaparser.clause.CharSet;
import pikaparser.clause.Clause;
import pikaparser.clause.FirstMatch;
import pikaparser.clause.Nothing;
import pikaparser.clause.OneOrMore;
import pikaparser.clause.RuleName;
import pikaparser.clause.Seq;

public class MetaGrammarSimple {

    public static Clause name(String name, Clause clause) {
        return clause.addRuleName(name);
    }

    public static Clause optional(Clause clause) {
        return new FirstMatch(clause, new Nothing());
    }

    public static Clause zeroOrMore(Clause clause) {
        return optional(new OneOrMore(clause));
    }

    public static Clause oneOrMore(Clause clause) {
        return new OneOrMore(clause);
    }

    public static Clause seq(Clause... clause) {
        return new Seq(clause);
    }

    public static Clause first(Clause... clause) {
        return new FirstMatch(clause);
    }

    public static Clause r(String ruleName) {
        return new RuleName(ruleName);
    }

    public static Clause ws = r("WS");

    public static Clause c(char chr) {
        return new CharSet(chr);
    }

    public static Clause str(String str) {
        return new CharSeq(str, false);
    }

    public static final CharSet WHITESPACE = new CharSet(" \n\r\t");

    public static final CharSet LETTER = new CharSet(new CharSet('a', 'z'), new CharSet('A', 'Z'));

    public static final CharSet DIGIT = new CharSet('0', '9');

    public static final CharSet LETTER_OR_DIGIT = new CharSet(LETTER, DIGIT);

    public static Parser newParser(String input) {

        var grammar = new Grammar(Arrays.asList(//
                name("Clause", //
                        r("P5l")),

                name("P5l", //
                        first( //
                                r("P5"), //
                                r("P4l") //
                        )), //

                name("P4l", //
                        first( //
                                r("P4"), //
                                r("P3l") //
                        )), //

                name("P3l", //
                        first( //
                                r("P3"), //
                                r("P2l") //
                        )), //

                name("P2l", //
                        first( //
                                r("P2"), //
                                r("P1l") //
                        )), //

                name("P1l", //
                        first( //
                                r("P1"), //
                                r("P0") //
                        )), //

                name("P5", //
                        seq(r("P5l"), str(" + "), r("P4l"))),

                name("P4", //
                        seq(r("P4l"), str(" - "), r("P3l"))),

                name("P3", //
                        seq(r("P3l"), str(" * "), r("P2l"))),

                name("P2", //
                        seq(r("P2l"), str(" / "), r("P1l"))),

                name("P1", //
                        seq(c('('), r("P5l"), c(')'))),

                name("P0", //
                        LETTER_OR_DIGIT)

        ));

        return new Parser(grammar, input);
    }

}
