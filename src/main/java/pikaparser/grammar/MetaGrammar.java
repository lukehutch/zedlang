package pikaparser.grammar;

import static pikaparser.clause.Clause.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pikaparser.clause.CharSet;
import pikaparser.clause.Clause;
import pikaparser.parser.ASTNode;
import pikaparser.parser.Parser;

public class MetaGrammar {
    // Rule names:

    private static final String GRAMMAR = "Grammar";
    private static final String LEX = "Lex";
    private static final String WS = "WS";
    private static final String RULE = "Rule";
    private static final String CLAUSE = "Clause";
    private static final String NAME = "Name";
    private static final String LABEL = "Label";
    private static final String NAME_CHAR = "NameChar";
    private static final String PARENS = "Parens";
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
    private static final String ESCAPED_CTRL_CHAR = "EscapedCtrlChar";
    private static final String SINGLE_QUOTED_CHAR = "SingleQuotedChar";
    private static final String STR_QUOTED_CHAR = "StrQuotedChar";
    private static final String NOTHING = "Nothing";
    private static final String START = "Start";

    // AST node names:

    private static final String RULE_AST = "Rule";
    private static final String NAME_AST = "Name";
    private static final String LABEL_AST = "Label";
    private static final String SEQ_AST = "Seq";
    private static final String FIRST_MATCH_AST = "FirstMatch";
    private static final String FOLLOWED_BY_AST = "FollowedBy";
    private static final String NOT_FOLLOWED_BY_AST = "NotFollowedBy";
    private static final String ONE_OR_MORE_AST = "OneOrMore";
    private static final String SINGLE_QUOTED_CHAR_AST = "SingleQuotedChar";
    private static final String CHAR_RANGE_AST = "CharRange";
    private static final String QUOTED_STRING_AST = "QuotedString";

    // Basic rules:

    public static final Clause ws = r(WS);
    public static final CharSet WHITESPACE = new CharSet(" \n\r\t");
    public static final CharSet LETTER = new CharSet(new CharSet('a', 'z'), new CharSet('A', 'Z'));
    public static final CharSet DIGIT = new CharSet('0', '9');

    // Toplevel rule for lex preprocessing (use null to disable lexing)

    public static final String LEX_RULE_NAME = LEX;

    public static Grammar grammar = new Grammar(LEX_RULE_NAME, Arrays.asList(//
            rule(GRAMMAR, //
                    seq(start(), ws, oneOrMore(r(RULE)))), //

            rule(RULE, //
                    ast(RULE_AST, seq(r(NAME), ws, c('='), ws, r(CLAUSE), ws, c(';'), ws))), //

            rule(CLAUSE, //
                    seq( // 
                            optional(r(LABEL)), //
                            first( //
                                    // This has to come first, since it's right-associative.
                                    // Otherwise, anything that depends upon Clause will greedily
                                    // consume a shorter match not including the '+' suffix.
                                    r(ONE_OR_MORE),

                                    r(PARENS), //
                                    r(SEQ), //
                                    r(FIRST_MATCH), //
                                    r(FOLLOWED_BY), //
                                    r(NOT_FOLLOWED_BY), //
                                    r(NAME), //
                                    r(QUOTED_STRING), //
                                    r(CHAR_SET), //
                                    r(NOTHING), //
                                    r(START)))), //

            rule(LABEL, seq(ast(LABEL_AST, r(NAME)), ws, c(':'), ws)), //

            rule(PARENS, seq(c('('), ws, r(CLAUSE), c(')'))), //

            rule(SEQ, //
                    ast(SEQ_AST, seq(r(CLAUSE), oneOrMore(seq(ws, r(CLAUSE)))))),

            rule(ONE_OR_MORE, //
                    ast(ONE_OR_MORE_AST, seq(r(CLAUSE), ws, //
                            //                                first(str("++"), // TODO: OneOrMoreSuffix
                            //                                        c('+'))
                            c('+')))),

            rule(FIRST_MATCH, //
                    ast(FIRST_MATCH_AST, seq(r(CLAUSE), oneOrMore(seq(ws, c('|'), ws, r(CLAUSE)))))),

            rule(FOLLOWED_BY, //
                    ast(FOLLOWED_BY_AST, seq(c('&'), r(CLAUSE)))),

            rule(NOT_FOLLOWED_BY, //
                    ast(NOT_FOLLOWED_BY_AST, seq(c('!'), r(CLAUSE)))),

            // Lex rule for preprocessing:
            rule(LEX, //
                    oneOrMore( //
                            first( //
                                    r(NAME), //
                                    r(QUOTED_STRING), //
                                    r(CHAR_SET), //
                                    new CharSet("()[]=;^"), //

                                    // WS has to come last, since it can match Nothing
                                    r(WS)) //
                    )), //

            rule(WS, //
                    optional(oneOrMore(WHITESPACE))), //

            rule(NAME, //
                    ast(NAME_AST, oneOrMore(r(NAME_CHAR)))), //

            rule(NAME_CHAR, new CharSet(LETTER, DIGIT, new CharSet("_-."))),

            rule(CHAR_SET, //
                    first( //
                            seq(c('\''), ast(SINGLE_QUOTED_CHAR_AST, r(SINGLE_QUOTED_CHAR)), c('\'')), //
                            seq(c('['), //
                                    ast(CHAR_RANGE_AST, seq(optional(c('^')), //
                                            oneOrMore(first( //
                                                    r(CHAR_RANGE), //
                                                    r(CHAR_RANGE_CHAR))))),
                                    c(']')))), //

            rule(SINGLE_QUOTED_CHAR, //
                    first( //
                            r(ESCAPED_CTRL_CHAR), //
                            new CharSet("\'\\").invert())), //

            rule(HEX, new CharSet(DIGIT, new CharSet('a', 'f'), new CharSet('A', 'F'))), //

            rule(CHAR_RANGE, //
                    seq(r(CHAR_RANGE_CHAR), c('-'), r(CHAR_RANGE_CHAR))), //

            rule(CHAR_RANGE_CHAR, //
                    first( //
                            new CharSet('\\', ']').invert(), //
                            r(SINGLE_QUOTED_CHAR), //
                            str("\\]"), //
                            str("\\^"))),

            rule(QUOTED_STRING, //
                    seq(c('"'), ast(QUOTED_STRING_AST, zeroOrMore(r(STR_QUOTED_CHAR))), c('"'))), //

            rule(STR_QUOTED_CHAR, //
                    first( //
                            r(ESCAPED_CTRL_CHAR), //
                            new CharSet("\"\\").invert() //
                    )), //

            rule(ESCAPED_CTRL_CHAR, //
                    first( //
                            str("\\t"), //
                            str("\\b"), //
                            str("\\n"), //
                            str("\\r"), //
                            str("\\f"), //
                            str("\\'"), //
                            str("\\\""), //
                            str("\\\\"), //
                            seq(str("\\u"), r(HEX), r(HEX), r(HEX), r(HEX)))), //

            rule(NOTHING, //
                    seq(c('('), ws, c(')'))),

            rule(START, c('^')) //
    ));

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

