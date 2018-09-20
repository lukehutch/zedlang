package pikaparser.clause;

import java.util.Collections;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

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
    public Match match(String inputIgnored, ParsingContext parsingContextIgnored, int startPos,
            Set<MemoEntry> memoEntriesWithNewBestMatchIgnored) {
        return startPos == 0 ? new Match(this, startPos, 0, Collections.emptyList(), 0) : null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = START_STR;
        }
        return toStringCached;
    }
}
