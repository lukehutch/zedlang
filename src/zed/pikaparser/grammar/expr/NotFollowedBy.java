package zed.pikaparser.grammar.expr;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class NotFollowedBy extends Expr {
    public NotFollowedBy(Expr expr) {
        super(expr, "!(" + expr + ")");
    }

    @Override
    public Memo match(DirectionalParser parser, MemoRef ref) {
        Memo subExprMemo = parser.matchMemoized(ref, new MemoRef(subExprs.get(0), ref.pos));
        // If the subexpr does not match, return a match but consume no input, otherwise return a non-match
        return !subExprMemo.matches() ? Memo.matchedEmpty(ref) : Memo.DID_NOT_MATCH(ref);
    }
}