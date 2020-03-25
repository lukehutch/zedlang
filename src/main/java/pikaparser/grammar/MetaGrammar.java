package pikaparser.grammar;

import static pikaparser.clause.Clause.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pikaparser.clause.Clause;
import pikaparser.parser.ASTNode;
import pikaparser.parser.Parser;

public class MetaGrammar {
    // Rule names:

    private static final String GRAMMAR = "GRAMMAR";
    private static final String LEX = "LEX";
    private static final String WSC = "WSC";
    private static final String COMMENT = "COMMENT";
    private static final String RULE = "RULE";
    private static final String CLAUSE = "CLAUSE";
    private static final String IDENT = "IDENT";
    private static final String PREC = "PREC";
    private static final String NUM = "NUM";
    private static final String NAME_CHAR = "NAME_CHAR";
    private static final String CHAR_SET = "CHARSET";
    private static final String HEX = "Hex";
    private static final String CHAR_RANGE = "CHAR_RANGE";
    private static final String CHAR_RANGE_CHAR = "CHAR_RANGE_CHAR";
    private static final String QUOTED_STRING = "QUOTED_STR";
    private static final String ESCAPED_CTRL_CHAR = "ESCAPED_CTRL_CHAR";
    private static final String SINGLE_QUOTED_CHAR = "SINGLE_QUOTED_CHAR";
    private static final String STR_QUOTED_CHAR = "STR_QUOTED_CHAR";
    private static final String NOTHING = "NOTHING";
    private static final String START = "START";

    // AST node names:

    private static final String RULE_AST = "RuleAST";
    private static final String PREC_AST = "PrecAST";
    private static final String IDENT_AST = "IdentAST";
    private static final String LABEL_AST = "LabelAST";
    private static final String SEQ_AST = "SeqAST";
    private static final String FIRST_AST = "FirstAST";
    private static final String LONGEST_AST = "LongestAST";
    private static final String FOLLOWED_BY_AST = "FollowedByAST";
    private static final String NOT_FOLLOWED_BY_AST = "NotFollowedByAST";
    private static final String ONE_OR_MORE_AST = "OneOrMoreAST";
    private static final String ZERO_OR_MORE_AST = "ZeroOrMoreAST";
    private static final String OPTIONAL_AST = "OptionalAST";
    private static final String SINGLE_QUOTED_CHAR_AST = "SingleQuotedCharAST";
    private static final String CHAR_RANGE_AST = "CharRangeAST";
    private static final String QUOTED_STRING_AST = "QuotedStringAST";
    private static final String START_AST = "StartAST";
    private static final String NOTHING_AST = "NothingAST";

    // Metagrammar:

