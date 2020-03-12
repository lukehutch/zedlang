package pikaparser.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pikaparser.clause.Clause;
import pikaparser.clause.Nothing;
import pikaparser.clause.OneOrMore;
import pikaparser.clause.RuleName;
import pikaparser.clause.Seq;
import pikaparser.clause.Terminal;

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

    static void getReachableClauses(Clause clause, Set<Clause> reachable) {
        if (reachable.add(clause)) {
            for (var subClause : clause.subClauses) {
                getReachableClauses(subClause, reachable);
            }
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

        // Find reachable clauses 
        var reachableClausesUnique = new LinkedHashSet<Clause>();
        getReachableClauses(topLevelClause, reachableClausesUnique);
        allClauses = new ArrayList<Clause>(reachableClausesUnique);

        // Find seed parent clauses
        for (var clause : allClauses) {
            clause.backlinkToSeedParentClauses();
        }

        // Find all clauses that can match Nothing, i.e. Nothing itself, or FirstMatch(X | Nothing), etc.,
        // and set their canMatchNothing field to true
        var nothing = (Clause) null;
        for (var clause : allClauses) {
            if (clause instanceof Nothing) {
                nothing = clause;
                // Should only be one instance, since clauses were interned
                break;
            }
        }
        if (nothing != null) {
            var allTopDownClauses = new HashSet<Clause>();
            var topDownClauseQueue = new ArrayDeque<Clause>();
            topDownClauseQueue.add(nothing);
            do {
                // All parents of clauses that can match Nothing, except for Seq, can also match Nothing
                while (!topDownClauseQueue.isEmpty()) {
                    var topDownClause = topDownClauseQueue.remove();
                    if (allTopDownClauses.add(topDownClause)) {
                        topDownClause.matchTopDown = true;
                        for (var parentClause : topDownClause.seedParentClauses) {
                            if (!(parentClause instanceof Seq)) {
                                topDownClauseQueue.add(parentClause);
                            }
                        }
                    }
                }
                // Seq clauses can match Nothing if all their sub-clauses can match Nothing
                for (var clause : allClauses) {
                    if (clause instanceof Seq && !allTopDownClauses.contains(clause)) {
                        var seqCanMatchNothing = true;
                        for (Clause subClause : clause.subClauses) {
                            if (!subClause.matchTopDown) {
                                seqCanMatchNothing = false;
                            }
                        }
                        if (seqCanMatchNothing) {
                            topDownClauseQueue.add(clause);
                        }
                    }
                }
            } while (!topDownClauseQueue.isEmpty());
        }
        for (var clause : allClauses) {
            if (clause instanceof OneOrMore) {
                // Match all OneOrMore clauses top-down, to avoid creating memo entries at each match in a run
                clause.matchTopDown = true;
            } else if (clause instanceof Terminal) {
                // Match all terminals top-down, since they are not memoized
                clause.matchTopDown = true;
            }
        }

        // Find ancestral seed clauses by skipping over top-down clauses
        for (var clause : allClauses) {
            clause.findSeedAncestorClauses(clause, new HashSet<Clause>());
        }
    }
}
