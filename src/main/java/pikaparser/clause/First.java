package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;
import pikaparser.parser.Parser;

public class First extends Clause {

    First(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException(First.class.getSimpleName() + " expects 2 or more subclauses");
        }
    }

    @Override
    public void testWhetherCanMatchZeroChars() {
        for (int i = 0; i < subClauses.length; i++) {
            Clause subClause = subClauses[i];
            if (subClause.canMatchZeroChars) {
                canMatchZeroChars = true;
                if (i < subClauses.length - 1) {
                    throw new IllegalArgumentException("Subclause " + i + " of " + First.class.getSimpleName()
                            + " can evaluate to " + Nothing.class.getSimpleName()
                            + ", which means subsequent subclauses will never be matched: " + this);
                }
                break;
            }
        }
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        for (int subClauseIdx = 0; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMemoKey = new MemoKey(subClause, memoKey.startPos);
            var subClauseMatch = memoTable.lookUpMemo(matchDirection, subClauseMemoKey, input, memoKey,
                    updatedEntries);
            if (subClauseMatch != null) {
                return memoTable.addMatch(memoKey, subClauseIdx, /* terminalLen = */ 0,
                        new Match[] { subClauseMatch }, updatedEntries);
            }
        }
        if (Parser.DEBUG) {
            System.out.println("All subclauses failed to match at position " + memoKey.startPos + ": " + memoKey);
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
                    buf.append(" / ");
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
