package zed.pikaparser;

import zed.pikaparser.ParseTreeToAST.ASTNode;
import zed.pikaparser.grammar.Grammar;
import zed.pikaparser.grammar.expr.Expr;
import zed.pikaparser.parser.ParseTask;
import zed.pikaparser.parser.memo.Memo;

// TODO: Can probably figure out which rules are right-recursive with themselves (including zero-or-more / one-or-more rules), and call them top-down rather than bottom-up, to save on space in the memo table when there are long runs

public class Main {

    //    public static class TestGrammar extends Grammar {
    //        {
    //            addRule("TOP", Rule("Sum"));
    //            addRule("Sum", Seq(Rule("Prod"), ZeroOrMore(Seq(FirstMatch(Char('+'), Char('-')), Rule("Prod")))));
    //            addRule("Prod", Seq(Rule("Val"), ZeroOrMore(Seq(FirstMatch(Char('*'), Char('/')), Rule("Val")))));
    //            // addRule("Val", OneOrMore(Char('0', '9')));
    //            // Indirectly self-recursive version:
    //            addRule("Val", FirstMatch(OneOrMore(Char('0', '9')), Seq(Char('('), Rule("Sum"), Char(')'))));
    //
    //            //            addRule("TOP", Rule("x"));
    //            //            addRule("x", Rule("Expr"));
    //            //            addRule("Expr", FirstMatch(Seq(Rule("x"), Char('-'), Rule("Num")), Rule("Num")));
    //            //            addRule("Num", Char('0', '9'));
    //        }
    //    }

    public static class GrammarDefGrammar extends Grammar {
        {
            addRule("TOP", Seq(Rule("Assnmt"), Lift(ZeroOrMore(Rule("NLAssnmt"))), Rule("WSNL")));
            addRule("Assnmt",
                    Seq(Rule("WS"), Rule("Ident"), Rule("WS"), Drop(Tok("::=")), Rule("WS"), Rule("Match"), Rule("WS")));
            Expr nlAssnmt = addRule("NLAssnmt", Seq(OneOrMore(Rule("NL")), Rule("Assnmt")));
            setRecoveryLevelExpr(nlAssnmt);
            addRule("WS", Drop(ZeroOrMore(NonNewlineWhitespace())));
            addRule("WSNL", Drop(ZeroOrMore(Whitespace())));
            addRule("NL", Drop(Newline()));
            addRule("Ident", Seq(Rule("IdentFirst"), ZeroOrMore(Rule("IdentRest"))));
            addRule("IdentFirst", FirstMatch(Char('_'), Letter()));
            addRule("IdentRest", FirstMatch(Rule("IdentFirst"), Digit()));
            addRule("Match", Char('x'));
        }
    }

    public static String inputSeq = "   test  ::= x  \n 3test2 ::= x\n_test3 ::=x";

    // TODO: make note in paper that rules should put generic stuff to skip (like whitespace) at end of rules,
    // not at beginning, in order to make memo table smaller. 

    // TODO: when a terminal expr is a subexpr of only one other expr, and that expr is a OneOrMore, can collapse a run of OneOrMores down into a single memo table entry at the end of an iteration. (Will need to track all superexprs, not just superexprs with the terminal in the first position.)

    // Tony's suggested test example 
    public static class TestGrammar extends Grammar {
        {
            addRule("TOP", Rule("P"));
            addRule("L", FirstMatch(Seq(Rule("P"), Tok(".x")), Char('x')));
            addRule("P", FirstMatch(Seq(Rule("P"), Tok("(n)")), Rule("L")));
        }
    }

    // public static String inputSeq = "x(n)";

    //    // Tony's suggested test example 
    //    public static class TestGrammar extends Grammar {
    //        {
    //            //addRule("TOP", Rule("Q"));
    //            addRule("TOP", Seq(Rule("Q"), Char('x')));
    //            addRule("Q", FirstMatch(Seq(Char('x'), Rule("Q")), Nothing()));
    //        }
    //    }
    //    public static String inputSeq = "";

    public static void main(String[] args) {

        // TODO: Move Drop(), Lift() etc. into fields of Expr when they are present, and unwrap their contents (otherwise memo table is bigger).
        // TODO: Add a Span() wrapper node too that wraps idents etc.

        // TODO: get rid of the "bottomUp" variable by subclassing Parser into TopDownParser and BottomUpParser

        // TODO: fix this, the static block doesn't get run unless the constructor is called.
        GrammarDefGrammar grammar = new GrammarDefGrammar();

        // Grammar grammar = new TestGrammar();

        Expr[] exprs = grammar.getExprs();
        for (int i = 0; i < exprs.length; i++) {
            System.out.println(exprs[i].format());
        }

        long startTime = System.currentTimeMillis();
        Memo topMatch = new ParseTask(grammar, inputSeq).parse();
        long timeTaken = System.currentTimeMillis() - startTime;

        System.out.println("\nParse:\n" + (topMatch.matches() ? topMatch.toString(inputSeq) : " no match"));

        System.out.println("\nTime taken: " + timeTaken);

        ASTNode ast = ParseTreeToAST.convertToAST(topMatch, inputSeq);
        System.out.println(ast);
    }
}
