package pikaparser.clause;

import java.util.Arrays;
import java.util.List;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

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
    public Match match(MemoEntry memoEntry, String input) {
        for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMatch = subClause.lookUpBestMatch(/* parentMemoEntry = */ memoEntry,
                    /* subClauseStartPos = */ memoEntry.startPos, input);
            if (subClauseMatch != null) {
                return new Match(this, subClauseIdx, subClauseMatch);
            }
        }
        return null;
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
                buf.append(subClauses[i].toString());
            }
            buf.append(')');
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
