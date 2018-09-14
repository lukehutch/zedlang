package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class OneOrMore extends Clause {

    public OneOrMore(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Match extendParsingContext(String input, MemoEntry parentMemoEntry,
            ParsingContext prevSubClauseParsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContexts) {
        var matched = false;
        var prevContext = prevSubClauseParsingContext;
        for (var currPos = startPos;;) {
            var subClauseMatch = subClauses[0].getCurrBestMatch(input, prevContext, currPos,
                    memoEntriesWithNewParsingContexts);
            if (subClauseMatch == null) {
                break;
            }
            matched = true;
            prevContext = new ParsingContext(parentMemoEntry, prevContext, subClauseMatch, /* subClauseIdx = */ 0);
            if (subClauseMatch.len == 0) {
                // Prevent infinite loop -- if match consumed zero characters, can only match it once
                // (i.e. OneOrMore(Nothing) will match exactly one Nothing)
                break;
            }
            currPos += subClauseMatch.len;
        }
        return matched ? prevContext.getParentMatch(this) : null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = subClauses[0] + "+";
        }
        return toStringCached;
    }
}
