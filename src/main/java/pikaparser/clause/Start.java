package pikaparser.clause;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

/**
 * Always use this rule at the start of the toplevel rule -- it will trigger parsing even if the rest of the subclauses
 * of the toplevel clause are topdown.
 * 
 * <p>
 * Without using this, a toplevel rule "G = (WS R+)" will try matching rule R after every whitespace position. Using
 * Start, the toplevel rule "G = (^ WS R+)" will only match R once, after any initial whitespace.
 */
public class Start extends Terminal {

    public static final String START_STR = "^";

    public Start() {
        super();
    }

    @Override
    public Match match(MemoEntry memoEntry, String input) {
        return memoEntry.startPos == 0 ? new Match(this, 0) : null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = START_STR;
        }
        return toStringCached;
    }
}
