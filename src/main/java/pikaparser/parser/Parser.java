package pikaparser.parser;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pikaparser.memotable.MemoEntry;

public class Parser {

    public final String input;

    public Grammar grammar;

    public Set<MemoEntry> parsingContextSeedMemoEntries = Collections
            .newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());
    public Set<MemoEntry> memoEntriesWithNewBestMatch = Collections
            .newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());
    public Set<MemoEntry> memoEntriesWithNewParsingContexts = Collections
            .newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());
    public Set<MemoEntry> memoEntriesWithNewMatches = Collections
            .newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());

    // TODO private static final boolean PARALLELIZE = false;

    public Parser(Grammar grammar, String input) {
        this.grammar = grammar;
        this.input = input;

        // Find positions that all terminals match, and create the initial set of parsing context seeds from.
        // N.B. need to run to (startPos == input.length()) inclusive, so that NotFollowedBy(X) and Y(Nothing)
        // work at the end of the input.
        for (var clause : grammar.allClauses) {
            if (clause.isTerminal()) {
                for (int startPos = 0; startPos <= input.length(); startPos++) {
                    clause.initTerminalParentSeeds(this, startPos);
                }
            }
        }

        // Main parsing loop
        while (!parsingContextSeedMemoEntries.isEmpty() || !memoEntriesWithNewParsingContexts.isEmpty()
                || !memoEntriesWithNewBestMatch.isEmpty()) {

            // TODO ParserInfo.printParseResult(this);

            // For all parsing context seeds, try matching the clause at the requested start position,
            // and if it matches, add the new match to the memo table 
            for (var memoEntry : parsingContextSeedMemoEntries) {
                // Find the initial match for the clause at the requested start position
                memoEntry.seedSubClauseParsingContext(this, /* visited = */ null);
            }
            parsingContextSeedMemoEntries.clear();

            // -- Barrier --

            // For all new bestMatch memo entries, try extending all partial matches starting at the new bestMatch 
            for (var memoEntry : memoEntriesWithNewBestMatch) {
                memoEntry.extendSubClauseParsingContexts(this, /* visited = */ null);
            }
            memoEntriesWithNewBestMatch.clear();

            // -- Barrier --

            // Move all newParsingContexts into parsingContexts (this is done right before iterating through
            // parsingContexts, so that parsingContexts doesn't change while iterating through it)
            for (var memoEntry : memoEntriesWithNewParsingContexts) {

                // TODO: some duplicate parsing contexts get added more than once to the same MemoEntry (should not happen)

                memoEntry.updateParsingContexts();
            }
            memoEntriesWithNewParsingContexts.clear();

            // -- Barrier --

            // For all MemoEntries that have new matches, check if any of the new matches is a new best match
            for (var memoEntry : memoEntriesWithNewMatches) {
                memoEntry.updateBestMatch(parsingContextSeedMemoEntries, memoEntriesWithNewBestMatch);
            }
            memoEntriesWithNewMatches.clear();

            // -- Barrier --

        }

        // TODO: This won't work if the parsing stops more than one level down from the toplevel match
        // If toplevel clause requires top-down matching, run toplevel match
        if (grammar.topLevelClause.matchTopDown()) {
            int startPos = 0;
            MemoEntry memoEntry = grammar.topLevelClause.getOrCreateMemoEntry(startPos);
            memoEntry.bestMatch = grammar.topLevelClause.extendParsingContext(this, memoEntry,
                    /* prevSubClauseParsingContext = */ null, startPos, /* visited = */ null);
        }
    }
}
