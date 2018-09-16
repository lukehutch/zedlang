package pikaparser.clause;

import java.util.Collections;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;
import pikaparser.parser.Parser;

public class Nothing extends Clause {

    public static final String NOTHING_STR = "()";

    public Nothing() {
        super(new Clause[0]);
    }

    @Override
    public Match extendParsingContext(Parser parser, MemoEntry parentMemoEntryUnused,
            ParsingContext prevSubClauseParsingContextUnused, int startPos, Set<MemoEntry> visited) {
        return getCurrBestMatch(parser, prevSubClauseParsingContextUnused, startPos, visited);
    }

    @Override
    public Match getCurrBestMatch(Parser parser, ParsingContext prevSubClauseParsingContextUnused, int startPos,
            Set<MemoEntry> visited) {
        return new Match(this, startPos, /* len = */ 0, /* subClauseMatches = */ Collections.emptyList(),
                /* firstMatchingSubClauseIdx = */ 0);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = NOTHING_STR;
        }
        return toStringCached;
    }
}
