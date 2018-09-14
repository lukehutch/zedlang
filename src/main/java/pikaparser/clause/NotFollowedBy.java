package pikaparser.clause;

import java.util.Collections;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class NotFollowedBy extends Clause {

    public NotFollowedBy(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Match extendParsingContext(String input, MemoEntry parentMemoEntry,
            ParsingContext prevSubClauseParsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContexts) {
        var subClauseMatch = subClauses[0].getCurrBestMatch(input, prevSubClauseParsingContext, startPos,
                memoEntriesWithNewParsingContexts);
        return subClauseMatch == null ? new Match(this, startPos, /* len = */ 0,
                /* subClauseMatches = */ Collections.emptyList(), /* firstMatchingSubClauseIdx = */ 0) : null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = "!(" + subClauses[0] + ")";
        }
        return toStringCached;
    }
}
