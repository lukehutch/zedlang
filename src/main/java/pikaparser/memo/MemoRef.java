package pikaparser.memo;

import pikaparser.clause.Clause;

public class MemoRef {
    public Clause clause;
    public int startPos;

    public MemoRef(Clause clause, int startPos) {
        this.clause = clause;
        this.startPos = startPos;
    }

    @Override
    public int hashCode() {
        return clause.hashCode() + startPos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj instanceof MemoRef)) {
            return false;
        } else {
            MemoRef o = (MemoRef) obj;
            return o.clause == this.clause && o.startPos == this.startPos;
        }
    }

    @Override
    public String toString() {
        return clause.toStringWithRuleNames() + " : " + startPos;
    }
}
