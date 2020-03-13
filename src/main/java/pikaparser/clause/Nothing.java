package pikaparser.clause;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class Nothing extends Terminal {

    public static final String NOTHING_STR = "()";

    public Nothing() {
        super();
    }

    @Override
    public void testWhetherAlwaysMatches() {
        alwaysMatches = true;
    }

    @Override
    public Match match(MemoEntry memoEntry, String input) {
        return new Match(this, 0, 0);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = NOTHING_STR;
        }
        return toStringCached;
    }
}
