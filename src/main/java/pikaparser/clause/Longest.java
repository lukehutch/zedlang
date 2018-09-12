package pikaparser.clause;

import java.util.Arrays;
import java.util.List;

import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public class Longest extends Clause {

    public Longest(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException("Expected 2 or more subclauses");
        }
    }

    @Override
    public Memo match(String input, MemoRef memoRef, boolean isFirstMatchPosition) {
        var longestMatchMemo = (Memo) null;
        var longestMatchLen = -1;
        for (var subClause : subClauses) {
            var subClauseMemoRef = new MemoRef(subClause, memoRef.startPos);
            var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef, isFirstMatchPosition);
            if (subClauseMemo.matched()) {
                if (subClauseMemo.len > longestMatchLen) {
                    longestMatchLen = subClauseMemo.len;
                    longestMatchMemo = subClauseMemo;
                }
            }
        }
        return new Memo(memoRef, longestMatchLen, longestMatchMemo);
    }

    @Override
    public List<Clause> getTriggerSubClauses() {
        // All sub-clauses are evaluated => need to override this
        return Arrays.asList(subClauses);
    }

    @Override
    public String toStr() {
        var buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < subClauses.length; i++) {
            if (i > 0) {
                buf.append(" ^ ");
            }
            buf.append(subClauses[i].toString());
        }
        buf.append(')');
        return buf.toString();
    }
}
