package zed.pikaparser.grammar.expr;

import java.util.ArrayList;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class FirstMatch extends Expr {
    public FirstMatch(Expr... exprs) {
        super(exprs, null);
        StringBuilder buf = new StringBuilder();
        for (Expr e : exprs) {
            if (buf.length() > 1) {
                buf.append(" / ");
            }
            boolean addParens = e instanceof Seq || e instanceof FirstMatch;
            if (addParens) {
                buf.append('(');
            }
            buf.append(e);
            if (addParens) {
                buf.append(')');
            }
        }
        stringRep = buf.toString();
    }

    @Override
    public Memo match(DirectionalParser parser, MemoRef ref) {
        ArrayList<Memo> subExprMemos = new ArrayList<>();

        for (int i = 0; i < subExprs.size(); i++) {
            // Look up memo for subexpr.
            Memo subExprMemo = parser.matchMemoized(ref, new MemoRef(subExprs.get(i), ref.pos));

            if (subExprMemo.matches()) {
                // Return on first match
                subExprMemos.add(subExprMemo);
                return Memo.matched(ref, subExprMemos);
            }
        }
        // None of the options matched
        return Memo.DID_NOT_MATCH(ref);
    }
}