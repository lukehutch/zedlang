
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
            rule("P4", 
                    seq(r("P0"))),
            
            rule("P3", //
                    first( //
                            seq( //
                                    c('('), //
                                    r("P4"), //
                                    c(')')), //
                            r("P4"), //
                            ast("SYM", LETTER_OR_DIGIT))), //

            rule("P2", //
                    first( //
                            ast("P2", seq( //
                                    ast("NEGATE", c('-')), //
                                    ast("operand", r("P2")))), //
                            r("P3"))), //

            rule("P1", //
                    ast("P1", first( //
                            seq( //
                                    ast("operand0", r("P1")), //
                                    first(ast("MUL", c('*')), ast("DIV", c('/'))), //
                                    ast("operand1", r("P2"))), //
                            r("P2")))), //

            rule("P0", //
                    ast("P0", first( //
                            seq( //
                                    ast("operand0", r("P0")), //
                                    first(ast("ADD", c('+')), ast("SUB", c('-'))), //
                                    ast("operand1", r("P1"))), //
                            r("P1")))) //

//            rule("PZ", //
//                    ast("PZ", first( //
//                            seq( //
//                                    ast("operand0", r("PZ")), //
//                                    first(ast("MUL2", c('*')), ast("DIV2", c('/'))), //
//                                    ast("operand1", r("P0"))), //
//                            r("P0")))) //
    ));
}
