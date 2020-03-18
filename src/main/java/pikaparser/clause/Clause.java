package pikaparser.clause;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public abstract class Clause {
    public String ruleName;
    public final Clause[] subClauses;

    public String ruleNodeLabel;
    public String[] subClauseASTNodeLabels;

    /** The parent clauses to seed when this clause's match memo at a given position changes. */
    public final Set<Clause> seedParentClauses = new HashSet<>();

    /** If true, the clause can match zero characters. */
    public boolean canMatchZeroChars;

    protected String toStringCached;

    // -------------------------------------------------------------------------------------------------------------

    protected Clause(Clause... subClauses) {
        this.subClauses = subClauses;
    }

    protected Clause(String ruleName, Clause... subClauses) {
        this(subClauses);
        this.ruleName = ruleName;
    }

    /**
     * Get the list of subclause(s) that are "seed clauses" (first clauses that will be matched in the starting
     * position of this clause). Prevents having to evaluate every clause at every position to put a backref into
     * position from the first subclause back to this clause. Overridden only by {@link Longest}, since this
     * evaluates all of its sub-clauses, and {@link First}, since any one of the sub-clauses can match in the first
     * position.
     */
    protected List<Clause> getSeedSubClauses() {
        return subClauses.length == 0 ? Collections.emptyList() : Arrays.asList(subClauses[0]);
    }

    /** For all seed subclauses, add backlink from subclause to this clause. */
    public void backlinkToSeedParentClauses() {
        for (Clause seedSubClause : getSeedSubClauses()) {
            seedSubClause.seedParentClauses.add(this);
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    /**
     * Sets {@link #canMatchZeroChars} to true if this clause can match zero characters, i.e. always matches at any
     * input position.
     * 
     * <p>
     * Overridden in subclasses.
     */
    public void testWhetherAlwaysMatches() {
    }

    // -------------------------------------------------------------------------------------------------------------

    public static enum MatchDirection {
        BOTTOM_UP, TOP_DOWN;
    }

    /**
     * Match a clause bottom-up at a given start position.
     * 
     * @param matchDirection
     *            TODO
     */
    public abstract Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Set<MemoEntry> updatedEntries);

    // -------------------------------------------------------------------------------------------------------------

    protected void appendRulePrefix(StringBuilder buf) {
        if (ruleName != null) {
            buf.append('(');
            buf.append(ruleName);
            buf.append(" = ");
            if (ruleNodeLabel != null) {
                buf.append(ruleNodeLabel);
                buf.append(':');
            }
        }
    }

    protected void appendRuleSuffix(StringBuilder buf) {
        if (ruleName != null) {
            buf.append(')');
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    // Clause factories, for clause optimization and sanity checking

    public static Clause rule(String ruleName, Clause ruleClause) {
        ruleClause.ruleName = ruleName;
        return ruleClause;
    }

    public static Clause ast(String astLabel, Clause clause) {
        return new CreateASTNode(astLabel, clause);
    }

    public static Clause oneOrMore(Clause subClause) {
        // It doesn't make sense to wrap these clause types in OneOrMore, but the OneOrMore should have
        // no effect if this does occur in the grammar, so remove it
        if (subClause instanceof OneOrMore || subClause instanceof Nothing || subClause instanceof FollowedBy
                || subClause instanceof NotFollowedBy || subClause instanceof Start) {
            return subClause;
        }
        return new OneOrMore(subClause);
    }

    public static Clause zeroOrMore(Clause subClause) {
        // ZeroOrMore(X) -> FirstMatch(OneOrMore(X), Nothing)
        return optional(oneOrMore(subClause));
    }

    public static Clause optional(Clause subClause) {
        // Optional(X) -> FirstMatch(X, Nothing)
        return first(subClause, nothing());
    }

    public static Clause first(Clause... subClauses) {
        for (int i = 0; i < subClauses.length; i++) {
            if (subClauses[i] instanceof Nothing && i < subClauses.length - 1) {
                throw new IllegalArgumentException("Subclauses of " + First.class.getSimpleName() + " after "
                        + Nothing.class.getSimpleName() + " will not be matched");
            }
        }
        return new First(subClauses);
    }

    public static Clause followedBy(Clause subClause) {
        if (subClause instanceof Nothing) {
            // FollowedBy(Nothing) -> Nothing (since Nothing always matches)
            return subClause;
        } else if (subClause instanceof FollowedBy || subClause instanceof NotFollowedBy
                || subClause instanceof Start) {
            throw new IllegalArgumentException(FollowedBy.class.getSimpleName() + "("
                    + subClause.getClass().getSimpleName() + "(X)) is nonsensical");
        }
        return new FollowedBy(subClause);
    }

    public static Clause notFollowedBy(Clause subClause) {
        if (subClause instanceof Nothing) {
            throw new IllegalArgumentException(NotFollowedBy.class.getSimpleName() + "("
                    + Nothing.class.getSimpleName() + ") will never match anything");
        } else if (subClause instanceof FollowedBy || subClause instanceof NotFollowedBy
                || subClause instanceof Start) {
            throw new IllegalArgumentException(NotFollowedBy.class.getSimpleName() + "("
                    + subClause.getClass().getSimpleName() + "(X)) is nonsensical");
        }
        return new NotFollowedBy(subClause);
    }

    public static Clause start() {
        return new Start();
    }

    public static Clause nothing() {
        return new Nothing();
    }

    public static Clause seq(Clause... subClauses) {
        return new Seq(subClauses);
    }

    public static Clause r(String ruleName) {
        return new RuleRef(ruleName);
    }

    public static Clause c(char chr) {
        return new CharSet(chr);
    }

    public static Clause cRange(String charRanges) {
        boolean invert = charRanges.startsWith("^");
        List<CharSet> charSets = new ArrayList<>();
        for (int i = invert ? 1 : 0; i < charRanges.length(); i++) {
            char c = charRanges.charAt(i);
            if (i <= charRanges.length() - 3 && charRanges.charAt(i + 1) == '-') {
                char cEnd = charRanges.charAt(i + 2);
                if (cEnd < c) {
                    throw new IllegalArgumentException("Char range limits out of order: " + c + ", " + cEnd);
                }
                charSets.add(new CharSet(c, cEnd));
                i += 2;
            } else {
                charSets.add(new CharSet(c));
            }
        }
        return charSets.size() == 1 ? charSets.get(0) : new CharSet(charSets);
    }

    public static Clause str(String str) {
        return new CharSeq(str, /* ignoreCase = */ false);
    }
}
