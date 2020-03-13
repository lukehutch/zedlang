package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class OneOrMore extends Clause {

    public OneOrMore(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public void testWhetherAlwaysMatches() {
        if (subClauses[0].alwaysMatches) {
            alwaysMatches = true;
        }
    }

    @Override
    public Match match(MemoEntry memoEntry, String input) {
        var subClause = subClauses[0];
        var subClauseMatches = (List<Match>) null;
        var currStartPos = memoEntry.startPos;
        for (;;) {
            var subClauseMatch = subClause.lookUpBestMatch(/* parentMemoEntry = */ memoEntry,
                    /* subClauseStartPos = */ currStartPos, input);
            if (subClauseMatch == null) {
                break;
            }
            if (subClauseMatches == null) {
                subClauseMatches = new ArrayList<>();
            }
            subClauseMatches.add(subClauseMatch);
            if (subClauseMatch.len == 0) {
                // Prevent infinite loop -- if match consumed zero characters, can only match it once
                // (i.e. OneOrMore(Nothing) will match exactly one Nothing)
                break;
            }
            currStartPos += subClauseMatch.len;
        }
        return subClauseMatches != null ? new Match(this, /* firstMatchingSubClauseIdx = */ 0, subClauseMatches) : null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = subClauses[0] + "+";
        }
        return toStringCached;
    }
}
