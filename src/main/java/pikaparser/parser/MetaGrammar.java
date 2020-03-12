package pikaparser.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pikaparser.clause.CharSeq;
import pikaparser.clause.CharSet;
import pikaparser.clause.Clause;
import pikaparser.clause.FirstMatch;
import pikaparser.clause.FollowedBy;
import pikaparser.clause.NotFollowedBy;
import pikaparser.clause.Nothing;
import pikaparser.clause.OneOrMore;
import pikaparser.clause.RuleName;
import pikaparser.clause.Seq;
import pikaparser.clause.Start;
import pikaparser.memotable.ASTNode;

public class MetaGrammar {
    public static Clause rule(String name, Clause clause) {
        clause.addRuleName(name);
        return clause;
    }

    public static Clause ast(String astLabel, Clause clause) {
        clause.setLabel(astLabel);
        return clause;
    }

    public static Clause start() {
        return new Start();
    }

    public static Clause nothing() {
        return new Nothing();
    }

    public static Clause optional(Clause clause) {
        return new FirstMatch(clause, nothing());
    }

    public static Clause zeroOrMore(Clause clause) {
        return optional(new OneOrMore(clause));
    }

    public static Clause oneOrMore(Clause clause) {
        return new OneOrMore(clause);
    }

    public static Clause seq(Clause... clause) {
        return new Seq(clause);
    }

    public static Clause first(Clause... clause) {
        return new FirstMatch(clause);
    }

    public static Clause r(String ruleName) {
        return new RuleName(ruleName);
    }

    public static Clause c(char chr) {
        return new CharSet(chr);
    }

    public static Clause cRange(String charRanges) {
        boolean invert = charRanges.startsWith("^");
        List<CharSet> charSets = new ArrayList<>();
        for (int i = invert ? 1 : 0; i < charRanges.length(); i++) {
            char c = charRanges.charAt(i);
            if (i <= charRanges.length() - 3 && charRanges.charAt(i + 1) == '-') {
                char cEnd = charRanges.charAt(i + 2);
                if (cEnd < c) {
                    throw new IllegalArgumentException("Char range limits out of order: " + c + ", " + cEnd);
                }
                charSets.add(new CharSet(c, cEnd));
                i += 2;
            } else {
                charSets.add(new CharSet(c));
            }
        }
        return charSets.size() == 1 ? charSets.get(0) : new CharSet(charSets);
    }

    public static Clause text(String str) {
        return new CharSeq(str, /* ignoreCase = */ false);
    }

    //    /**
    //     * <a href="http://stackoverflow.com/questions/4731055/whitespace-matching-regex-java">
    //     * Valid unicode whitespace chars</a>
    //     */
    //    public static final CharSet WHITESPACE = new CharSet(new char[] { (char) 0x0009, // CHARACTER TABULATION
    //            (char) 0x000A, // LINE FEED (LF)
    //            (char) 0x000B, // LINE TABULATION
    //            (char) 0x000C, // FORM FEED (FF)
    //            (char) 0x000D, // CARRIAGE RETURN (CR)
    //            (char) 0x0020, // SPACE
    //            (char) 0x0085, // NEXT LINE (NEL) 
    //            (char) 0x00A0, // NO-BREAK SPACE
    //            (char) 0x1680, // OGHAM SPACE MARK
    //            (char) 0x180E, // MONGOLIAN VOWEL SEPARATOR
    //            (char) 0x2000, // EN QUAD 
    //            (char) 0x2001, // EM QUAD 
    //            (char) 0x2002, // EN SPACE
    //            (char) 0x2003, // EM SPACE
    //            (char) 0x2004, // THREE-PER-EM SPACE
    //            (char) 0x2005, // FOUR-PER-EM SPACE
    //            (char) 0x2006, // SIX-PER-EM SPACE
    //            (char) 0x2007, // FIGURE SPACE
    //            (char) 0x2008, // PUNCTUATION SPACE
    //            (char) 0x2009, // THIN SPACE
    //            (char) 0x200A, // HAIR SPACE
    //            (char) 0x2028, // LINE SEPARATOR
    //            (char) 0x2029, // PARAGRAPH SEPARATOR
    //            (char) 0x202F, // NARROW NO-BREAK SPACE
    //            (char) 0x205F, // MEDIUM MATHEMATICAL SPACE
    //            (char) 0x3000 // IDEOGRAPHIC SPACE
    //    });

