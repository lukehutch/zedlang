package pikaparser.parser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import pikaparser.clause.Clause;
import pikaparser.clause.Nothing;
import pikaparser.clause.Terminal;
import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;

public class ParserInfo {

    private static void getConsumedChars(Match match, BitSet consumedChars) {
        for (int i = match.startPos, ii = match.startPos + match.len; i < ii; i++) {
            consumedChars.set(i);
        }
        List<Match> subClauseMatches = match.subClauseMatches;
        if (subClauseMatches != null) {
            for (int i = 0; i < subClauseMatches.size(); i++) {
                Match subClauseMatch = subClauseMatches.get(i);
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
            if (clause == parser.grammar.topLevelClause) {
                buf[i].append("<toplevel> ");
            }
            if (clause instanceof Terminal) {
                buf[i].append("<terminal> ");
            }
            if (clause.alwaysMatches) {
                buf[i].append("<alwaysMatches> ");
            }
            buf[i].append(clause.toStringWithRuleNames());
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
                for (int j = 0; j <= input.length(); j++) {
                    Match match = new MemoEntry(clause, j).match(input);
                    if (match != null) {
                        buf[i].setCharAt(marginWidth + j, '#');
                        for (int k = 1; k < match.len; k++) {
                            buf[i].setCharAt(marginWidth + j + k, '=');
                        }
                        j += match.len - 1;
                    }
                }
            } else {
                // Render non-matches
                for (var startPos : clause.getNonMatches()) {
                    if (startPos <= input.length()) {
                        buf[i].setCharAt(marginWidth + startPos, 'x');
                    }
                }
                // Render matches
                for (var match : clause.getNonOverlappingMatches()) {
                    if (match.startPos <= input.length()) {
                        buf[i].setCharAt(marginWidth + match.startPos, '#');
                        for (int j = match.startPos + 1; j < match.startPos + match.len; j++) {
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
        for (int j = 0; j < marginWidth; j++) {
            System.out.print(' ');
        }
        System.out.println(input.replace('\n', '⏎'));
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

    public static void printParseResult(Parser parser) {
        // Print parse tree, and find which characters were consumed and which weren't
        BitSet consumedChars = new BitSet(parser.input.length() + 1);

        var topLevelMatches = parser.grammar.topLevelClause.getNonOverlappingMatches();
        if (topLevelMatches.isEmpty()) {
            System.out.println("\nToplevel rule did not match");
        } else {
            System.out.println("\nFinal toplevel matches:");
            for (int i = 0; i < topLevelMatches.size(); i++) {
                var topLevelMatch = topLevelMatches.get(i);
                topLevelMatch.printParseTree(parser.input, "", i == topLevelMatches.size() - 1);
            }

            System.out.println(""
                    + "\nFinal AST:");
            for (int i = 0; i < topLevelMatches.size(); i++) {
                var topLevelMatch = topLevelMatches.get(i);
                var ast = topLevelMatch.toAST(parser.input);
                if (ast != null) {
                    ast.printParseTree(parser.input);
                }
            }

            for (int i = 0; i < topLevelMatches.size(); i++) {
                var topLevelMatch = topLevelMatches.get(i);
                getConsumedChars(topLevelMatch, consumedChars);
            }
        }

        // Find reachable clauses, by reversing topological order of clauses, and putting terminals last 
        var sortedClauses = new ArrayList<Clause>();
        List<Clause> allClauses = parser.grammar.allClauses;
        for (int i = 0; i < allClauses.size(); i++) {
            Clause clause = allClauses.get(allClauses.size() - 1 - i);
            if (!(clause instanceof Terminal)) {
                // Don't include terminals in clause list
                sortedClauses.add(clause);
            }
        }
        for (int i = 0; i < allClauses.size(); i++) {
            Clause clause = allClauses.get(i);
            if (clause instanceof Terminal && !(clause instanceof Nothing)) {
                sortedClauses.add(clause);
            }
        }

        // Print memo table
        System.out.println();
        printMemoTable(parser, sortedClauses, consumedChars);
    }
}
