package pikaparser.clause;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class NotFollowedBy extends Clause {

    public NotFollowedBy(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        var subClauseMemoRef = new MemoRef(subClauses[0], memoRef.startPos);
        var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef);
        boolean matched = subClauseMemo.matched();
        return new Memo(memoRef, matched ? -1 : 0, matched ? null : subClauseMemo);
    }

    @Override
    public String toStr() {
        return "!(" + subClauses[0] + ")";
    }
}
