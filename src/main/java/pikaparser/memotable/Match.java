package pikaparser.memotable;

import java.util.List;

import pikaparser.clause.Clause;

/** A complete match of a {@link Clause} at a given start position. */
public class Match implements Comparable<Match> {

    /** The matching {@link Clause}. */
    public Clause clause;

    /** The start position of the match. */
    public int startPos;

    /** The length of the match. */
    public int len;

    /** The subclause matches. */
    public List<Match> subClauseMatches;

    /**
     * The subclause index of the first matching subclause (will be 0 unless {@link #clause} is a {@link FirstMatch},
     * and the matching clause was not the first subclause).
     */
    public int firstMatchingSubClauseIdx;

    public Match(Clause clause, int startPos, int len, List<Match> subClauseMatches, int firstMatchingSubClauseIdx) {
        this.clause = clause;
        this.startPos = startPos;
        this.len = len;
        this.subClauseMatches = subClauseMatches;
        this.firstMatchingSubClauseIdx = firstMatchingSubClauseIdx;
    }

    /**
     * Compare this {@link Match} to another {@link Match} of the same {@link Clause} type.
     * 
     * @return a negative value if this {@link Match} is a better match than the other {@link Match}, or a positive
     *         value if the other {@link Match} is a better match than this one.
     */
    @Override
    public int compareTo(Match o) {
        if (o == this) {
            // Fast path to stop recursive comparison when subclause matches are identical
            return 0;
        }
        // If this is a FirstMatch clause, then a smaller matching subclause index beats a larger index
        // (a smaller firstMatchingSubClauseIdx index wins over a larger firstMatchingSubClauseIdx)
        int diff0 = this.firstMatchingSubClauseIdx - o.firstMatchingSubClauseIdx;
        if (diff0 != 0) {
            return diff0;
        }
        // Compare subclause matches (this finds FirstMatch subclauses that match with an earlier sub-subclause index)
        for (int i = 0, ii = Math.min(this.subClauseMatches.size(), o.subClauseMatches.size()); i < ii; i++) {
            int diff1 = this.subClauseMatches.get(i).compareTo(o.subClauseMatches.get(i));
            if (diff1 != 0) {
                return diff1;
            }
        }
        // Compare number of matching subclauses (more matching subclauses wins over fewer, e.g. for OneOrMore)
        int diff2 = o.subClauseMatches.size() - this.subClauseMatches.size();
        if (diff2 != 0) {
            return diff2;
        }
        // Otherwise, a longer overall match wins over a shorter match (should never reach this test) 
        int diff3 = o.len - this.len;
        return diff3;
    }

    @Override
    public String toString() {
        return clause.toStringWithRuleNames() + " : " + startPos + "+" + len;
    }
}
