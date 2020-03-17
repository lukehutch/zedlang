package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

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
    public Match match(MemoTable memoTable, MemoKey memoKey, String input, Set<MemoEntry> newMatchMemoEntries) {
        Match[] subClauseMatches = null;
        var currStartPos = memoKey.startPos;
        for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMatch = memoTable.lookUpBestMatch(memoKey, new MemoKey(subClause, currStartPos), input,
                    newMatchMemoEntries);
            if (subClauseMatch == null) {
                // Fail after first subclause fails to match
                return null;
            }
            if (subClauseMatches == null) {
                subClauseMatches = new Match[subClauses.length];
            }
            subClauseMatches[subClauseIdx] = subClauseMatch;
            currStartPos += subClauseMatch.len;
        }
        return memoTable.addMatch(memoKey, /* firstMatchingSubClauseIdx = */ 0, subClauseMatches,
                newMatchMemoEntries);
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
                    buf.append(" ");
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
