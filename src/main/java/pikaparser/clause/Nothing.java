package pikaparser.clause;

import java.util.Collections;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class Nothing extends Clause {

    public Nothing() {
        super(new Clause[0]);
    }

    @Override
    public Match extendParsingContext(String input, MemoEntry parentMemoEntryUnused,
            ParsingContext prevSubClauseParsingContextUnused, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContexts) {
        return getCurrBestMatch(input, prevSubClauseParsingContextUnused, startPos, memoEntriesWithNewParsingContexts);
    }

    @Override
    public Match getCurrBestMatch(String input, ParsingContext prevSubClauseParsingContextUnused, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContextsUnused) {
        return new Match(this, startPos, /* len = */ 0, /* subClauseMatches = */ Collections.emptyList(),
                /* firstMatchingSubClauseIdx = */ 0);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = "()";
        }
        return toStringCached;
    }
}
