package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class Seq extends Clause {

    public Seq(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException("Expected 2 or more subclauses");
        }
    }

    @Override
    public Match extendParsingContext(String input, MemoEntry parentMemoEntry,
            ParsingContext prevSubClauseParsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContexts) {
        var matched = true;
        var currPos = startPos;
        var prevContext = prevSubClauseParsingContext;
        var startSubClauseIdx = prevContext == null ? 0 : prevContext.subClauseIdx + 1;
        for (var subClauseIdx = startSubClauseIdx; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMatch = subClause.getCurrBestMatch(input, prevContext, currPos,
                    memoEntriesWithNewParsingContexts);
            if (subClauseMatch == null) {
                matched = false;
                break;
            }
            prevContext = new ParsingContext(parentMemoEntry, prevContext, subClauseMatch, subClauseIdx);
            currPos += subClauseMatch.len;
        }
        return matched ? prevContext.getParentMatch(this) : null;
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
