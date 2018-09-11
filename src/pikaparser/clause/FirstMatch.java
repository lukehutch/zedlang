package pikaparser.clause;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class FirstMatch extends Clause {

    public FirstMatch(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException("Expected 2 or more subclauses");
        }
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        var matchingSubClauseMemo = (Memo) null;
        var matchLen = -1;
        for (var subClause : subClauses) {
            var subClauseMemoRef = new MemoRef(subClause, memoRef.startPos);
            var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef);
            if (subClauseMemo.matched()) {
                matchLen = subClauseMemo.len;
                matchingSubClauseMemo = subClauseMemo;
                break;
            }
        }
        return new Memo(memoRef, matchLen, matchingSubClauseMemo);
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
