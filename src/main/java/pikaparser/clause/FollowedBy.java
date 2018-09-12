package pikaparser.clause;

import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public class FollowedBy extends Clause {

    public FollowedBy(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Memo match(String input, MemoRef memoRef, boolean isFirstMatchPosition) {
        var subClauseMemoRef = new MemoRef(subClauses[0], memoRef.startPos);
        var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef, isFirstMatchPosition);
        boolean matched = subClauseMemo.matched();
        return new Memo(memoRef, matched ? 0 : -1, matched ? subClauseMemo : null);
    }

    @Override
    public String toStr() {
        return "&(" + subClauses[0] + ")";
    }
}
