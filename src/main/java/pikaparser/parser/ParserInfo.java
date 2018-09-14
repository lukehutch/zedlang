package pikaparser.parser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import pikaparser.clause.Clause;
import pikaparser.memotable.Match;

public class ParserInfo {

    private static void printMemoTable(List<Clause> clauseOrder, String input, BitSet consumedChars) {
        StringBuilder[] buf = new StringBuilder[clauseOrder.size()];
        int marginWidth = 0;
        for (int i = 0; i < clauseOrder.size(); i++) {
            buf[i] = new StringBuilder();
            buf[i].append(String.format("%3d", i) + " : ");
            Clause clause = clauseOrder.get(i);
            if (i == 0) {
                buf[i].append("<toplevel> ");
            }
            if (clause.isTerminal()) {
                buf[i].append("<terminal> ");
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
            if (clause.isTerminal()) {
                // Terminals are not memoized -- have to render them directly
                for (int j = 0; j <= input.length(); j++) {
                    buf[i].setCharAt(marginWidth + j, clause.extendParsingContext(input, /* unused */ null,
                            /* unused */ null, j, /* unused */ null) != null ? '#' : '.');
                }
            } else {
                // Render non-matches
                for (var ent : clause.startPosToMemoEntry.entrySet()) {
                    var memoEntry = ent.getValue();
                    if (memoEntry.bestMatch == null && memoEntry.startPos <= input.length()) {
                        buf[i].setCharAt(marginWidth + memoEntry.startPos, '.');
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

    private static void printParseTree(Match match, String indentStr, boolean isLastChild, String input,
            BitSet consumedChars) {
        for (int i = match.startPos, ii = match.startPos + match.len; i < ii; i++) {
            consumedChars.set(i);
        }
        int inpLen = 40;
        String inp = input.substring(match.startPos,
                Math.min(input.length(), match.startPos + Math.min(match.len, inpLen)));
        if (inp.length() == inpLen) {
            inp += "...";
        }
        System.out.println(indentStr + "|   ");
        System.out.println(indentStr + "+-- " + match + " : \"" + inp + "\"");
        List<Match> subClauseMatches = match.subClauseMatches;
        if (subClauseMatches != null) {
            for (int i = 0; i < subClauseMatches.size(); i++) {
                Match subClauseMatch = subClauseMatches.get(i);
                printParseTree(subClauseMatch, indentStr + (isLastChild ? "    " : "|   "),
                        i == subClauseMatches.size() - 1, input, consumedChars);
            }
        }
    }

    private static ArrayList<Clause> getClauseOrder(Clause topLevelClause) {
        // Find reachable clauses, then sort in order of toplevel clause, then internal clauses, then terminals 
        var reachableClausesUnique = new HashSet<Clause>();
        Parser.getReachableClauses(topLevelClause, reachableClausesUnique);
        var clauseOrder = new ArrayList<Clause>();
        clauseOrder.add(topLevelClause);
        var sortedClauses = new ArrayList<Clause>();
        for (Clause clause : reachableClausesUnique) {
            if (clause != topLevelClause && !clause.isTerminal()) {
                sortedClauses.add(clause);
            }
        }
        Comparator<? super Clause> comparator = (t1, t2) -> {
            int diff = t1.toStringWithRuleNames().compareTo(t2.toStringWithRuleNames());
            if (diff != 0) {
                return diff;
            } else {
                return t1.toString().compareTo(t2.toString());
            }
        };
        Collections.sort(sortedClauses, comparator);
        clauseOrder.addAll(sortedClauses);
        sortedClauses.clear();
        for (Clause clause : reachableClausesUnique) {
            if (clause != topLevelClause && clause.isTerminal()) {
                sortedClauses.add(clause);
            }
        }
        Collections.sort(sortedClauses, comparator);
        clauseOrder.addAll(sortedClauses);
        sortedClauses.clear();
        return clauseOrder;
    }

    public static void printParseResult(Parser parser) {
        // Print parse tree, and find which characters were consumed and which weren't
        BitSet consumedChars = new BitSet(parser.input.length() + 1);
        var topLevelMatches = parser.topLevelClause.getNonOverlappingMatches();
        if (topLevelMatches.isEmpty()) {
            System.out.println("Toplevel rule did not match");
        } else {
            for (int i = 0; i < topLevelMatches.size(); i++) {
                var topLevelMatch = topLevelMatches.get(i);
                printParseTree(topLevelMatch, "", i == topLevelMatches.size() - 1, parser.input, consumedChars);
            }
        }

        // Print memo table
        System.out.println();
        printMemoTable(getClauseOrder(parser.topLevelClause), parser.input, consumedChars);
    }
}
