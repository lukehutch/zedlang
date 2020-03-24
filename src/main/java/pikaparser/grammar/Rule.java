package pikaparser.grammar;

import java.util.Map;
import java.util.Set;

import pikaparser.clause.ASTNodeLabel;
import pikaparser.clause.Clause;
import pikaparser.clause.RuleRef;

public class Rule {
    public final String ruleName;
    public final int precedence;
    public Clause clause;
    public String astNodeLabel;

    public Rule(String ruleName, int precedence, Clause clause) {
        this.ruleName = ruleName;
        this.precedence = precedence;

        // If the toplevel clause of this rule is an ASTNodeLabel, then copy the node label into the rule,
        // and remove the ASTNodeLabel node
        if (clause instanceof ASTNodeLabel) {
            this.astNodeLabel = ((ASTNodeLabel) clause).astNodeLabel;
            this.clause = clause.subClauses[0];
        } else {
            this.clause = clause;
        }

        // Move AST node labels from ASTNodeLabel subclauses to subClauseASTNodeLabels in the parent, so that
        // RuleRef instances and interned subclauses (which can be shared by multiple clauses) are labeled
        // by position within the parent.
        liftASTNodeLabels(clause);

        // Register rule in clause, for toString()
        this.clause.registerRule(this);
    }

    public Rule(String ruleName, Clause clause) {
        // Use precedence of -1 for rules that only have one precedence
        // (this causes the precedence number not to be shown in the output of toStringWithRuleNames())
        this(ruleName, -1, clause);
    }

    /**
     * Label subclause positions with the AST node label from any {@link CreateASTNode} nodes in each subclause
     * position.
     */
    private static void liftASTNodeLabels(Clause clause) {
        for (int subClauseIdx = 0; subClauseIdx < clause.subClauses.length; subClauseIdx++) {
            Clause subClause = clause.subClauses[subClauseIdx];
            if (subClause instanceof ASTNodeLabel) {
                // Copy any AST node labels from subclause node to subClauseASTNodeLabels array within the parent
                var subClauseASTNodeLabel = ((ASTNodeLabel) subClause).astNodeLabel;
                if (subClauseASTNodeLabel != null) {
                    if (clause.subClauseASTNodeLabels == null) {
                        // Alloc array for subclause node labels, if not already done
                        clause.subClauseASTNodeLabels = new String[clause.subClauses.length];
                    }
                    if (clause.subClauseASTNodeLabels[subClauseIdx] == null) {
                        // Update subclause label, if it hasn't already been labeled
                        clause.subClauseASTNodeLabels[subClauseIdx] = subClauseASTNodeLabel;
                    }
                } else {
                    throw new IllegalArgumentException(ASTNodeLabel.class.getSimpleName() + " is null");
                }
                // Remove the ASTNodeLabel node 
                clause.subClauses[subClauseIdx] = subClause.subClauses[0];
            }
            // Recurse
            liftASTNodeLabels(subClause);
        }
    }

    /**
     * Recursively call toString() on clause tree, so that toString() values are cached before {@link RuleRef}
     * objects are replaced with direct references, and so that shared subclauses are only matched once.
     */
    private static Clause intern(Clause clause, Map<String, Clause> toStringToClause, Set<Clause> visited) {
        if (visited.add(clause)) {
            // Call toString() on (and intern) subclauses, bottom-up
            for (int i = 0; i < clause.subClauses.length; i++) {
                clause.subClauses[i] = intern(clause.subClauses[i], toStringToClause, visited);
            }
            // Call toString after recursing to child nodes
            var toStr = clause.toString();

            // Intern the clause based on the toString value
            var prevInternedClause = toStringToClause.putIfAbsent(toStr, clause);
            return prevInternedClause != null ? prevInternedClause : clause;
        } else {
            // Avoid infinite loop
            var internedClause = toStringToClause.get(clause.toString());
            return internedClause != null ? internedClause : clause;
        }
    }

    /**
     * Recursively call toString() on the clause tree for this {@link Rule}, so that toString() values are cached
     * before {@link RuleRef} objects are replaced with direct references, and so that shared subclauses are only
     * matched once.
     */
    void intern(Map<String, Clause> toStringToClause, Set<Clause> visited) {
        var internedClause = intern(clause, toStringToClause, visited);
        if (internedClause != clause) {
            // Toplevel clause was already interned as a subclause of another rule
            internedClause.registerRule(this);
            clause = internedClause;
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        buf.append(ruleName);
        buf.append(" = ");
        buf.append(clause.toString());
        buf.append(')');
        return buf.toString();
    }
}
