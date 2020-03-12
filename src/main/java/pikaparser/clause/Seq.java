package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class Seq extends Clause {

    public Seq(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException(Seq.class.getSimpleName() + " expects 2 or more subclauses");
        }
    }

    public Seq(List<Clause> subClauses) {
        this(subClauses.toArray(new Clause[0]));
    }

    @Override
    public Match match(String input, ParsingContext parsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewBestMatch) {
        boolean isParsingContextRoot = this == parsingContext.parentMemoEntry.clause
                && startPos == parsingContext.parentMemoEntry.startPos;
        if (isParsingContextRoot) {
            // This is the root of a ParsingContext --  need to extend match from current ParsingContext
            var matched = true;
            var currContext = parsingContext;
            var resumeSubClauseIdx = currContext.subClauseIdx;
            var resumeStartPos = parsingContext.subClauseMatchNode != null
                    ? parsingContext.subClauseMatchNode.match.startPos + parsingContext.subClauseMatchNode.match.len
                    : startPos;
            for (int subClauseIdx = resumeSubClauseIdx, currStartPos = resumeStartPos; //
                    subClauseIdx < subClauses.length; subClauseIdx++) {
                var subClause = subClauses[subClauseIdx];
                var subClauseMatch = subClause.match(input, currContext, currStartPos, memoEntriesWithNewBestMatch);
                if (subClauseMatch == null) {
                    matched = false;
                    break;
                }
                currStartPos += subClauseMatch.len;
                currContext = new ParsingContext(currContext, subClauseMatch, subClauseIdx + 1);
            }
            if (matched) {
                var match = currContext.getParentMatch();
                parsingContext.parentMemoEntry.newMatches.add(match);
                memoEntriesWithNewBestMatch.add(parsingContext.parentMemoEntry);
                return match;
            }
            return null;

        } else if (matchTopDown) {
            var subClauseMatches = (List<Match>) null;
            var currStartPos = startPos;
            for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
                var subClause = subClauses[subClauseIdx];
                var subClauseMatch = subClause.match(input, parsingContext, startPos, memoEntriesWithNewBestMatch);
                if (subClauseMatch == null) {
                    break;
                }
                if (subClauseMatches == null) {
                    subClauseMatches = new ArrayList<>(subClauses.length);
                }
                subClauseMatches.add(subClauseMatch);
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
            var buf = new StringBuilder();
            buf.append('(');
            for (int i = 0; i < subClauses.length; i++) {
                if (i > 0) {
                    buf.append(" ");
                }
                buf.append(subClauses[i].toString());
            }
            buf.append(')');
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
