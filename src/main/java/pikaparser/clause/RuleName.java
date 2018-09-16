package pikaparser.clause;

import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;
import pikaparser.parser.Parser;

public class RuleName extends Clause {

    public final String refdRuleName;

    public RuleName(String refdRuleName) {
        super(new Clause[0]);
        this.refdRuleName = refdRuleName;
    }

    @Override
    public Match extendParsingContext(Parser parser, MemoEntry parentMemoEntry,
            ParsingContext prevSubClauseParsingContext, int startPos, Set<MemoEntry> visited) {
        throw new IllegalArgumentException(RuleName.class.getSimpleName() + " should not be part of final grammar");
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            toStringCached = refdRuleName;
        }
        return toStringCached;
    }
}
