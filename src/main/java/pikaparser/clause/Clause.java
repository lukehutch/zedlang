package pikaparser.clause;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public abstract class Clause {

    public Set<String> ruleNames = new HashSet<>();
    public Clause[] subClauses;

    /** A map from startPos to Memo for both successful and unsuccessful matches of this clause. */
    public ConcurrentSkipListMap<Integer, Memo> startPosToMemo = new ConcurrentSkipListMap<>();

    /** A set of all end positions (startPos + len) for successful matches of this clause. */
    private Set<Integer> matchEndPositions = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

    /** The parent clauses to trigger when this clause's match memo at a given position changes. */
    private Set<Clause> triggerParentClauses = new HashSet<>();

    // -----------------------------------------------------------------------------------------------------------------

    public Clause(Clause... subClauses) {
        this.subClauses = subClauses;
    }

    public Clause addRuleName(String ruleName) {
        this.ruleNames.add(ruleName);
        return this;
    }

    /**
     * Get the list of subclause(s) that are "trigger clauses" (first clauses that will be matched in the starting
     * position of this clause). Prevents having to evaluate every clause at every position to put a backref into
     * position from the first subclause back to this clause. Overridden only by {@link Longest}, since this evaluates
     * all of its sub-clauses, and {@link FirstMatch}, since any one of the sub-clauses can match in the first position.
     */
    protected List<Clause> getTriggerSubClauses() {
        return isTerminal() ? Collections.emptyList() : Arrays.asList(subClauses[0]);
    }

    public void initTriggerClauses() {
        // Add link from subClause to this clause, for all trigger subClauses
        for (Clause triggerClause : getTriggerSubClauses()) {
            // TODO System.out.println(triggerClause.toStringWithRuleNames() + " => " + this.toStringWithRuleNames());
            triggerClause.triggerParentClauses.add(this);
        }
    }

    /**
     * The equivalent of {@link #storeMemo(String, MemoRef, Set)} for terminals (which are not memoized, for space
     * efficiency).
     */
    public void initTerminalParentClauses(String input, int startPos, Set<MemoRef> activeSet) {
        var memoRef = new MemoRef(this, startPos);
        if (match(input, memoRef, true).matched()) {
            for (var triggerParentClause : triggerParentClauses) {
                var triggerParentMemoRef = new MemoRef(triggerParentClause, memoRef.startPos);
                activeSet.add(triggerParentMemoRef);

                System.out.println(" init: " + this + " : " + startPos + " => " + triggerParentMemoRef);
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    public abstract Memo match(String input, MemoRef memoRef, boolean isFirstMatchPosition);

    /**
     * Return the memo at the given position (without recursing), or creating a "no match" (len == -1) placeholder if
     * none. Also adds back-refs from the referencing parent clause to the sub clause.
     */
    protected static Memo lookUpSubClauseMemo(String input, MemoRef parentMemoRef, MemoRef subClauseMemoRef,
            boolean isFirstMatchPosition) {
        Memo subClauseMemo;
        if (subClauseMemoRef.clause.isTerminal()) {
            // Terminals are not memoized for space efficiency -- just call match() every time
            subClauseMemo = subClauseMemoRef.clause.match(input, subClauseMemoRef, isFirstMatchPosition);
        } else {
            // Look up a memo at the requested position
            subClauseMemo = subClauseMemoRef.clause.startPosToMemo.get(subClauseMemoRef.startPos);
            if (subClauseMemo == null) {
                // If there was no memo in the table at memoRef.startPos, create a "no match" (len == -1) placeholder
                subClauseMemo = new Memo(subClauseMemoRef, -1);
                Memo raceMemo = subClauseMemoRef.clause.startPosToMemo.put(subClauseMemoRef.startPos, subClauseMemo);
                // Handle race condition, in case another thread added the new memo entry before this one could
                if (raceMemo != null) {
                    subClauseMemo = raceMemo;
                    // In this case, raceMemo.len must equal memo.len, since both threads see the same current memo table
                }
            }
        }

        System.out.println("*** " + subClauseMemo + " => " + parentMemoRef);

        // Add a back-ref from the sub clause memo to the parent clause memo
        // TODO: don't add back-ref if subclause is the trigger clause
        subClauseMemo.referencedBySuperExprs.add(parentMemoRef);
        return subClauseMemo;
    }

    /**
     * Match the clause at the given position, and return null if the existing match in this position is longer,
     * otherwise return the {@link Memo} for the new match.
     */
    public static void storeMemo(String input, Memo newMemo, Set<MemoRef> activeSet) {
        MemoRef newMemoRef = newMemo.memoRef;

        // Check if there was an old memo already in place
        var oldMemo = newMemoRef.clause.startPosToMemo.get(newMemoRef.startPos);
        // Store the new memo if there was no old memo, or this is a FirstMatch clause and there was an old memo
        // but the new memo matches an earlier subclause (enforcing the semantics of FirstMatch), or there was
        // an old memo but the new memo is longer than the old memo
        if (oldMemo == null || newMemo.subClauseIdx < oldMemo.subClauseIdx || oldMemo.len < newMemo.len) {
            // No possibility of race condition here since newMemoRef (derived from prev active set) is unique
            newMemoRef.clause.startPosToMemo.put(newMemoRef.startPos, newMemo);
            if (newMemo.matched()) {
                // Add any trigger parent clauses to the active set at the same start position
                // (Since the new memo was written into the memo table, need to add its trigger parents to the active set)
                for (var triggerParentClause : newMemoRef.clause.triggerParentClauses) {
                    var triggerParentMemoRef = new MemoRef(triggerParentClause, newMemoRef.startPos);
                    activeSet.add(triggerParentMemoRef);

                    System.out.println("    ||| " + newMemoRef + " => " + triggerParentClause);
                }

                // If the old memo was replaced, need to trigger non-first-position parent clauses
                if (oldMemo != null) {
                    for (var parentMemoRef : oldMemo.referencedBySuperExprs) {
                        activeSet.add(parentMemoRef);

                        System.out.println("    >>> " + oldMemo + " => " + newMemo + " :: " + parentMemoRef);
                    }
                }

                // If there was a match in this position, record end position of memo for use by OneOrMore
                var endPos = newMemoRef.startPos + newMemo.len;
                if (endPos <= input.length()) {
                    var oldMatchEndsAtPos = newMemoRef.clause.matchEndPositions.add(endPos);
                    if (oldMatchEndsAtPos) {
                        // If a match of this clause has not yet ended at this end position of (endPos),
                        // then effectively any match of the same clause at that end position has also changed, since if a
                        // OneOrMore matched starting from this end position, it will no longer match.
                        activeSet.add(new MemoRef(newMemoRef.clause, endPos));
                    }
                }
            }
            // The reference to newMemo is thrown away, which is why we need to add trigger parent clauses to active set
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    public boolean isTerminal() {
        return subClauses.length == 0;
    }

    /** Returns whether this clause is the first in a run of matches of the same clause. */
    protected boolean isFirstOfRun(String input, int startPos) {
        return !matchEndPositions.contains(startPos);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Get the {@link Memo} entries for all nonoverlapping matches of this clause, obtained by greedily matching from
     * the beginning of the string to the end.
     */
    public List<Memo> getNonOverlappingMatches(boolean matchesOnly) {
        var firstEntry = startPosToMemo.firstEntry();
        var nonoverlappingMatches = new ArrayList<Memo>();
        if (firstEntry != null) {
            // If there was at least one memo entry
            for (var ent = firstEntry; ent != null;) {
                var memo = ent.getValue();
                if (!matchesOnly || memo.matched()) {
                    nonoverlappingMatches.add(memo);
                }
                var startPos = ent.getKey();
                // Only greedily start looking for a new match in the memo table after the end of the previous match.
                // Also, len has to increase monotonically (need to consume at least one character per match)
                // to avoid getting stuck in an infinite loop. (Then have to subtract 1 again because higherEntry()
                // searches starting from one greater than the parameter value.)
                ent = startPosToMemo.higherEntry(startPos + Math.max(1, memo.len) - 1);
            }
        }
        return nonoverlappingMatches;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /** The hashCode compares only the string representation of sub-clauses, not rule names. */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /** Equality compares only the string representation of sub-clauses, not rule names. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || !(o instanceof Clause)) {
            return false;
        }
        return this.toString().equals(o.toString());
    }

    public String toStringCached;

    public abstract String toStr();

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = toStr();
        }
        return toStringCached;
    }

    public String toStringWithRuleNames() {
        if (ruleNames.isEmpty()) {
            return toString();
        } else {
            List<String> namesSorted = new ArrayList<>(ruleNames);
            Collections.sort(namesSorted);
            return String.join(", ", namesSorted) + " = " + toString();
        }
    }
}
