package pikaparser.clause;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class RuleName extends Clause {

    public final String refdRuleName;

    public RuleName(String refdRuleName) {
        super(new Clause[0]);
        this.refdRuleName = refdRuleName;
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        var subClauseMemoRef = new MemoRef(subClauses[0], memoRef.startPos);
        var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef);
        boolean matched = subClauseMemo.matched();
        return new Memo(memoRef, subClauseMemo.len, matched ? subClauseMemo : null);
    }

    @Override
    public String toStr() {
        return refdRuleName;
    }
}
