package zed.pikaparser.grammar.expr;

import java.util.ArrayList;
import java.util.HashSet;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public abstract class Expr {

    public int idx;

    public ArrayList<Expr> subExprs = new ArrayList<>();

    /** Superexprs that have this expr as the first subexpr. */
    public ArrayList<Expr> superExprsWithThisAsFirstSubExpr = new ArrayList<>();

    /** The name(s) of the rule name(s) for this expression. */
    public HashSet<String> ruleNames = new HashSet<>();

    public String stringRep;

    // TODO: figure out how to annotate nodes in the grammar with these, rather than wrapping them.
    // TODO: The issue is that the AST transformation needs to be part of the interned expr's stringRep, so that exprs that have these attrs and matching exprs that don't can both be used. 

    //    ASTTransformation astXForm = ASTTransformation.NONE;
    //    
    //    public static enum ASTTransformation {
    //        NONE,
    //        /** Group all the terminal characters of all subtrees of this node into a single string token in the AST */
    //        SPAN,
    //        /** Lift all child exprs of this expr one level higher in the AST (only useful for wrapping Seq nodes) */ 
    //        LIFT,
    //        /** Drop all sub-nodes when forming the AST (e.g. for ignoring whitespace) */
    //        DROP;
    //    }

    protected Expr(Expr expr, String stringRep) {
        this.subExprs.add(expr);
        this.stringRep = stringRep;
    }

    protected Expr(Expr[] exprs, String stringRep) {
        for (Expr expr : exprs) {
            this.subExprs.add(expr);
        }
        this.stringRep = stringRep;
    }

    protected Expr(String stringRep) {
        this.stringRep = stringRep;
    }

    @Override
    public String toString() {
        return stringRep;
    }

    public String format() {
        StringBuilder buf = new StringBuilder();
        buf.append(idx);
        if (!ruleNames.isEmpty()) {
            buf.append(" [");
            boolean first = true;
            for (String ruleName : ruleNames) {
                if (first) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(ruleName);
            }
            buf.append(']');
        }
        buf.append(":\t");
        buf.append(stringRep);
        return buf.toString();
    }

    public abstract Memo match(DirectionalParser parser, MemoRef ref);
}
