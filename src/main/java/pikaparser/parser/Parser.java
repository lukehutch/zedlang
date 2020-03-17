package pikaparser.parser;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import pikaparser.clause.Clause;
import pikaparser.clause.Nothing;
import pikaparser.clause.Terminal;
import pikaparser.clause.Clause.MatchDirection;
import pikaparser.grammar.Grammar;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class Parser {

    public final String input;

    public final Grammar grammar;

    public final MemoTable memoTable = new MemoTable();

    private static final boolean PARALLELIZE = true;

    public Parser(Grammar grammar, String input) {
        this.grammar = grammar;
        this.input = input;

        var activeSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoKey, Boolean>());
        var updatedEntries = Collections.newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());

        // Find positions that all terminals match, and create the initial active set from parents of terminals
        (PARALLELIZE ? grammar.allClauses.parallelStream() : grammar.allClauses.stream()).forEach(clause -> {
            // Create initial active set by matching all terminals at every character position.
            // Assume Nothing (as a Terminal) does not need to trigger any parent rules by being in the first position. TODO: check this for each rule
            if (clause instanceof Terminal && !(clause instanceof Nothing)) {
                // Terminals are matched top down, to avoid creating memo table entries for them
                for (int startPos = 0; startPos < input.length(); startPos++) {
                    var memoKey = new MemoKey(clause, startPos);
                    var match = memoKey.clause.match(MatchDirection.TOP_DOWN, memoTable, memoKey, input,
                            updatedEntries);
                    if (match != null) {
                        for (Clause seedParentClause : clause.seedParentClauses) {
                            // Add parent clause to active set. This will cause the terminal to be
                            // evaluated again top-down by the parent clause, but avoids creating a memo
                            // table entry for the terminal.
                            activeSet.add(new MemoKey(seedParentClause, startPos));
                        }
                    }
                }
            }
        });

        // Main parsing loop
        while (!activeSet.isEmpty()) {

            // For each MemoKey in activeSet, try finding a match, and add matches to newMatches
            (PARALLELIZE ? activeSet.parallelStream() : activeSet.stream()).forEach(memoKey -> {
                memoKey.clause.match(MatchDirection.BOTTOM_UP, memoTable, memoKey, input, updatedEntries);
            });

            // Clear the active set for the next round
            activeSet.clear();

            // For each MemoEntry in newMatches, find best new match, and if the match 
            // improves, add the MemoEntry to activeSet for the next round
            (PARALLELIZE ? updatedEntries.parallelStream() : updatedEntries.stream()).forEach(memoEntry -> {
                memoEntry.updateBestMatch(memoTable, input, activeSet);
            });

            // Clear memoEntriesWithNewMatches for the next round
            updatedEntries.clear();
        }
    }
}
