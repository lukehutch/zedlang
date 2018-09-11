package pikaparser.clause;

import java.util.ArrayList;
import java.util.List;

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
    protected List<Clause> getTriggerSubClauses() {
        // Return any initial terms in the sequence that can consume zero characters 
        List<Clause> triggerSubClauses = new ArrayList<>(subClauses.length);
        int minMatchLen = 0;
        for (int i = 0; i < subClauses.length; i++) {
            minMatchLen += subClauses[i].minMatchLen();
            if (minMatchLen == 0
                    // Always add first subclause as a trigger subclause
                    || i == 0) {
                triggerSubClauses.add(subClauses[i]);
            } else {
                break;
            }
        }
        return triggerSubClauses;
    }

    @Override
    protected int minMatchLen() {
        int minMatchLen = 0;
        for (int i = 0; i < subClauses.length; i++) {
            minMatchLen += subClauses[i].minMatchLen();
        }
        return minMatchLen;
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
