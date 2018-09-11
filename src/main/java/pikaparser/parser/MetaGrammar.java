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

public class MetaGrammar {

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

    //    /**
    //     * <a href="http://stackoverflow.com/questions/4731055/whitespace-matching-regex-java"Valid unicode whitespace
    //     * chars</a>
    //     */
    //    public static final CharSet WHITESPACE = new CharSet(new char[] { (char) 0x0009, // CHARACTER TABULATION
    //            (char) 0x000A, // LINE FEED (LF)
    //            (char) 0x000B, // LINE TABULATION
    //            (char) 0x000C, // FORM FEED (FF)
    //            (char) 0x000D, // CARRIAGE RETURN (CR)
    //            (char) 0x0020, // SPACE
    //            (char) 0x0085, // NEXT LINE (NEL) 
    //            (char) 0x00A0, // NO-BREAK SPACE
    //            (char) 0x1680, // OGHAM SPACE MARK
    //            (char) 0x180E, // MONGOLIAN VOWEL SEPARATOR
    //            (char) 0x2000, // EN QUAD 
    //            (char) 0x2001, // EM QUAD 
    //            (char) 0x2002, // EN SPACE
    //            (char) 0x2003, // EM SPACE
    //            (char) 0x2004, // THREE-PER-EM SPACE
    //            (char) 0x2005, // FOUR-PER-EM SPACE
    //            (char) 0x2006, // SIX-PER-EM SPACE
    //            (char) 0x2007, // FIGURE SPACE
    //            (char) 0x2008, // PUNCTUATION SPACE
    //            (char) 0x2009, // THIN SPACE
    //            (char) 0x200A, // HAIR SPACE
    //            (char) 0x2028, // LINE SEPARATOR
    //            (char) 0x2029, // PARAGRAPH SEPARATOR
    //            (char) 0x202F, // NARROW NO-BREAK SPACE
    //            (char) 0x205F, // MEDIUM MATHEMATICAL SPACE
    //            (char) 0x3000 // IDEOGRAPHIC SPACE
    //    });

    public static final CharSet WHITESPACE = new CharSet(" \n\r\t");

    public static final CharSet LETTER = new CharSet(new CharSet('a', 'z'), new CharSet('A', 'Z'));

    public static final CharSet DIGIT = new CharSet('0', '9');

    public static Parser newParser(String input) {
        return new Parser(Arrays.asList(//
                name("Grammar", //
                        seq(ws, oneOrMore(r("Rule")))), //

                name("WS", //
                        optional(oneOrMore(WHITESPACE))), //

                name("Rule", //
                        seq(r("RuleNames"), ws, c('='), ws, r("Clause"), ws, c(';'), ws)), //

                name("Clause", //
                        first( //
                                seq(c('('), ws, r("Rule"), c(')')), //
                                r("CharSeq"), //
                                r("CharSet"), //
                                r("FirstMatch"), //
                                r("FollowedBy"), //
                                r("NotFollowedBy"), //
                                r("Longest"), //
                                r("Nothing"), //
                                r("OneOrMore"), //
                                r("Seq"), //
                                r("RuleName"))), //

                name("RuleNames", //
                        seq(r("RuleIdent"), zeroOrMore(seq(ws, c(','), ws, r("RuleIdent"))), ws)), //

                name("RuleIdent", //
                        oneOrMore(r("RuleIdentChar"))), //

                name("RuleIdentChar", new CharSet(LETTER, DIGIT, new CharSet("_-."))),

                name("CharSet", first( //
                        seq(c('\''), r("QuotedChar"), c('\'')), //
                        seq(c('['), optional(c('^')), //
                                oneOrMore(first( //
                                        r("CharRange"), //
                                        r("CharRangeChar"))),
                                c(']')))), //
                name("QuotedChar", //
                        first( //
                                text("\\t"), //
                                text("\\b"), //
                                text("\\n"), //
                                text("\\r"), //
                                text("\\f"), //
                                text("\\'"), //
                                text("\\\""), //
                                text("\\\\"), //
                                seq(text("\\u"), r("Hex"), r("Hex"), r("Hex"), r("Hex")))), //

                name("Hex", new CharSet(DIGIT, new CharSet('a', 'f'), new CharSet('A', 'F'))), //

                name("CharRange", //
                        seq(r("CharRangeChar"), c('-'), r("CharRangeChar"))), //

                name("CharRangeChar", //
                        first( //
                                new CharSet('\\', ']').invert(), //
                                r("QuotedChar"), //
                                text("\\]"), //
                                text("\\^"))),

                name("CharSeq", seq(c('"'), zeroOrMore(r("StrQuotedChar")), c('"'))), //

                name("StrQuotedChar", //
                        first( //
                                new CharSet('\\', '\"').invert(), //
                                r("QuotedChar"))), //

                name("FirstMatch", //
                        seq(r("Clause"), oneOrMore(seq(ws, c('|'), ws, r("Clause"))))),

                name("FollowedBy", //
                        seq(c('&'), r("Clause"))),

                name("NotFollowedBy", //
                        seq(c('!'), r("Clause"))),

                name("Longest", //
                        seq(r("Clause"), oneOrMore(seq(ws, c('^'), ws, r("Clause"))))),

                name("Nothing", //
                        text("Nothing")),

                name("OneOrMore", //
                        seq(r("Clause"), ws, first( //
                                text("++"), // TODO: OneOrMoreSuffix
                                c('+')))),

                name("Seq", //
                        seq(r("Clause"), oneOrMore(seq(ws, r("Clause"))))),

                name("RuleName", //
                        r("RuleIdent"))), //

                input);
    }

}
