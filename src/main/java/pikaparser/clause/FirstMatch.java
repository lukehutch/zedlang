package pikaparser.clause;

import java.util.Arrays;
import java.util.List;

import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public class FirstMatch extends Clause {

    public FirstMatch(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException("Expected 2 or more subclauses");
        }
    }

    @Override
    public Memo match(String input, MemoRef memoRef, boolean isFirstMatchPosition) {
        var matchLen = -1;
        var matchingSubClauseIdx = 0;
        var matchingSubClauseMemo = (Memo) null;
        for (int i = 0; i < subClauses.length; i++) {
            var subClause = subClauses[i];
            var subClauseMemoRef = new MemoRef(subClause, memoRef.startPos);
            var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef, isFirstMatchPosition);
            if (subClauseMemo.matched()) {
                matchLen = subClauseMemo.len;
                matchingSubClauseIdx = i;
                matchingSubClauseMemo = subClauseMemo;
                break;
            }
        }
        return new Memo(memoRef, matchLen, matchingSubClauseMemo, matchingSubClauseIdx);
    }

    @Override
    public List<Clause> getTriggerSubClauses() {
        // Any sub-clause could be the matching clause, so need to override this
        return Arrays.asList(subClauses);
    }

    @Override
    public String toStr() {
        var buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < subClauses.length; i++) {
            if (i > 0) {
                buf.append(" | ");
            }
            buf.append(subClauses[i].toString());
        }
        buf.append(')');
        return buf.toString();
    }
}
