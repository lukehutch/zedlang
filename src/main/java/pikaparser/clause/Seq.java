package pikaparser.clause;

import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class Seq extends Clause {

    public Seq(Clause... subClauses) {
        super(subClauses);
        if (subClauses.length < 2) {
            throw new IllegalArgumentException("Expected 2 or more subclauses");
        }
    }

    @Override
    public Match extendParsingContext(String input, MemoEntry parentMemoEntry,
            ParsingContext prevSubClauseParsingContext, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContexts) {
        var matched = true;
        var currPos = startPos;
        var prevContext = prevSubClauseParsingContext;
        var startSubClauseIdx = prevContext == null ? 0 : prevContext.subClauseIdx + 1;
        for (var subClauseIdx = startSubClauseIdx; subClauseIdx < subClauses.length; subClauseIdx++) {
            var subClause = subClauses[subClauseIdx];
            var subClauseMatch = subClause.getCurrBestMatch(input, prevContext, currPos,
                    memoEntriesWithNewParsingContexts);
            if (subClauseMatch == null) {
                matched = false;
                break;
            }
            prevContext = new ParsingContext(parentMemoEntry, prevContext, subClauseMatch, subClauseIdx);
            currPos += subClauseMatch.len;
        }
        Match match = matched ? prevContext.getParentMatch(this) : null;
        if (matched && this.toString().equals("(Clause (WS '|' WS Clause)+)")) {
            System.out.println();
            System.out.println("** " + this + "\n" + toStr(match, 0));
        }
        return match;
    }

    private static String toStr(Match match, int depth) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < depth * 2 + 2; i++) {
            buf.append(" ");
        }
        buf.append(match + "\n");
        List<Match> subClauseMatches = match.subClauseMatches;
        for (int i = 0; i < subClauseMatches.size(); i++) {
            Match scm = subClauseMatches.get(i);
            buf.append(toStr(scm, depth + 1));
        }
        return buf.toString();
    }
    
    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            buf.append('(');
            for (int i = 0; i < subClauses.length; i++) {
                if (i > 0) {
                    buf.append(" ");
                }
                buf.append(subClauses[i].toString());
            }
            buf.append(')');
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
