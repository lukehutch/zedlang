package pikaparser.clause;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class FirstMatch extends Clause {

    public FirstMatch(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException(FirstMatch.class.getSimpleName() + " expects 2 or more subclauses");
        }
    }

    @Override
    public Match match(String input, ParsingContext parsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewBestMatch) {
        boolean isParsingContextRoot = this == parsingContext.parentMemoEntry.clause
                && startPos == parsingContext.parentMemoEntry.startPos;
        if (isParsingContextRoot) {
            // This is the root of a ParsingContext --  need to extend match from current ParsingContext
            var matched = false;
            var currContext = parsingContext;
            for (int subClauseIdx = currContext.subClauseIdx; subClauseIdx < subClauses.length; subClauseIdx++) {
                var subClause = subClauses[subClauseIdx];
                var subClauseMatch = subClause.match(input, currContext, startPos, memoEntriesWithNewBestMatch);
                currContext = new ParsingContext(currContext, subClauseMatch, subClauseIdx + 1);
                if (subClauseMatch != null) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                var match = currContext.getParentMatch();
                parsingContext.parentMemoEntry.newMatches.add(match);
                memoEntriesWithNewBestMatch.add(parsingContext.parentMemoEntry);
                return match;
            }
            return null;

        } else if (matchTopDown) {
            var subClauseMatch = (Match) null;
            var matchingSubClauseIdx = -1;
            for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
                var subClause = subClauses[subClauseIdx];
                subClauseMatch = subClause.match(input, parsingContext, startPos, memoEntriesWithNewBestMatch);
                if (subClauseMatch != null) {
                    matchingSubClauseIdx = subClauseIdx;
                    break;
                }
            }
            var match = subClauseMatch != null
                    ? new Match(this, startPos, subClauseMatch.len, Arrays.asList(subClauseMatch), matchingSubClauseIdx)
                    : null;

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
    public List<Clause> getSeedSubClauses() {
        // Any sub-clause could be the matching clause, so need to override this
        return Arrays.asList(subClauses);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            buf.append('(');
            for (int i = 0; i < subClauses.length; i++) {
                if (i > 0) {
                    buf.append(" | ");
                }
                buf.append(subClauses[i].toString());
            }
            buf.append(')');
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
