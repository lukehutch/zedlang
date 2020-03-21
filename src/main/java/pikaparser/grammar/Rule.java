package pikaparser.grammar;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pikaparser.clause.Clause;
import pikaparser.clause.RuleRef;

public class Rule {
    public final String ruleName;
    public String astNodeLabel;
    public Clause clause;

    public Rule(String ruleName, Clause clause) {
        this.ruleName = ruleName;
        clause.ruleNames.add(ruleName);

        this.astNodeLabel = clause.astNodeLabel;
        clause.astNodeLabel = null;

        this.clause = clause;

        // Move AST node labels from subclauses to an array of names in the parent, so that RuleRef
        // instances and interned subclauses (which can be shared by multiple clauses) are labeled
        // by position within the parent.
        liftASTNodeLabels(clause);
    }

    /**
     * Label the subclause position with the AST node label of the ref'd rule, unless subclause is already labeled
     * in the parent clause.
     */
    private static void updateSubClauseASTNodeLabel(Clause clause, int subClauseIdx, String subClauseASTNodeLabel) {
        if (subClauseASTNodeLabel != null) {
            if (clause.subClauseASTNodeLabels == null) {
                // Alloc array for subclause node labels, if not already done
                clause.subClauseASTNodeLabels = new String[clause.subClauses.length];
            }
            if (clause.subClauseASTNodeLabels[subClauseIdx] == null) {
                // Update subclause label, if it hasn't already been labeled
                clause.subClauseASTNodeLabels[subClauseIdx] = subClauseASTNodeLabel;
            }
        }
    }

    /**
     * Label subclause positions with the AST node label from any {@link CreateASTNode} nodes in each subclause
     * position.
     */
    private static void liftASTNodeLabels(Clause clause) {
        for (int i = 0; i < clause.subClauses.length; i++) {
            Clause subClause = clause.subClauses[i];
            if (subClause.astNodeLabel != null) {
                // Copy the label from subclause node to subClauseASTNodeLabels array within the parent
                updateSubClauseASTNodeLabel(clause, i, subClause.astNodeLabel);

                // Remove the label from the subclause, so that the toString() for the subclause does not
                // include the node label (the node label is only included if the parent clause's toString()
                // method is called). This allows for interning to work for shared sub-clauses, even if
                // multiple parent clauses create different AST nodes from the same sub-clause. 
                subClause.astNodeLabel = null;
            }
            // Recurse
            liftASTNodeLabels(subClause);
        }
    }

    /** Resolve {@link RuleRef} clauses to a reference to the named rule. */
    private static void resolveRuleRefs(Clause clause, Map<String, Rule> ruleNameToRule, Set<Clause> visited) {
        if (visited.add(clause)) {
            for (int i = 0; i < clause.subClauses.length; i++) {
                Clause subClause = clause.subClauses[i];
                if (subClause instanceof RuleRef) {
                    // Look up rule from name in RuleRef
                    String refdRuleName = ((RuleRef) subClause).refdRuleName;
                    var refdRule = ruleNameToRule.get(refdRuleName);
                    if (refdRule == null) {
                        throw new IllegalArgumentException("Unknown rule name: " + refdRuleName);
                    }
                    if (refdRule.clause == subClause) {
                        throw new IllegalArgumentException("Rule refers to itself: " + subClause);
                    }

                    // Replace RuleRef with direct reference to the named rule 
                    clause.subClauses[i] = refdRule.clause;

                    // In case the referenced rule creates an AST node, add the AST node label
                    updateSubClauseASTNodeLabel(clause, i, refdRule.astNodeLabel);

                } else {
                    // Recurse through subclause tree if subclause was not a RuleRef 
                    resolveRuleRefs(subClause, ruleNameToRule, visited);
                }
            }
        }
    }

