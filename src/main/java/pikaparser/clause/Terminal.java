package pikaparser.clause;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public abstract class Terminal extends Clause {
    public Terminal() {
        super(new Clause[0]);
    }

    public Match lookUpBestMatch(MemoEntry parentMemoEntry, int subClauseStartPos, String input) {
        // Match top-down, i.e. run match() directly rather than looking in memo table.
        // N.B. don't call getOrCreateMemoEntry(subClauseStartPos), so that terminals don't add
        // memo table entries
        return match(new MemoEntry(this, subClauseStartPos), input);
    }
}
