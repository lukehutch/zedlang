package zed.pikaparser.grammar.expr;

import java.util.ArrayList;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class Seq extends Expr {
    public Seq(Expr... exprs) {
        super(exprs, null);
        StringBuilder buf = new StringBuilder();
        for (Expr e : exprs) {
            if (buf.length() > 0) {
                buf.append(' ');
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
        for (int i = 0, currIdx = ref.pos; i < subExprs.size(); i++) {
            // Look up memo for subexpr.

            Memo subExprMemo = parser.matchMemoized(ref, new MemoRef(subExprs.get(i), currIdx));

            if (subExprMemo.matches()) {
                // If the subexpr matches the sequence at currIdx, record the memo for the subexpr
                subExprMemos.add(subExprMemo);

                // Consume the match
                currIdx = subExprMemo.endPos;

            } else {
                // Bail on the first non-match in the sequence
                return Memo.DID_NOT_MATCH(ref);
            }
        }
        // The whole sequence matched
        return Memo.matched(ref, subExprMemos);
    }
}