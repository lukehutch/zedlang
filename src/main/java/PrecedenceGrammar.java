
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

    // Precedence parsing:

    // P3P4 <- P3 / P4
    // P2P4 <- P2 / P3 / P4
    // P1P4 <- P1 / P2 / P3 / P4
    // P0P4 <- P0 / P1 / P2 / P3 / P4
    // P4 <- '(' P0P4 ')'
    // P3 <- [0-9]+ / ([a-zA-Z_][0-9a-zA-Z_]*)
    // P2 <- ('-')+ P3P4                // Or: '-' P2P4 (however this breaks symmetry and only works for unary operators)
    // P1 <- P2P4 (('*' / '/') P2P4)+ 
    // P0 <- P1P4 (('+' / '-') P1P4)+ 

    // Alternative form: fewer rules, but builds deeper parse trees 

    // P4 <- '(' P0 ')'
    // P3 <- [0-9]+ / ([a-zA-Z_][0-9a-zA-Z_]*)
    // P2 <- (('-')+ P3) / P3
    // P1 <- (P2 (('*' / '/') P2)+) / P2 
    // P0 <- (P1 (('+' / '-') P1)+) / P1 

    // Left associative version of first form: (Note the '+' above, which collects all items of same precedence)

    // P1 <- P1P4 ('*' / '/') P2P4 

    // Right associative version of first form:

    // P2 <- ('-') P2P4
    // P1 <- P2P4 ('*' / '/') P1P4 

    // Left associative version of second form:

    // P1 <- ((P1 / P2) ('*' / '/') P2) / P2 

    // Right associative version of second form:

    // P2 <- (('-') (P2 / P3)) / P3
    // P1 <- (P2 ('*' / '/') (P1 / P2)) / P2 

    // Structure of precedence selectors for P0-P4, for non-associative vs. associative cases:
    
    //    P4HigherPrecSelector: P0 / P1 / P2 / P3 / P4
    //    P3HigherPrecSelector: P4
    //    P2HigherPrecSelector: P3 / P4
    //    P1HigherPrecSelector: P2 / P3 / P4
    //    P0HigherPrecSelector: P1 / P2 / P3 / P4
    //
    //    P4AssocSelector: P4 / P0 / P1 / P2 / P3
    //    P3AssocSelector: P3 / P4
    //    P2AssocSelector: P2 / P3 / P4
    //    P1AssocSelector: P1 / P2 / P3 / P4
    //    P0AssocSelector: P0 / P1 / P2 / P3 / P4

    public static final Grammar grammar = new Grammar(Arrays.asList(//

            rule("Expr", 4, /* associativity = */ null, //
                    seq(c('('), r("Expr"), c(')'))), //

            rule("Expr", 3, /* associativity = */ null, //
                    ast("SYM", oneOrMore(LETTER_OR_DIGIT))), //

            rule("Expr", 2, /* associativity = */ null, //
                    ast("EXPR", seq( //
                            oneOrMore(ast("OP", c('-'))), //
                            r("Expr")))),

            rule("Expr", 1, /* associativity = */ null, //
                    ast("EXPR", seq( //
                            r("Expr"), // Note -- this is "P2P4" not "P1P4", otherwise parsing greedily matches and breaks (associativity is swept under the rug by using oneOrMore)
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('*'), c('/'))), //
                                            r("Expr")))))), //

            rule("Expr", 0, /* associativity = */ null, //
                    ast("EXPR", seq( //
                            r("Expr"), //
                            oneOrMore( //
                                    seq( //
                                            ast("OP", first(c('+'), c('-'))), //
                                            r("Expr")))))) //

    //            rule("P3P4", //
    //                    first(r("P3"), r("P4"))),
    //
    //            rule("P2P4", //
    //                    first(r("P2"), r("P3"), r("P4"))),
    //
    //            rule("P1P4", //
    //                    first(r("P1"), r("P2"), r("P3"), r("P4"))),
    //
    //            rule("P0P4", //
    //                    first(r("P0"), r("P1"), r("P2"), r("P3"), r("P4"))),
    //
    //            rule("P4", seq(c('('), r("P0P4"), c(')'))), //
    //
    //            rule("P3", //
    //                    ast("SYM", LETTER_OR_DIGIT)), //
    //
    //            rule("P2", //
    //                    ast("ASSOC", seq( //
    //                            oneOrMore(ast("OP", c('-'))), //
    //                            r("P3P4")))),
    //
    //            rule("P1", //
    //                    ast("ASSOC", seq( //
    //                            r("P2P4"), // Note -- this is "P2P4" not "P1P4", otherwise parsing greedily matches and breaks (associativity is swept under the rug by using oneOrMore)
    //                            oneOrMore( //
    //                                    seq( //
    //                                            ast("OP", first(c('*'), c('/'))), //
    //                                            r("P2P4")))))), //
    //
    //            rule("P0", //
    //                    ast("ASSOC", seq( //
    //                            r("P1P4"), //
    //                            oneOrMore( //
    //                                    seq( //
    //                                            ast("OP", first(c('+'), c('-'))), //
    //                                            r("P1P4")))))) //

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
