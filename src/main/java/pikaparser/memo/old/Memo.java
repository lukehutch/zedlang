package pikaparser.memo.old;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Memo {
    /** The clause and start position of this memo. */
    public final MemoRef memoRef;

    /** The length of the match, or -1 if the clause didn't match at this position. */
    public final int len;

    /** The index of the subclause that matched, for FirstMatch clauses, else 0. */
    public int subClauseIdx;

    /** {@link Memo} entries for any matching subclauses. */
    public final List<Memo> matchingSubClauseMemos;

    /** The parent clauses that refer to this memo's clause at the position given by memoRef. */
    public Set<MemoRef> referencedBySuperExprs = Collections.newSetFromMap(new ConcurrentHashMap<MemoRef, Boolean>());

    /** Construct a new Memo. If len == -1, indicates no match. */
    public Memo(MemoRef memoRef, int len, List<Memo> matchingSubClauseMemos) {
        this.memoRef = memoRef;
        this.len = len;
        this.matchingSubClauseMemos = matchingSubClauseMemos == null || matchingSubClauseMemos.isEmpty() ? null
                : matchingSubClauseMemos;
    }

    /** Construct a new Memo. If len == -1, indicates no match. */
    public Memo(MemoRef memoRef, int len, Memo matchingSubClauseMemoRef) {
        this(memoRef, len, matchingSubClauseMemoRef == null ? null : Arrays.asList(matchingSubClauseMemoRef));
    }

    /** Construct a new Memo for a FirstMatch clause. If len == -1, indicates no match. */
    public Memo(MemoRef memoRef, int len, Memo matchingSubClauseMemoRef, int subClauseIdx) {
        this(memoRef, len, matchingSubClauseMemoRef);
        this.subClauseIdx = subClauseIdx;
    }

    /** Construct a new Memo for a terminal clause. If len == -1, indicates no match. */
    public Memo(MemoRef memoRef, int len) {
        this(memoRef, len, (List<Memo>) null);
    }

    public boolean matched() {
        return len >= 0;
    }

    @Override
    public String toString() {
        return memoRef + "+" + len;
    }
}
