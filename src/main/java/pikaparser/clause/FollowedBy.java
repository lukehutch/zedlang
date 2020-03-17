package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

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
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        var subClauseMemoKey = new MemoKey(subClauses[0], memoKey.startPos);
        var subClauseMatch = memoTable.match(matchDirection, subClauseMemoKey, input, memoKey, updatedEntries);
        // Replace any valid subclause match with a zero-char-consuming match
        if (subClauseMatch != null) {
            return memoTable.addMatch(memoKey, /* firstMatchingSubClauseIdx = */ 0, new Match[] { subClauseMatch },
                    updatedEntries);
        }
        return null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            appendRulePrefix(buf);
            buf.append("&(");
            if (subClauseASTNodeLabels != null && subClauseASTNodeLabels[0] != null) {
                buf.append(subClauseASTNodeLabels[0]);
                buf.append(':');
            }
            buf.append(subClauses[0].toString());
            buf.append(')');
            appendRuleSuffix(buf);
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
