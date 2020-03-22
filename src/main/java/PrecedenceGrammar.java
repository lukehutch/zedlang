
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

            rule("P2-P3", //
                    first(r("P2"), r("P3"), r("PARENS"))),

            rule("P1-P3", //
                    first(r("P1"), r("P2"), r("P3"), r("PARENS"))),

            rule("P0-P3", //
                    first(r("P0"), r("P1"), r("P2"), r("P3"), r("PARENS"))),

            rule("PARENS", seq(c('('), r("P0-P3"), c(')'))), //

            rule("P3", //
                    ast("SYM", LETTER_OR_DIGIT)), //

            rule("P2", //
                    ast("ASSOC", seq( //
                            ast("OP", c('-')), //
                            r("P2-P3")))), //

            rule("P1", //
                    ast("ASSOC", seq( //
                            r("P2-P3"), //
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('*'), c('/'))), //
                                            r("P2-P3")))))), //

            rule("P0", //
                    ast("ASSOC", seq( //
                            r("P1-P3"), //
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('+'), c('-'))), //
                                            r("P1-P3")))))) //

    //            rule("P3", //
    //                    longest( //
    //                            ast("P3", seq( //
    //                                    c('('), //
    //                                    r("P0"), //
    //                                    c(')'))), //
    //                            ast("SYM", LETTER_OR_DIGIT), //
    //                            r("P0") //
    //                    )), //
    //
    //            rule("P2", //
    //                    first( //
    //                            ast("P2", seq( //
    //                                    ast("NEGATE", c('-')), //
    //                                    ast("operand", r("P2")))), //
    //                            r("P3") //
    //                    )), //
    //
    //            rule("P1", //
    //                    first( //
    //                            ast("P1", seq( //
    //                                    ast("operand0", r("P1")), //
    //                                    first(ast("MUL", c('*')), ast("DIV", c('/'))), //
    //                                    ast("operand1", r("P2")))), //
    //                            r("P2") //
    //                    )), //
    //
    //            rule("P0", //
    //                    first( //
    //                            ast("P0", seq( //
    //                                    ast("operand0", r("P0")), //
    //                                    first(ast("ADD", c('+')), ast("SUB", c('-'))), //
    //                                    ast("operand1", r("P1")))), //
    //                            r("P1") //
    //                    )) //

    )); //
}
