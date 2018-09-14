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
    public final Clause[] subClauses;

    /** A map from startPos to {@link MemoEntry} for this clause. */
    public final ConcurrentSkipListMap<Integer, MemoEntry> startPosToMemoEntry = new ConcurrentSkipListMap<>();

    /** The parent clauses to seed when this clause's match memo at a given position changes. */
    public final Set<Clause> seedParentClauses = new HashSet<>();

    // -----------------------------------------------------------------------------------------------------------------

    public Clause(Clause... subClauses) {
        this.subClauses = subClauses;
    }

    public Clause addRuleName(String ruleName) {
        this.ruleNames.add(ruleName);
        return this;
    }

    public boolean isTerminal() {
        return subClauses.length == 0;
    }

    /**
     * Get the list of subclause(s) that are "seed clauses" (first clauses that will be matched in the starting position
     * of this clause). Prevents having to evaluate every clause at every position to put a backref into position from
     * the first subclause back to this clause. Overridden only by {@link Longest}, since this evaluates all of its
     * sub-clauses, and {@link FirstMatch}, since any one of the sub-clauses can match in the first position.
     */
    protected List<Clause> getSeedSubClauses() {
        return isTerminal() ? Collections.emptyList() : Arrays.asList(subClauses[0]);
    }

    /** For all seed subclauses, add backlink from subclause to this clause. */
    public void backlinkSeedSubClauses() {
        for (Clause seedSubClause : getSeedSubClauses()) {
            seedSubClause.seedParentClauses.add(this);
        }
    }

    /** Seed parent clauses that have a terminal as the first subclause. */
    public void initTerminalParentSeeds(String input, int startPos, Set<MemoEntry> parsingContextSeeds) {
        var terminalMatch = getCurrBestMatch(input, /* prevSubClauseParsingContext = */ null, startPos,
                /* memoEntriesWithNewParsingContexts not needed by terminals */ null);
        if (terminalMatch != null) {
            for (var seedParentClause : seedParentClauses) {
                parsingContextSeeds.add(seedParentClause.getOrCreateMemoEntry(terminalMatch.startPos));
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    public abstract Match extendParsingContext(String input, MemoEntry parentMemoEntry,
            ParsingContext prevSubClauseParsingContext, int startPos, Set<MemoEntry> memoEntriesWithNewParsingContexts);

    // TODO: override this in terminals
    /**
     * Check if this {@link Clause} matches at the given start position. As a side effect, stores a backref from the
     * {@link MemoEntry} to prevSubClauseParsingContext.
     * 
     * <p>
     * N.B. This method is overridden by terminals, since they are not memoized to save space. (The value of bestMatch
     * will never change for terminals, so the {@link ParsingContext} objects for previous subclauses don't need to be
     * stored, and therefore terminals do not need to be written into the memo table.)
     * 
     * @param input
     *            The input.
     * @param prevSubClauseParsingContext
     *            The {@link ParsingContext} of the previous subclause.
     * @param startPos
     *            The start position to check for a match.
     * @return A new {@link Match}, if this {@link Clause} matches at the given start position, otherwise null.
     */
    public Match getCurrBestMatch(String input, ParsingContext prevSubClauseParsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContexts) {
        // Get or create a MemoEntry for the start position
        var memoEntry = getOrCreateMemoEntry(startPos);

        // Add backref to prevSubClauseParsingContext
        if (prevSubClauseParsingContext != null) {
            memoEntry.newParsingContexts.add(prevSubClauseParsingContext);
            memoEntriesWithNewParsingContexts.add(memoEntry);
        }

        // Return the best match of this clause at this start position, or null if no match yet 
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
