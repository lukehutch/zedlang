package pikaparser.grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pikaparser.clause.Clause;
import pikaparser.clause.CreateASTNode;
import pikaparser.clause.RuleRef;
import pikaparser.memotable.Match;
import pikaparser.memotable.MemoTable;

public class Grammar {
    public final List<Clause> rules;
    public final List<Clause> allClauses;
    public Clause lexRule;
    public final Map<String, Clause> ruleNameToRule = new HashMap<>();
    public final Map<String, Clause> toStringToClause = new HashMap<>();

    public Grammar(List<Clause> rules) {
        this(/* lexRuleName = */ null, rules);
    }

    public Grammar(String lexRuleName, List<Clause> rules) {
        // Find all rules that have an AST node label at the top level, and remove the CreateASTNode
        for (int i = 0, n = rules.size(); i < n; i++) {
            Clause rule = rules.get(i);
            if (rule instanceof CreateASTNode) {
                // If there's a CreateASTNode at the top of a rule, it is not a subclause of another clause,
                // so it cannot be labeled using Clause.subClauseASTNodeLabels. Remove the CreateASTNode from
                // the clause hierarchy, and label the whole rule with the AST node label.
                Clause subClause = rule.subClauses[0];
                rules.set(i, subClause);
                subClause.ruleASTNodeLabel = rule.ruleASTNodeLabel;
                subClause.ruleName = rule.ruleName;
                rule = subClause;
            }
            // For all subclauses of rule, remove any CreateASTNode nodes in subclauses, and label subclauses
            addASTNodeLabels(rule);
        }

        // Create mapping from rule name to rule, so RuleRef nodes can be resolved (this needs to be done after
        // removing CreateASTNode nodes, otherwise the rule name could point to a toplevel CreateASTNode node)
        for (var rule : rules) {
            if (rule.ruleName == null) {
                throw new IllegalArgumentException("All rules must be named");
            }
            if (ruleNameToRule.put(rule.ruleName, rule) != null) {
                throw new IllegalArgumentException("Duplicate rule name: " + rule.ruleName);
            }
        }

        // Find the toplevel lex rule, if lexRuleName is specified
        if (lexRuleName != null) {
            lexRule = ruleNameToRule.get(lexRuleName);
            if (lexRule == null) {
                throw new IllegalArgumentException("Unknown lex rule name: " + lexRuleName);
            }
            // Check the lex rule does not contain any cycles
            checkNoCycles(lexRule, new HashSet<Clause>());
        }

        if (rules.size() == 0) {
            throw new IllegalArgumentException("Grammar must consist of at least one rule");
        }
        this.rules = rules;

        // Run toString() method on all clauses, bottom-up, so that toString() values are cached, and so that
        // after replacing all RuleRef objects with direct Clause references, calling toString() on a cyclic
        // clause structure does not get stuck in an infinite loop. This also interns clauses, coalescing shared
        // sub-clauses are into a DAG, so that effort is not wasted parsing different instances of the same
        // clause multiple times. This should be done after addASTNodeLabels and before resolveRuleRefs.
        Set<Clause> internVisited = new HashSet<>();
        for (Clause rule : rules) {
            Clause internedRule = intern(rule, internVisited);
            if (internedRule != rule) {
                // Should not need to update the rules array with the interned rule, because rule names
                // were already checked for duplicates, so rules should be distinct
                throw new IllegalArgumentException("Duplicate rule: " + internedRule);
            }
        }

        // For all subclauses of rules, remove any CreateASTNode nodes in subclauses, and label subclauses
        for (int i = 0, n = rules.size(); i < n; i++) {
            Clause rule = rules.get(i);
            if (rule.ruleName != null && rule instanceof RuleRef) {
                // A rule cannot just consisnt of a RuleRef, since that would mean the same rule gets two names
                throw new IllegalArgumentException("Rule " + rule.ruleName
                        + " consists of only a reference to another rule: " + ((RuleRef) rule).refdRuleName);
            }
            resolveRuleRefs(rule);
        }

        // Find clauses reachable from the toplevel clause, in reverse topological order.
        // Clauses can form a DAG structure, via RuleRef.
        allClauses = new ArrayList<Clause>();
        HashSet<Clause> visited = new HashSet<Clause>();
        for (Clause rule : rules) {
            findReachableClauses(rule, visited, allClauses);
        }

        // Find clauses that always match zero or more characters, e.g. FirstMatch(X | Nothing).
        // allClauses is in reverse topological order, i.e. bottom-up
        for (Clause clause : allClauses) {
            clause.testWhetherAlwaysMatches();
        }

        // Find seed parent clauses (in the case of Seq, this depends upon alwaysMatches being set in the prev step)
        for (var clause : allClauses) {
            clause.backlinkToSeedParentClauses();
        }
    }

