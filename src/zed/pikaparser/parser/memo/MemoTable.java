package zed.pikaparser.parser.memo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import zed.pikaparser.grammar.Grammar;
import zed.pikaparser.grammar.expr.Expr;

public class MemoTable {
    /**
     * A weak reference to a memo in the memo table, used to look up the current value in the memo table for a given
     * expression and input position.
     */
    public static class MemoRef {
        /** Expression for the memo entry. */
        public Expr expr;
        
        /** Position of the memo entry within the input string. */
        public int pos;

        public MemoRef(Expr expr, int startIdx) {
            if (expr == null) {
                throw new IllegalArgumentException("Expr cannot be null");
            }
            this.expr = expr;
            this.pos = startIdx;
        }

        @Override
        public int hashCode() {
            return expr.idx * 57 + pos;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof MemoRef)) {
                return false;
            }
            MemoRef o = (MemoRef) obj;
            return pos == o.pos && expr == o.expr;
        }

        @Override
        public String toString() {
            return expr.idx + "@" + pos;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    public Grammar grammar;
    public String input;

    /**
     * Allocate the memo table. We have a separate skiplist for each expression type, so that it's easy to recover after
     * syntax errors by scanning ahead in the memo table for the next matching memo of a given expression type.
     */
    private final ArrayList<ConcurrentSkipListMap<Integer, Memo>> exprIdxToMemos;

    // -----------------------------------------------------------------------------------------------------------------

    public MemoTable(Grammar grammar, String input) {
        this.grammar = grammar;
        this.input = input;
        this.exprIdxToMemos = new ArrayList<>();
        for (int i = 0; i < grammar.getExprs().length; i++) {
            exprIdxToMemos.add(new ConcurrentSkipListMap<>());
        }
    }

    /**
     * Get the memo table entry for the given memo reference (representing a specific expression and input position), or
     * null if there is no corresponding memo.
     */
    public Memo getEntry(MemoRef memoRef) {
        return exprIdxToMemos.get(memoRef.expr.idx).get(memoRef.pos);
    }

    /**
     * Update the memo table with a new memo entry.
     * 
     * @return The old memo at this location in the memo table, if any.
     */
    public Memo updateEntry(Memo memo) {
        return exprIdxToMemos.get(memo.ref.expr.idx).put(memo.ref.pos, memo);
    }

    /**
     * Update the memo table with a new memo entry, as long as the table doesn't yet contain an entry at that location.
     * 
     * @return The old memo at this location in the memo table, if any. Returns null if there was no memo at the memo
     *         location, indicating that the table was updated to include the new memo.
     */
    public Memo setEntryIfAbsent(Memo memo) {
        return exprIdxToMemos.get(memo.ref.expr.idx).putIfAbsent(memo.ref.pos, memo);
    }

    /**
     * Set all memo table entries for a given expression.
     */
    public ConcurrentSkipListMap<Integer, Memo> getAllMemosForExpr(Expr expr) {
        return exprIdxToMemos.get(expr.idx);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public void printTable(TreeMap<Integer, Integer> syntaxErrors) {
        Expr[] exprs = grammar.getExprs();

        // Print memos
        System.out.println();
        for (int i = 0; i < exprs.length; i++) {
            ConcurrentSkipListMap<Integer, Memo> startIdxToMemo = exprIdxToMemos.get(i);
            System.out.print(i + " :");
            for (Memo memo : startIdxToMemo.values()) {
                System.out.print(" " + memo.ref + "-" + memo.endPos + ":" + (memo.matches() ? "Y" : "N"));
            }
            System.out.println();
        }

        // Print memo table
        System.out.println();
        for (int i = 0; i < exprs.length; i++) {
            char[] matchDisplay = new char[input.length()];
            Arrays.fill(matchDisplay, '-');
            ConcurrentSkipListMap<Integer, Memo> startIdxToMemo = exprIdxToMemos.get(i);
            for (Memo memo : startIdxToMemo.values()) {
                for (int j = memo.ref.pos; j < memo.endPos; j++) {
                    if (j == memo.ref.pos) {
                        matchDisplay[j] = memo.matches() ? 'Y' : 'N';
                    } else {
                        matchDisplay[j] = '=';
                    }
                }
            }
            System.out.println((i + "  ").substring(0, 3) + " " + new String(matchDisplay));
        }
        String indent = "    ";
        System.out.print(indent);
        for (int i = 0; i < input.length(); i++) {
            System.out.print(i % 10);
        }
        System.out.println();
        System.out.println(indent + input.replace('\n', '^'));

        // Highlight any syntax errors
        if (syntaxErrors != null && syntaxErrors.size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                buf.append(' ');
            }
            for (Entry<Integer, Integer> ent : syntaxErrors.entrySet()) {
                int startIdx = ent.getKey(), endIdx = ent.getValue();
                for (int i = startIdx; i < endIdx; i++) {
                    buf.setCharAt(i, '^');
                }
            }
            System.out.println(indent + buf);
        }
    }
}
