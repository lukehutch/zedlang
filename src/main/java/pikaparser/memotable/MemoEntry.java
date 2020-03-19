package pikaparser.memotable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import pikaparser.clause.Clause;

/** A memo entry for a specific {@link Clause} at a specific start position. */
public class MemoEntry {
    /** The {@link MemoKey} for this entry. */
    public final MemoKey memoKey;

    /** The current best {@link Match} for this {@link Clause} at this start position. */
    public Match bestMatch;

    /**
     * The match for this MemoEntry added in the current iteration -- this will replace {@link bestMatch} if it is a
     * better match.
     */
    public AtomicReference<Match> newMatch = new AtomicReference<>();

    /** The subclause matches from the previous iteration. */
    public List<Match> subClauseMatches = new ArrayList<>();

    public Set<MemoKey> backrefs = Collections.newSetFromMap(new ConcurrentHashMap<MemoKey, Boolean>());

    public MemoEntry(MemoKey memoKey) {
        this.memoKey = memoKey;
    }

    public void setNewBestMatch(MemoTable memoTable, Match newBestMatch, Collection<MemoEntry> updatedEntries) {
        // Update the memo table if a new better match was found.
        if (newBestMatch != null && newBestMatch != bestMatch
                && (bestMatch == null || newBestMatch.compareTo(bestMatch) < 0)) {
            // Need to use compareAndSet since two threads might try to do the same work to create the match,
            // by referring to the same subexpression. Any two matches in the sam iteration will be equal.
            // Only allow the first to succeed.
            if (newMatch.compareAndSet(null, newBestMatch)) {
                // Mark entry as changed
                updatedEntries.add(this);
                memoTable.numMatchObjectsMemoized.incrementAndGet();
            }
        }
    }

    public void updateBestMatch(String input, Set<MemoKey> activeSetOut) {
        // Compare the previous best match to the new best matches using Comparator<Match>.
        // Find new best match in list (avoid sorting matches, only need the first one).
        // Prev best match could still be the best match, so add it to allMatches.
        Match newMatchVal = newMatch.getAndSet(null);
        if (newMatchVal != null) {
            // Replace bestMatch with newMatch
            bestMatch = newMatchVal;

            // Since there was a new best match at this memo entry, any parent clauses that have this clause
            // in the first position (that must match one or more characters) needs to be added to the active set
            for (var seedParentClause : memoKey.clause.seedParentClauses) {
                activeSetOut.add(new MemoKey(seedParentClause, bestMatch.memoKey.startPos));
            }

            // Any parent clause that depended upon the previous match also needs to be added to the active set
            activeSetOut.addAll(backrefs);
            backrefs.clear();
        }
    }

    @Override
    public String toString() {
        return memoKey.clause + " : " + memoKey.startPos;
    }
}
