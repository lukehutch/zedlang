package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;
import pikaparser.parser.Parser;

/**
 * Always use this rule at the start of the toplevel rule -- it will trigger parsing even if the rest of the
 * subclauses of the toplevel clause are topdown.
 * 
 * <p>
 * Without using this, a toplevel rule "G = (WS R+)" will try matching rule R after every whitespace position. Using
 * Start, the toplevel rule "G = (^ WS R+)" will only match R once, after any initial whitespace.
 */
public class Start extends Terminal {
    public static final String START_STR = "^";

    Start() {
        super();
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries) {
        // Terminals are always matched top-down
        // Match zero characters at beginning of input
        if (memoKey.startPos == 0) {
            return memoTable.addMatch(memoKey, /* firstMatchingSubClauseIdx = */ 0, /* terminalLen = */ 0,
                    Match.NO_SUBCLAUSE_MATCHES, updatedEntries);
        }
        // Don't call MemoTable.addMatch for non-matching terminals, to limit size of memo table
        if (Parser.DEBUG) {
            System.out.println(getClass().getSimpleName() + " failed to match at position " + memoKey.startPos
                    + ": " + memoKey);
        }
        return null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = START_STR;
        }
        return toStringCached;
    }
}
