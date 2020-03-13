package pikaparser.clause;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class FollowedBy extends Clause {

    public FollowedBy(Clause subClause) {
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
        var subClauseMatch = subClauses[0].lookUpBestMatch(/* parentMemoEntry = */ memoEntry,
                /* subClauseStartPos = */ memoEntry.startPos, input);
        // Replace any valid subclause match with a zero-char-consuming match
        return subClauseMatch == null ? null : new Match(this, memoEntry.startPos, /* len = */ 0);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = "&(" + subClauses[0] + ")";
        }
        return toStringCached;
    }
}
