package pikaparser.memotable;

import pikaparser.clause.Clause;
import pikaparser.clause.FirstMatch;
import pikaparser.parser.ASTNode;

/** A complete match of a {@link Clause} at a given start position. */
public class Match implements Comparable<Match> {
    /** The {@link MemoKey}. */
    public final MemoKey memoKey;

    /** The length of the match. */
    public final int len;

    /** The subclause matches. */
    public final Match[] subClauseMatches;

    /**
     * The subclause index of the first matching subclause (will be 0 unless {@link #clause} is a
     * {@link FirstMatch}, and the matching clause was not the first subclause).
     */
    public int firstMatchingSubClauseIdx;

    public static final Match[] NO_SUBCLAUSE_MATCHES = new Match[0];

    public Match(MemoKey memoKey, int firstMatchingSubClauseIdx, int len, Match[] subClauseMatches) {
        this.memoKey = memoKey;
        this.firstMatchingSubClauseIdx = firstMatchingSubClauseIdx;
        this.len = len;
        this.subClauseMatches = subClauseMatches;
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

        // TODO: build operator precedence and associativity into this comparison function

        //        // If this is a FirstMatch clause, then a smaller matching subclause index beats a larger index
        //        // (a smaller firstMatchingSubClauseIdx index wins over a larger firstMatchingSubClauseIdx)
        //        int diff0 = this.firstMatchingSubClauseIdx - o.firstMatchingSubClauseIdx;
        //        if (diff0 != 0) {
        //            return diff0;
        //        }
        //        // Compare number of matching subclauses (more matching subclauses wins over fewer, e.g. for OneOrMore)
        //        int diff1 = o.subClauseMatches.length - this.subClauseMatches.length;
        //        if (diff1 != 0) {
        //            return diff1;
        //        }

        // An earlier subclause match in a FirstMatch clause wins over a later match
        int diff0 = this.firstMatchingSubClauseIdx - o.firstMatchingSubClauseIdx;
        if (diff0 != 0) {
            return diff0;
        }

        // A longer overall match (i.e. a match that spans more characters in the input) wins over a shorter match
        // (but need to ensure this at the subclause level -- ensure that every subclause in the longer match is
        // the same length as or longer than every subclause in the shorter match).
        for (int i = 0, ii = Math.min(this.subClauseMatches.length, o.subClauseMatches.length); i < ii; i++) {
            int diff1 = o.subClauseMatches[i].len - this.subClauseMatches[i].len;
            if (diff1 != 0) {
                return diff1;
            }
        }
        int diff2 = o.len - this.len;
        if (diff2 != 0) {
            return diff2;
        }

        // Recursively compare subclause matches (do this as a last resort to try to avoid O(N) scaling)
        for (int i = 0, ii = Math.min(this.subClauseMatches.length, o.subClauseMatches.length); i < ii; i++) {
            // TODO: make this non-recursive
            int diff3 = this.subClauseMatches[i].compareTo(o.subClauseMatches[i]);
            if (diff3 != 0) {
                return diff3;
            }
        }

        // A longer list of subclause matches wins over a shorter list of subclause matches
        return o.subClauseMatches.length - this.subClauseMatches.length;
    }

    public String getText(String input) {
        return input.substring(memoKey.startPos, memoKey.startPos + len);
    }

    private void toAST(ASTNode parent, String input) {
        ASTNode currParent0 = parent;
        // Recurse to descendants
        for (int i = 0; i < subClauseMatches.length; i++) {
            var subClauseMatch = subClauseMatches[i];
            var subClauseASTNodeLabel = memoKey.clause.subClauseASTNodeLabels == null ? null
                    : memoKey.clause.subClauseASTNodeLabels[firstMatchingSubClauseIdx + i];
            ASTNode currParent1 = currParent0;
            if (subClauseASTNodeLabel != null) {
                // Create an AST node for any labeled sub-clauses
                var newASTNode = new ASTNode(subClauseASTNodeLabel, subClauseMatch.memoKey.clause,
                        subClauseMatch.memoKey.startPos, subClauseMatch.len);
                currParent0.addChild(newASTNode);
                currParent1 = newASTNode;
            }
            subClauseMatch.toAST(currParent1, input);
        }
    }

    public ASTNode toAST(String input) {
        // If root clause is labeled, use label as name. Otherwise labels will be added only from subclauses.
        var rootNodeLabel = memoKey.clause.ruleNodeLabel != null ? memoKey.clause.ruleNodeLabel : "<root>";
        var root = new ASTNode(rootNodeLabel, memoKey.clause, memoKey.startPos, len);
        toAST(root, input);
        return root;
    }

    public void printParseTree(String input, String indentStr, boolean isLastChild) {
        int inpLen = 80;
        String inp = input.substring(memoKey.startPos,
                Math.min(input.length(), memoKey.startPos + Math.min(len, inpLen)));
        if (inp.length() == inpLen) {
            inp += "...";
        }
        inp = inp.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
        System.out.println(indentStr + "|   ");
        System.out.println(indentStr + "+-- " + memoKey.clause + " : "
                + memoKey.startPos + "+" + len + " \"" + inp + "\"");
        if (subClauseMatches != null) {
            for (int i = 0; i < subClauseMatches.length; i++) {
                var subClauseMatch = subClauseMatches[i];
                subClauseMatch.printParseTree(input, indentStr + (isLastChild ? "    " : "|   "),
                        i == subClauseMatches.length - 1);
            }
        }
    }

    public void printParseTree(String input) {
        printParseTree(input, "", true);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(
                memoKey.clause + " : " + memoKey.startPos + "+" + len + " => [ ");
        for (int i = 0; i < subClauseMatches.length; i++) {
            var s = subClauseMatches[i];
            if (i > 0) {
                buf.append(" ; ");
            }
            buf.append(s.toString());
        }
        buf.append(" ]");
        return buf.toString();
    }
}
