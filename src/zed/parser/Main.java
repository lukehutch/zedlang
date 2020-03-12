package zed.parser;

import zed.parser.ParseTreeToAST.ASTNode;

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
            addRule("TOP", Seq(Rule("Rule"), Lift(ZeroOrMore(Seq(OneOrMore(Rule("NL")), Rule("Rule")))), Rule("WSNL")));
            addRule("Rule",
                    Seq(Rule("WS"), Rule("Ident"), Rule("WS"), Drop(Tok("::=")), Rule("WS"), Rule("Match"), Rule("WS")));
            addRule("WS", Drop(ZeroOrMore(NonNewlineWhitespace())));
            addRule("WSNL", Drop(ZeroOrMore(Whitespace())));
            addRule("NL", Drop(Newline()));
            addRule("Ident", Seq(Rule("IdentFirst"), ZeroOrMore(Rule("IdentRest"))));
            addRule("IdentFirst", FirstMatch(Char('_'), Letter()));
            addRule("IdentRest", FirstMatch(Rule("IdentFirst"), Digit()));
            addRule("Match", Char('x'));
        }
    }

    //    // Tony's suggested test example 
    //    public static class TestGrammar extends Grammar {
    //        {
    //            addRule("TOP", Rule("L"));
    //            addRule("L", FirstMatch(Seq(Rule("P"), Tok(".x")), Char('x')));
    //            addRule("P", FirstMatch(Seq(Rule("P"), Tok("(n)")), Rule("L")));
    //        }
    //    }

    public static void main(String[] args) {

        // TODO: fix this, the static block doesn't get run unless the constructor is called.
        GrammarDefGrammar grammar = new GrammarDefGrammar();

        Expr[] exprs = grammar.getExprs();
        for (int i = 0; i < exprs.length; i++) {
            System.out.println(exprs[i].format());
        }

        // String inputSeq = "1-2-3";
        // String inputSeq = "(1*2)+3";

        String inputSeq = "   test  ::= x  \n test2 ::= x\n_test3 ::=x";

        Memo topMatch = new Parser(inputSeq, grammar).parse();

        System.out.println("\nParse:\n" + topMatch.toString(inputSeq));

        ASTNode ast = ParseTreeToAST.convertToAST(topMatch, inputSeq);
        System.out.println(ast);
    }
}
