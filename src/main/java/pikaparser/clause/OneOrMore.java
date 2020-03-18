package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class OneOrMore extends Clause {
    OneOrMore(Clause subClause) {
        super(new Clause[] { subClause });
    }

    @Override
    public void testWhetherAlwaysMatches() {
        if (subClauses[0].canMatchZeroChars) {
            canMatchZeroChars = true;
        }
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        var subClause = subClauses[0];
        List<Match> subClauseMatches = null;
        var currStartPos = memoKey.startPos;
        for (;;) {
            var subClauseMemoKey = new MemoKey(subClause, currStartPos);
            var subClauseMatch = memoTable.match(matchDirection, subClauseMemoKey, input, memoKey, updatedEntries);
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
                        subClauseMatches.toArray(Match.NO_SUBCLAUSE_MATCHES), updatedEntries);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            appendRulePrefix(buf);
            buf.append('(');
            if (subClauseASTNodeLabels != null && subClauseASTNodeLabels[0] != null) {
                buf.append(subClauseASTNodeLabels[0]);
                buf.append(':');
            }
            buf.append(subClauses[0].toString());
            buf.append(")+");
            appendRuleSuffix(buf);
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
