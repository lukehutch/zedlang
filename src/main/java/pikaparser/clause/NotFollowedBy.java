package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class NotFollowedBy extends Clause {

    public NotFollowedBy(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public Match match(MemoTable memoTable, MemoKey memoKey, String input, Set<MemoEntry> newMatchMemoEntries) {
        var subClauseMatch = memoTable.lookUpBestMatch(memoKey, new MemoKey(subClauses[0], memoKey.startPos), input,
                newMatchMemoEntries);
        // Replace any invalid subclause match with a zero-char-consuming match
        if (subClauseMatch == null) {
            return memoTable.addMatch(memoKey, /* firstMatchingSubClauseIdx = */ 0, new Match[] { subClauseMatch },
                    newMatchMemoEntries);
        }
        return null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = (ruleNodeLabel != null ? ruleNodeLabel + ':' : "") //
                    + "!(" //
                    + (subClauseASTNodeLabels != null && subClauseASTNodeLabels[0] != null
                            ? subClauseASTNodeLabels[0] + ':'
                            : "")
                    + subClauses[0] + ")";
        }
        return toStringCached;
    }
}
