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
import pikaparser.memotable.ParsingContext;

public abstract class Clause {

    public final Set<String> ruleNames = new HashSet<>();
    public String label;
    public final Clause[] subClauses;

    /** A map from startPos to {@link MemoEntry} for this clause. */
    public final ConcurrentSkipListMap<Integer, MemoEntry> startPosToMemoEntry = new ConcurrentSkipListMap<>();

    /** The parent clauses to seed when this clause's match memo at a given position changes. */
    public final Set<Clause> seedParentClauses = new HashSet<>();

    /** The bottom-up ancestral clauses to seed when a bottom-up clause's match memo at a given position changes. */
    public final Set<Clause> seedAncestorClauses = new HashSet<>();

    public boolean matchTopDown;

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
     * If true, the clause can match Nothing, so match top-down (on demand), rather than bottom up (dramatically reduces
     * the number of memo table entries).
     */
    public boolean matchTopDown() { // TODO: this has the same name as the recursive matching function
        return matchTopDown;
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

    public void findSeedAncestorClauses(Clause descendant, HashSet<Clause> visited) {
        if (visited.add(this)) {
            if (!matchTopDown && this != descendant) {
                // Stop recursing at bottom-up clauses
                descendant.seedAncestorClauses.add(this);
            } else {
                // For top-down clauses, keep recursing to ancestors
                for (Clause parent : seedParentClauses) {
                    parent.findSeedAncestorClauses(descendant, visited);
                }
            }
        } else {
            throw new RuntimeException("Found cycle in grammar: " + visited); // TODO
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    public abstract Match match(String input, ParsingContext parsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewBestMatch);

    public Match lookUpBestMatch(ParsingContext parsingContext, int startPos) {
        // Reached a bottom-up clause -- return the best match in the memo table and stop recursing
        var memoEntry = getOrCreateMemoEntry(startPos);
        // TODO: don't add backref if this is the first sub-clause to avoid duplication? (Duplication is removed by using a set currently)
        memoEntry.backrefs.add(parsingContext);
        System.out.println("    Looking up: " + memoEntry + " at " + startPos + " ## " + memoEntry.bestMatch + " ## "
                + parsingContext);
        return memoEntry.bestMatch;
    }

    /**
     * Get the existing {@link MemoEntry} for this clause at the requested start position, or create and return a new
     * {@link MemoEntry} if one did not exist.
     * 
     * @param startPos
     *            The start position to check for a match.
     * @return The existing {@link MemoEntry} for this clause at the requested start position, or a new
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
