package pikaparser.clause;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public abstract class Clause {

    public final Set<String> ruleNames = new HashSet<>();
    public String label;
    public final Clause[] subClauses;

    /** A map from startPos to {@link MemoEntry} for this clause. */
    private final ConcurrentSkipListMap<Integer, MemoEntry> startPosToMemoEntry = new ConcurrentSkipListMap<>();

    /** The parent clauses to seed when this clause's match memo at a given position changes. */
    public final Set<Clause> seedParentClauses = new HashSet<>();

    /** If true, the clause can match Nothing. */
    public boolean alwaysMatches;

    protected String toStringCached;

    // -----------------------------------------------------------------------------------------------------------------

    protected Clause(Clause... subClauses) {
        this.subClauses = subClauses;
    }

    public void setLabel(String clauseLabel) {
        this.label = clauseLabel;
    }

    public Clause addRuleName(String ruleName) {
        this.ruleNames.add(ruleName);
        return this;
    }

    /**
     * Get the list of subclause(s) that are "seed clauses" (first clauses that will be matched in the starting position
     * of this clause). Prevents having to evaluate every clause at every position to put a backref into position from
     * the first subclause back to this clause. Overridden only by {@link Longest}, since this evaluates all of its
     * sub-clauses, and {@link FirstMatch}, since any one of the sub-clauses can match in the first position.
     */
    protected List<Clause> getSeedSubClauses() {
        return subClauses.length == 0 ? Collections.emptyList() : Arrays.asList(subClauses[0]);
    }

    /** For all seed subclauses, add backlink from subclause to this clause. */
    public void backlinkToSeedParentClauses() {
        for (Clause seedSubClause : getSeedSubClauses()) {
            seedSubClause.seedParentClauses.add(this);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Sets {@link #alwaysMatches} to true if this clause always matches at any input position. Overridden in
     * subclasses.
     */
    public void testWhetherAlwaysMatches() {
    }

    public abstract Match match(MemoEntry memoEntry, String input);

    /**
     * Get the existing {@link MemoEntry} for this clause at the requested start position, or create and return a new
     * empty {@link MemoEntry} if one did not exist.
     * 
     * @param startPos
     *            The start position to check for a match.
     * @return The existing {@link MemoEntry} for this clause at the requested start position, or a new empty
     *         {@link MemoEntry} if one did not exist.
     */
    public MemoEntry getOrCreateMemoEntry(int startPos) {
        // Look up a memo at the start position
        var memoEntry = startPosToMemoEntry.get(startPos);
        if (memoEntry == null) {
            // If there was no memo at the start position, create one
            memoEntry = new MemoEntry(this, startPos);
            var raceMemo = startPosToMemoEntry.putIfAbsent(startPos, memoEntry);
            // Handle race condition, in case another thread added the new memo entry before this one could
            if (raceMemo != null) {
                memoEntry = raceMemo;
            }
        }
        return memoEntry;
    }

    // Overridden in Terminal for top-down matches
    public Match lookUpBestMatch(MemoEntry parentMemoEntry, int subClauseStartPos, String input) {
        // Reached a bottom-up clause -- return the best match in the memo table and stop recursing
        var memoEntry = getOrCreateMemoEntry(subClauseStartPos);

        // TODO: don't add backref if this is the first sub-clause to avoid duplication? (Duplication is removed by using a set currently)
        memoEntry.backrefs.add(parentMemoEntry);
        //        System.out.println("    Looking up: " + memoEntry + " at " + subClauseStartPos + " ## " + memoEntry.bestMatch
        //                + " ## " + parentMemoEntry);

        // Return current best match for memo (may be null) 
        return memoEntry.bestMatch;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Get the {@link Match} entries for all nonoverlapping matches of this clause, obtained by greedily matching from
     * the beginning of the string, then looking for the next match after the end of the current match.
     */
    public List<Match> getNonOverlappingMatches() {
        var firstEntry = startPosToMemoEntry.firstEntry();
        var nonoverlappingMatches = new ArrayList<Match>();
        if (firstEntry != null) {
            // If there was at least one memo entry
            for (var ent = firstEntry; ent != null;) {
                var startPos = ent.getKey();
                var memoEntry = ent.getValue();
                if (memoEntry.bestMatch != null) {
                    // Only store matches
                    nonoverlappingMatches.add(memoEntry.bestMatch);
                    // Start looking for a new match in the memo table after the end of the previous match.
                    // Need to consume at least one character per match to avoid getting stuck in an infinite loop,
                    // hence the Math.max(1, X) term. Have to subtract 1, because higherEntry() starts searching
                    // at a position one greater than its parameter value.
                    ent = startPosToMemoEntry.higherEntry(startPos + Math.max(1, memoEntry.bestMatch.len) - 1);
                } else {
                    // Move to next MemoEntry
                    ent = startPosToMemoEntry.higherEntry(startPos);
                }
            }
        }
        return nonoverlappingMatches;
    }

    /**
     * Get the {@link Match} entries for all postions where a match was queried, but there was no match.
     */
    public List<Integer> getNonMatches() {
        var firstEntry = startPosToMemoEntry.firstEntry();
        var nonMatches = new ArrayList<Integer>();
        if (firstEntry != null) {
            // If there was at least one memo entry
            for (var ent = firstEntry; ent != null;) {
                var startPos = ent.getKey();
                var memoEntry = ent.getValue();
                if (memoEntry.bestMatch == null) {
                    nonMatches.add(startPos);
                }
                // Move to next MemoEntry
                ent = startPosToMemoEntry.higherEntry(startPos);
            }
        }
        return nonMatches;
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
