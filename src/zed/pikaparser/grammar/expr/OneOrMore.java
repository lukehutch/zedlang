package zed.pikaparser.grammar.expr;

import java.util.ArrayList;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class OneOrMore extends Expr {
    public OneOrMore(Expr expr) {
        super(expr, "(" + expr + ")+");
    }

    @Override
    public Memo match(DirectionalParser parser, MemoRef ref) {
        Expr subExpr = subExprs.get(0);

        // Greedily match the subexpr as many times as possible
        ArrayList<Memo> subExprMemos = new ArrayList<>();
        int currIdx = ref.pos;
        for (boolean done = false; !done;) {
            // Look up memo for subexpr at curr input index
            Memo subExprMemo = parser.matchMemoized(ref, new MemoRef(subExpr, currIdx));

            if (subExprMemo.matches()) {
                // Consume the match
                currIdx = subExprMemo.endPos;
                subExprMemos.add(subExprMemo);
            } else {
                done = true;
            }

            if (subExprMemo.matchLen() == 0) {
                // If the subexpr consumed no input (i.e. matched Nothing, or an expression that matched Nothing),
                // it can only match once, otherwise we end up in an infinite loop.
                done = true;
            }
        }

        if (subExprMemos.size() >= 1) {
            return Memo.matched(ref, subExprMemos);
        } else {
            return Memo.DID_NOT_MATCH(ref);
        }
    }
}