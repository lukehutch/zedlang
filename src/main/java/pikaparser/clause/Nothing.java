package pikaparser.clause;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class Nothing extends Clause {

    public Nothing() {
        super(new Clause[0]);
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        return new Memo(memoRef, 0);
    }

    @Override
    public boolean isFirstOfRun(String input, int startPos) {
        // "Nothing" always matches before current position
        // This will mean that OneOrMore(Nothing) will never match -- have to use OneOrMoreSuffix(Nothing)
        return false;
    }

    @Override
    protected int minMatchLen() {
        return 0;
    }

    @Override
    public String toStr() {
        return "Nothing";
    }
}
