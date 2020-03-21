package pikaparser.memotable;

import pikaparser.clause.Clause;
import pikaparser.clause.First;
import pikaparser.clause.OneOrMore;
import pikaparser.clause.Seq;
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
     * The subclause index of the first matching subclause (will be 0 unless {@link #clause} is a {@link First}, and
     * the matching clause was not the first subclause).
     */
    public int firstMatchingSubClauseIdx;

    /** There are no subclause matches for terminals. */
    public static final Match[] NO_SUBCLAUSE_MATCHES = new Match[0];

    public Match(MemoTable memoTable, MemoKey memoKey, int firstMatchingSubClauseIdx, int len,
            Match[] subClauseMatches) {
        this.memoKey = memoKey;
        this.firstMatchingSubClauseIdx = firstMatchingSubClauseIdx;
        this.len = len;
        this.subClauseMatches = subClauseMatches;
        memoTable.numMatchObjectsCreated.incrementAndGet();
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

        // An earlier subclause match in a FirstMatch clause wins over a later match
        int diff0 = this.firstMatchingSubClauseIdx - o.firstMatchingSubClauseIdx;
        if (diff0 != 0) {
            return diff0;
        }
        for (int i = 0, ii = Math.min(this.subClauseMatches.length, o.subClauseMatches.length); i < ii; i++) {
            int diff1 = this.firstMatchingSubClauseIdx - o.firstMatchingSubClauseIdx;
            if (diff1 != 0) {
                return diff1;
            }
        }

        // A longer overall match (i.e. a match that spans more characters in the input) wins over a shorter match
        int diff2 = o.len - this.len;
        if (diff2 != 0) {
            return diff2;
        }
        for (int i = 0, ii = Math.min(this.subClauseMatches.length, o.subClauseMatches.length); i < ii; i++) {
            int diff1 = o.subClauseMatches[i].len - this.subClauseMatches[i].len;
            if (diff1 != 0) {
                return diff1;
            }
        }

        // A longer list of subclause matches wins over a shorter list of subclause matches
        return o.subClauseMatches.length - this.subClauseMatches.length;
    }

    public String getText(String input) {
        return input.substring(memoKey.startPos, memoKey.startPos + len);
    }

    private void toAST(ASTNode parent, String input) {
        // Recurse to descendants
        for (int i = 0; i < subClauseMatches.length; i++) {
            var subClauseMatch = subClauseMatches[i];
            int labelIdx = memoKey.clause instanceof OneOrMore ? 0
                    : memoKey.clause instanceof First ? firstMatchingSubClauseIdx + i
                            : memoKey.clause instanceof Seq ? i : 0;
            var subClauseASTNodeLabel = memoKey.clause.subClauseASTNodeLabels == null ? null
                    : memoKey.clause.subClauseASTNodeLabels[labelIdx];
            ASTNode parentOfSubclause = parent;
            if (subClauseASTNodeLabel != null) {
                // Create an AST node for any labeled sub-clauses
                var newASTNode = new ASTNode(subClauseASTNodeLabel, subClauseMatch.memoKey.clause,
                        subClauseMatch.memoKey.startPos, subClauseMatch.len);
                parent.addChild(newASTNode);
                parentOfSubclause = newASTNode;
            }
            subClauseMatch.toAST(parentOfSubclause, input);
        }
    }

    public ASTNode toAST(String rootNodeLabel, String input) {
        var root = new ASTNode(rootNodeLabel, memoKey.clause, memoKey.startPos, len);
        toAST(root, input);
        return root;
    }

    public void printTree(String input, String indentStr, boolean isLastChild) {
        int inpLen = 80;
        String inp = input.substring(memoKey.startPos,
                Math.min(input.length(), memoKey.startPos + Math.min(len, inpLen)));
        if (inp.length() == inpLen) {
            inp += "...";
        }
        inp = inp.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
        System.out.println(indentStr + "|   ");
        System.out.println(indentStr + "+-- " + memoKey.toStringWithRuleName() + "+" + len + " \"" + inp + "\"");
        if (subClauseMatches != null) {
            for (int i = 0; i < subClauseMatches.length; i++) {
                var subClauseMatch = subClauseMatches[i];
                subClauseMatch.printTree(input, indentStr + (isLastChild ? "    " : "|   "),
                        i == subClauseMatches.length - 1);
            }
        }
    }

    public void printTree(String input) {
        printTree(input, "", true);
    }

    public String toStringWithRuleName() {
        StringBuilder buf = new StringBuilder();
        buf.append(memoKey.toStringWithRuleName() + "+" + len + " => [ ");
        for (int i = 0; i < subClauseMatches.length; i++) {
            var s = subClauseMatches[i];
            if (i > 0) {
                buf.append(" ; ");
            }
            buf.append(s.toStringWithRuleName());
        }
        buf.append(" ]");
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(memoKey + "+" + len + " => [ ");
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
