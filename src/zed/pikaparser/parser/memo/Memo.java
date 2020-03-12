package zed.pikaparser.parser.memo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import zed.pikaparser.parser.memo.MemoTable.MemoRef;

/** A reference to an expression match (or attempted match) that starts at a specific position. */
public class Memo {

    public final MemoRef ref;

    /**
     * The end index of the match within the input sequence. If endPos < ref.pos, then the expr does not (yet) match
     * the input at the start position indicated by ref.pos.
     */
    public int endPos;

    /**
     * The memo entries for subexprs, one per subexpr of this expr.
     */
    public final ArrayList<Memo> subExprMemos;

    /** The superexprs that refer to this memo's expr at this position. */
    public Set<MemoRef> referencedBySuperExprs = Collections.newSetFromMap(new ConcurrentHashMap<MemoRef, Boolean>());

    protected static final ArrayList<Memo> EMPTY_SUBEXPR_MEMO_LIST = new ArrayList<>();

    public static final Memo DID_NOT_MATCH(MemoRef ref) {
        // endPos < ref.pos, indicating there was no match at position ref.pos
        return new Memo(ref, ref.pos - 1);
    }

    Memo(MemoRef ref, int endPos) {
        this.ref = ref;
        this.endPos = endPos;
        this.subExprMemos = EMPTY_SUBEXPR_MEMO_LIST;
    }

    /** Duplicate this memo entry, copying the subexpr list (allows the subexpr list to be modified). */
    private Memo(Memo other) {
        this.ref = other.ref;
        this.endPos = other.endPos;
        this.subExprMemos = new ArrayList<>(other.subExprMemos);
        this.referencedBySuperExprs = other.referencedBySuperExprs;
    }

    private Memo(MemoRef ref, int endPos, ArrayList<Memo> subExprMemos) {
        this.ref = ref;
        this.endPos = endPos;
        this.subExprMemos = subExprMemos;
    }

    /** Copy a memo entry and then add a new child memo to the entry. */
    public static Memo addChild(Memo other, Memo childToAdd) {
        Memo copy = new Memo(other);
        copy.subExprMemos.add(childToAdd);
        copy.endPos = childToAdd.endPos;
        return copy;
    }

    /** Copy a memo entry and then replace one of the child memos with the given memo entry. */
    public static Memo replaceChild(Memo other, int childPos, Memo replacementChild) {
        Memo copy = new Memo(other);
        copy.subExprMemos.set(childPos, replacementChild);
        return copy;
    }

    /** An expression without sub-expressions matched at this position. */
    public static Memo matched(MemoRef ref, int endPos) {
        return new Memo(ref, endPos, EMPTY_SUBEXPR_MEMO_LIST);
    }

    /** An expression with one or more sub-expressions matched at this position */
    public static Memo matched(MemoRef ref, ArrayList<Memo> subExprMemos) {
        if (subExprMemos == null || subExprMemos.isEmpty()) {
            throw new IllegalArgumentException("No sub-expressions");
        }
        return new Memo(ref, subExprMemos.get(subExprMemos.size() - 1).endPos, subExprMemos);
    }

    /** An expression with one sub-expression matched at this position */
    public static Memo matched(MemoRef ref, Memo subExprMemo) {
        ArrayList<Memo> subExprMemos = new ArrayList<>();
        subExprMemos.add(subExprMemo);
        return new Memo(ref, subExprMemo.endPos, subExprMemos);
    }

    /** An expression without subexpressions matched at this position, and didn't consume any input. */
    public static Memo matchedEmpty(MemoRef ref) {
        // endPos == ref.pos, indicating a match that consumes no input
        return new Memo(ref, ref.pos, EMPTY_SUBEXPR_MEMO_LIST);
    }

    /** A terminal, single-character expression matched at this position */
    public static Memo matchedChar(MemoRef ref) {
        // Consume one character of input per terminal
        return new Memo(ref, ref.pos + 1, EMPTY_SUBEXPR_MEMO_LIST);
    }

    /** Return the number of characters matched, or < 0 if there was no match. */
    public int matchLen() {
        return endPos - ref.pos;
    }

    /** Return true if this expression matches the input at this start position. */
    public boolean matches() {
        return endPos >= ref.pos;
    }

    public boolean isRule(String ruleName) {
        return ref.expr.ruleNames.contains(ruleName);
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String inputSeq) {
        StringBuilder buf = new StringBuilder();
        toString(inputSeq, this, 0, buf);
        buf.append(" " + matches());
        return buf.toString();
    }

    private static void toString(String inputSeq, Memo memo, int depth, StringBuilder buf) {
        for (int i = 0; i < depth - 1; i++) {
            buf.append("  |");
        }
        if (depth > 0)
            buf.append("  ");
        buf.append("Rule " + memo.ref.expr.idx + " " + memo.ref.expr.ruleNames + " ");
        buf.append((inputSeq == null ? memo.ref.pos + "," + memo.endPos : inputSeq.substring(memo.ref.pos, memo.endPos))
                + "\n");
        for (Memo subExprMemo : memo.subExprMemos) {
            toString(inputSeq, subExprMemo, depth + 1, buf);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Memo)) {
            return false;
        }
        Memo o = (Memo) obj;
        if (!ref.equals(o.ref)) {
            return false;
        }
        if (endPos != o.endPos) {
            return false;
        }
        // Perform shallow (non-recursive) check of child memos (can just do a pointer comparison --
        // if child memos have changed, their pointer will have changed, since we don't do in-place updates)
        if (subExprMemos.size() != o.subExprMemos.size()) {
            return false;
        }
        for (int i = 0, i2 = subExprMemos.size(); i < i2; i++) {
            if (subExprMemos.get(i) != o.subExprMemos.get(i)) {
                return false;
            }
        }
        return true;
    }
}
