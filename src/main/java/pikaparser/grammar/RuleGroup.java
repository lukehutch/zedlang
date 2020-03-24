package pikaparser.grammar;

import static pikaparser.clause.Clause.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import pikaparser.clause.Clause;
import pikaparser.clause.RuleRef;

public class RuleGroup {
    public final String ruleName;
    public TreeMap<Integer, Rule> precedenceToRule = new TreeMap<>();
    private List<Clause> higherPrecedenceSelectors = new ArrayList<>();
    private Clause allPrecedenceSelector;

    public RuleGroup(String ruleName, List<Rule> rules) {
        this.ruleName = ruleName;
        for (var rule : rules) {
            if (precedenceToRule.put(rule.precedence, rule) != null) {
                throw new IllegalArgumentException(
                        "Multiple rules with name " + ruleName + " and precedence " + rule.precedence);
            }
        }

        // Get clauses in order from lowest to highest precedence
        var numRules = precedenceToRule.size();
        var hasPrecedence = numRules > 1;
        if (hasPrecedence) {
            // Create precedence selector clauses
            var precedenceOrder = new ArrayList<>(precedenceToRule.entrySet());
            for (int precedenceIdx = 0; precedenceIdx < numRules; precedenceIdx++) {
                // Create higher precedence selector clause (P{i+1} / P{i+2} / ... / P{N-1}) for each P{i}
                // The highest precedence term (e.g. parens) should select (P{0} / P{1} / ... / P{N-1}),
                // hence mod (i + 1) by N
                int startIdx = (precedenceIdx + 1) % numRules;
                int numSelectorSubClauses = numRules - startIdx;
                var selectorSubClauses = new Clause[numSelectorSubClauses];
                var selectorSubClauseASTNodeLabels = new String[numSelectorSubClauses];
                for (int j = startIdx; j < numRules; j++) {
                    var higherPrecedenceRule = precedenceOrder.get(j).getValue();
                    selectorSubClauses[j - startIdx] = higherPrecedenceRule.clause;
                    selectorSubClauseASTNodeLabels[j - startIdx] = higherPrecedenceRule.astNodeLabel;
                }

                // Create First(...) clause wrapping the higher precedence selectors
                Clause higherPrecedenceSelector;
                if (numSelectorSubClauses > 1) {
                    // If there are two or more higher precedences than the current precedence level,
                    // (or the current precedence level is the highest level, which selects P{0}..P{N-1}),
                    // then wrap the higher precedence selector terms in a First node
                    higherPrecedenceSelector = first(selectorSubClauses);
                    // Copy AST node labels across from referenced clauses
                    higherPrecedenceSelector.subClauseASTNodeLabels = selectorSubClauseASTNodeLabels;
                    // Override toString to show only the precedence range rather than the full rule
                    higherPrecedenceSelector.toStringCached = "<" + ruleName + "["
                            + precedenceOrder.get(startIdx).getKey()
                            + (startIdx < precedenceOrder.size() - 1
                                    ? "-" + precedenceOrder.get(precedenceOrder.size() - 1).getKey()
                                    : "")
                            + "]>";
                } else {
                    // If there's only one higher precedence, don't wrap the higher precedence clause in an
                    // unnecessary First node
                    higherPrecedenceSelector = selectorSubClauses[0];
                }
                higherPrecedenceSelectors.add(higherPrecedenceSelector);

                // Save reference to selector for all precedence levels (P{0} / ... / P{N-1})
                if (precedenceIdx == precedenceOrder.size() - 1) {
                    allPrecedenceSelector = higherPrecedenceSelector;
                }
            }
        } else {
            // There's only one precedence level -- just use the clause itself, there's no precedence to select
            allPrecedenceSelector = precedenceToRule.entrySet().iterator().next().getValue().clause;
            higherPrecedenceSelectors.add(allPrecedenceSelector);
        }
    }

    /**
     * Return the AST node label for the toplevel clause, if there is a single clause for the rule group, and it has
     * an AST node label, otherwise return null.
     */
    public String getASTNodeLabel() {
        if (precedenceToRule.size() == 1) {
            // There's only one precedence level for rule group -- return AST node label of toplevel clause
            return precedenceToRule.entrySet().iterator().next().getValue().astNodeLabel;
        } else {
            // There are multiple precedence levels for rule group -- return null
            return null;
        }
    }

    /**
     * Return a single clause for the rule group, which is either the toplevel clause of the rule, if this rule
     * group contains only a single rule, or the all-precedence-selector clause, if this rule group contains
     * multiple rules of different precedence.
     */
    public Clause getBaseClause() {
        if (precedenceToRule.size() == 1) {
            // There's only one precedence level for rule group -- return toplevel clause
            return precedenceToRule.entrySet().iterator().next().getValue().clause;
        } else {
            // There are multiple precedence levels for rule group -- return precedence selector
            return allPrecedenceSelector;
        }
    }

    /**
     * Recursively call toString() on the clause tree for all rules in this {@link RuleGroup}, so that toString()
     * values are cached before {@link RuleRef} objects are replaced with direct references, and so that shared
     * subclauses are only matched once.
     */
    public void intern(Map<String, Clause> toStringToClause, Set<Clause> visited) {
        for (var rule : precedenceToRule.values()) {
            rule.intern(toStringToClause, visited);
        }
    }