    /** Resolve {@link RuleRef} clauses to a reference to the named rule. */
    public void resolveRuleRefs(Map<String, Rule> ruleNameToRule, Set<Clause> visited) {
        if (clause instanceof RuleRef) {
            // Look up rule from name in RuleRef
            String refdRuleName = ((RuleRef) clause).refdRuleName;
            var refdRule = ruleNameToRule.get(refdRuleName);
            if (refdRule == null) {
                throw new IllegalArgumentException("Unknown rule name: " + refdRuleName);
            }
            if (refdRule.clause == clause) {
                throw new IllegalArgumentException("Rule refers to itself: " + clause);
            }

            // Replace RuleRef with direct reference to the named rule 
            clause = refdRule.clause;

            // In case the referenced rule creates an AST node, add the AST node label
            if (astNodeLabel == null) {
                astNodeLabel = refdRule.astNodeLabel;
            }

            // If a rule's toplevel clause is a RuleRef, then the referenced rule now has two rule labels
            refdRule.clause.ruleNames.add(ruleName);

        } else {
            // Recurse through subclause tree if toplevel clause was not a RuleRef 
            resolveRuleRefs(clause, ruleNameToRule, visited);
        }
    }

    /**
     * Recursively call toString() on clause tree, so that toString() values are cached before {@link RuleRef}
     * objects are replaced with direct references.
     */
    private static Clause intern(Clause clause, Map<String, Clause> toStringToClause, Set<Clause> visited) {
        if (visited.add(clause)) {
            for (int i = 0; i < clause.subClauses.length; i++) {
                clause.subClauses[i] = intern(clause.subClauses[i], toStringToClause, visited);
            }

            // Call toString() bottom-up (after recursing to child nodes) so subclause toString() values are cached
            String toStr = clause.toString();

            // Intern the clause, and return interned copy 
            Clause prevInternedClause = toStringToClause.putIfAbsent(toStr, clause);
            return prevInternedClause != null ? prevInternedClause : clause;
        } else {
            // Avoid infinite loop
            Clause internedClause = toStringToClause.get(clause.toString());
            return internedClause != null ? internedClause : clause;
        }
    }

    /**
     * Recursively call toString() on the clause tree for this {@link Rule}, so that toString() values are cached
     * before {@link RuleRef} objects are replaced with direct references.
     */
    public void intern(Map<String, Clause> toStringToClause, Set<Clause> visited) {
        var internedClause = intern(clause, toStringToClause, visited);
        if (internedClause != clause) {
            internedClause.ruleNames.addAll(clause.ruleNames);
            clause = internedClause;
        }
    }

    /** Check a {@link Clause} tree does not contain any cycles (needed for top-down lex). */
    private void checkNoCycles(Clause clause, Map<String, Rule> ruleNameToRule, Set<Clause> visited) {
        if (visited.add(clause)) {
            if (clause instanceof RuleRef) {
                var refdRuleName = ((RuleRef) clause).refdRuleName;
                var refdRule = ruleNameToRule.get(refdRuleName);
                if (refdRule == null) {
                    throw new IllegalArgumentException("Referenced rule does not exist: " + refdRuleName);
                }
                checkNoCycles(refdRule.clause, ruleNameToRule, visited);
            } else {
                for (var subClause : clause.subClauses) {
                    checkNoCycles(subClause, ruleNameToRule, visited);
                }
            }
            visited.remove(clause);
        } else {
            throw new IllegalArgumentException("Lex rule's clause tree contains a cycle at " + clause);
        }
    }

    /** Check a {@link Clause} tree does not contain any cycles (needed for top-down lex). */
    public void checkNoCycles(Map<String, Rule> ruleNameToRule) {
        checkNoCycles(clause, ruleNameToRule, new HashSet<Clause>());
    }

    /** Find reachable clauses, and bottom-up (postorder), find clauses that always match in every position. */
    private static void findReachableClauses(Clause clause, Set<Clause> visited, List<Clause> revTopoOrderOut) {
        if (visited.add(clause)) {
            for (var subClause : clause.subClauses) {
                findReachableClauses(subClause, visited, revTopoOrderOut);
            }
            revTopoOrderOut.add(clause);
        }
    }

    /** Find reachable clauses, and bottom-up (postorder), find clauses that always match in every position. */
    public void findReachableClauses(HashSet<Clause> visited, List<Clause> allClauses) {
        findReachableClauses(clause, visited, allClauses);
    }

    @Override
    public String toString() {
        return clause.toStringWithRuleName();
    }
}
