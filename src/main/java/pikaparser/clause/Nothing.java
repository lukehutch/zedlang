package pikaparser.clause;

import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public class Nothing extends Clause {

    public Nothing() {
        super(new Clause[0]);
    }

    @Override
    public Memo match(String input, MemoRef memoRef, boolean isFirstMatchPosition) {
        return new Memo(memoRef, 0);
    }

    @Override
    public boolean isFirstOfRun(String input, int startPos) {
        // "Nothing" always matches before current position
        // This will mean that OneOrMore(Nothing) will never match -- have to use OneOrMoreSuffix(Nothing)
        return false;
    }

    @Override
    public String toStr() {
        return "()";
    }
}
