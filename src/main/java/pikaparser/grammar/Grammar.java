package pikaparser.grammar;

import static pikaparser.clause.Clause.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import pikaparser.clause.Clause;
import pikaparser.clause.RuleRef;
import pikaparser.memotable.Match;
import pikaparser.memotable.MemoTable;

public class Grammar {
    public final List<Clause> allClauses;
    public Clause lexClause;
    public Map<String, Rule> ruleNameWithPrecedenceToRule;

    public Grammar(List<Rule> rules) {
        this(/* lexRuleName = */ null, rules);
    }

    public Grammar(String lexRuleName, List<Rule> rules) {
        if (rules.size() == 0) {
            throw new IllegalArgumentException("Grammar must consist of at least one rule");
        }

        // Group rules by name
        Map<String, List<Rule>> ruleNameToRules = new HashMap<>();
        for (var rule : rules) {
            if (rule.ruleName == null) {
                throw new IllegalArgumentException("All rules must be named");
            }
            var rulesWithName = ruleNameToRules.get(rule.ruleName);
            if (rulesWithName == null) {
                ruleNameToRules.put(rule.ruleName, rulesWithName = new ArrayList<>());
            }
            rulesWithName.add(rule);
        }
        List<Rule> allRules = new ArrayList<>(rules);
        for (var ent : ruleNameToRules.entrySet()) {
            // Rewrite rules that have multiple precedence levels
            var rulesWithName = ent.getValue();
            if (rulesWithName.size() > 1) {
                var ruleName = ent.getKey();
                // Add rules for higher precedence selectors, and rewrite rule self-references to select
                // higher precedence. e.g. given max precedence of 5:
                // R[3] <- '+' R is replaced with R[3] <- '+' R[4-5]
                // R[5] <- '(' R ')' is replaced with R[5] <- '(' R[0-5] ')'
                handlePrecedence(ruleName, rulesWithName, allRules);
            }
        }

        // If there is more than one precedence level for a rule, the handlePrecedence call above modifies
        // rule names to include a precedence suffix, and also adds an all-precedence selector clause with the
        // orgininal rule name. All rule names should now be unique.
        ruleNameWithPrecedenceToRule = new HashMap<>();
        for (var rule : allRules) {
            if (ruleNameWithPrecedenceToRule.put(rule.ruleName, rule) != null) {
                // Should not happen
                throw new IllegalArgumentException("Duplicate rule name");
            }
        }

        // Intern clauses based on their toString() value, coalescing shared sub-clauses into a DAG, so that
        // effort is not wasted parsing different instances of the same clause multiple times, and so that
        // when a subclause matches, all parent clauses will be added to the active set in the next iteration.
        // Also causes the toString() values to be cached, so that after RuleRefs are replaced with direct
        // Clause references, toString() doesn't get stuck in an infinite loop.
        Map<String, Clause> toStringToClause = new HashMap<>();
        Set<Clause> internVisited = new HashSet<>();
        for (var rule : allRules) {
            rule.intern(toStringToClause, internVisited);
        }

        // Resolve each RuleRef into a direct reference to the referenced clause
        Set<Clause> ruleClausesVisited = new HashSet<>();
        for (var rule : allRules) {
            rule.resolveRuleRefs(ruleNameWithPrecedenceToRule, ruleClausesVisited);
        }

        if (lexRuleName != null) {
            // Find the toplevel lex rule, if lexRuleName is specified
            var lexRule = ruleNameWithPrecedenceToRule.get(lexRuleName);
            if (lexRule == null) {
                throw new IllegalArgumentException("Unknown lex rule name: " + lexRuleName);
            }
            // Check the lex rule does not contain any cycles
            lexRule.checkNoCycles();
            lexClause = lexRule.clause;
        }

        // Find clauses reachable from the toplevel clause, in reverse topological order.
        // Clauses can form a DAG structure, via RuleRef.
        allClauses = new ArrayList<Clause>();
        HashSet<Clause> allClausesVisited = new HashSet<Clause>();
        for (var rule : allRules) {
            rule.findReachableClauses(allClausesVisited, allClauses);
        }

        // Find clauses that always match zero or more characters, e.g. FirstMatch(X | Nothing).
        // allClauses is in reverse topological order, i.e. bottom-up
        for (Clause clause : allClauses) {
            clause.testWhetherCanMatchZeroChars();
        }

        // Find seed parent clauses (in the case of Seq, this depends upon alwaysMatches being set in the prev step)
        for (var clause : allClauses) {
            clause.backlinkToSeedParentClauses();
        }
    }

    /** Resolve {@link RuleRef} clauses that reference the rule with a precedence selector reference. */
    private static void rewriteSelfRefs(Clause clause, String ruleName, String higherPrecedenceSelectorRuleName,
            Set<Clause> visited) {
        if (visited.add(clause)) {
            if (clause instanceof RuleRef && ((RuleRef) clause).refdRuleName.equals(ruleName)) {
                // Replace rule self-ref with precedence selector, e.g.:
                // e.g. R[3] <- '+' R is replaced with R[3] <- '+' R[4-5], given max precedence 5
                ((RuleRef) clause).refdRuleName = higherPrecedenceSelectorRuleName;
                ((RuleRef) clause).toStringCached = null;
            } else {
                // Recurse through subclause tree if toplevel clause was not a RuleRef
                for (int i = 0; i < clause.subClauses.length; i++) {
                    rewriteSelfRefs(clause.subClauses[i], ruleName, higherPrecedenceSelectorRuleName, visited);
                }
            }
        }
    }

