package pikaparser.clause;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class RuleName extends Clause {

    public final String refdRuleName;

    public RuleName(String refdRuleName) {
        super(new Clause[0]);
        this.refdRuleName = refdRuleName;
    }

    @Override
    public Match match(MemoEntry memoEntry, String input) {
        throw new IllegalArgumentException(RuleName.class.getSimpleName() + " should not be part of final grammar");
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = refdRuleName;
        }
        return toStringCached;
    }
}
