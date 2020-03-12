package zed.pikaparser.grammar.action;

import zed.pikaparser.grammar.expr.Expr;
import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class Lift extends Expr {
    public Lift(Expr expr) {
        super(expr, "Lift(" + expr + ")");
    }

    @Override
    public Memo match(DirectionalParser parser, MemoRef ref) {
        // Match if the subexprs matched
        Memo subExprMemo = parser.matchMemoized(ref, new MemoRef(subExprs.get(0), ref.pos));
        return subExprMemo.matches() ? Memo.matched(ref, subExprMemo) : Memo.DID_NOT_MATCH(ref);
    }
}