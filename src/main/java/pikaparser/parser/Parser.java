package pikaparser.parser;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import pikaparser.clause.Clause.MatchDirection;
import pikaparser.clause.Nothing;
import pikaparser.clause.Start;
import pikaparser.clause.Terminal;
import pikaparser.grammar.Grammar;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class Parser {

    public final String input;

    public final Grammar grammar;

    public final MemoTable memoTable = new MemoTable();

    private static final boolean PARALLELIZE = true;

    public static final boolean DEBUG = true;

    public Parser(Grammar grammar, String input) {
        this.grammar = grammar;
        this.input = input;

        // A set of MemoKey instances for entries that need matching
        var activeSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoKey, Boolean>());

        // Memo table entries for which new matches were found in the current iteration
        var updatedEntries = Collections.newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());

        // If a lex rule was specified, seed the bottom-up parsing by running the lex rule top-down
        if (grammar.lexRule != null) {
            // Run lex preprocessing step, top-down
            var match = grammar.lexRule.clause.match(MatchDirection.TOP_DOWN, memoTable,
                    new MemoKey(grammar.lexRule.clause, /* startPos = */ 0), input, updatedEntries);
            if (match != null) {
                if (match.len == 0) {
                    // Without testing for zero-length matches, could get stuck in an infinite loop
                    throw new IllegalArgumentException("Lex rule cannot match zero characters");
                } else if (match.len < input.length()) {
                    throw new IllegalArgumentException("Lex rule did not match whole input (only the first "
                            + match.len + " out of " + input.length() + " characters were matched)");
                }
                if (Parser.DEBUG) {
                    System.out.println("Seed lex match: " + match + "\n");
                }
            } else {
                throw new IllegalArgumentException("Lex rule did not match input");
            }
        } else {
            // Find positions that all terminals match, and create the initial active set from parents of terminals,
            // without adding memo table entries for terminals that do not match (no non-matching placeholder needs
            // to be added to the memo table, because the match status of a given terminal at a given position will
            // never change).
            (PARALLELIZE ? grammar.allClauses.parallelStream() : grammar.allClauses.stream())
                    .filter(clause -> clause instanceof Terminal && !clause.seedParentClauses.isEmpty()
                    // Don't match Nothing everywhere -- it always matches
                            && !(clause instanceof Nothing))
                    .forEach(clause -> {
                        // Terminals are matched top down
                        for (int startPos = 0; startPos < input.length(); startPos++) {
                            var memoKey = new MemoKey(clause, startPos);
                            var match = clause.match(MatchDirection.TOP_DOWN, memoTable, memoKey, input,
                                    updatedEntries);
                            if (match != null) {
                                if (Parser.DEBUG) {
                                    System.out.println("Initial terminal match: " + match + "\n");
                                }
                            }
                            if (clause instanceof Start) {
                                // Only match Start in the first position
                                break;
                            }
                        }
                    });
        }

        // Main parsing loop
        // (need to check if (!updatedEntries.isEmpty()) in the while condition, even though updatedEntries.clear()
        // is called at the end of the loop, because updatedEntries can be populated by lex preprocessing)
        while (!activeSet.isEmpty() || !updatedEntries.isEmpty()) {

            // For each MemoKey in activeSet, try finding a match, and add matches to newMatches
            (PARALLELIZE ? activeSet.parallelStream() : activeSet.stream()).forEach(memoKey -> {
                memoKey.clause.match(MatchDirection.BOTTOM_UP, memoTable, memoKey, input, updatedEntries);
            });

            // Clear the active set for the next round
            activeSet.clear();

            // For each MemoEntry in newMatches, find best new match, and if the match 
            // improves, add the MemoEntry to activeSet for the next round
            (PARALLELIZE ? updatedEntries.parallelStream() : updatedEntries.stream()).forEach(memoEntry -> {
                if (memoEntry.toString().equals("(P0 = P0:(((operand0:(<P1> | <P0>) (ADD:'+' | SUB:'-') operand1:<P1>) | <P1>))) : 6+8")) {
                    System.out.println("here");
                }
                memoEntry.updateBestMatch(input, activeSet);
            });

            // Clear memoEntriesWithNewMatches for the next round
            updatedEntries.clear();
        }
    }
}
