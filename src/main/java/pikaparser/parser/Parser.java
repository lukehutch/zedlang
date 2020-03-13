package pikaparser.parser;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import pikaparser.clause.Clause;
import pikaparser.clause.Nothing;
import pikaparser.clause.Terminal;
import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class Parser {

    public final String input;

    public Grammar grammar;

    private static final boolean PARALLELIZE = false;

    public Parser(Grammar grammar, String input) {
        this.grammar = grammar;
        this.input = input;

        var activeSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());
        var memoEntriesWithNewMatches = Collections.newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());

        // Find positions that all terminals match, and create the initial active set from parents of terminals
        (PARALLELIZE ? grammar.allClauses.parallelStream() : grammar.allClauses.stream()).forEach(clause -> {
            // Create initial active set by matching all terminals at every character position.
            // Assume Nothing (as a Terminal) does not need to trigger any parent rules by being in the first position. TODO: check this for each rule
            if (clause instanceof Terminal && !(clause instanceof Nothing)) {
                // Terminals are matched top down, to avoid creating memo table entries for them
                for (int startPos = 0; startPos < input.length(); startPos++) {
                    var memoEntry = new MemoEntry(clause, startPos);
                    var match = memoEntry.match(input);
                    if (match != null) {
                        for (Clause seedParentClause : clause.seedParentClauses) {
                            // Add parent clause to active set. This will cause the terminal to be
                            // evaluated again top-down by the parent clause, but avoids creating a memo
                            // table entry for the terminal.
                            activeSet.add(seedParentClause.getOrCreateMemoEntry(startPos));
                        }
                    }
                }
            }
        });

        // Main parsing loop
        while (!activeSet.isEmpty()) {
            //            System.out.println("\nActive set:");

            (PARALLELIZE ? activeSet.parallelStream() : activeSet.stream()).forEach(memoEntry -> {
                // Match each MemoEntry in activeSet, populating memoEntriesWithNewMatches
                //                System.out.println("  " + memoEntry);
                Match newMatch = memoEntry.match(input);
                if (newMatch != null) {
                    memoEntry.newMatches.add(newMatch);
                    memoEntriesWithNewMatches.add(memoEntry);
                }
            });

            // Clear the active set for the next round
            activeSet.clear();

            //            System.out.println("\nMemo entries with new best match:");
            (PARALLELIZE ? memoEntriesWithNewMatches.parallelStream() : memoEntriesWithNewMatches.stream())
                    // For each MemoEntry in memoEntriesWithNewMatches, find best new match, and if the match 
                    // improves, add the MemoEntry to the active set for the next round
                    .forEach(memoEntry -> {
                        //                        System.out.println("  " + memoEntry);
                        memoEntry.updateBestMatch(input, activeSet);
                    });

            // Clear memoEntriesWithNewMatches for the next round
            memoEntriesWithNewMatches.clear();
        }
    }
}
