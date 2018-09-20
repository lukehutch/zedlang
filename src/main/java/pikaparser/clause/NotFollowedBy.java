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
    public Match match(String input, ParsingContext parsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewBestMatch) {

        boolean isParsingContextRoot = this == parsingContext.parentMemoEntry.clause
                && startPos == parsingContext.parentMemoEntry.startPos;
        if (isParsingContextRoot) {
            // This is the root of a ParsingContext --  need to extend match from current ParsingContext
            var subClauseMatch = subClauses[0].match(input, parsingContext, startPos, memoEntriesWithNewBestMatch);
            var match = subClauseMatch == null ? new Match(this, startPos, /* len = */ 0,
                    /* subClauseMatches = */ Collections.emptyList(), /* firstMatchingSubClauseIdx = */ 0) : null;
            if (match != null) {
                parsingContext.parentMemoEntry.newMatches.add(match);
                memoEntriesWithNewBestMatch.add(parsingContext.parentMemoEntry);
            }
            return match;

        } else if (matchTopDown) {
            var subClauseMatch = subClauses[0].match(input, parsingContext, startPos, memoEntriesWithNewBestMatch);
            var match = subClauseMatch == null ? new Match(this, startPos, /* len = */ 0,
                    /* subClauseMatches = */ Collections.emptyList(), /* firstMatchingSubClauseIdx = */ 0) : null;

            if (match != null && matchTopDown) {
                // Store memo (even though it is not needed) for debugging purposes -- TODO remove this?
                getOrCreateMemoEntry(startPos).bestMatch = match;
            }
            return match;

        } else {
            return lookUpBestMatch(parsingContext, startPos);
        }
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = "!(" + subClauses[0] + ")";
        }
        return toStringCached;
    }
}
