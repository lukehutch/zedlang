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

    public static Clause text(String str) {
        return new CharSeq(str, false);
    }

    public static final CharSet WHITESPACE = new CharSet(" \n\r\t");

    public static final CharSet LETTER = new CharSet(new CharSet('a', 'z'), new CharSet('A', 'Z'));

    public static final CharSet DIGIT = new CharSet('0', '9');

    public static Parser newParser(String input) {
        //        return new Parser(Arrays.asList(//
        //                name("Grammar", //
        //                        seq(ws, oneOrMore(r("Rule")))), //
        //
        //                name("WS", //
        //                        optional(oneOrMore(WHITESPACE))), //
        //
        //                name("Rule", //
        //                        seq(r("RuleIdent"), ws, c('='), ws, r("Clause"), ws, c(';'), ws)), //
        //
        //                name("Clause", //
        //                        first( //
        //                                r("Seq"), //
        //                                r("RuleName"), //
        //                                DIGIT)), //
        //
        ////                name("Clause", //
        ////                        first( //
        ////                                r("Nothing"), //
        ////                                seq(c('('), ws, r("Clause"), c(')')), //
        ////                                r("FirstMatch"), //
        ////                                r("Seq"), //
        ////                                r("RuleName"), //
        ////                                DIGIT)), //
        //
        //                name("RuleIdent", //
        //                        oneOrMore(r("RuleIdentChar"))), //
        //
        //                name("RuleIdentChar", new CharSet(LETTER, DIGIT, new CharSet("_-."))),
        //
        //                name("FirstMatch", //
        //                        seq(r("Clause"), oneOrMore(seq(ws, c('|'), ws, r("Clause"))))),
        //
        //                name("Nothing", //
        //                        seq(c('('), ws, c(')'))),
        //
        //                name("Seq", //
        //                        seq(r("Clause"), oneOrMore(seq(ws, r("Clause"))))),
        //
        //                name("RuleName", //
        //                        r("RuleIdent"))), //
        //
        //                input);

        return new Parser(Arrays.asList(//
                name("Clause", //
                        first( //
                                r("Seq"), //
                                seq(c('('), r("Clause"), c(')')), //
                                LETTER, //
                                DIGIT)), //

                name("Seq", //
                        seq(r("Clause"), oneOrMore(seq(c(' '), r("Clause")))))),

                input);
    }

}
