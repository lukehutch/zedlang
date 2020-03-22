package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class Longest extends Clause {

    Longest(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException(Longest.class.getSimpleName() + " expects 2 or more subclauses");
        }
    }

    @Override
    public void testWhetherCanMatchZeroChars() {
        for (int i = 0; i < subClauses.length; i++) {
            Clause subClause = subClauses[i];
            if (subClause.canMatchZeroChars) {
                canMatchZeroChars = true;
                break;
            }
        }
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        Match longestSubClauseMatch = null;
        int longestSubClauseMatchIdx = 0;
        for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMemoKey = new MemoKey(subClause, memoKey.startPos);
            var subClauseMatch = memoTable.lookUpMemo(matchDirection, subClauseMemoKey, input, memoKey,
                    updatedEntries);
            if (subClauseMatch != null
                    && (longestSubClauseMatch == null || longestSubClauseMatch.len < subClauseMatch.len)) {
                longestSubClauseMatch = subClauseMatch;
                longestSubClauseMatchIdx = subClauseIdx;
            }
        }
        return longestSubClauseMatch == null ? null
                : memoTable.addMatch(memoKey, longestSubClauseMatchIdx, new Match[] { longestSubClauseMatch },
                        updatedEntries);
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
                if (subClauseASTNodeLabels != null && subClauseASTNodeLabels[i] != null) {
                    buf.append(subClauseASTNodeLabels[i]);
                    buf.append(':');
                }
                buf.append(subClauses[i].toString());
            }
            buf.append(')');
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
