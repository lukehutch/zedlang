package pikaparser.memotable;

import java.util.ArrayList;

import pikaparser.clause.Clause;

/** The context for an incomplete subclause match that referenced a given memo entry. */
public class ParsingContext {

    /** The parent clause */
    public final MemoEntry parentMemoEntry;

    /** Reference to prev subclause {@link ParsingContext} within the parent, or null if none. */
    public final ParsingContext prevSibling;

    /** Reference to the {@link Match} represented for this subclause, or null if this subclause did not match. */
    public final Match match;

    /** The index of this subclause within the parent subclauses. */
    public final int subClauseIdx;

    /**
     * Construct a new {@link ParsingContext} for a subclause.
     *
     * @param parentMemoEntry
     *            The parent {@link MemoEntry}.
     * @param prevSibling
     *            The {@link ParsingContext} for the previous sibling subclause.
     * @param match
     *            The {@link Match}, if this subclause matched, else null.
     * @param subClauseIdx
     *            The index of this subclause among the parent clause's subclauses.
     */
    public ParsingContext(MemoEntry parentMemoEntry, ParsingContext prevSibling, Match match, int subClauseIdx) {
        this.parentMemoEntry = parentMemoEntry;
        this.prevSibling = prevSibling;
        this.match = match;
        this.subClauseIdx = subClauseIdx;
    }

    /**
     * When all subclauses of the parent have matched, this is called on the final subclause {@link ParsingContext} to
     * retrieve the parent {@link Match} object.
     */
    public Match getParentMatch(Clause parentClause) {
        // Follow the previous sibling backlinks to retrieve ParsingContexts for all subclauses of the parent clause
        var subClauseParsingContextsReversed = new ArrayList<ParsingContext>();
        for (var curr = this; curr != null; curr = curr.prevSibling) {
            subClauseParsingContextsReversed.add(curr);
        }

        // Extract subclause matches from the list of subclause parsing contexts
        var subClauseMatches = new ArrayList<Match>(subClauseParsingContextsReversed.size());
        var firstMatchingSubClauseIdx = 0;
        boolean firstMatchingSubClause = true;
        for (int i = subClauseParsingContextsReversed.size() - 1; i >= 0; --i) {
            ParsingContext subClauseParsingContext = subClauseParsingContextsReversed.get(i);
            // Filter for only previous subclauses that actually matched (in particular, for FirstMatch,
            // skip subclauses that did not match) 
            if (subClauseParsingContext.match != null) {
                if (firstMatchingSubClause) {
                    // Record the subclause index of the first matching subclause
                    firstMatchingSubClauseIdx = subClauseParsingContextsReversed.size() - 1 - i;
                    firstMatchingSubClause = false;
                }
                subClauseMatches.add(subClauseParsingContext.match);
            }
        }
        if (subClauseMatches.isEmpty()) {
            // Should not happen
            throw new RuntimeException("Expected at least one matching subclause"); // TODO: not correct for terminals
        }

        // Find start and end of input spanned by this new parent clause match
        var startPos = subClauseMatches.get(0).startPos;
        var lastSubClauseMatch = subClauseMatches.get(subClauseMatches.size() - 1);
        var endPos = lastSubClauseMatch.startPos + lastSubClauseMatch.len;
        var len = endPos - startPos;

        // Return new parent clause match
        return new Match(parentClause, startPos, len, subClauseMatches, firstMatchingSubClauseIdx);
    }

    @Override
    public String toString() {
        return match + " :: " + subClauseIdx + (prevSibling == null ? "" : " / " + prevSibling);
    }
}
