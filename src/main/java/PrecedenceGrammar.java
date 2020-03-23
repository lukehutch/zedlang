
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

    // P3P4 <- P3 / P4
    // P2P4 <- P2 / P3 / P4
    // P1P4 <- P1 / P2 / P3 / P4
    // P0P4 <- P0 / P1 / P2 / P3 / P4
    // P4 <- '(' P0P4 ')'
    // P3 <- [0-9a-zA-Z_]+
    // P2 <- ('-')+ P3P4                // Or: '-' P2P4 (however this breaks symmetry and only works for unary operators)
    // P1 <- P2P4 ('*' / '/') P2P4 
    // P0 <- P1P4 ('+' / '-') P1P4 
    
    public static final Grammar grammar = new Grammar(Arrays.asList(//

            rule("P3P4", //
                    first(r("P3"), r("P4"))),

            rule("P2P4", //
                    first(r("P2"), r("P3"), r("P4"))),

            rule("P1P4", //
                    first(r("P1"), r("P2"), r("P3"), r("P4"))),

            rule("P0P4", //
                    first(r("P0"), r("P1"), r("P2"), r("P3"), r("P4"))),

            rule("P4", seq(c('('), r("P0P4"), c(')'))), //

            rule("P3", //
                    ast("SYM", LETTER_OR_DIGIT)), //

            rule("P2", //
                    ast("ASSOC", seq( //
                            oneOrMore(ast("OP", c('-'))), //
                            r("P3P4")))),

            rule("P1", //
                    ast("ASSOC", seq( //
                            r("P2P4"), // Note -- this is "P2P4" not "P1P4", otherwise parsing greedily matches and breaks
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('*'), c('/'))), //
                                            r("P2P4")))))), //

            rule("P0", //
                    ast("ASSOC", seq( //
                            r("P1P4"), //
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('+'), c('-'))), //
                                            r("P1P4")))))) //

    //            rule("P4", seq(c('('), r("P0"), c(')'))), //
    //
    //            rule("P3", //
    //                    first(ast("SYM", LETTER_OR_DIGIT), r("P4"))), //
    //
    //            rule("P2", //
    //                    first(ast("ASSOC", seq( //
    //                            oneOrMore(ast("OP", c('-'))), //
    //                            r("P3"))), //
    //                            r("P3"))), //
    //
    //            rule("P1", //
    //                    first(ast("ASSOC", seq( //
    //                            r("P2"), // Note -- this is "P2" not "P1", otherwise parsing greedily matches and breaks
    //                            oneOrMore( //
    //                                    seq( //
    //                                            ast("OP", first(c('*'), c('/'))), //
    //                                            r("P2"))))), //
    //                            r("P2"))), //
    //
    //            rule("P0", //
    //                    first(ast("ASSOC", seq( //
    //                            r("P1"), //
    //                            oneOrMore( //
    //                                    seq( //
    //                                            ast("OP", first(c('+'), c('-'))), //
    //                                            r("P1"))))), //
    //                            r("P1"))) //

    )); //
}
