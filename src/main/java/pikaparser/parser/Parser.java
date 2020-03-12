package pikaparser.parser;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import pikaparser.clause.Clause;
import pikaparser.clause.Nothing;
import pikaparser.clause.Start;
import pikaparser.clause.Terminal;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class Parser {

    public final String input;

    public Grammar grammar;

    public Parser(Grammar grammar, String input) {
        this.grammar = grammar;
        this.input = input;

        var activeSet = Collections.newSetFromMap(new ConcurrentHashMap<ParsingContext, Boolean>());

        // Find positions that all terminals match, and create the initial set of parsing context seeds from these.
        for (var clause : grammar.allClauses) {
            if (clause instanceof Start //
                    || (clause instanceof Terminal //
                            // TODO: this might break things 
                            && !(clause instanceof Nothing))) {
                Terminal terminal = (Terminal) clause;
                // Run to (startPos == input.length()) inclusive for Nothing, since Nothing matches beyond end of input
                int len = terminal instanceof Nothing ? input.length() + 1 : input.length();
                for (int startPos = 0; startPos < len; startPos++) {
                    // TODO: creating a ParsingContext requires creating memo entries for terminals 
                    if (terminal.match(input, /* ignored */ null, startPos, /* ignored */ null) != null) {
                        for (Clause seedAncestorClause : terminal.seedAncestorClauses) {
                            activeSet.add(new ParsingContext(seedAncestorClause.getOrCreateMemoEntry(startPos)));
                        }
                    }
                }
            }
        }

        // Main parsing loop
        while (!activeSet.isEmpty()) {

            System.out.println("\nActive set:");
            var memoEntriesWithNewBestMatch = Collections.newSetFromMap(new ConcurrentHashMap<MemoEntry, Boolean>());
            for (var parsingContext : activeSet) {
                System.out.println("  " + parsingContext);
                parsingContext.parentMemoEntry.clause.match(input, parsingContext,
                        parsingContext.parentMemoEntry.startPos, memoEntriesWithNewBestMatch);
            }
            activeSet.clear();

            System.out.println("\nMemo entries with new best match:");
            for (var memoEntry : memoEntriesWithNewBestMatch) {
                System.out.println("  " + memoEntry);
                memoEntry.updateBestMatch(input, activeSet);
            }
            memoEntriesWithNewBestMatch.clear();

        }

        ParserInfo.printParseResult(this);
        
        //        // TODO: This won't work if the parsing stops more than one level down from the toplevel match
        //        // If toplevel clause requires top-down matching, run toplevel match
        //        if (grammar.topLevelClause.matchTopDown()) {
        //            int startPos = 0;
        //            MemoEntry memoEntry = grammar.topLevelClause.getOrCreateMemoEntry(startPos);
        //            memoEntry.bestMatch = grammar.topLevelClause.extendParsingContext(this, memoEntry,
        //                    /* prevSubClauseParsingContext = */ null, startPos, /* visited = */ null);
        //        }
    }
}
