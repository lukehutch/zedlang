package pikaparser.clause;

import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public class NotFollowedBy extends Clause {

    public NotFollowedBy(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Memo match(String input, MemoRef memoRef, boolean isFirstMatchPosition) {
        var subClauseMemoRef = new MemoRef(subClauses[0], memoRef.startPos);
        var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef, isFirstMatchPosition);
        boolean matched = subClauseMemo.matched();
        return new Memo(memoRef, matched ? -1 : 0, matched ? null : subClauseMemo);
    }

    @Override
    public String toStr() {
        return "!(" + subClauses[0] + ")";
    }
}