    // Rule names:

    private static final String GRAMMAR = "Grammar";
    private static final String WS = "WS";
    private static final String RULE = "Rule";
    private static final String CLAUSE = "Clause";
    private static final String NAME = "Name";
    private static final String LABEL = "Label";
    private static final String NAME_CHAR = "NameChar";
    private static final String SEQ = "Seq";
    private static final String FIRST_MATCH = "FirstMatch";
    private static final String FOLLOWED_BY = "FollowedBy";
    private static final String NOT_FOLLOWED_BY = "NotFollowedBy";
    private static final String ONE_OR_MORE = "OneOrMore";
    private static final String CHAR_SET = "CharSet";
    private static final String HEX = "Hex";
    private static final String CHAR_RANGE = "CharRange";
    private static final String CHAR_RANGE_CHAR = "CharRangeChar";
    private static final String QUOTED_STRING = "QuotedString";
    private static final String SINGLE_QUOTED_CHAR = "SingleQuotedChar";
    private static final String STRING_QUOTED_CHAR = "StringQuotedChar";
    private static final String NOTHING = "Nothing";
    private static final String START = "Start";

    // Basic rules:

    public static final Clause ws = r(WS);
    public static final CharSet WHITESPACE = new CharSet(" \n\r\t");
    public static final CharSet LETTER = new CharSet(new CharSet('a', 'z'), new CharSet('A', 'Z'));
    public static final CharSet DIGIT = new CharSet('0', '9');

