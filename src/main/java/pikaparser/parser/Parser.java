package pikaparser.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pikaparser.clause.Clause;
import pikaparser.clause.RuleName;
import pikaparser.memo.old.Memo;
import pikaparser.memo.old.MemoRef;

public class Parser {

    public final String input;

    public final Clause topLevelClause;

    public final Map<String, Clause> ruleNameToClause = new HashMap<>();

    public final Map<String, Clause> clauseStrToClauseInterned = new HashMap<>();

    public final List<Clause> allClauses;

    private static final boolean PARALLELIZE = false;

    private Clause internClause(Clause clause, Set<Clause> visited) {
        String clauseStr = clause.toString();
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
            for (Clause subClause : clause.subClauses) {
                getReachableClauses(subClause, reachable);
            }
        }
    }

    public Parser(List<Clause> grammar, String input) {
        this.input = input;
        if (grammar.size() == 0) {
            throw new IllegalArgumentException("Grammar must consist of at least one rule");
        }

        // Intern clauses, and map rule name to clause
        Set<Clause> internClauseVisited = new HashSet<>();
        for (Clause rule : grammar) {
            if (rule.ruleNames.size() == 0) {
                throw new IllegalArgumentException("All toplevel clauses must have a single name");
            }
            String ruleName = rule.ruleNames.iterator().next();

            Clause ruleInterned = internClause(rule, internClauseVisited);
            if (ruleNameToClause.put(ruleName, ruleInterned) != null) {
                throw new IllegalArgumentException("Duplicate rule name: " + ruleName);
            }
        }

        // Look up named clause for each RuleName
        var nameRedirect = new HashMap<Clause, Clause>();
        for (var ent : clauseStrToClauseInterned.entrySet()) {
            Clause clause = ent.getValue();
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
                for (Clause visitedClause : visited) {
                    nameRedirect.put(visitedClause, currClause);
                    // The final clause should inherit the names of all clauses on the chain
                    currClause.ruleNames.addAll(visitedClause.ruleNames);
                }
            }
        }

        // Replace name references with direct clause references
        for (var ent : new ArrayList<>(clauseStrToClauseInterned.entrySet())) {
            Clause clause = ent.getValue();
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
            Clause clause = ent.getValue();
            if (nameRedirect.containsKey(clause)) {
                ruleNameToClause.put(ent.getKey(), nameRedirect.get(clause));
            }
        }
        Clause topLevelClause = grammar.get(0);
        if (nameRedirect.containsKey(topLevelClause)) {
            topLevelClause = nameRedirect.get(topLevelClause);
        }
        this.topLevelClause = topLevelClause;

        // Find reachable clauses 
        var reachableClausesUnique = new HashSet<Clause>();
        getReachableClauses(topLevelClause, reachableClausesUnique);
        allClauses = new ArrayList<Clause>(reachableClausesUnique);

        // Initialize trigger clauses
        for (Clause clause : allClauses) {
            clause.initTriggerClauses();
        }

        // Seed the active set: find positions that all terminals match, and create the initial active set from
        // their trigger clauses. N.B. need to run to (startPos == input.length()) inclusive, so that NotFollowedBy
        // works at end of input.
        Set<MemoRef> activeSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoRef, Boolean>());
        for (int startPos = 0; startPos <= input.length(); startPos++) {
            for (Clause clause : allClauses) {
                // Only match terminal clauses
                if (clause.isTerminal()) {
                    // Try matching the terminal clause at the current position
                    // (can throw away the returned Memo, the goal is just to populate the activeSet)
                    // N.B. it is precisely because the returned Memo is thrown away here (and in the invocation
                    // below) that triggers are needed to add the correct parents to the active set.
                    clause.initTerminalParentClauses(input, startPos, activeSet);
                }
            }
        }

        // Main parsing loop
        while (!activeSet.isEmpty()) {
            ParserInfo.printParseResult(this);

            // Set<MemoRef> nextActiveSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoRef, Boolean>());
            //            if (PARALLELIZE) {
            //                activeSet.parallelStream().forEach(activeSetMemoRef -> {
            //                    Clause.matchMemoized(input, activeSetMemoRef, nextActiveSet);
            //                });
            //            } else {
            var newMemos = new ArrayList<Memo>(activeSet.size());
            for (MemoRef activeSetMemoRef : activeSet) {
                Memo match = activeSetMemoRef.clause.match(input, activeSetMemoRef, /* isFirstMatchPosition = */ true);
                newMemos.add(match);

                System.out.println("-> " + activeSetMemoRef + " => " + match);
            }
            Set<MemoRef> nextActiveSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoRef, Boolean>());
            for (var newMemo : newMemos) {
                // TODO: don't store memo if it is the result of a sub-* clause matching Nothing?

                Clause.storeMemo(input, newMemo, nextActiveSet);
            }
            //            }
            activeSet = nextActiveSet;
        }
    }
}
