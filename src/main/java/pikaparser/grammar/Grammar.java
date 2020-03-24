package pikaparser.grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pikaparser.clause.Clause;
import pikaparser.memotable.Match;
import pikaparser.memotable.MemoTable;

public class Grammar {
    public final List<Clause> allClauses;
    public Clause lexClause;
    public final Map<String, RuleGroup> ruleNameToRuleGroup = new HashMap<>();

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
                rulesWithName = new ArrayList<>();
                ruleNameToRules.put(rule.ruleName, rulesWithName);
            }
            rulesWithName.add(rule);
        }
        for (var ent : ruleNameToRules.entrySet()) {
            var ruleName = ent.getKey();
            var rulesWithName = ent.getValue();
            ruleNameToRuleGroup.put(ruleName, new RuleGroup(ruleName, rulesWithName));
        }

        // Run toString() method on all clauses, bottom-up, so that toString() values are cached, and so that
        // after replacing all RuleRef objects with direct Clause references, calling toString() on a cyclic
        // clause structure does not get stuck in an infinite loop. This also interns clauses, coalescing shared
        // sub-clauses are into a DAG, so that effort is not wasted parsing different instances of the same
        // clause multiple times, and so that when a subclause matches, all parent clauses will be added to
        // the active set in the next iteration.
        Map<String, Clause> toStringToClause = new HashMap<>();
        Set<Clause> internVisited = new HashSet<>();
        for (var ruleGroup : ruleNameToRuleGroup.values()) {
            ruleGroup.intern(toStringToClause, internVisited);
        }

        // Resolve each RuleRef into a direct reference to the referenced clause
        Set<Clause> ruleClausesVisited = new HashSet<>();
        for (var ruleGroup : ruleNameToRuleGroup.values()) {
            ruleGroup.resolveRuleRefs(ruleNameToRuleGroup, ruleClausesVisited);
        }

        if (lexRuleName != null) {
            // Find the toplevel lex rule, if lexRuleName is specified
            var lexRuleGroup = ruleNameToRuleGroup.get(lexRuleName);
            if (lexRuleGroup == null) {
                throw new IllegalArgumentException("Unknown lex rule name: " + lexRuleName);
            }
            // Check the lex rule does not contain any cycles
            lexRuleGroup.checkNoCycles(ruleNameToRuleGroup);
            lexClause = lexRuleGroup.getBaseClause();
        }

        // Find clauses reachable from the toplevel clause, in reverse topological order.
        // Clauses can form a DAG structure, via RuleRef.
        allClauses = new ArrayList<Clause>();
        HashSet<Clause> allClausesVisited = new HashSet<Clause>();
        for (var ruleGroup : ruleNameToRuleGroup.values()) {
            ruleGroup.findReachableClauses(allClausesVisited, allClauses);
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

    private Clause getBaseClause(String ruleName) {
        RuleGroup ruleGroup = ruleNameToRuleGroup.get(ruleName);
        if (ruleGroup == null) {
            throw new IllegalArgumentException("Unknown rule name: " + ruleName);
        }
        var clause = ruleGroup.getBaseClause();
        return clause;
    }

    /**
     * Get the {@link Match} entries for all nonoverlapping matches of the named rule, obtained by greedily matching
     * from the beginning of the string, then looking for the next match after the end of the current match.
     */
    public List<Match> getNonOverlappingMatches(MemoTable memoTable, String ruleName, int precedence) {
        var clause = getBaseClause(ruleName);
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
        var clause = getBaseClause(ruleName);
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
