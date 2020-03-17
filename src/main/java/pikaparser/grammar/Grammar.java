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
    public final Map<String, Clause> ruleNameToRule = new HashMap<>();

    public Grammar(List<Clause> rules) {
        if (rules.size() == 0) {
            throw new IllegalArgumentException("Grammar must consist of at least one rule");
        }
        this.rules = rules;

        // Create mapping from rule name to rule, so RuleRef nodes can be resolved
        for (var rule : rules) {
            if (rule.ruleName == null) {
                throw new IllegalArgumentException("All rules must be named");
            }
            if (ruleNameToRule.put(rule.ruleName, rule) != null) {
                throw new IllegalArgumentException("Duplicate rule name: " + rule.ruleName);
            }
        }

        // Find all rules that have an AST node label at the top level, and remove the CreateASTNode
        for (int i = 0, n = rules.size(); i < n; i++) {
            Clause rule = rules.get(i);
            if (rule instanceof CreateASTNode) {
                // If there's a CreateASTNode at the top of a rule, it is not a subclause of another clause,
                // so it cannot be labeled using Clause.subClauseASTNodeLabels. Remove the CreateASTNode from
                // the clause hierarchy, and label the whole rule with the AST node label.
                Clause subClause = rule.subClauses[0];
                rules.set(i, subClause);
                subClause.ruleNodeLabel = rule.ruleNodeLabel;
            }
        }

        // For all subclauses of rules, remove any CreateASTNode nodes in subclauses, and label subclauses
        for (int i = 0, n = rules.size(); i < n; i++) {
            Clause rule = rules.get(i);
            resolveSubclauses(rule);
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

    /** Find reachable clauses, and bottom-up (postorder), find clauses that always match in every position. */
    private static void findReachableClauses(Clause clause, Set<Clause> visited, List<Clause> revTopoOrderOut) {
        if (visited.add(clause)) {
            for (var subClause : clause.subClauses) {
                findReachableClauses(subClause, visited, revTopoOrderOut);
            }
            revTopoOrderOut.add(clause);
        }
    }

    /**
     * Resolve {@link RuleRef} clauses to a reference to the named rule, and label subclause positions with the AST
     * node label from any {@link CreateASTNode} nodes in each subclause position.
     */
    private void resolveSubclauses(Clause clause) {
        for (int i = 0; i < clause.subClauses.length; i++) {
            Clause subClause = clause.subClauses[i];
            String subClauseASTNodeLabel = null;
            Clause replacementSubClause = subClause;

            if (replacementSubClause instanceof CreateASTNode || replacementSubClause instanceof RuleRef) {
                var visited = new LinkedHashSet<Clause>();
                for (boolean changed = true; changed;) {
                    // Stop when no more CreateASTNode or RuleRef nodes are encountered
                    changed = false;
                    // Avoid infinite loop
                    if (visited.add(replacementSubClause)) {
                        // RuleRef and CreateASTNode nodes can be nested, so need to follow the chain all the way to the end
                        while (replacementSubClause instanceof CreateASTNode) {
                            if (subClauseASTNodeLabel == null) {
                                // Label the subclause position with the highest of any encountered AST node labels
                                subClauseASTNodeLabel = replacementSubClause.ruleNodeLabel;
                            }

                            // Remove the CreateASTNode node from the grammar
                            replacementSubClause = replacementSubClause.subClauses[0];
                            changed = true;
                        }
                        while (replacementSubClause instanceof RuleRef) {
                            if (subClauseASTNodeLabel == null) {
                                // Label the subclause position with the highest of any encountered AST node labels
                                subClauseASTNodeLabel = replacementSubClause.ruleNodeLabel;
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

            // Replace subclause if necessary
            if (replacementSubClause != subClause) {
                clause.subClauses[i] = replacementSubClause;
            }

            // Label the subclause position with the AST node label of the ref'd rule, unless subclause is
            // already labeled in the parent clause
            if (subClauseASTNodeLabel != null
                    && (clause.subClauseASTNodeLabels == null || clause.subClauseASTNodeLabels[i] == null)) {
                if (subClauseASTNodeLabel != null && clause.subClauseASTNodeLabels == null) {
                    // Alloc array for subclause node labels, if not already done
                    clause.subClauseASTNodeLabels = new String[clause.subClauses.length];
                }
                if (subClauseASTNodeLabel != null) {
                    clause.subClauseASTNodeLabels[i] = subClauseASTNodeLabel;
                }
                clause.subClauseASTNodeLabels[i] = subClauseASTNodeLabel;
            }

            // Add AST node label in subclause position, if node is labeled

            // Recurse
            resolveSubclauses(replacementSubClause);
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
