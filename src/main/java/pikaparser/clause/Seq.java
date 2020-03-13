package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class Seq extends Clause {

    public Seq(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException(Seq.class.getSimpleName() + " expects 2 or more subclauses");
        }
    }

    public Seq(List<Clause> subClauses) {
        this(subClauses.toArray(new Clause[0]));
    }

    @Override
    public void testWhetherAlwaysMatches() {
        // For Seq, all subclauses must always match for the whole clause to always match
        alwaysMatches = true;
        for (Clause subClause : subClauses) {
            if (!subClause.alwaysMatches) {
                alwaysMatches = false;
                break;
            }
        }
    }

    @Override
    public List<Clause> getSeedSubClauses() {
        // Any sub-clause up to and including the first clause that doesn't always match could be the matching clause
        List<Clause> seedSubClauses = new ArrayList<>(subClauses.length);
        for (int i = 0; i < subClauses.length; i++) {
            seedSubClauses.add(subClauses[i]);
            if (!subClauses[i].alwaysMatches) {
                break;
            }
        }
        return seedSubClauses;
    }

    @Override
    public Match match(MemoEntry memoEntry, String input) {
        var subClauseMatches = (List<Match>) null;
        var currStartPos = memoEntry.startPos;
        for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMatch = subClause.lookUpBestMatch(/* parentMemoEntry = */ memoEntry,
                    /* subClauseStartPos = */ currStartPos, input);
            if (subClauseMatch == null) {
                return null;
            }
            if (subClauseMatches == null) {
                subClauseMatches = new ArrayList<>(subClauses.length);
            }
            subClauseMatches.add(subClauseMatch);
            currStartPos += subClauseMatch.len;
        }
        if (subClauseMatches == null) {
            // Should not happen, because Seq constructor requires at least 2 subclauses
            throw new IllegalArgumentException("No subclauses");
        }
        return new Match(this, /* firstMatchingSubClauseIdx = */ 0, subClauseMatches);
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