    private static void handlePrecedence(String ruleName, List<Rule> rules, List<Rule> allRulesOut) {
        // There's nothing to do if there's only one precedence level
        // Check there are no duplicate precedence levels
        var precedenceToRule = new TreeMap<Integer, Rule>();
        for (var rule : rules) {
            if (precedenceToRule.put(rule.precedence, rule) != null) {
                throw new IllegalArgumentException(
                        "Multiple rules with name " + ruleName + " and precedence " + rule.precedence);
            }
        }

        // Get rules in ascending order of precedence
        var precedenceOrder = new ArrayList<>(precedenceToRule.values());

        // Create precedence selector clauses
        var numRules = rules.size();
        for (int precedenceIdx = 0; precedenceIdx < numRules; precedenceIdx++) {
            // Since there is more than one precedence level, update rule name to include precedence
            var rule = precedenceOrder.get(precedenceIdx);
            rule.ruleName += "[" + rule.precedence + "]";

            // Create higher precedence selector clause (P{i+1} / P{i+2} / ... / P{N-1}) for each P{i}
            // The highest precedence term (e.g. parens) should select (P{0} / P{1} / ... / P{N-1}),
            // hence mod (i + 1) by N
            var startIdx = (precedenceIdx + 1) % numRules;
            var numPrecedenceSelectorClauses = numRules - startIdx;
            // Precedence level N-2 has only one higher precedence level, N-1, so there's no need to
            // select the first of all higher precedences
            String higherPrecedenceSelectorRuleName;
            if (numPrecedenceSelectorClauses > 1) {
                // Create clause that selects all higher precedence clauses in ascending order of precedence
                var precedenceSelectorSubClauses = new Clause[numPrecedenceSelectorClauses];
                for (int j = startIdx; j < numRules; j++) {
                    // Create RuleRef for the j-th precedence level 
                    precedenceSelectorSubClauses[j - startIdx] = r(
                            ruleName + "[" + precedenceOrder.get(j).precedence + "]");
                }
                var higherPrecedenceSelectorClause = first(precedenceSelectorSubClauses);

                // Name the precedence selector using the lowest and highest precedence matched
                higherPrecedenceSelectorRuleName = ruleName + "[" + precedenceOrder.get(startIdx).precedence + "-"
                        + precedenceOrder.get(numRules - 1).precedence + "]";

                // Override toString to show only the precedence range rather than the full First clause
                // (for compactness)
                higherPrecedenceSelectorClause.toStringCached = "(" + ruleName + "["
                        + precedenceOrder.get(startIdx).precedence + "] / "
                        + (numPrecedenceSelectorClauses > 2 ? "... / " : "") + ruleName + "["
                        + precedenceOrder.get(numRules - 1).precedence + "])";

                // Create a new rule for the higher precedence
                allRulesOut.add(rule(higherPrecedenceSelectorRuleName, higherPrecedenceSelectorClause));

                // The last precedence selector clause selects all precedence levels.
                // Create a new rule without the precedence level suffix that uses this clause, so that if
                // other rules refer to this one, all precedence levels will be selected in precedence order.
                if (precedenceIdx == numRules - 1) {
                    allRulesOut.add(rule(ruleName, higherPrecedenceSelectorClause));
                }

            } else {
                // If there's only one higher precedence, don't wrap the higher precedence clause in an
                // unnecessary First node -- just directly use a RuleRef
                higherPrecedenceSelectorRuleName = ruleName + "[" + precedenceOrder.get(numRules - 1).precedence
                        + "]";
            }

            // Replace all self-references in the rule with a reference to the higher precedence clause(s).
            // N.B. this assumes that subclauses are not referentially shared between different rules of the
            // same name with different precedence
            rewriteSelfRefs(precedenceOrder.get(precedenceIdx).clause, ruleName, higherPrecedenceSelectorRuleName,
                    new HashSet<>());
        }
    }

    public Rule getRule(String ruleNameWithPrecedence) {
        var rule = ruleNameWithPrecedenceToRule.get(ruleNameWithPrecedence);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown rule name: " + ruleNameWithPrecedence);
        }
        return rule;
    }

    /**
     * Get the {@link Match} entries for all nonoverlapping matches of the named rule, obtained by greedily matching
     * from the beginning of the string, then looking for the next match after the end of the current match.
     */
    public List<Match> getNonOverlappingMatches(MemoTable memoTable, String ruleName, int precedence) {
        var clause = getRule(ruleName).clause;
        return memoTable.getNonOverlappingMatches(clause);
    }

    /**
     * Get the {@link Match} entries for all nonoverlapping matches of the named rule, obtained by greedily matching
     * from the beginning of the string, then looking for the next match after the end of the current match.
     */
    public List<Match> getNonOverlappingMatches(MemoTable memoTable, String ruleName) {
        return getNonOverlappingMatches(memoTable, ruleName, 0);
    }

    /**
     * Get the {@link Match} entries for all postions where a match was queried for the named rule, but there was no
     * match.
     */
    public List<Integer> getNonMatches(MemoTable memoTable, String ruleName, int precedence) {
        var clause = getRule(ruleName).clause;
        return memoTable.getNonMatchPositions(clause);
    }

    /**
     * Get the {@link Match} entries for all postions where a match was queried for the named rule, but there was no
     * match.
     */
    public List<Integer> getNonMatches(MemoTable memoTable, String ruleName) {
        return getNonMatches(memoTable, ruleName, 0);
    }
}