    public static Parser newParser(String input) {
        var grammar = new Grammar(Arrays.asList(//
                rule(GRAMMAR, //
                        seq(start(), ws, oneOrMore(r(RULE)))), //

                rule(WS, //
                        optional(oneOrMore(WHITESPACE))), //

                rule(RULE, //
                        ast(RULE, seq(r(NAME), ws, c('='), ws, r(CLAUSE), ws, c(';'), ws))), //

                rule(NAME, //
                        ast(NAME, oneOrMore(r(NAME_CHAR)))), //

                rule(NAME_CHAR, new CharSet(LETTER, DIGIT, new CharSet("_-."))),

                rule(CLAUSE, //
                        ast(CLAUSE, seq( // 
                                optional(r(LABEL)), //
                                first( //
                                        seq(c('('), ws, r(CLAUSE), c(')')), //
                                        r(SEQ), //
                                        r(ONE_OR_MORE), //
                                        r(FIRST_MATCH), //
                                        r(FOLLOWED_BY), //
                                        r(NOT_FOLLOWED_BY), //
                                        r(NAME), //
                                        r(QUOTED_STRING), //
                                        r(CHAR_SET), //
                                        r(NOTHING), //
                                        r(START))))), //

                rule(LABEL, seq(ast(LABEL, r(NAME)), ws, c(':'), ws)), //

                rule(SEQ, //
                        ast(SEQ, seq(r(CLAUSE), oneOrMore(seq(ws, r(CLAUSE)))))),

                rule(ONE_OR_MORE, //
                        ast(ONE_OR_MORE, seq(r(CLAUSE), ws, //
                                //                                first(text("++"), // TODO: OneOrMoreSuffix
                                //                                        c('+'))
                                c('+')))),

                rule(FIRST_MATCH, //
                        ast(FIRST_MATCH, seq(r(CLAUSE), oneOrMore(seq(ws, c('|'), ws, r(CLAUSE)))))),

                rule(FOLLOWED_BY, //
                        ast(FOLLOWED_BY, seq(c('&'), r(CLAUSE)))),

                rule(NOT_FOLLOWED_BY, //
                        ast(NOT_FOLLOWED_BY, seq(c('!'), r(CLAUSE)))),

                rule(CHAR_SET, //
                        first( //
                                seq(c('\''), ast(SINGLE_QUOTED_CHAR, r(SINGLE_QUOTED_CHAR)), c('\'')), //
                                seq(c('['), //
                                        ast(CHAR_RANGE, seq(optional(c('^')), //
                                                oneOrMore(first( //
                                                        r(CHAR_RANGE), //
                                                        r(CHAR_RANGE_CHAR))))),
                                        c(']')))), //
                rule(SINGLE_QUOTED_CHAR, //
                        first( //
                                text("\\t"), //
                                text("\\b"), //
                                text("\\n"), //
                                text("\\r"), //
                                text("\\f"), //
                                text("\\'"), //
                                text("\\\""), //
                                text("\\\\"), //
                                seq(text("\\u"), r(HEX), r(HEX), r(HEX), r(HEX)), //
                                new CharSet('\\', '\'').invert())), //

                rule(HEX, new CharSet(DIGIT, new CharSet('a', 'f'), new CharSet('A', 'F'))), //

                rule(CHAR_RANGE, //
                        seq(r(CHAR_RANGE_CHAR), c('-'), r(CHAR_RANGE_CHAR))), //

                rule(CHAR_RANGE_CHAR, //
                        first( //
                                new CharSet('\\', ']').invert(), //
                                r(SINGLE_QUOTED_CHAR), //
                                text("\\]"), //
                                text("\\^"))),

                rule(QUOTED_STRING, //
                        seq(c('"'), ast(QUOTED_STRING, zeroOrMore(r(STRING_QUOTED_CHAR))), c('"'))), //

                rule(STRING_QUOTED_CHAR, //
                        first( //
                                new CharSet('\\', '\"').invert(), //
                                r(SINGLE_QUOTED_CHAR))), //

                rule(NOTHING, //
                        seq(c('('), ws, c(')'))),

                rule(START, c('^')) //
        ));

        return new Parser(grammar, input);
    }

