package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class Nothing extends Terminal {
    public static final String NOTHING_STR = "()";

    public Nothing() {
        super();
        alwaysMatches = true;
    }

    @Override
    public void testWhetherAlwaysMatches() {
    }

    // (This shouldn't be called under normal circumstances.)
    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        // Terminals are always matched top-down
        // Don't call MemoTable.addMatch for terminals, to limit size of memo table
        memoTable.numMatchObjectsCreated.incrementAndGet();
        return new Match(memoKey, /* firstMatchingSubClauseIdx = */ 0, /* len = */ 0, Match.NO_SUBCLAUSE_MATCHES);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            appendRulePrefix(buf);
            buf.append(NOTHING_STR);
            appendRuleSuffix(buf);
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
