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
    public final List<Rule> rules;
    public final List<Clause> allClauses;
    public Rule lexRule;
    public final Map<String, Rule> ruleNameToRule = new HashMap<>();

    public Grammar(List<Rule> rules) {
        this(/* lexRuleName = */ null, rules);
    }

    public Grammar(String lexRuleName, List<Rule> rules) {
        if (rules.size() == 0) {
            throw new IllegalArgumentException("Grammar must consist of at least one rule");
        }
        this.rules = rules;

        // Create mapping from rule name to rule, so RuleRef nodes can be resolved (this needs to be done after
        // removing CreateASTNode nodes, otherwise the rule name could point to a toplevel CreateASTNode node,
        // and after interning matching subclauses, so that the rule name matches to the interned clause).
        for (var rule : rules) {
            if (rule.ruleName == null) {
                throw new IllegalArgumentException("All rules must be named");
            }
            if (ruleNameToRule.put(rule.ruleName, rule) != null) {
                throw new IllegalArgumentException("Duplicate rule name: " + rule.ruleName);
            }
        }

        // Run toString() method on all clauses, bottom-up, so that toString() values are cached, and so that
        // after replacing all RuleRef objects with direct Clause references, calling toString() on a cyclic
        // clause structure does not get stuck in an infinite loop. This also interns clauses, coalescing shared
        // sub-clauses are into a DAG, so that effort is not wasted parsing different instances of the same
        // clause multiple times, and so that when a subclause matches, all parent clauses will be added to
        // the active set in the next iteration.
        // This should be done after liftASTNodeLabels and before resolveRuleRefs.
        Map<String, Clause> toStringToClause = new HashMap<>();
        Set<Clause> internVisited = new HashSet<>();
        for (int i = 0; i < rules.size(); i++) {
            var rule = rules.get(i);
            rule.intern(toStringToClause, internVisited);
        }

        // Resolve each RuleRef into a direct reference to the referenced clause
        Set<Clause> rulesVisited = new HashSet<>();
        for (Rule rule : rules) {
            rule.resolveRuleRefs(ruleNameToRule, rulesVisited);
        }

        if (lexRuleName != null) {
            // Find the toplevel lex rule, if lexRuleName is specified
            lexRule = ruleNameToRule.get(lexRuleName);
            if (lexRule == null) {
                throw new IllegalArgumentException("Unknown lex rule name: " + lexRuleName);
            }
            // Check the lex rule does not contain any cycles
            lexRule.checkNoCycles(ruleNameToRule);
        }

        // Find clauses reachable from the toplevel clause, in reverse topological order.
        // Clauses can form a DAG structure, via RuleRef.
        allClauses = new ArrayList<Clause>();
        HashSet<Clause> allClausesVisited = new HashSet<Clause>();
        for (Rule rule : rules) {
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

    /**
     * Get the {@link Match} entries for all nonoverlapping matches of the named rule, obtained by greedily matching
     * from the beginning of the string, then looking for the next match after the end of the current match.
     */
    public List<Match> getNonOverlappingMatches(MemoTable memoTable, String ruleName) {
        Rule rule = ruleNameToRule.get(ruleName);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown rule name: " + ruleName);
        }
        return memoTable.getNonOverlappingMatches(rule.clause);
    }

    /**
     * Get the {@link Match} entries for all postions where a match was queried for the named rule, but there was no
     * match.
     */
    public List<Integer> getNonMatches(MemoTable memoTable, String ruleName) {
        Rule rule = ruleNameToRule.get(ruleName);
        if (rule == null) {
            throw new IllegalArgumentException("Unknown rule name: " + ruleName);
        }
        return memoTable.getNonMatchPositions(rule.clause);
    }
}
