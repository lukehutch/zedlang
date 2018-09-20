package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class OneOrMore extends Clause {

    public OneOrMore(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Match match(String input, ParsingContext parsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewBestMatch) {
        boolean isParsingContextRoot = this == parsingContext.parentMemoEntry.clause
                && startPos == parsingContext.parentMemoEntry.startPos;
        if (isParsingContextRoot) {
            throw new RuntimeException(getClass().getSimpleName() + " cannot be a parsing context root");

        } else if (matchTopDown) {
            var subClauseMatches = (List<Match>) null;
            var currStartPos = startPos;
            for (;;) {
                var subClauseMatch = subClauses[0].match(input, parsingContext, currStartPos,
                        memoEntriesWithNewBestMatch);
                if (subClauseMatch == null) {
                    break;
                }
                if (subClauseMatches == null) {
                    subClauseMatches = new ArrayList<>(subClauses.length);
                }
                subClauseMatches.add(subClauseMatch);
                if (subClauseMatch.len == 0) {
                    // Prevent infinite loop -- if match consumed zero characters, can only match it once
                    // (i.e. OneOrMore(Nothing) will match exactly one Nothing)
                    break;
                }
                currStartPos += subClauseMatch.len;
            }
            var match = subClauseMatches != null
                    ? new Match(this, startPos, currStartPos - startPos, subClauseMatches, 0)
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
    public String toString() {
        if (toStringCached == null) {
            toStringCached = subClauses[0] + "+";
        }
        return toStringCached;
    }
}
