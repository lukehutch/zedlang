package pikaparser.clause;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class NotFollowedBy extends Clause {

    public NotFollowedBy(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Match match(MemoEntry memoEntry, String input) {
        var subClauseMatch = subClauses[0].lookUpBestMatch(/* parentMemoEntry = */ memoEntry,
                /* subClauseStartPos = */ memoEntry.startPos, input);
        // Replace any invalid subclause match with a zero-char-consuming match
        return subClauseMatch == null ? new Match(this, memoEntry.startPos, /* len = */ 0) : null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = "!(" + subClauses[0] + ")";
        }
        return toStringCached;
    }
}
