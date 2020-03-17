package pikaparser.clause;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class FirstMatch extends Clause {

    public FirstMatch(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException(FirstMatch.class.getSimpleName() + " expects 2 or more subclauses");
        }
    }

    public FirstMatch(List<Clause> subClauses) {
        this(subClauses.toArray(new Clause[0]));
    }

    @Override
    public void testWhetherAlwaysMatches() {
        for (Clause subClause : subClauses) {
            if (subClause.alwaysMatches) {
                alwaysMatches = true;
                break;
            }
        }
    }

    @Override
    public List<Clause> getSeedSubClauses() {
        // Any sub-clause could be the matching clause, so need to override this
        return Arrays.asList(subClauses);
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMemoKey = new MemoKey(subClause, memoKey.startPos);
            var subClauseMatch = memoTable.match(matchDirection, memoKey, subClauseMemoKey, input, updatedEntries);
            if (subClauseMatch != null) {
                return memoTable.addMatch(memoKey, subClauseIdx, new Match[] { subClauseMatch }, updatedEntries);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            if (ruleNodeLabel != null) {
                buf.append(ruleNodeLabel);
                buf.append(':');
            }
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