    private static int hexDigitToInt(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a';
        } else if (c >= 'A' && c <= 'F') {
            return c - 'F';
        }
        throw new IllegalArgumentException("Illegal Unicode hex char: " + c);
    }

    private static char unescapeChar(String escapedChar) {
        if (escapedChar.length() == 0) {
            throw new IllegalArgumentException("Empty char string");
        } else if (escapedChar.length() == 1) {
            return escapedChar.charAt(0);
        }
        switch (escapedChar) {
        case "\\t":
            return '\t';
        case "\\b":
            return '\b';
        case "\\n":
            return '\n';
        case "\\r":
            return '\r';
        case "\\f":
            return '\f';
        case "\\'":
            return '\'';
        case "\\\"":
            return '"';
        case "\\\\":
            return '\\';
        default:
            if (escapedChar.startsWith("\\u") && escapedChar.length() == 6) {
                int c0 = hexDigitToInt(escapedChar.charAt(2));
                int c1 = hexDigitToInt(escapedChar.charAt(3));
                int c2 = hexDigitToInt(escapedChar.charAt(4));
                int c3 = hexDigitToInt(escapedChar.charAt(5));
                return (char) ((c0 << 24) | (c1 << 16) | (c2 << 8) | c3);
            } else {
                throw new IllegalArgumentException("Invalid character: " + escapedChar);
            }
        }
    }

    private static String unescapeString(String str) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                if (i == str.length() - 1) {
                    // Should not happen
                    throw new IllegalArgumentException("Got backslash at end of quoted string");
                }
                buf.append(unescapeChar(str.substring(i, i + 2)));
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private static List<Clause> parseClauses(List<ASTNode> clauseNodes, String input) {
        return clauseNodes.stream().map(clauseNode -> parseClause(clauseNode, input)).collect(Collectors.toList());
    }

    private static Clause parseClause(ASTNode clauseNode, String input) {
        ASTNode firstChild = clauseNode.getFirstChild();
        // Get clause label if present
        boolean hasLabel = firstChild.nodeName.equals(LABEL);
        if (clauseNode.children.size() > (hasLabel ? 2 : 1)) {
            throw new IllegalArgumentException("Too many subcauses");
        }
        String clauseLabel = hasLabel ? firstChild.getText(input) : null;
        ASTNode subClause = clauseNode.getChild(hasLabel ? 1 : 0);
        Clause clause;
        switch (subClause.nodeName) {
        case CLAUSE: // Parens -- just recurse
            clause = parseClause(subClause, input);
            break;
        case SEQ:
            clause = new Seq(parseClauses(subClause.getAllDescendantsNamed(CLAUSE), input));
            break;
        case ONE_OR_MORE:
            clause = new OneOrMore(parseClause(subClause.getFirstDescendantNamed(CLAUSE), input));
            break;
        case FIRST_MATCH:
            clause = new FirstMatch(parseClauses(subClause.getAllDescendantsNamed(CLAUSE), input));
            break;
        case FOLLOWED_BY:
            clause = new FollowedBy(parseClause(subClause.getFirstDescendantNamed(CLAUSE), input));
            break;
        case NOT_FOLLOWED_BY:
            clause = new NotFollowedBy(parseClause(subClause.getFirstDescendantNamed(CLAUSE), input));
            break;
        case NAME:
            clause = r(NAME); // Rule name ref
            break;
        case QUOTED_STRING: // Doesn't include surrounding quotes
            clause = text(unescapeString(subClause.getText(input)));
            break;
        case SINGLE_QUOTED_CHAR:
            clause = c(unescapeChar(subClause.getText(input)));
            break;
        case CHAR_RANGE:
            clause = cRange(unescapeString(subClause.getText(input)));
            break;
        default:
            throw new IllegalArgumentException("Unexpected AST node: " + subClause.nodeName);
        }
        if (hasLabel) {
            clause.setLabel(clauseLabel);
        }
        return clause;
    }

    private static void assertChildNames(ASTNode node, String... names) {
        if (node.children.size() != names.length) {
            throw new IllegalArgumentException("Expected " + names.length + " children; got " + node.children.size());
        }
        for (int i = 0; i < names.length; i++) {
            if (!names[i].equals(node.children.get(i).nodeName)) {
                throw new IllegalArgumentException(
                        "Expected child " + i + " to have name " + names[i] + "; got " + node.children.get(i).nodeName);
            }
        }
    }

    private static Clause parseRule(ASTNode ruleNode, String input) {
        assertChildNames(ruleNode, NAME, CLAUSE);
        String name = ruleNode.getFirstChild().getText(input);
        ASTNode clauseNode = ruleNode.getSecondChild();
        return rule(name, parseClause(clauseNode, input));
    }

    public static Grammar parseGrammar(Parser parser) {
        var topLevelMatches = parser.grammar.topLevelClause.getNonOverlappingMatches();
        if (topLevelMatches.isEmpty()) {
            throw new IllegalArgumentException("Toplevel rule did not match");
        } else if (topLevelMatches.size() > 1) {
            throw new IllegalArgumentException("Multiple toplevel matches");
        }
        var topLevelASTNode = topLevelMatches.get(0).toAST(parser.input);
        List<Clause> ruleClauses = new ArrayList<>();
        for (ASTNode rule : topLevelASTNode.children) {
            if (!rule.nodeName.equals(RULE)) {
                throw new IllegalArgumentException("Wrong node type");
            }
            ruleClauses.add(parseRule(rule, parser.input));
        }
        return new Grammar(ruleClauses);
    }
}
