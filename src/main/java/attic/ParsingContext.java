package attic;

/** The context for an incomplete subclause match that referenced a given memo entry. */
public class ParsingContext {
    //
    //    /** The parent clause */
    //    public final MemoEntry parentMemoEntry;
    //
    //    /** The subclause matches so far. */
    //    public final SubClauseMatchNode subClauseMatchNode;
    //    
    //    /** The index of the subclause to continue matching from. */
    //    public final int subClauseIdx;
    //
    //    public static class SubClauseMatchNode {
    //        /** Reference to the {@link Match} represented for the prev subclause, or null if it did not match. */
    //        public final Match match;
    //
    //        /** Reference to the {@link ParsingContext} before this one within the parent, or null if none. */
    //        public final SubClauseMatchNode prev;
    //
    //        public SubClauseMatchNode(Match match, SubClauseMatchNode prev) {
    //            this.match = match;
    //            this.prev = prev;
    //        }
    //    }
    //
    //    public ParsingContext(MemoEntry parentMemoEntry) {
    //        this.parentMemoEntry = parentMemoEntry;
    //        this.subClauseMatchNode = null;
    //        this.subClauseIdx = 0;
    //    }
    //
    //    public ParsingContext(ParsingContext prevParsingContext, Match currMatch, int nextSubClauseIdx) {
    //        this.parentMemoEntry = prevParsingContext.parentMemoEntry;
    //        this.subClauseMatchNode = new SubClauseMatchNode(currMatch, prevParsingContext.subClauseMatchNode);
    //        this.subClauseIdx = nextSubClauseIdx;
    //    }
    //
    //    /**
    //     * When all subclauses of the parent have matched, this is called on the final subclause {@link ParsingContext} to
    //     * retrieve the parent {@link Match} object.
    //     */
    //    public Match getParentMatch() {
    //        // Follow the previous sibling backlinks to retrieve ParsingContexts for all subclauses of the parent clause
    //        var subClauseMatchesReversed = new ArrayList<Match>();
    //        for (var curr = subClauseMatchNode; curr != null; curr = curr.prev) {
    //            subClauseMatchesReversed.add(curr.match);
    //        }
    //
    //        // Extract subclause matches from the list of subclause parsing contexts
    //        var subClauseMatches = new ArrayList<Match>(subClauseMatchesReversed.size());
    //        var firstMatchingSubClauseIdx = 0;
    //        boolean firstMatchingSubClause = true;
    //        for (int i = subClauseMatchesReversed.size() - 1; i >= 0; --i) {
    //            var subClauseMatch = subClauseMatchesReversed.get(i);
    //            // Filter for only previous subclauses that actually matched (in particular, for FirstMatch,
    //            // skip subclauses that did not match) 
    //            if (subClauseMatch != null) {
    //                if (firstMatchingSubClause) {
    //                    // Record the subclause index of the first matching subclause
    //                    firstMatchingSubClauseIdx = subClauseMatchesReversed.size() - 1 - i;
    //                    firstMatchingSubClause = false;
    //                }
    //                subClauseMatches.add(subClauseMatch);
    //            }
    //        }
    //        if (subClauseMatches.isEmpty()) {
    //            // Should not happen
    //            throw new RuntimeException("Expected at least one matching subclause"); // TODO: not correct for terminals
    //        }
    //
    //        // Find start and end of input spanned by this new parent clause match
    //        var startPos = subClauseMatches.get(0).startPos;
    //        var lastSubClauseMatch = subClauseMatches.get(subClauseMatches.size() - 1);
    //        var endPos = lastSubClauseMatch.startPos + lastSubClauseMatch.len;
    //        var len = endPos - startPos;
    //
    //        // Return new parent clause match
    //        return new Match(parentMemoEntry.clause, startPos, len, subClauseMatches, firstMatchingSubClauseIdx);
    //    }
    //
    //    @Override
    //    public String toString() {
    //        return parentMemoEntry + " :: " + subClauseIdx;
    //    }
    //
    //    @Override
    //    public int hashCode() {
    //        return parentMemoEntry.hashCode() * (subClauseMatchNode == null ? 1 : subClauseMatchNode.hashCode())
    //                + subClauseIdx;
    //    }
    //
    //    @Override
    //    public boolean equals(Object obj) {
    //        if (obj == this) {
    //            return true;
    //        } else if (obj == null || !(obj instanceof ParsingContext)) {
    //            return false;
    //        }
    //        var o = (ParsingContext) obj;
    //        if (subClauseIdx != o.subClauseIdx
    //                // MemoEntry objects are singletons, so can be compared by pointer
    //                || parentMemoEntry != o.parentMemoEntry
    //                || (subClauseMatchNode == null) != (o.subClauseMatchNode == null)) {
    //            return false;
    //        }
    //        if (subClauseMatchNode == null) {
    //            return true;
    //        }
    //        // Just do pointer comparison rather than deep comparison of subclause context, for efficiency
    //        return subClauseMatchNode == o.subClauseMatchNode;
    //    }
}