package zed.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Parser {

    String input;

    private Expr topExpr;

    private ArrayList<HashMap<Integer, Memo>> memosForExpr = new ArrayList<>();

    // Handle left recursion -- a nightmare algorithm to try to figure out how to implement based
    // on the paper, but it works...
    // See "Packrat Parsers Can Support Left Recursion", Warth, Douglas, Millstein.
    // http://www.vpri.org/pdf/tr2007002_packrat.pdf
    public static class LeftRecursionCheckMemo extends Memo {

        public Memo seed = Memo.DID_NOT_MATCH;

        public Head head;

        public LeftRecursionCheckMemo(Expr expr) {
            // No match by default
            super(expr, 0, -1, EMPTY_SUBEXPR_MEMO_LIST);
            this.seed = Memo.DID_NOT_MATCH;
            this.head = null;
        }
    }

    public static class Head {
        public Expr expr;
        public HashSet<Expr> involvedSet, evalSet;

        public Head(Expr expr) {
            this.expr = expr;
        }
    }

    private Head[] heads;

    private ArrayList<LeftRecursionCheckMemo> lrStack = new ArrayList<>();

    /** Look up the current longest memoized match for the given expression starting from the given input index. */
    Memo matchMemoized(Expr subExpr, int startIdx) {
        // RECALL
        // Find memo for subexpression
        HashMap<Integer, Memo> memosForSubExpr = memosForExpr.get(subExpr.getIdx());
        Memo memo = memosForSubExpr.get(startIdx);
        Head head = startIdx < heads.length ? heads[startIdx] : null;
        // If not growing a seed parse, just use what is stored in the memo table
        if (head != null) {
            // Do not evaluate any rule that is not involved in this left recursion
            if (memo == null && subExpr != head.expr
                    && (head.involvedSet == null || !head.involvedSet.contains(subExpr))) {
                return Memo.DID_NOT_MATCH;
            } else if (head.evalSet.remove(subExpr)) {
                // Allow involved rules to be evaluated, but only once, during a seed-growing iteration
                memo = subExpr.match(this, startIdx);
            }
        }

        // APPLY-RULE
        if (memo == null) {
            LeftRecursionCheckMemo lrCheck = new LeftRecursionCheckMemo(subExpr);
            lrStack.add(lrCheck);
            memosForSubExpr.put(startIdx, lrCheck);

            // Try matching subexpression
            memo = subExpr.match(this, startIdx);

            lrStack.remove(lrStack.size() - 1);

            if (lrCheck.head == null) {
                memosForSubExpr.put(memo.startIdx, memo);
                return memo;
            } else {
                lrCheck.seed = memo;

                // LR-ANSWER
                Head h = lrCheck.head;
                if (h.expr != subExpr) {
                    return lrCheck.seed;
                } else {
                    memosForSubExpr.put(memo.startIdx, memo);
                    if (!lrCheck.seed.matches()) {
                        // Return FAIL
                        return lrCheck.seed;
                    } else {
                        // GROW-LR
                        heads[startIdx] = h;
                        // Grow left recursive seed
                        for (;;) {
                            h.evalSet = h.involvedSet == null ? new HashSet<>() : new HashSet<>(h.involvedSet);
                            int prevEndIdx = memo.endIdx;
                            Memo subExprMemo = subExpr.match(this, startIdx);
                            if (!subExprMemo.matches() || subExprMemo.endIdx <= prevEndIdx) {
                                break;
                            }
                            memo = subExprMemo;
                            memosForSubExpr.put(memo.startIdx, memo);
                        }
                        heads[startIdx] = null;
                        return memo;
                    }
                }
            }

        } else if (memo instanceof LeftRecursionCheckMemo) {
            // Hit this left recursion check for the second time, so this is a left recursion. Create seed match
            // and return a non-matching response (memo.matches() will return false for LeftRecursionCheckMemos)
            LeftRecursionCheckMemo lr = (LeftRecursionCheckMemo) memo;

            // SETUP-LR
            if (lr.head == null) {
                lr.head = new Head(subExpr);
            }
            for (int i = lrStack.size() - 1; i >= 0; --i) {
                LeftRecursionCheckMemo s = lrStack.get(i);
                if (s.head == lr.head) {
                    break;
                }
                s.head = lr.head;
                if (lr.head.involvedSet == null) {
                    lr.head.involvedSet = new HashSet<>();
                }
                lr.head.involvedSet.add(s.expr);
            }

            return lr.seed;

        } else {
            return memo;
        }
    }

    public Parser(String input, Grammar grammar) {
        this.input = input;
        this.topExpr = grammar.getTopExpr();
        for (int i = 0; i < grammar.getExprs().length; i++) {
            memosForExpr.add(new HashMap<>());
        }
        this.heads = new Head[input.length()];
    }

    public Memo parse() {
        // TODO: avoid allocating Memo nodes for all the terminals.

        Memo topMatch = matchMemoized(topExpr, 0);

        // See if TOP matched the whole input
        if (!topMatch.matches()) {
            // TODO: Figure out how to give meaningful errors
            // TODO: and keep track of max successful matched endIdx so far, so syntax err position can be reported
            throw new IllegalArgumentException("Did not match input");
        } else if (topMatch.endIdx < input.length()) {
            throw new IllegalArgumentException("There were " + (input.length() - topMatch.endIdx)
                    + " unmatched characters at the end of the input: " + input.substring(topMatch.endIdx));
        }

        return topMatch;
    }
}
