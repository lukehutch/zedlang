package zed.parser;

import java.util.ArrayList;

/** A reference to an expression match (or attempted match) that starts at a specific position. */
public class Memo {

    /** The expression */
    final Expr expr;

    /** The start index within the input sequence at which this expression match is attempted. */
    final int startIdx;

    /**
     * The end index within the input sequence at which this expression match is attempted. (If endIdx < startIdx, then
     * the expr does not (yet) match the input at this position.)
     */
    final int endIdx;

    /**
     * The memo entries for subexprs, one per subexpr of this expr.
     */
    final ArrayList<Memo> subExprMemos;

    protected static final ArrayList<Memo> EMPTY_SUBEXPR_MEMO_LIST = new ArrayList<>();

    public static final Memo DID_NOT_MATCH = new Memo(null, 0, -1, EMPTY_SUBEXPR_MEMO_LIST);

    Memo(Expr expr, int startIdx, int endIdx, ArrayList<Memo> subExprMemos) {
        this.expr = expr;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
        this.subExprMemos = subExprMemos;
    }

    /** An expression without sub-expressions matched at this position. */
    public static Memo matched(Expr expr, int startIdx, int endIdx) {
        return new Memo(expr, startIdx, endIdx, EMPTY_SUBEXPR_MEMO_LIST);
    }

    /** An expression with one or more sub-expressions matched at this position */
    public static Memo matched(Expr expr, ArrayList<Memo> subExprMemos) {
        if (subExprMemos == null || subExprMemos.isEmpty()) {
            throw new IllegalArgumentException("No sub-expressions");
        }
        return new Memo(expr, subExprMemos.get(0).startIdx, subExprMemos.get(subExprMemos.size() - 1).endIdx,
                subExprMemos);
    }

    /** An expression with one sub-expression matched at this position */
    public static Memo matched(Expr expr, Memo subExprMemo) {
        ArrayList<Memo> subExprMemos = new ArrayList<>();
        subExprMemos.add(subExprMemo);
        return new Memo(expr, subExprMemo.startIdx, subExprMemo.endIdx, subExprMemos);
    }

    /** An expression without subexpressions matched at this position, and didn't consume any input. */
    public static Memo matchedEmpty(Expr expr, int startIdx) {
        // startIdx == endIdx, indicating a match that consumes no input
        return new Memo(expr, startIdx, startIdx, EMPTY_SUBEXPR_MEMO_LIST);
    }

    /** A terminal expression matched at this position */
    public static Memo matchedTerminal(Expr expr, int startIdx) {
        // Consume one character of input per terminal
        return new Memo(expr, startIdx, startIdx + 1, EMPTY_SUBEXPR_MEMO_LIST);
    }

    /** Return true if this expression matches the input at this start position. */
    public boolean matches() {
        return endIdx >= startIdx;
    }

    public boolean isRule(String ruleName) {
        return expr.getRuleNames().contains(ruleName);
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String inputSeq) {
        StringBuilder buf = new StringBuilder();
        toString(inputSeq, this, 0, buf);
        return buf.toString();
    }

    private static void toString(String inputSeq, Memo memo, int depth, StringBuilder buf) {
        for (int i = 0; i < depth - 1; i++) {
            buf.append("  |");
        }
        if (depth > 0)
            buf.append("  ");
        buf.append("Rule " + memo.expr.getIdx() + " " + memo.expr.getRuleNames() + " ");
        buf.append((inputSeq == null ? memo.startIdx + "," + memo.endIdx : inputSeq.substring(memo.startIdx,
                memo.endIdx)) + "\n");
        for (Memo subExprMemo : memo.subExprMemos) {
            toString(inputSeq, subExprMemo, depth + 1, buf);
        }
    }
}
