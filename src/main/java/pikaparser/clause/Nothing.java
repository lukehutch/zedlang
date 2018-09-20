package pikaparser.clause;

import java.util.Collections;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class Nothing extends Terminal {

    public static final String NOTHING_STR = "()";

    public Nothing() {
        super();
    }

    @Override
    public Match match(String input, ParsingContext parsingContextIgnored, int startPos,
            Set<MemoEntry> memoEntriesWithNewBestMatchIgnored) {
        return new Match(this, startPos, 0, Collections.emptyList(), 0);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = NOTHING_STR;
        }
        return toStringCached;
    }
}
