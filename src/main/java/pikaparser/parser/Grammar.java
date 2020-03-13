package pikaparser.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pikaparser.clause.Clause;
import pikaparser.clause.RuleName;

public class Grammar {

    public final Clause topLevelClause;

    private final Map<String, Clause> ruleNameToClause = new HashMap<>();

    private final Map<String, Clause> clauseStrToClauseInterned = new HashMap<>();

    public final List<Clause> allClauses;

    private Clause internClause(Clause clause, Set<Clause> visited) {
        var clauseStr = clause.toString();
        var alreadyInternedClause = clauseStrToClauseInterned.get(clauseStr);
        if (alreadyInternedClause != null) {
            // Rule is already interned, just add any missing rule names and return interned version
            alreadyInternedClause.ruleNames.addAll(clause.ruleNames);
            return alreadyInternedClause;
        }

        // Prevent infinite loop, in case grammar is self-referential
        if (visited.add(clause)) {
            // Clause has not yet been interned
            for (int i = 0; i < clause.subClauses.length; i++) {
                // For RuleNameRef, subClause starts out null
                if (clause.subClauses[i] != null) {
                    // Overwrite clauses in-place with interned versions
                    clause.subClauses[i] = internClause(clause.subClauses[i], visited);
                }
            }
            clauseStrToClauseInterned.put(clauseStr, clause);
        }
        return clause;
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

    public Grammar(List<Clause> grammar) {
        if (grammar.size() == 0) {
            throw new IllegalArgumentException("Grammar must consist of at least one rule");
        }

        // Intern clauses, and map rule name to clause
        Set<Clause> internClauseVisited = new HashSet<>();
        for (var clause : grammar) {
            if (clause.ruleNames.size() == 0) {
                throw new IllegalArgumentException("All toplevel clauses must have a single name");
            }
            var ruleName = clause.ruleNames.iterator().next();

            var ruleInterned = internClause(clause, internClauseVisited);
            if (ruleNameToClause.put(ruleName, ruleInterned) != null) {
                throw new IllegalArgumentException("Duplicate rule name: " + ruleName);
            }
        }

        // Look up named clause for each RuleName
        var nameRedirect = new HashMap<Clause, Clause>();
        for (var ent : clauseStrToClauseInterned.entrySet()) {
            var clause = ent.getValue();
            if (clause instanceof RuleName) {
                // Name references can form a chain, so need to follow the chain all the way to the end
                var visited = new HashSet<Clause>();
                var currClause = clause;
                do {
                    if (visited.add(currClause)) {
                        String refdRuleName = ((RuleName) currClause).refdRuleName;
                        var refdClause = ruleNameToClause.get(refdRuleName);
                        if (refdClause == null) {
                            throw new IllegalArgumentException("Unknown rule name: " + refdRuleName);
                        }
                        currClause = refdClause;
                    } else {
                        throw new IllegalArgumentException("Reached infinite loop in name references: " + visited);
                    }
                } while (currClause instanceof RuleName);

                // Redirect all named clauses to the final non-named clause
                for (var visitedClause : visited) {
                    nameRedirect.put(visitedClause, currClause);
                    // The final clause should inherit the names of all clauses on the chain
                    currClause.ruleNames.addAll(visitedClause.ruleNames);
                }
            }
        }

        // Replace name references with direct clause references
        for (var ent : new ArrayList<>(clauseStrToClauseInterned.entrySet())) {
            var clause = ent.getValue();
            if (nameRedirect.containsKey(clause)) {
                clauseStrToClauseInterned.put(ent.getKey(), nameRedirect.get(clause));
            }
            for (int i = 0; i < clause.subClauses.length; i++) {
                if (nameRedirect.containsKey(clause.subClauses[i])) {
                    clause.subClauses[i] = nameRedirect.get(clause.subClauses[i]);
                }
            }
        }
        for (var ent : new ArrayList<>(ruleNameToClause.entrySet())) {
            var clause = ent.getValue();
            if (nameRedirect.containsKey(clause)) {
                ruleNameToClause.put(ent.getKey(), nameRedirect.get(clause));
            }
        }
        var topLevelClause = grammar.get(0);
        if (nameRedirect.containsKey(topLevelClause)) {
            topLevelClause = nameRedirect.get(topLevelClause);
        }
        this.topLevelClause = topLevelClause;

        // Find clauses reachable from the toplevel clause, and set alwaysMatches field on each clause, bottom-up 
        allClauses = new ArrayList<Clause>();
        findReachableClauses(topLevelClause, new HashSet<Clause>(), allClauses);

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
}
