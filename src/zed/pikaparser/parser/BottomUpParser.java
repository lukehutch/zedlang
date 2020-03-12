package zed.pikaparser.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import zed.pikaparser.grammar.expr.Char;
import zed.pikaparser.grammar.expr.Expr;
import zed.pikaparser.grammar.expr.Nothing;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class BottomUpParser extends DirectionalParser {
    private Set<MemoRef> activeSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoRef, Boolean>());

    public BottomUpParser(MemoTable memoTable) {
        super(memoTable);

        // Find leaf nodes in grammar
        ArrayList<Char> charExprs = new ArrayList<>();
        Expr nothingExpr = null;
        Expr[] exprs = memoTable.grammar.getExprs();
        for (int i = 0; i < exprs.length; i++) {
            Expr expr = exprs[i];
            if (expr instanceof Char) {
                // Handle char terminals specially (don't just add them to the active set, in order to avoid
                // creating DID_NOT_MATCH memo entries everywhere that a given terminal didn't match)
                charExprs.add((Char) expr);
            } else if (expr instanceof Nothing) {
                nothingExpr = expr;
            }
        }

        // Find all non-terminals that match at each position, and add memo entries for their superexprs if they match
        // at the given position.

        // N.B. the alternative to this is to put a matching memo for the terminal itself into updatedMemos, then
        // change the order of the two sub-passes during the bottom-up parsing loop, then remove the special-casing
        // for terminals (Char and Nothing) in BottomUpParser.matchMemoized(). However, this will result in more
        // total memo entries.

        // TODO: don't store entries for all these, the memo table entries will never change, since the match-status of terminals can never change. **********************
        
        // TODO: parallelize this
        for (int inputIdx = 0; inputIdx < memoTable.input.length(); inputIdx++) {
            char c = memoTable.input.charAt(inputIdx);
            for (int j = 0; j < charExprs.size(); j++) {
                // TODO: Speed this up by producing a mapping from char to terminals that match the char
                // TODO: (at least for ASCII chars) 
                Char charExpr = charExprs.get(j);
                // We only add a memo if the terminal expr matches, to save on memory (DID_NOT_MATCH memo entries
                // will be added on demand if exprs try to look for a non-matching character in a given position)
                if (charExpr.matches(c)) {
                    for (Expr superExpr : charExpr.superExprsWithThisAsFirstSubExpr) {
                        activeSet.add(new MemoRef(superExpr, inputIdx));
                    }
                }
            }
        }

        // "Nothing" matches everywhere, including at the end of the input (note the "<= input.length()")
        if (nothingExpr != null) {
            for (int inputIdx = 0; inputIdx <= memoTable.input.length(); inputIdx++) {
                // TODO: can Nothing exprs be the first expr of any valid expr? I think if Nothing is the first expr, it can always be factored out during grammar-building ************************ 
                for (Expr superExpr : nothingExpr.superExprsWithThisAsFirstSubExpr) {
                    activeSet.add(new MemoRef(superExpr, inputIdx));
                }
            }
        }
    }

    @Override
    public void parse() {
        // TODO: parallelize this.
        for (;;) {
            // (1) Match exprs in the current active set.
            ConcurrentLinkedQueue<Memo> updatedMemos = new ConcurrentLinkedQueue<>();
            for (MemoRef memoRef : activeSet) {
                // See if there's an existing memo entry for this memoRef
                Memo oldMemo = memoTable.getEntry(memoRef);
                // Try matching the referenced expr at this position
                Memo newMemo = memoRef.expr.match(this, memoRef);
                // Check if the memo has changed
                if (!newMemo.equals(oldMemo)) {
                    // Deal with left recursion in the bottom-up parse:
                    // Make sure the memo match has increased in length, otherwise it is possible to get in a cycle,
                    // e.g. for the grammar:
                    //
                    // P = P x / y
                    // L = P x
                    //
                    // Because if L matches at position i, P will match at position i, but then L will appear to no
                    // longer match, because P P x doesn't match at position i.
                    // By requiring the length of memos for the same expr at the same position to increase before a
                    // memo at this position will be updated, we can never get stuck in a loop, and the bottom-up
                    // stage is guaranteed to terminate.

                    // NB. The following length comparison also handles the case of a matching memo replacing a
                    // non-matching memo, since, for a non-matching memo in the memo table, matchLen() is -1.
                    if (oldMemo == null || newMemo.matchLen() > oldMemo.matchLen()) {
                        // Memo has changed -- schedule the memo for updating at the beginning of the next iteration
                        updatedMemos.add(newMemo);
                    }
                }
            }
            // TODO: barrier here

            // Clear the active set for the next iteration
            activeSet.clear();

            // (2) Empty the updated memos queue, updating memos in the memo table if the memoized expr match has
            // changed, and build the active set for the next iteration
            for (Memo updatedMemo; (updatedMemo = updatedMemos.poll()) != null;) {
                // Update memo table with the changed memo
                Memo oldMemo = memoTable.updateEntry(updatedMemo);
                // Put all superexprs that have this expr as the first subexpr into the active set
                for (Expr superExpr : updatedMemo.ref.expr.superExprsWithThisAsFirstSubExpr) {
                    activeSet.add(new MemoRef(superExpr, updatedMemo.ref.pos));
                }
                // For any backlink refs from old memo to superexpressions that referred to the memo,
                // the superexpressions need to be scheduled to be in the active set for the next iteration.
                if (oldMemo != null) {
                    // Put all superexprs that have referred to this memo since it last change into the active set 
                    for (MemoRef superExprRef : oldMemo.referencedBySuperExprs) {
                        activeSet.add(superExprRef);
                    }
                }
            }
            // TODO: barrier here

            if (activeSet.isEmpty()) {
                // Nothing changed
                break;
            }
        }
    }

    /**
     * Look up a Memo in the memo table given a MemoRef during bottom-up traversal. If memo doesn't exist, create a
     * non-matching placeholder and store it in the memo table.
     */
    @Override
    public Memo matchMemoized(MemoRef superExprRef, MemoRef subExprRef) {

        // FIXME: look for a way to not store matchedEmpty() and DID_NOT_MATCH in the memo table **********************************

        // See if a memo with this MemoRef exits in the memo table. 
        // When parsing bottom-up, if a memo doesn't exist, put a non-match placeholder memo
        // in the memo table at thie current position, because we need to place a backlink
        // from the referenced subexpr memo to its referencing superexpr memos.
        Memo subExprMemo = memoTable.getEntry(subExprRef);
        if (subExprMemo == null) {
            // Special-case handling of terminals (Char and Nothing), to avoid storing entries in the memo table
            // unless they are actually used by a superexpression (if the superexpr contains a FirstMatch,
            // and this terminal is the second or subsequent option, the terminal may never be referenced).
            if (subExprRef.expr instanceof Nothing) {
                // Nothing always matches
                subExprMemo = Memo.matchedEmpty(subExprRef);

            } else if (subExprRef.expr instanceof Char) {
                if (subExprRef.pos < memoTable.input.length()
                        && ((Char) subExprRef.expr).matches(memoTable.input.charAt(subExprRef.pos))) {
                    subExprMemo = Memo.matchedChar(subExprRef);
                } else {
                    subExprMemo = Memo.DID_NOT_MATCH(subExprRef);
                }
            } else {
                // Referencing a sub-expr that hasn't yet matched in the bottom-up parse
                subExprMemo = Memo.DID_NOT_MATCH(subExprRef);
            }
            // FIXME: Is it possible to remove the race by working on superexprs rather than subexprs? ****************************
            Memo raceMemo = memoTable.setEntryIfAbsent(subExprMemo);
            if (raceMemo != null) {
                // Another thread beat us to updating the map, use the version put by the other thread instead
                subExprMemo = raceMemo;
            }
        }

        // Record the back-reference from the subexpr to its referencing superexpr.
        // There's no need to record a back-reference if this subexpr is the first child expr of the superexpr,
        // because first child exprs always add their superexpr to the active set.
        // FIXME: make this consistent between the two cases, so that this special case logic doesn't have to be here? ************
        if (superExprRef.expr.subExprs.get(0) != subExprRef.expr) {
            subExprMemo.referencedBySuperExprs.add(superExprRef);
        }

        return subExprMemo;
    }
}