    public static Grammar grammar = new Grammar(LEX, Arrays.asList(//
            rule(GRAMMAR, //
                    seq(start(), r(WSC), oneOrMore(r(RULE)))), //

            rule(RULE, //
                    ast(RULE_AST, seq(r(IDENT), r(WSC), //
                            optional(r(PREC)), //
                            c('='), r(WSC), //
                            r(CLAUSE), r(WSC), c(';'), r(WSC)))), //

            // Define precedence order for clause sequences

            rule(CLAUSE, 8, seq(c('('), r(WSC), r(CLAUSE), r(WSC), c(')'))), //

            rule(CLAUSE, 7, //
                    first( //
                            r(IDENT), //
                            r(QUOTED_STRING), //
                            r(CHAR_SET), //
                            r(NOTHING), //
                            r(START))), //

            rule(CLAUSE, 6, //
                    first( //
                            seq(ast(ONE_OR_MORE_AST, r(CLAUSE)), r(WSC), c("+")),
                            seq(ast(ZERO_OR_MORE_AST, r(CLAUSE)), r(WSC), c('*')))), //

            rule(CLAUSE, 5, //
                    first( //
                            seq(c('&'), ast(FOLLOWED_BY_AST, r(CLAUSE))), //
                            seq(c('!'), ast(NOT_FOLLOWED_BY_AST, r(CLAUSE))))), //

            rule(CLAUSE, 4, //
                    seq(ast(OPTIONAL_AST, r(CLAUSE)), r(WSC), c('?'))), //

            rule(CLAUSE, 3,
                    seq(optional(seq(ast(LABEL_AST, r(IDENT)), r(WSC), c(':'), r(WSC))), r(CLAUSE), r(WSC))), //

            rule(CLAUSE, 2, //
                    ast(SEQ_AST, seq(r(CLAUSE), r(WSC), oneOrMore(seq(r(CLAUSE), r(WSC)))))),

            rule(CLAUSE, 1, //
                    ast(FIRST_AST, seq(r(CLAUSE), r(WSC), oneOrMore(seq(c('/'), r(WSC), r(CLAUSE), r(WSC)))))),

            rule(CLAUSE, 0, //
                    ast(LONGEST_AST, seq(r(CLAUSE), r(WSC), oneOrMore(seq(c('|'), r(WSC), r(CLAUSE), r(WSC)))))),

            // Lex rule for preprocessing

            rule(LEX, //
                    first( //
                            c('('), //
                            c(')'), //
                            c('='), //
                            c(';'), //
                            c(':'), //
                            c('^'), //
                            c('*'), //
                            c('+'), //
                            c('?'), //
                            c('|'), //
                            c('/'), //
                            c('^'), //
                            c('-'), //
                            // Match both CHAR_SET and PREC, since PREC looks like a CHAR_SET
                            longest(r(PREC), r(CHAR_SET)), //
                            r(IDENT), //
                            r(NUM), //
                            r(QUOTED_STRING), //

                            // WS/comment has to come last, since it can match Nothing
                            r(WSC))), //

            rule(WSC, //
                    zeroOrMore(first(c(" \n\r\t"), r(COMMENT)))),

            rule(COMMENT, //
                    seq(c('#'), zeroOrMore(c('\n', /* invert = */ true)))),

            rule(IDENT, //
                    ast(IDENT_AST, oneOrMore(r(NAME_CHAR)))), //

            rule(NUM, //
                    oneOrMore(c('0', '9'))), //

            rule(NAME_CHAR, //
                    c(c('a', 'z'), c('A', 'Z'), c("_-"))),

            rule(PREC, //
                    seq(c('['), r(WSC), ast(PREC_AST, r(NUM)), r(WSC), c(']'), r(WSC))), //

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
                            c("\'\\").invert())), //

            rule(HEX, c(c('0', '9'), c('a', 'f'), c('A', 'F'))), //

            rule(CHAR_RANGE, //
                    seq(r(CHAR_RANGE_CHAR), c('-'), r(CHAR_RANGE_CHAR))), //

            rule(CHAR_RANGE_CHAR, //
                    first( //
                            c('\\', ']').invert(), //
                            r(ESCAPED_CTRL_CHAR), //
                            str("\\\\"), //
                            str("\\]"), //
                            str("\\^"))),

            rule(QUOTED_STRING, //
                    seq(c('"'), ast(QUOTED_STRING_AST, zeroOrMore(r(STR_QUOTED_CHAR))), c('"'))), //