    private static Clause[] parseClauses(List<ASTNode> clauseNodes, String input) {
        return clauseNodes.stream().map(clauseNode -> parseClause(clauseNode, input)).collect(Collectors.toList())
                .toArray(new Clause[0]);
    }

    private static Clause parseClause(ASTNode clauseNode, String input) {
        Clause clause;
        switch (clauseNode.astLabel) {
        case SEQ_AST:
            clause = seq(parseClauses(clauseNode.getAllDescendantsNamed(CLAUSE), input));
            break;
        case ONE_OR_MORE_AST:
            clause = oneOrMore(parseClause(clauseNode.getFirstDescendantNamed(CLAUSE), input));
            break;
        case FIRST_MATCH_AST:
            clause = first(parseClauses(clauseNode.getAllDescendantsNamed(CLAUSE), input));
            break;
        case FOLLOWED_BY_AST:
            clause = followedBy(parseClause(clauseNode.getFirstDescendantNamed(CLAUSE), input));
            break;
        case NOT_FOLLOWED_BY_AST:
            clause = notFollowedBy(parseClause(clauseNode.getFirstDescendantNamed(CLAUSE), input));
            break;
        case NAME_AST:
            clause = r(clauseNode.getText(input)); // Rule name ref
            break;
        case QUOTED_STRING_AST: // Doesn't include surrounding quotes
            clause = str(unescapeString(clauseNode.getText(input)));
            break;
        case SINGLE_QUOTED_CHAR_AST:
            clause = c(unescapeChar(clauseNode.getText(input)));
            break;
        case CHAR_RANGE_AST:
            clause = cRange(unescapeString(clauseNode.getText(input)));
            break;
        default:
            // Keep recursing for parens (the only type of CLAUSE that doesn't have a label)
            clause = parseClause(clauseNode, input);
            break;
        }
        // Insert CreateASTNode node into grammar if there is an AST node label 
        String astNodeLabel = clauseNode.children.size() > 0
                && clauseNode.getFirstChild().astLabel.equals(LABEL_AST) ? clauseNode.getFirstChild().getText(input)
                        : null;
        if (astNodeLabel != null) {
            // Wrap clause in CreateASTNode node, if it is labeled
            clause = ast(astNodeLabel, clause);
        }
        return clause;
    }

    private static Clause parseRule(ASTNode ruleNode, String input) {
        if (ruleNode.children.size() != 2) {
            throw new IllegalArgumentException("Expected 2 children; got " + ruleNode.children.size());
        }
        String name = ruleNode.getFirstChild().getText(input);
        ASTNode clauseNode = ruleNode.getSecondChild();
        Clause clause = parseClause(clauseNode, input);
        return rule(name, clause);
    }

    public static Grammar parseGrammar(Parser parser) {
        var topLevelMatches = parser.grammar.getNonOverlappingMatches(parser.memoTable, GRAMMAR);
        if (topLevelMatches.isEmpty()) {
            throw new IllegalArgumentException("Toplevel rule did not match");
        } else if (topLevelMatches.size() > 1) {
            throw new IllegalArgumentException("Multiple toplevel matches");
        }
        var topLevelASTNode = topLevelMatches.get(0).toAST(parser.input);
        List<Clause> ruleClauses = new ArrayList<>();
        String lexRuleName = null;
        for (ASTNode astNode : topLevelASTNode.children) {
            if (!astNode.astLabel.equals(RULE_AST)) {
                throw new IllegalArgumentException("Wrong node type");
            }
            Clause rule = parseRule(astNode, parser.input);
            ruleClauses.add(rule);
            if (rule.ruleName != null && rule.ruleName.equals(LEX)) {
                // If a rule is named "Lex", then use that as the toplevel lex rule
                lexRuleName = LEX;
            }
        }
        return new Grammar(lexRuleName, ruleClauses);
    }
}
