package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class Nothing extends Terminal {
    public static final String NOTHING_STR = "()";

    Nothing() {
        super();
        canMatchZeroChars = true;
    }

    @Override
    public void testWhetherCanMatchZeroChars() {
    }

    // (This shouldn't be called under normal circumstances.)
    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        // Terminals are always matched top-down
        // Don't call MemoTable.addMatch for Nothing, to limit size of memo table
        return new Match(memoKey, /* firstMatchingSubClauseIdx = */ 0, /* len = */ 0, Match.NO_SUBCLAUSE_MATCHES);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = NOTHING_STR;
        }
        return toStringCached;
    }
}
