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

    /** The {@link Clause}. */
    public final Clause clause;

    /** The start position. */
    public final int startPos;

    /** The current best {@link Match} for this {@link Clause} at this start position. */
    public Match bestMatch;

    /** The matches added in the current iteration -- the lowest-sorted match will replace {@link bestMatch}. */
    public Queue<Match> newMatches = new ConcurrentLinkedQueue<Match>(); // TODO: make a lightweight version that doesn't initialize this field, for intermediate MemoEntries

    /** The subclause matches from the previous iteration. */
    public List<Match> subClauseMatches = new ArrayList<>();

    public Set<MemoEntry> backrefs = Collections.newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());

    public MemoEntry(Clause clause, int startPos) {
        this.clause = clause;
        this.startPos = startPos;
    }

    public Match match(String input) {
        return clause.match(this, input);
    }

    public void updateBestMatch(String input, Set<MemoEntry> activeSetOut) {
        Match newBestMatch = null;
        if (bestMatch == null && newMatches.size() == 1) {
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
            // Compare the previous best match to the new best matches using Comparator<Match>
            var allMatches = new ArrayList<Match>();
            if (bestMatch != null) {
                allMatches.add(bestMatch);
            }
            while (!newMatches.isEmpty()) {
                allMatches.add(newMatches.remove());
            }
            // Find new best match in list (avoid sorting matches, only need the first one)
            Match firstMatch = null;
            for (var match : newMatches) {
                if (firstMatch == null || match.compareTo(firstMatch) < 0) {
                    firstMatch = match;
                }
            }
            if (firstMatch != bestMatch) {
                newBestMatch = firstMatch;
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
            for (var seedParentClause : clause.seedParentClauses) {
                //                System.out.println("      Seed: " + seedParentClause.toStringWithRuleNames());
                activeSetOut.add(seedParentClause.getOrCreateMemoEntry(bestMatch.startPos));
            }

            // Any parent clause that depended upon the previous match also needs to be added to the active set
            for (var backref : backrefs) {
                //                System.out.println("      Backref: " + backref);
                activeSetOut.add(backref);
            }
            backrefs.clear();
        }
    }

    @Override
    public String toString() {
        return clause.toStringWithRuleNames() + " : " + startPos;
    }
}
