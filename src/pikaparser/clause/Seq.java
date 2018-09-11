package pikaparser.clause;

import java.util.ArrayList;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class Seq extends Clause {

    public Seq(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException("Expected 2 or more subclauses");
        }
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        var matchedSubClauseMemo = new ArrayList<Memo>();
        var currPos = memoRef.startPos;
        var matched = true;
        for (var subClause : subClauses) {
            var subClauseMemoRef = new MemoRef(subClause, currPos);
            var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef);
            if (!subClauseMemo.matched()) {
                matched = false;
                break;
            } else {
                matchedSubClauseMemo.add(subClauseMemo);
                currPos += subClauseMemo.len;
            }
        }
        return new Memo(memoRef, matched ? currPos - memoRef.startPos : -1, matchedSubClauseMemo);
    }

    @Override
    public String toStr() {
        var buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < subClauses.length; i++) {
            if (i > 0) {
                buf.append(" ");
            }
            buf.append(subClauses[i].toString());
        }
        buf.append(')');
        return buf.toString();
    }
}
