package pikaparser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import pikaparser.clause.Clause;
import pikaparser.clause.RuleName;
import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class Parser {

    public final String input;

    public final Clause topLevelClause;

    public final Map<String, Clause> ruleNameToClause = new HashMap<>();

    public final Map<String, Clause> clauseStrToClauseInterned = new HashMap<>();

    public final List<Clause> allClauses;

    private Clause internClause(Clause clause, Set<Clause> visited) {
        // Prevent infinite loop, in case grammar is self-referential
        if (visited.add(clause)) {
            String clauseStr = clause.toString();
            var alreadyInternedClause = clauseStrToClauseInterned.get(clauseStr);
            if (alreadyInternedClause != null) {
                // Rule is already interned, just add any missing rule names and return interned version
                alreadyInternedClause.ruleNames.addAll(clause.ruleNames);
                return alreadyInternedClause;
            }

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

    private static void topologicalSort(Clause clause, Set<Clause> visited, List<Clause> orderReversedOut) {
        if (visited.add(clause)) {
            for (Clause subClause : clause.subClauses) {
                topologicalSort(subClause, visited, orderReversedOut);
            }
            orderReversedOut.add(clause);
        }
    }

    private static List<Clause> topologicalSort(Clause topLevelClause) {
        var topoOrderReversed = new ArrayList<Clause>();
        topologicalSort(topLevelClause, new HashSet<Clause>(), topoOrderReversed);
        // Modify order to move all terminals to end of topo order
        var topoOrderOut = new ArrayList<Clause>(topoOrderReversed.size());
        var terminals = new ArrayList<Clause>();
        for (int i = topoOrderReversed.size() - 1; i >= 0; --i) {
            Clause clause = topoOrderReversed.get(i);
            if (clause.isTerminal()) {
                terminals.add(clause);
            } else {
                topoOrderOut.add(clause);
            }
        }
        Collections.sort(terminals, (t1, t2) -> t1.toString().compareTo(t2.toString()));
        topoOrderOut.addAll(terminals);
        return topoOrderOut;
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

        // Get a list of all unique clauses
        allClauses = topologicalSort(topLevelClause);

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
                    Clause.matchMemoized(input, new MemoRef(clause, startPos), activeSet);
                }
            }
        }

        // Main parsing loop
        while (!activeSet.isEmpty()) {
            Set<MemoRef> nextActiveSet = Collections.newSetFromMap(new ConcurrentHashMap<MemoRef, Boolean>());
            activeSet.parallelStream().forEach(activeSetMemoRef -> {
                Clause.matchMemoized(input, activeSetMemoRef, nextActiveSet);
            });
            //            for (MemoRef activeSetMemoRef : activeSet) {
            //                Clause.matchMemoized(input, activeSetMemoRef, nextActiveSet);
            //            }
            activeSet = nextActiveSet;
        }
    }

    private void printMemoTable(BitSet consumedChars) {
        StringBuilder[] buf = new StringBuilder[allClauses.size()];
        int marginWidth = 0;
        for (int i = 0; i < allClauses.size(); i++) {
            buf[i] = new StringBuilder();
            buf[i].append(String.format("%3d", i) + " : ");
            Clause clause = allClauses.get(i);
            if (clause.isTerminal()) {
                buf[i].append("<terminal> ");
            }
            if (!clause.ruleNames.isEmpty()) {
                buf[i].append(String.join(", ", clause.ruleNames) + " = ");
            }
            buf[i].append(clause);
            marginWidth = Math.max(marginWidth, buf[i].length() + 2);
        }
        int tableWidth = marginWidth + input.length() + 1;
        for (int i = 0; i < allClauses.size(); i++) {
            while (buf[i].length() < marginWidth) {
                buf[i].append(' ');
            }
            while (buf[i].length() < tableWidth) {
                buf[i].append('-');
            }
        }
        for (int i = 0; i < allClauses.size(); i++) {
            Clause clause = allClauses.get(i);
            ConcurrentSkipListMap<Integer, Memo> startIdxToMemo = clause.startPosToMemo;
            for (Memo memo : startIdxToMemo.values()) {
                buf[i].setCharAt(marginWidth + memo.memoRef.startPos, memo.matched() ? '#' : '.');
                for (int j0 = memo.memoRef.startPos + 1, j1 = j0 + memo.len, j = j0; j < j1; j++) {
                    buf[i].setCharAt(marginWidth + j, '=');
                }
            }
            System.out.println(buf[i]);
        }

        for (int j = 0; j < marginWidth; j++) {
            System.out.print(' ');
        }
        for (int i = 0; i < input.length(); i++) {
            System.out.print(i % 10);
        }
        System.out.println();
        for (int j = 0; j < marginWidth; j++) {
            System.out.print(' ');
        }
        System.out.println(input.replace('\n', '^'));
        for (int j = 0; j < marginWidth; j++) {
            System.out.print(' ');
        }
        for (int i = 0; i < input.length(); i++) {
            System.out.print(consumedChars.get(i) ? " " : "~");
        }
        System.out.println();

        //        // Highlight any syntax errors
        //        if (syntaxErrors != null && syntaxErrors.size() > 0) {
        //            StringBuilder buf = new StringBuilder();
        //            for (int i = 0; i < input.length(); i++) {
        //                buf.append(' ');
        //            }
        //            for (Entry<Integer, Integer> ent : syntaxErrors.entrySet()) {
        //                int startIdx = ent.getKey(), endIdx = ent.getValue();
        //                for (int i = startIdx; i < endIdx; i++) {
        //                    buf.setCharAt(i, '^');
        //                }
        //            }
        //            System.out.println(indent + buf);
        //        }
    }

    private void printParseTree(Memo memo, String indentStr, boolean isLastChild, BitSet consumedChars) {
        for (int i = memo.memoRef.startPos, ii = memo.memoRef.startPos + memo.len; i < ii; i++) {
            consumedChars.set(i);
        }
        var ruleNames = memo.memoRef.clause.ruleNames;
        System.out.println(indentStr + "|   ");
        System.out
                .println(indentStr + "+-- " + (ruleNames.isEmpty() ? "" : String.join(", ", ruleNames) + " = ") + memo);
        List<Memo> matchingSubClauseMemos = memo.matchingSubClauseMemos;
        if (matchingSubClauseMemos != null) {
            for (int i = 0; i < matchingSubClauseMemos.size(); i++) {
                Memo subClause = matchingSubClauseMemos.get(i);
                printParseTree(subClause, indentStr + (isLastChild ? "    " : "|   "),
                        i == matchingSubClauseMemos.size() - 1, consumedChars);
            }
        }
    }

    public void printParseResult() {
        BitSet consumedChars = new BitSet(input.length() + 1);
        var topLevelMatches = topLevelClause.getNonoverlappingMatches();
        if (topLevelMatches.isEmpty()) {
            System.out.println("Toplevel rule did not match");
        } else {
            for (int i = 0; i < topLevelMatches.size(); i++) {
                var topLevelMatch = topLevelMatches.get(i);
                printParseTree(topLevelMatch, "", i == topLevelMatches.size() - 1, consumedChars);
            }
        }
        System.out.println();
        printMemoTable(consumedChars);
    }
}
