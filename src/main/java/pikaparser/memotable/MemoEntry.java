package pikaparser.memotable;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * The current best {@link Match} for this {@link Clause} at this start position. The partial matches in
     * {@link parsingContexts} will be extended with this match.
     */
    public Match bestMatch;

    public Queue<Match> newMatches = new ConcurrentLinkedQueue<Match>();

    public Set<ParsingContext> backrefs = Collections.newSetFromMap(new ConcurrentHashMap<ParsingContext, Boolean>());

    public MemoEntry(Clause clause, int startPos) {
        this.clause = clause;
        this.startPos = startPos;
    }

    public void updateBestMatch(String input, Set<ParsingContext> activeSet) {
        var newBestMatch = (Match) null;
        if (bestMatch == null && newMatches.size() == 1) {
            // There was no previous best match, and there is only one new match 
            newBestMatch = newMatches.remove();

            System.out.println("     new:");
            newBestMatch.printParseTree(input, "        ", true);
            System.out.println("     old:");
            if (bestMatch != null) {
                bestMatch.printParseTree(input, "        ", true);
            } else {
                System.out.println("        null");
            }

        } else {
            // Compare the previous best match to the new best matches using Comparator<Match>
            var allMatches = new ArrayList<Match>();
            if (bestMatch != null) {
                allMatches.add(bestMatch);
            }
            while (!newMatches.isEmpty()) {
                allMatches.add(newMatches.remove());
            }
            Collections.sort(allMatches);
            var firstMatch = allMatches.get(0);
            if (firstMatch != bestMatch) {
                newBestMatch = firstMatch;
            }

            for (var m : allMatches) {
                System.out.println("      " + (m == bestMatch ? "old:" : m == newBestMatch ? "new:" : ""));
                m.printParseTree(input, "          ", true);
            }
        }
        // If there is a new best match
        if (newBestMatch != null) {
            // Update the bestMatch field in the MemoEntry
            bestMatch = newBestMatch;

            for (var seedAncestorClause : clause.seedAncestorClauses) {
                System.out.println("      Seed: " + seedAncestorClause.toStringWithRuleNames());
                activeSet.add(new ParsingContext(seedAncestorClause.getOrCreateMemoEntry(bestMatch.startPos)));
            }
            for (var backref : backrefs) {
                System.out.println("      Backref: " + backref);
                activeSet.add(backref);
            }
            backrefs.clear();
        }
    }

    @Override
    public String toString() {
        return clause.toStringWithRuleNames() + " : " + startPos;
    }
}