    /** Resolve {@link RuleRef} clauses to a reference to the named rule. */
    private void resolveRuleRefs(Rule rule, Clause clause, int precedence, int precedenceIdx,
            Map<String, RuleGroup> ruleNameToRuleGroup, Set<Clause> visited) {
        if (visited.add(clause)) {
            for (int i = 0; i < clause.subClauses.length; i++) {
                Clause subClause = clause.subClauses[i];
                if (subClause instanceof RuleRef) {
                    // Look up rule from name in RuleRef
                    String refdRuleName = ((RuleRef) subClause).refdRuleName;
                    if (refdRuleName.equals(rule.ruleName)) {
                        // This is a rule self-reference -- replace RuleRef with a higher precedence selector clause
                        clause.subClauses[i] = higherPrecedenceSelectors.get(precedenceIdx);

                    } else {
                        // Referenced rule is different from the current rule -- set current clause to
                        // the base clause of the referenced rule
                        var refdRuleGroup = ruleNameToRuleGroup.get(refdRuleName);
                        if (refdRuleGroup == null) {
                            throw new IllegalArgumentException("Unknown rule name: " + refdRuleName);
                        }
                        clause.subClauses[i] = refdRuleGroup.getBaseClause();
                    }
                    // Stop recursing at RuleRef
                } else {
                    // Recurse through subclause tree if subclause was not a RuleRef 
                    resolveRuleRefs(rule, subClause, precedence, precedenceIdx, ruleNameToRuleGroup, visited);
                }
            }
        }
    }

    /** Resolve {@link RuleRef} clauses to a reference to the named rule. */
    public void resolveRuleRefs(Map<String, RuleGroup> ruleNameToRuleGroup, Set<Clause> visited) {
        var hasPrecedence = precedenceToRule.size() > 1;
        int precedenceIdx = 0;
        for (var ent : precedenceToRule.entrySet()) {
            var precedence = ent.getKey();
            var rule = ent.getValue();
            if (rule.clause instanceof RuleRef) {
                // Follow a chain of toplevel RuleRef instances
                Set<Clause> chainVisited = new HashSet<>();
                var currClause = rule.clause;
                while (currClause instanceof RuleRef) {
                    if (!chainVisited.add(currClause)) {
                        throw new IllegalArgumentException(
                                "Cycle in " + RuleRef.class.getSimpleName() + " references for rule " + ruleName);
                    }
                    // Look up rule from name in RuleRef
                    String refdRuleName = ((RuleRef) currClause).refdRuleName;
                    var refdRuleGroup = ruleNameToRuleGroup.get(refdRuleName);
                    if (refdRuleGroup == null) {
                        throw new IllegalArgumentException("Unknown rule name: " + refdRuleName);
                    }

                    if (refdRuleGroup == this) {
                        // Referenced rule group is the same as the current rule group
                        if (!hasPrecedence) {
                            // There is only one precedence for rule, and it is a reference back to
                            // the rule itself, i.e. R <- R
                            throw new IllegalArgumentException("Rule references only itself: " + ruleName);
                        }
                        // There are multiple precedences for this rule group, and one of them is a reference
                        // back to the rule itself -- replace rule self-reference with higher prec selector
                        currClause = higherPrecedenceSelectors.get(precedenceIdx);

                    } else {
                        // Else referenced rule is different from current rule -- set current clause to
                        // all-precedence-selector clause of referenced rule
                        currClause = refdRuleGroup.allPrecedenceSelector;

                        // If the referenced rule has only one precedence level, and it creates an AST node,
                        // in its toplevel clause, need to add the AST node label to the rule
                        if (rule.astNodeLabel == null && refdRuleGroup.precedenceToRule.size() == 1) {
                            rule.astNodeLabel = refdRuleGroup.precedenceToRule.entrySet().iterator().next()
                                    .getValue().astNodeLabel;
                        }
                    }

                    // Record rule name in the rule's toplevel clause, for toString
                    currClause.registerRule(rule);
                }

                // Overwrite RuleRef with direct reference to the named rule 
                rule.clause = currClause;

            } else {
                // Recurse through subclause tree if toplevel clause was not a RuleRef 
                resolveRuleRefs(rule, rule.clause, precedence, precedenceIdx, ruleNameToRuleGroup, visited);
            }
            precedenceIdx++;
        }
    }

    /** Check a {@link Clause} tree does not contain any cycles (needed for top-down lex). */
    private static void checkNoCycles(Clause clause, Map<String, RuleGroup> ruleNameToRuleGroup,
            Set<Clause> visited) {
        if (visited.add(clause)) {
            if (clause instanceof RuleRef) {
                throw new IllegalArgumentException(
                        "There should not be any " + RuleRef.class.getSimpleName() + " nodes left in grammar");
            }
            for (var subClause : clause.subClauses) {
                checkNoCycles(subClause, ruleNameToRuleGroup, visited);
            }
            // Clause graph is a DAG, not a tree, after interning subclauses and replacing RuleRef instances,
            // so need to remove clause from visited again on exit to properly detect cycles
            visited.remove(clause);
        } else {
            throw new IllegalArgumentException("Lex rule's clause tree contains a cycle at " + clause);
        }
    }

    /** Check a {@link Clause} tree does not contain any cycles (needed for top-down lex). */
    public void checkNoCycles(Map<String, RuleGroup> ruleNameToRuleGroup) {
        checkNoCycles(getBaseClause(), ruleNameToRuleGroup, new HashSet<Clause>());
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
        for (var clause : higherPrecedenceSelectors) {
            findReachableClauses(clause, visited, allClauses);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        var hasPrecedence = precedenceToRule.size() > 1;
        if (hasPrecedence) {
            buf.append('(');
        }
        boolean first = true;
        for (var ent : precedenceToRule.entrySet()) {
            if (first) {
                first = false;
            } else {
                buf.append("; ");
            }
            var precedenceRule = ent.getValue();
            buf.append(precedenceRule.toString());
        }
        if (hasPrecedence) {
            buf.append(')');
        }
        return buf.toString();
    }
}
