package pikaparser.clause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class Seq extends Clause {

    Seq(Clause... subClauses) {
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
        canMatchZeroChars = true;
        for (Clause subClause : subClauses) {
            if (!subClause.canMatchZeroChars) {
                canMatchZeroChars = false;
                break;
            }
        }
    }

    @Override
    public List<Clause> getSeedSubClauses() {
        // Any sub-clause up to and including the first clause that requires a non-zero-char match could be
        // the matching clause.
        List<Clause> seedSubClauses = new ArrayList<>(subClauses.length);
        for (int i = 0; i < subClauses.length; i++) {
            seedSubClauses.add(subClauses[i]);
            if (!subClauses[i].canMatchZeroChars) {
                // Don't need to seed any subsequent subclauses
                break;
            }
        }
        return seedSubClauses;
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Collection<MemoEntry> updatedEntries) {
        Match[] subClauseMatches = null;
        var currStartPos = memoKey.startPos;
        for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMemoKey = new MemoKey(subClause, currStartPos);
            var subClauseMatch = memoTable.lookUpMemo(matchDirection, subClauseMemoKey, input, memoKey, updatedEntries);
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
        return memoTable.addMatch(memoKey, /* firstMatchingSubClauseIdx = */ 0, subClauseMatches, updatedEntries);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            appendRulePrefix(buf);
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
            appendRuleSuffix(buf);
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
