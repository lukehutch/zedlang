package pikaparser.clause;

import java.util.ArrayList;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class OneOrMore extends Clause {

    private boolean suffixMatch;

    public OneOrMore(Clause subClause) {
        this(subClause, /* suffixMatch = */ false);
    }

    protected OneOrMore(Clause subClause, boolean suffixMatch) {
        super(new Clause[] { subClause });
        this.suffixMatch = suffixMatch;
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        var matchingSubClauseMemos = new ArrayList<Memo>(subClauses.length);
        var currPos = memoRef.startPos;
        for (boolean first = true; currPos < input.length();) {
            var subClauseMemoRef = new MemoRef(subClauses[0], currPos);
            var subClauseMemo = lookUpSubClauseMemo(input, memoRef, subClauseMemoRef);
            if (!subClauseMemo.matched()) {
                // Ran out of subclause matches
                break;
            } else {
                if (first) {
                    if (!suffixMatch && !subClauses[0].isFirstOfRun(input, currPos)) {
                        // For non-suffix matches: this subclause matched, but was not the first of a run => no match
                        break;
                    }
                    first = false;
                }
                matchingSubClauseMemos.add(subClauseMemo);
                if (subClauseMemo.len == 0) {
                    // Prevent infinite loop -- if match consumed zero characters, can only match it once
                    // (i.e. OneOrMore(Nothing) will match exactly one Nothing)
                    break;
                } else {
                    currPos += subClauseMemo.len;
                }
            }
        }
        return new Memo(memoRef, !matchingSubClauseMemos.isEmpty() ? currPos - memoRef.startPos : -1,
                matchingSubClauseMemos);
    }

    @Override
    public String toStr() {
        return subClauses[0] + "+";
    }
}
