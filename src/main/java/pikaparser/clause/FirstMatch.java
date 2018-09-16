package pikaparser.clause;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;
import pikaparser.parser.Parser;

public class FirstMatch extends Clause {

    public FirstMatch(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException(FirstMatch.class.getSimpleName() + " expects 2 or more subclauses");
        }
    }

    @Override
    public Match extendParsingContext(Parser parser, MemoEntry parentMemoEntry,
            ParsingContext prevSubClauseParsingContext, int startPos, Set<MemoEntry> visited) {
        var matched = false;
        var prevContext = prevSubClauseParsingContext;
        var startSubClauseIdx = prevContext == null ? 0 : prevContext.subClauseIdx + 1;
        for (int subClauseIdx = startSubClauseIdx; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMatch = subClause.getCurrBestMatch(parser, prevContext, startPos, visited);
            // Set prevContext before breaking, so that it is set to the context of the last successful match
            prevContext = new ParsingContext(parentMemoEntry, prevContext, subClauseMatch, subClauseIdx);
            if (subClauseMatch != null) {
                matched = true;
                break;
            }
        }
        return matched ? prevContext.getParentMatch(this) : null;
    }

    @Override
    public List<Clause> getSeedSubClauses() {
        // Any sub-clause could be the matching clause, so need to override this
        return Arrays.asList(subClauses);
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            buf.append('(');
            for (int i = 0; i < subClauses.length; i++) {
                if (i > 0) {
                    buf.append(" | ");
                }
                buf.append(subClauses[i].toString());
            }
            buf.append(')');
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
