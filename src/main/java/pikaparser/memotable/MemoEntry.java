package pikaparser.memotable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import pikaparser.clause.Clause;

/** A memo entry for a specific {@link Clause} at a specific start position. */
public class MemoEntry {
    /** The {@link MemoKey} for this entry. */
    public final MemoKey memoKey;

    /** The current best {@link Match} for this {@link Clause} at this start position. */
    public Match bestMatch;

    /** The matches added in the current iteration -- the lowest-sorted match will replace {@link bestMatch}. */
    public Queue<Match> newMatches;

    /** The subclause matches from the previous iteration. */
    public List<Match> subClauseMatches = new ArrayList<>();

    public Set<MemoKey> backrefs;

    public MemoEntry(MemoKey memoKey) {
        this.memoKey = memoKey;
        newMatches = new ConcurrentLinkedQueue<Match>();
        backrefs = Collections.newSetFromMap(new ConcurrentHashMap<MemoKey, Boolean>());
    }

    public void updateBestMatch(MemoTable memoTable, String input, Set<MemoKey> activeSetOut) {
        Match newBestMatch;
        if (bestMatch == null && newMatches != null && newMatches.size() == 1) {
            // There was no previous best match, and there is only one new match 
            newBestMatch = newMatches.remove();

            //            System.out.println("     new:");
            //            newBestMatch.printParseTree(input, "        ", true);
            //            System.out.println("     old:");
            //            if (bestMatch != null) {
            //                bestMatch.printParseTree(input, "        ", true);
            //            } else {
            //                System.out.println("        null");
            //            }

        } else {
            // Compare the previous best match to the new best matches using Comparator<Match>.
            // Find new best match in list (avoid sorting matches, only need the first one).
            // Prev best match could still be the best match, so add it to allMatches.
            newBestMatch = bestMatch;
            while (!newMatches.isEmpty()) {
                // Compare optimal match so far to each new match
                var match = newMatches.remove();
                if (newBestMatch == null || match.compareTo(newBestMatch) < 0) {
                    newBestMatch = match;
                }
            }

            //            for (var m : allMatches) {
            //                System.out.println("      " + (m == bestMatch ? "old:" : m == newBestMatch ? "new:" : ""));
            //                m.printParseTree(input, "          ", true);
            //            }
        }

        // If there is a new best match
        if (newBestMatch != null) {
            // Update the bestMatch field in the MemoEntry
            bestMatch = newBestMatch;

            // Since there was a new best match at this memo entry, any parent clauses that have this clause
            // in the first position (that must match one or more characters) needs to be added to the active set
            for (var seedParentClause : memoKey.clause.seedParentClauses) {
                activeSetOut.add(new MemoKey(seedParentClause, bestMatch.memoKey.startPos));
            }

            // Any parent clause that depended upon the previous match also needs to be added to the active set
            for (var backref : backrefs) {
                activeSetOut.add(backref);
            }
            backrefs.clear();
        }
    }

    @Override
    public String toString() {
        return memoKey.clause.toStringWithRuleNamesAndLabels() + " : " + memoKey.startPos;
    }
}
