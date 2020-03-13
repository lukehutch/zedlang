package pikaparser.memotable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pikaparser.clause.Clause;
import pikaparser.clause.FirstMatch;

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

    public Match(Clause clause, int firstMatchingSubClauseIdx, Match... subClauseMatches) {
        this.clause = clause;
        this.firstMatchingSubClauseIdx = firstMatchingSubClauseIdx;
        this.startPos = subClauseMatches[0].startPos;
        for (Match subClauseMatch : subClauseMatches) {
            this.len += subClauseMatch.len;
        }
        this.subClauseMatches = Arrays.asList(subClauseMatches);
    }

    public Match(Clause clause, int firstMatchingSubClauseIdx, List<Match> subClauseMatches) {
        this.clause = clause;
        this.firstMatchingSubClauseIdx = firstMatchingSubClauseIdx;
        this.startPos = subClauseMatches.get(0).startPos;
        for (Match subClauseMatch : subClauseMatches) {
            this.len += subClauseMatch.len;
        }
        this.subClauseMatches = subClauseMatches;
    }

    public Match(Clause clause, int startPos, int len) {
        this.clause = clause;
        this.firstMatchingSubClauseIdx = 0;
        this.startPos = startPos;
        this.len = len;
        this.subClauseMatches = Collections.emptyList();
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
        //        int diff1 = o.subClauseMatches.size() - this.subClauseMatches.size();
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
        for (int i = 0, ii = Math.min(this.subClauseMatches.size(), o.subClauseMatches.size()); i < ii; i++) {
            int diff1 = o.subClauseMatches.get(i).len - this.subClauseMatches.get(i).len;
            if (diff1 != 0) {
                return diff1;
            }
        }
        int diff2 = o.len - this.len;
        if (diff2 != 0) {
            return diff2;
        }

        // Recursively compare subclause matches (do this as a last resort to try to avoid O(N) scaling)
        for (int i = 0, ii = Math.min(this.subClauseMatches.size(), o.subClauseMatches.size()); i < ii; i++) {
            int diff3 = this.subClauseMatches.get(i).compareTo(o.subClauseMatches.get(i));
            if (diff3 != 0) {
                return diff3;
            }
        }

        // A longer list of subclause matches wins over a shorter list of subclause matches
        return this.subClauseMatches.size() - o.subClauseMatches.size();
    }

    public String getText(String input) {
        return input.substring(startPos, len);
    }

    private void toAST(ASTNode parent, String input) {
        ASTNode currParent = parent;
        if (clause.label != null) {
            // Labeled nodes become nodes of the final AST
            var newASTNode = new ASTNode(clause.label, clause, startPos, len);
            parent.addChild(newASTNode);
            currParent = newASTNode;
        }
        // Recurse to descendants
        for (int i = 0; i < subClauseMatches.size(); i++) {
            // var subClauseIdx = (clause instanceof OneOrMore ? 0 : i) + firstMatchingSubClauseIdx;
            var subClauseMatch = subClauseMatches.get(i);
            subClauseMatch.toAST(currParent, input);
        }
    }

    public ASTNode toAST(String input) {
        ASTNode root = new ASTNode("<root>", clause, startPos, len);
        toAST(root, input);
        return root;
    }

    public void printParseTree(String input, String indentStr, boolean isLastChild) {
        int inpLen = 80;
        String inp = input.substring(startPos, Math.min(input.length(), startPos + Math.min(len, inpLen)));
        if (inp.length() == inpLen) {
            inp += "...";
        }
        inp = inp.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
        System.out.println(indentStr + "|   ");
        System.out.println(indentStr + "+-- " + clause.toStringWithRuleNames() + " : " + startPos + "+" + len + " \""
                + inp + "\"");
        if (subClauseMatches != null) {
            for (int i = 0; i < subClauseMatches.size(); i++) {
                var subClauseMatch = subClauseMatches.get(i);
                subClauseMatch.printParseTree(input, indentStr + (isLastChild ? "    " : "|   "),
                        i == subClauseMatches.size() - 1);
            }
        }
    }

    public void printParseTree(String input) {
        printParseTree(input, "", true);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(clause.toStringWithRuleNames() + " : " + startPos + "+" + len + " => [ ");
        for (int i = 0; i < subClauseMatches.size(); i++) {
            var s = subClauseMatches.get(i);
            if (i > 0) {
                buf.append(" ; ");
            }
            buf.append(s.toString());
        }
        buf.append(" ]");
        return buf.toString();
    }
}