    /**
     * Recursively call toString() on clause tree, so that toString() values are cached before {@link RuleRef}
     * objects are replaced with direct references.
     * 
     * @return
     */
    private Clause intern(Clause clause, Set<Clause> visited) {
        if (visited.add(clause)) {
            for (int i = 0; i < clause.subClauses.length; i++) {
                clause.subClauses[i] = intern(clause.subClauses[i], visited);
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

    /** Check a {@link Clause} tree does not contain any cycles (needed for top-down lex). */
    private static void checkNoCycles(Clause clause, Set<Clause> visited) {
        if (visited.add(clause)) {
            for (var subClause : clause.subClauses) {
                checkNoCycles(subClause, visited);
            }
        } else {
            throw new IllegalArgumentException("Lex rule's clause tree contains a cycle at " + clause);
        }
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

    private void updateSubClause(Clause clause, int subClauseIdx, Clause replacementSubClause,
            String subClauseASTNodeLabel) {
        // Replace subclause to remove CreateASTNode node(s)
        clause.subClauses[subClauseIdx] = replacementSubClause;

        // Label the subclause position with the AST node label of the ref'd rule, unless subclause is
        // already labeled in the parent clause
        if (subClauseASTNodeLabel != null
                && (clause.subClauseASTNodeLabels == null || clause.subClauseASTNodeLabels[subClauseIdx] == null)) {
            if (subClauseASTNodeLabel != null && clause.subClauseASTNodeLabels == null) {
                // Alloc array for subclause node labels, if not already done
                clause.subClauseASTNodeLabels = new String[clause.subClauses.length];
            }
            clause.subClauseASTNodeLabels[subClauseIdx] = subClauseASTNodeLabel;
        }
    }

    /**
     * Label subclause positions with the AST node label from any {@link CreateASTNode} nodes in each subclause
     * position.
     */
    private void addASTNodeLabels(Clause clause) {
        for (int i = 0; i < clause.subClauses.length; i++) {
            Clause subClause = clause.subClauses[i];
            String subClauseASTNodeLabel = null;
            Clause replacementSubClause = subClause;
            if (replacementSubClause instanceof CreateASTNode) {
                var visited = new LinkedHashSet<Clause>();
                for (boolean changed = true; changed;) {
                    // Stop when no more CreateASTNode nodes are encountered
                    changed = false;
                    // Avoid infinite loop
                    if (visited.add(replacementSubClause)) {
                        // CreateASTNode nodes can be nested, so need to follow the chain all the way to the end
                        while (replacementSubClause instanceof CreateASTNode) {
                            if (subClauseASTNodeLabel == null) {
                                // Label the subclause position with the highest of any encountered AST node labels
                                subClauseASTNodeLabel = replacementSubClause.ruleASTNodeLabel;
                            }

                            // Remove the CreateASTNode node from the grammar
                            replacementSubClause = replacementSubClause.subClauses[0];
                            changed = true;
                        }
                    } else {
                        throw new IllegalArgumentException("Reached infinite loop in node references: " + visited);
                    }
                }
            }

            // Replace subclause to remove CreateASTNode node(s), and label sub-subclause with AST node label
            if (replacementSubClause != subClause) {
                updateSubClause(clause, i, replacementSubClause, subClauseASTNodeLabel);
            }

            // Recurse through subclause tree, starting from sub-subclause, if a CreateASTNode node was removed
            addASTNodeLabels(replacementSubClause);
        }
    }

    /**
     * Resolve {@link RuleRef} clauses to a reference to the named rule, and label subclause positions with the AST
     * node label from any {@link CreateASTNode} nodes in each subclause position.
     */
    private void resolveRuleRefs(Clause clause) {
        for (int i = 0; i < clause.subClauses.length; i++) {
            Clause subClause = clause.subClauses[i];
            String subClauseASTNodeLabel = null;
            Clause replacementSubClause = subClause;
            if (replacementSubClause instanceof RuleRef) {
                var visited = new LinkedHashSet<Clause>();
                for (boolean changed = true; changed;) {
                    // Stop when no more RuleRef nodes are encountered
                    changed = false;
                    // Avoid infinite loop
                    if (visited.add(replacementSubClause)) {
                        // RuleRef nodes can be nested, so need to follow the chain all the way to the end
                        while (replacementSubClause instanceof RuleRef) {
                            if (subClauseASTNodeLabel == null) {
                                // Label the subclause position with the highest of any encountered AST node labels
                                subClauseASTNodeLabel = replacementSubClause.ruleASTNodeLabel;
                            }

                            // Look up rule from name
                            String refdRuleName = ((RuleRef) replacementSubClause).refdRuleName;
                            replacementSubClause = ruleNameToRule.get(refdRuleName);
                            if (replacementSubClause == null) {
                                throw new IllegalArgumentException("Unknown rule name: " + refdRuleName);
                            }
                            changed = true;
                        }
                    } else {
                        throw new IllegalArgumentException("Reached infinite loop in node references: " + visited);
                    }
                }
            }

            if (replacementSubClause != subClause) {
                // Replace RuleRef with direct reference to the named rule 
                updateSubClause(clause, i, replacementSubClause, subClauseASTNodeLabel);
            } else {
                // Recurse through subclause tree, but only if a RuleRef was not reached 
                resolveRuleRefs(subClause);
            }
        }
    }

    /**
     * Get the {@link Match} entries for all nonoverlapping matches of the named rule, obtained by greedily matching
     * from the beginning of the string, then looking for the next match after the end of the current match.
     */
    public List<Match> getNonOverlappingMatches(MemoTable memoTable, String ruleName) {
        Clause clause = ruleNameToRule.get(ruleName);
        if (clause == null) {
            throw new IllegalArgumentException("Unknown rule name: " + ruleName);
        }
        return memoTable.getNonOverlappingMatches(clause);
    }

    /**
     * Get the {@link Match} entries for all postions where a match was queried for the named rule, but there was no
     * match.
     */
    public List<Integer> getNonMatches(MemoTable memoTable, String ruleName) {
        Clause clause = ruleNameToRule.get(ruleName);
        if (clause == null) {
            throw new IllegalArgumentException("Unknown rule name: " + ruleName);
        }
        return memoTable.getNonMatchPositions(clause);
    }
}
