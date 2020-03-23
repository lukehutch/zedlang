
import static pikaparser.clause.Clause.*;

import java.util.Arrays;

import pikaparser.clause.CharSet;
import pikaparser.clause.Clause;
import pikaparser.grammar.Grammar;

public class PrecedenceGrammar {

    public static Clause ws = r("WS");

    public static final CharSet WHITESPACE = new CharSet(" \n\r\t");

    public static final CharSet LETTER = new CharSet(new CharSet('a', 'z'), new CharSet('A', 'Z'));

    public static final CharSet DIGIT = new CharSet('0', '9');

    public static final CharSet LETTER_OR_DIGIT = new CharSet(LETTER, DIGIT);

    public static final Grammar grammar = new Grammar(Arrays.asList(//

            //            rule("P2-P4", //
            //                    first(r("P2"), r("P3"), r("P4"))),
            //
            //            rule("P1-P4", //
            //                    first(r("P1"), r("P2"), r("P3"), r("P4"))),
            //
            //            rule("P0-P4", //
            //                    first(r("P0"), r("P1"), r("P2"), r("P3"), r("P4"))),
            //
            //            rule("P4", seq(c('('), r("P0-P4"), c(')'))), //
            //            
            //            rule("P3", //
            //                    ast("SYM", LETTER_OR_DIGIT)), //
            //
            //            rule("P2", //
            //                    ast("ASSOC", seq( //
            //                            oneOrMore(ast("OP", c('-'))), //
            //                            r("P3-P4")))), 
            //
            //            rule("P1", //
            //                    ast("ASSOC", seq( //
            //                            r("P2-P4"), // Note -- this is "P2-P4" not "P1-P4", otherwise parsing greedily matches and breaks
            //                            oneOrMore( //
            //                                    seq( //
            //                                            ast("OP", first(c('*'), c('/'))), //
            //                                            r("P2-P4")))))), //
            //
            //            rule("P0", //
            //                    ast("ASSOC", seq( //
            //                            r("P1-P4"), //
            //                            oneOrMore( //
            //                                    seq( //
            //                                            ast("OP", first(c('+'), c('-'))), //
            //                                            r("P1-P4")))))) //

            rule("P4", seq(c('('), r("P0"), c(')'))), //

            rule("P3", //
                    first(ast("SYM", LETTER_OR_DIGIT), r("P4"))), //

            rule("P2", //
                    first(ast("ASSOC", seq( //
                            oneOrMore(ast("OP", c('-'))), //
                            r("P3"))), //
                            r("P3"))), //

            rule("P1", //
                    first(ast("ASSOC", seq( //
                            r("P2"), // Note -- this is "P2-P4" not "P1-P4", otherwise parsing greedily matches and breaks
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('*'), c('/'))), //
                                            r("P2"))))), //
                            r("P2"))), //

            rule("P0", //
                    first(ast("ASSOC", seq( //
                            r("P1"), //
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('+'), c('-'))), //
                                            r("P1"))))), //
                            r("P1"))) //

    )); //
}
