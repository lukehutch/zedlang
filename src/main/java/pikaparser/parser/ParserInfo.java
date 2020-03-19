package pikaparser.parser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pikaparser.clause.Clause;
import pikaparser.clause.Clause.MatchDirection;
import pikaparser.clause.Nothing;
import pikaparser.clause.Terminal;
import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;

public class ParserInfo {

    private static void getConsumedChars(Match match, BitSet consumedChars) {
        for (int i = match.memoKey.startPos, ii = match.memoKey.startPos + match.len; i < ii; i++) {
            consumedChars.set(i);
        }
        Match[] subClauseMatches = match.subClauseMatches;
        if (subClauseMatches != null) {
            for (int i = 0; i < subClauseMatches.length; i++) {
                Match subClauseMatch = subClauseMatches[i];
                getConsumedChars(subClauseMatch, consumedChars);
            }
        }
    }

    private static void printMemoTable(Parser parser, List<Clause> clauseOrder, BitSet consumedChars) {
        String input = parser.input;
        StringBuilder[] buf = new StringBuilder[clauseOrder.size()];
        int marginWidth = 0;
        for (int i = 0; i < clauseOrder.size(); i++) {
            buf[i] = new StringBuilder();
            buf[i].append(String.format("%3d", i) + " : ");
            Clause clause = clauseOrder.get(i);
            if (clause instanceof Terminal) {
                buf[i].append("<terminal> ");
            }
            if (clause.canMatchZeroChars) {
                buf[i].append("<alwaysMatches> ");
            }
            buf[i].append(clause.toString());
            marginWidth = Math.max(marginWidth, buf[i].length() + 2);
        }
        int tableWidth = marginWidth + input.length() + 1;
        for (int i = 0; i < clauseOrder.size(); i++) {
            while (buf[i].length() < marginWidth) {
                buf[i].append(' ');
            }
            while (buf[i].length() < tableWidth) {
                buf[i].append('-');
            }
        }
        for (int i = 0; i < clauseOrder.size(); i++) {
            Clause clause = clauseOrder.get(i);
            if (clause instanceof Terminal) {
                // Terminals are not memoized -- have to render them directly
                Set<MemoEntry> set = new HashSet<>();
                for (int j = 0; j <= input.length(); j++) {
                    Match match = clause.match(MatchDirection.TOP_DOWN, parser.memoTable, new MemoKey(clause, j),
                            input, set);
                    if (match != null) {
                        buf[i].setCharAt(marginWidth + j, '#');
                        if (match.len > 1) {
                            for (int k = 1; k < match.len; k++) {
                                buf[i].setCharAt(marginWidth + j + k, '=');
                            }
                            j += match.len - 1;
                        }
                    }
                }
            } else {
                // Render non-matches
                for (var startPos : parser.memoTable.getNonMatchPositions(clause)) {
                    if (startPos <= input.length()) {
                        buf[i].setCharAt(marginWidth + startPos, 'x');
                    }
                }
                // Render matches
                for (var match : parser.memoTable.getNonOverlappingMatches(clause)) {
                    if (match.memoKey.startPos <= input.length()) {
                        buf[i].setCharAt(marginWidth + match.memoKey.startPos, '#');
                        for (int j = match.memoKey.startPos + 1; j < match.memoKey.startPos + match.len; j++) {
                            if (j <= input.length()) {
                                buf[i].setCharAt(marginWidth + j, '=');
                            }
                        }
                    }
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

        for (int i = 0; i < marginWidth; i++) {
            System.out.print(' ');
        }
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            System.out.print(c < 32 || c > 126 ? '■' : c);
        }
        System.out.println();

        for (int i = 0; i < marginWidth; i++) {
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

    public static void printParseResult(Parser parser, String topLevelRuleName) {
        Clause topLevelRule = parser.grammar.ruleNameToRule.get(topLevelRuleName);
        if (topLevelRule == null) {
            throw new IllegalArgumentException("No clause named \"" + topLevelRuleName + "\"");
        }

        // Print parse tree, and find which characters were consumed and which weren't
        BitSet consumedChars = new BitSet(parser.input.length() + 1);

        var topLevelMatches = parser.memoTable.getNonOverlappingMatches(topLevelRule);
        if (!topLevelMatches.isEmpty()) {
            for (int i = 0; i < topLevelMatches.size(); i++) {
                var topLevelMatch = topLevelMatches.get(i);
                getConsumedChars(topLevelMatch, consumedChars);
            }
        }

        // Find reachable clauses, by reversing topological order of clauses, and putting terminals last 
        var clauseOrder = new ArrayList<Clause>();
        List<Clause> allClauses = parser.grammar.allClauses;
        for (int i = 0; i < allClauses.size(); i++) {
            Clause clause = allClauses.get(allClauses.size() - 1 - i);
            if (!(clause instanceof Terminal)) {
                // First show nonterminals
                clauseOrder.add(clause);
            }
        }
        for (int i = 0; i < allClauses.size(); i++) {
            Clause clause = allClauses.get(i);
            if (clause instanceof Terminal && !(clause instanceof Nothing)) {
                // Then show terminals
                clauseOrder.add(clause);
            }
        }

        // Print memo table
        System.out.println();
        printMemoTable(parser, clauseOrder, consumedChars);

        // Print all matches for each clause
        for (Clause clause : parser.grammar.allClauses) {
            var matches = parser.memoTable.getNonOverlappingMatches(clause);
            if (!matches.isEmpty()) {
                System.out.println("\n====================================\n\nMatches for " + clause + " :");
                for (int i = 0; i < matches.size(); i++) {
                    System.out.println("\n#");
                    matches.get(i).printTree(parser.input, "", i == matches.size() - 1);
                }
            }
        }

        System.out.println(
                "\n====================================\n\nFinal AST for rule \"" + topLevelRuleName + "\":");
        if (!topLevelMatches.isEmpty()) {
            for (int i = 0; i < topLevelMatches.size(); i++) {
                var topLevelMatch = topLevelMatches.get(i);
                var ast = topLevelMatch.toAST(parser.input);
                if (ast != null) {
                    ast.printTree(parser.input);
                }
            }
        } else {
            System.out.println("\nRule \"" + topLevelRuleName + "\" did not match anything");
        }

        System.out.println("\nNum match objects created: " + parser.memoTable.numMatchObjectsCreated);
        System.out.println("Num match objects memoized:  " + parser.memoTable.numMatchObjectsMemoized);
    }
}
