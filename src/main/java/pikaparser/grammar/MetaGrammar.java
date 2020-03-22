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

    private static final String GRAMMAR = "Grammar";
    private static final String LEX = "Lex";
    private static final String WSC = "WhiteSpaceOrComment";
    private static final String COMMENT = "Comment";
    private static final String RULE = "Rule";
    private static final String CLAUSE = "Clause";
    private static final String IDENT = "Ident";
    private static final String LABEL = "Label";
    private static final String NAME_CHAR = "NameChar";
    private static final String PARENS = "Parens";
    private static final String SEQ = "Seq";
    private static final String FIRST = "First";
    private static final String LONGEST = "Longest";
    private static final String FOLLOWED_BY = "FollowedBy";
    private static final String NOT_FOLLOWED_BY = "NotFollowedBy";
    private static final String ONE_OR_MORE = "OneOrMore";
    private static final String ZERO_OR_MORE = "ZeroOrMore";
    private static final String OPTIONAL = "Optional";
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

    private static final String RULE_AST = "RuleAST";
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

    // Toplevel rule for lex preprocessing (use null to disable lexing):

    public static final String LEX_RULE_NAME = LEX;

    // Metagrammar:

    public static Grammar grammar = new Grammar(LEX_RULE_NAME, Arrays.asList(//
            rule(GRAMMAR, //
                    seq(start(), r(WSC), oneOrMore(r(RULE)))), //

            rule(RULE, //
                    ast(RULE_AST, seq(r(IDENT), r(WSC), c('='), r(WSC), r(CLAUSE), r(WSC), c(';'), r(WSC)))), //

            rule(CLAUSE, //
                    seq( // 
                            optional(r(LABEL)), //
                            first( //
                                    // Seq and First must come first, because they match sequences of clauses.
                                    // If they are not first, then the CLAUSE rule will greedily match a single
                                    // earlier subclause (e.g. ONE_OR_MORE) and will never visit SEQ or FIRST.
                                    // Also, counterintuitively, SEQ should come before FIRST so that (A A | B B)
                                    // parses as ((A A) | (B B)), and not (A (A | B) B). SEQ is shorter than FIRST,
                                    // so SEQ would be matched greedily, and FIRST would never match, if SEQ
                                    // came before FIRST;
                                    r(SEQ), //
                                    r(FIRST), //
                                    r(LONGEST), //

                                    // Parens are required for OneOrMore, ZeroOrMore and Optional, otherwise
                                    // expressions like (Clause1 Clause2+) would be parsed as
                                    // ((Clause1 Clause2)+) rather than (Clause1 (Clause2)+).
                                    // These have to come before PARENS, since they are right-associative.
                                    // Otherwise, a sequence like "(X)+" will be matched as "(X)" and the "+"
                                    // will never be reached.
                                    r(ONE_OR_MORE), //
                                    r(ZERO_OR_MORE), //
                                    r(OPTIONAL), //

                                    r(PARENS), //

                                    r(FOLLOWED_BY), //
                                    r(NOT_FOLLOWED_BY), //

                                    // Finally match individual tokens (these are the shortest clause types)
                                    r(IDENT), //
                                    r(QUOTED_STRING), //
                                    r(CHAR_SET), //
                                    r(NOTHING), //
                                    r(START)))), //

            rule(LABEL, seq(ast(LABEL_AST, r(IDENT)), r(WSC), c(':'), r(WSC))), //

            rule(PARENS, seq(c('('), r(WSC), r(CLAUSE), r(WSC), c(')'))), //

            rule(SEQ, //
                    ast(SEQ_AST, seq(r(CLAUSE), r(WSC), oneOrMore(seq(r(CLAUSE), r(WSC)))))),

            rule(ONE_OR_MORE, //
                    seq(c('('), r(WSC), ast(ONE_OR_MORE_AST, r(CLAUSE)), r(WSC), str(")+"))),

            rule(ZERO_OR_MORE, //
                    ast(ZERO_OR_MORE_AST, first( //
                            seq(c('('), r(WSC), ast(ONE_OR_MORE_AST, r(CLAUSE)), r(WSC), str(")*")), //
                            r(NOTHING)))),

            rule(OPTIONAL, //
                    ast(OPTIONAL_AST, first( //
                            seq(c('('), r(WSC), r(CLAUSE), r(WSC), str(")?")), //
                            r(NOTHING)))),

            rule(FIRST, //
                    ast(FIRST_AST, seq(r(CLAUSE), r(WSC), oneOrMore(seq(c('/'), r(WSC), r(CLAUSE), r(WSC)))))),

            rule(LONGEST, //
                    ast(LONGEST_AST, seq(r(CLAUSE), r(WSC), oneOrMore(seq(c('|'), r(WSC), r(CLAUSE), r(WSC)))))),

            rule(FOLLOWED_BY, //
                    ast(FOLLOWED_BY_AST, seq(c('&'), r(CLAUSE)))),

            rule(NOT_FOLLOWED_BY, //
                    ast(NOT_FOLLOWED_BY_AST, seq(c('!'), r(CLAUSE)))),

            // Lex rule for preprocessing:
            rule(LEX, //
                    oneOrMore( //
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
                                    c('^'), //
                                    r(IDENT), //
                                    r(QUOTED_STRING), //
                                    r(CHAR_SET), //

                                    // WS/comment has to come last, since it can match Nothing
                                    r(WSC)) //
                    )), //

            rule(WSC, //
                    zeroOrMore(first(c(" \n\r\t"), r(COMMENT)))),

            rule(COMMENT, //
                    seq(c('#'), zeroOrMore(c('\n', /* invert = */ true)))),

            rule(IDENT, //
                    ast(IDENT_AST, oneOrMore(r(NAME_CHAR)))), //

            rule(NAME_CHAR, c(c('a', 'z'), c('A', 'Z'), c("_-"))),

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

    private static List<Clause> parseClauses(List<ASTNode> astNodes, String input) {
        List<Clause> clauses = new ArrayList<>(astNodes.size());
        String nextNodeLabel = null;
        for (int i = 0; i < astNodes.size(); i++) {
            var astNode = astNodes.get(i);
            if (astNode.astLabel.equals(LABEL_AST)) {
                // A label AST node precedes the labeled clause
                nextNodeLabel = astNode.getText(input);
            } else {
                // Convert from ASTNode to Clause
                var clause = parseClause(astNode, input);
                if (nextNodeLabel != null) {
                    // Label the Clause with the preceding label, if present
                    clause.astNodeLabel = nextNodeLabel;
                    nextNodeLabel = null;
                }
                clauses.add(clause);
            }
        }
        return clauses;
    }

    private static Clause expectOne(List<Clause> clauses) {
        if (clauses.size() != 1) {
            throw new IllegalArgumentException("Expected one clause, got " + clauses.size());
        }
        return clauses.get(0);
    }

    private static Clause parseClause(ASTNode astNode, String input) {
        Clause clause;
        switch (astNode.astLabel) {
        case SEQ_AST:
            clause = seq(parseClauses(astNode.children, input));
            break;
        case FIRST_AST:
            clause = first(parseClauses(astNode.children, input));
            break;
        case LONGEST_AST:
            clause = longest(parseClauses(astNode.children, input));
            break;
        case ONE_OR_MORE_AST:
            clause = oneOrMore(expectOne(parseClauses(astNode.children, input)));
            break;
        case ZERO_OR_MORE_AST:
            clause = zeroOrMore(expectOne(parseClauses(astNode.children, input)));
            break;
        case OPTIONAL_AST:
            clause = optional(expectOne(parseClauses(astNode.children, input)));
            break;
        case FOLLOWED_BY_AST:
            clause = followedBy(expectOne(parseClauses(astNode.children, input)));
            break;
        case NOT_FOLLOWED_BY_AST:
            clause = notFollowedBy(expectOne(parseClauses(astNode.children, input)));
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
            clause = expectOne(parseClauses(astNode.children, input));
            break;
        }
        // Insert CreateASTNode node into grammar if there is an AST node label 
        String astNodeLabel = astNode.children.size() > 0 && astNode.getFirstChild().astLabel.equals(LABEL_AST)
                ? astNode.getFirstChild().getText(input)
                : null;
        if (astNodeLabel != null) {
            // Wrap clause in CreateASTNode node, if it is labeled
            clause = ast(astNodeLabel, clause);
        }
        return clause;
    }

    private static Rule parseRule(ASTNode ruleNode, String input) {
        if (ruleNode.children.size() != 2) {
            throw new IllegalArgumentException("Expected 2 children; got " + ruleNode.children.size());
        }
        String name = ruleNode.getFirstChild().getText(input);
        ASTNode astNode = ruleNode.getSecondChild();
        Clause clause = parseClause(astNode, input);
        return rule(name, clause);
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
            if (!astNode.astLabel.equals(RULE_AST)) {
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
