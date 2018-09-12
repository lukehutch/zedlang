package pikaparser.clause;

import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public class RuleName extends Clause {

    public final String refdRuleName;

    public RuleName(String refdRuleName) {
        super(new Clause[0]);
        this.refdRuleName = refdRuleName;
    }

    @Override
    public Memo match(String input, MemoRef memoRef, boolean isFirstMatchPosition) {
        throw new IllegalArgumentException("RuleName should not be part of final grammar");
    }

    @Override
    public String toStr() {
        return refdRuleName;
    }
}
