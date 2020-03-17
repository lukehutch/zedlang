package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class OneOrMore extends Clause {

    public OneOrMore(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public void testWhetherAlwaysMatches() {
        if (subClauses[0].alwaysMatches) {
            alwaysMatches = true;
        }
    }

    @Override
    public Match match(MemoTable memoTable, MemoKey memoKey, String input, Set<MemoEntry> newMatchMemoEntries) {
        var subClause = subClauses[0];
        List<Match> subClauseMatches = null;
        var currStartPos = memoKey.startPos;
        for (;;) {
            var subClauseMatch = memoTable.lookUpBestMatch(memoKey, new MemoKey(subClause, currStartPos), input,
                    newMatchMemoEntries);
            if (subClauseMatch == null) {
                break;
            }
            if (subClauseMatches == null) {
                subClauseMatches = new ArrayList<>();
            }
            subClauseMatches.add(subClauseMatch);
            if (subClauseMatch.len == 0) {
                // Prevent infinite loop -- if match consumed zero characters, can only match it once
                // (i.e. OneOrMore(Nothing) will match exactly one Nothing)
                break;
            }
            currStartPos += subClauseMatch.len;
        }
        return subClauseMatches == null ? null
                : memoTable.addMatch(memoKey, /* firstMatchingSubClauseIdx = */ 0,
                        subClauseMatches.toArray(Match.NO_SUBCLAUSE_MATCHES), newMatchMemoEntries);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = (ruleNodeLabel != null ? ruleNodeLabel + ':' : "") //
                    + (subClauseASTNodeLabels != null && subClauseASTNodeLabels[0] != null
                            ? subClauseASTNodeLabels[0] + ':'
                            : "") //
                    + subClauses[0] + "+";
        }
        return toStringCached;
    }
}