            rule(STR_QUOTED_CHAR, //
                    first( //
                            r(ESCAPED_CTRL_CHAR), //
                            c("\"\\").invert() //
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
                    ast(NOTHING_AST, seq(c('('), r(WSC), c(')')))),

            rule(START, ast(START_AST, c('^'))) //
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

    private static Clause expectOne(List<Clause> clauses) {
        if (clauses.size() != 1) {
            throw new IllegalArgumentException("Expected one clause, got " + clauses.size());
        }
        return clauses.get(0);
    }

    private static List<Clause> parseASTNodes(List<ASTNode> astNodes, String input) {
        List<Clause> clauses = new ArrayList<>(astNodes.size());
        String nextNodeLabel = null;
        for (int i = 0; i < astNodes.size(); i++) {
            var astNode = astNodes.get(i);
            if (astNode.label.equals(LABEL_AST)) {
                // A label AST node precedes the labeled clause
                nextNodeLabel = astNode.getText(input);
            } else {
                // Create a Clause from the ASTNode
                var clause = parseASTNode(astNode, input);
                if (nextNodeLabel != null) {
                    // Label the Clause with the preceding label, if present
                    clause = ast(nextNodeLabel, clause);
                    nextNodeLabel = null;
                }
                clauses.add(clause);
            }
        }
        return clauses;
    }

    private static Clause parseASTNode(ASTNode astNode, String input) {
        Clause clause;
        switch (astNode.label) {
        case SEQ_AST:
            clause = seq(parseASTNodes(astNode.children, input));
            break;
        case FIRST_AST:
            clause = first(parseASTNodes(astNode.children, input));
            break;
        case LONGEST_AST:
            clause = longest(parseASTNodes(astNode.children, input));
            break;
        case ONE_OR_MORE_AST:
            clause = oneOrMore(expectOne(parseASTNodes(astNode.children, input)));
            break;
        case ZERO_OR_MORE_AST:
            clause = zeroOrMore(expectOne(parseASTNodes(astNode.children, input)));
            break;
        case OPTIONAL_AST:
            clause = optional(expectOne(parseASTNodes(astNode.children, input)));
            break;
        case FOLLOWED_BY_AST:
            clause = followedBy(expectOne(parseASTNodes(astNode.children, input)));
            break;
        case NOT_FOLLOWED_BY_AST:
            clause = notFollowedBy(expectOne(parseASTNodes(astNode.children, input)));
            break;
        case IDENT_AST:
            clause = r(astNode.getText(input)); // Rule name ref
            break;
        case QUOTED_STRING_AST: // Doesn't include surrounding quotes
            clause = str(unescapeString(astNode.getText(input)));
            break;
        case SINGLE_QUOTED_CHAR_AST:
            clause = c(unescapeChar(astNode.getText(input)));
            break;
        case START_AST:
            clause = start();
            break;
        case NOTHING_AST:
            clause = nothing();
            break;
        case CHAR_RANGE_AST:
            String text = unescapeString(astNode.getText(input));
            boolean invert = text.startsWith("^");
            if (invert) {
                text = text.substring(1);
            }
            clause = invert ? cRange(text).invert() : cRange(text);
            break;
        default:
            // Keep recursing for parens (the only type of AST node that doesn't have a label)
            clause = expectOne(parseASTNodes(astNode.children, input));
            break;
        }
        // Insert CreateASTNode node into grammar if there is an AST node label 
        String astNodeLabel = astNode.children.size() > 0 && astNode.getFirstChild().label.equals(LABEL_AST)
                ? astNode.getFirstChild().getText(input)
                : null;
        if (astNodeLabel != null) {
            // Wrap clause in CreateASTNode node, if it is labeled
            clause = ast(astNodeLabel, clause);
        }
        return clause;
    }

    private static Rule parseRule(ASTNode ruleNode, String input) {
        String ruleName = ruleNode.getFirstChild().getText(input);
        var hasPrecedence = ruleNode.getSecondChild().label.equals(PREC_AST);
        if (ruleNode.children.size() > (hasPrecedence ? 3 : 2)) {
            throw new IllegalArgumentException(
                    "Expected " + (hasPrecedence ? 3 : 2) + " children; got " + ruleNode.children.size());
        }
        int precedence = hasPrecedence ? Integer.parseInt(ruleNode.getSecondChild().getText(input)) : -1;
        var astNode = hasPrecedence ? ruleNode.getThirdChild() : ruleNode.getSecondChild();
        Clause clause = parseASTNode(astNode, input);
        return rule(ruleName, precedence, clause);
    }

    public static Grammar parseGrammar(Parser parser) {
        var topLevelMatches = parser.grammar.getNonOverlappingMatches(parser.memoTable, GRAMMAR);
        if (topLevelMatches.isEmpty()) {
            throw new IllegalArgumentException("Toplevel rule did not match");
        } else if (topLevelMatches.size() > 1) {
            throw new IllegalArgumentException("Multiple toplevel matches");
        }
        var topLevelASTNode = topLevelMatches.get(0).toAST("<root>", parser.input);
        List<Rule> rules = new ArrayList<>();
        String lexRuleName = null;
        for (ASTNode astNode : topLevelASTNode.children) {
            if (!astNode.label.equals(RULE_AST)) {
                throw new IllegalArgumentException("Wrong node type");
            }
            Rule rule = parseRule(astNode, parser.input);
            rules.add(rule);
            if (rule.ruleName != null && rule.ruleName.equals(LEX)) {
                // If a rule is named "Lex", then use that as the toplevel lex rule
                lexRuleName = LEX;
            }
        }
        return new Grammar(lexRuleName, rules);
    }
}
