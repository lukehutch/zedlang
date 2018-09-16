package pikaparser.memotable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import pikaparser.clause.Clause;
import pikaparser.parser.Parser;

/** A memo entry for a specific {@link Clause} at a specific start position. */
public class MemoEntry {

    /** The {@link Clause}. */
    public final Clause clause;

    /** The start position. */
    public final int startPos;

    /** The {@link ParsingContext} for partial matches that referenced this clause at this position. */
    public final List<ParsingContext> parsingContexts = new ArrayList<>();

    /**
     * The {@link ParsingContext} for partial matches that referenced this clause at this position in the most recent
     * iteration.
     */
    public final Queue<ParsingContext> newParsingContexts = new ConcurrentLinkedQueue<>();

    /**
     * The current best {@link Match} for this {@link Clause} at this start position. The partial matches in
     * {@link parsingContexts} will be extended with this match.
     */
    public Match bestMatch;

    /** New {@link Match} objects to be compared to the current {@link #bestMatch}. */
    public final Queue<Match> newMatches = new ConcurrentLinkedQueue<>();

    public MemoEntry(Clause clause, int startPos) {
        this.clause = clause;
        this.startPos = startPos;
    }

    /** Start a new subclause {@link ParsingContext} for the parent clause at this {@link MemoEntry} position. */
    public void seedSubClauseParsingContext(Parser parser, Set<MemoEntry> visited) {
        // prevSubClauseParsingContext == null => start a new match of subclauses at startPos
        var match = clause.extendParsingContext(parser, this, /* prevSubClauseParsingContext = */ null, startPos,
                /* visited = */ null);
        if (match != null) {
            // If all subclauses matched, add the parent match to newMatches
            newMatches.add(match);
            parser.memoEntriesWithNewMatches.add(this);
        }
    }

    /**
     * For all subclause {@link ParsingContext} partial matches that depended upon this {@link MemoEntry} for their next
     * subclause match query, extend the match as far as possible given the new bestMatch in this {@link MemoEntry}.
     */
    public void extendSubClauseParsingContexts(Parser parser, Set<MemoEntry> visited) {
        for (var parsingContext : parsingContexts) {
            MemoEntry parentMemoEntry = parsingContext.parentMemoEntry;
            var match = parentMemoEntry.clause.extendParsingContext(parser, parentMemoEntry, parsingContext, startPos,
                    /* visited = */ null);
            if (match != null) {
                // If all subclauses matched, add the parent match to parentMemoEntry.newMatches
                parentMemoEntry.newMatches.add(match);
                parser.memoEntriesWithNewMatches.add(parentMemoEntry);
            }
        }
    }

    public void updateParsingContexts() {
        while (!newParsingContexts.isEmpty()) {
            ParsingContext newParsingContext = newParsingContexts.remove();
            if (this.toString().equals("(WS '|' WS Clause)+ : 6") && newParsingContext.toString().equals(
                    "Clause = (('(' WS Clause ')') | Seq | FirstMatch | OneOrMore | FollowedBy | NotFollowedBy | RuleName | CharSeq | CharSet | Nothing) : 5+1 :: 0")) {
                System.out.println("here -> " + this);
            }
            parsingContexts.add(newParsingContext);
        }
    }

    public void updateBestMatch(Set<MemoEntry> parsingContextSeedMemoEntries,
            Set<MemoEntry> memoEntriesWithNewBestMatch) {
        var newBestMatch = (Match) null;
        if (bestMatch == null && newMatches.size() == 1) {
            // There was no previous best match, and there is only one new match 
            newBestMatch = newMatches.remove();
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
        }
        // If there is a new best match
        if (newBestMatch != null) {
            // Update the bestMatch field in the MemoEntry
            bestMatch = newBestMatch;
            memoEntriesWithNewBestMatch.add(this);

            // Seed new parent clauses at the start position of the match
            for (var matchSeedParentClause : newBestMatch.clause.seedParentClauses) {
                // Skip top-down clauses

                // TODO: is this right?
                // TODO: Also need to handle cases of top down clauses that are in the first position of other top down clauses
                // TODO: Also need to handle left-recursive cycles entirely consisting of top down clauses (in this case, switch to bottom-up)

                if (!matchSeedParentClause.matchTopDown()) {
                    parsingContextSeedMemoEntries
                            .add(matchSeedParentClause.getOrCreateMemoEntry(newBestMatch.startPos));
                }
            }
        }
    }

    @Override
    public String toString() {
        return clause.toStringWithRuleNames() + " : " + startPos;
    }
}
