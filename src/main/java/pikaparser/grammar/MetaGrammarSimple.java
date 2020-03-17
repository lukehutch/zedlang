package pikaparser.grammar;

import java.util.Arrays;

import pikaparser.clause.CharSeq;
import pikaparser.clause.CharSet;
import pikaparser.clause.Clause;
import pikaparser.clause.CreateASTNode;
import pikaparser.clause.FirstMatch;
import pikaparser.clause.Nothing;
import pikaparser.clause.OneOrMore;
import pikaparser.clause.RuleRef;
import pikaparser.clause.Seq;
import pikaparser.parser.Parser;

public class MetaGrammarSimple {

    public static Clause rule(String ruleName, Clause clause) {
        clause.ruleName = ruleName;
        return clause;
    }

    public static Clause ast(String astLabel, Clause clause) {
        return new CreateASTNode(astLabel, clause);
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
        return new RuleRef(ruleName);
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
                rule("Clause", //
                        r("P5l")),

                rule("P5l", //
                        first( //
                                r("P5"), //
                                r("P4l") //
                        )), //

                rule("P4l", //
                        first( //
                                r("P4"), //
                                r("P3l") //
                        )), //

                rule("P3l", //
                        first( //
                                r("P3"), //
                                r("P2l") //
                        )), //

                rule("P2l", //
                        first( //
                                r("P2"), //
                                r("P1l") //
                        )), //

                rule("P1l", //
                        first( //
                                r("P1"), //
                                ast("SYM", r("P0")) //
                        )), //

                rule("P5", //
                        seq(r("P5l"), first(ast("PLUS", str(" + ")), ast("MINUS", str(" - "))), r("P4l"))),

                rule("P4", //
                        seq(r("P4l"), str(" $ "), r("P3l"))),

                rule("P3", //
                        seq(r("P3l"), first(ast("MUL", str(" * ")), ast("DIV", str(" / "))), r("P2l"))),

                rule("P2", //
                        seq(r("P2l"), str(" # "), r("P1l"))),

                rule("P1", //
                        first( //
                                seq(ast("UNARYMINUS", str(" - ")), r("P1l")), // 
                                seq(c('('), r("P5l"), c(')'))) //
                ),

                rule("P0", //
                        LETTER_OR_DIGIT)

        ));

        return new Parser(grammar, input);
    }

}
