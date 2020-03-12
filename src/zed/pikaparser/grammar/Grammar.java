package zed.pikaparser.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import zed.pikaparser.grammar.action.Drop;
import zed.pikaparser.grammar.action.Lift;
import zed.pikaparser.grammar.expr.Char;
import zed.pikaparser.grammar.expr.Expr;
import zed.pikaparser.grammar.expr.FirstMatch;
import zed.pikaparser.grammar.expr.FollowedBy;
import zed.pikaparser.grammar.expr.NotFollowedBy;
import zed.pikaparser.grammar.expr.Nothing;
import zed.pikaparser.grammar.expr.OneOrMore;
import zed.pikaparser.grammar.expr.Rule;
import zed.pikaparser.grammar.expr.Seq;
import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class Grammar {

    private HashMap<String, Expr> internedExprs = new HashMap<>();

    private HashSet<String> allRuleNames = new HashSet<>();

    private ArrayList<String> unusedRuleNames = new ArrayList<>();

    private HashMap<String, Expr> ruleNameToExpr = new HashMap<>();

    private Expr[] finalizedExprs;

    private Expr topExpr;

    public Expr recoveryLevelExpr;

    public Expr addRule(String ruleName, Expr expr) {
        if (finalizedExprs != null) {
            throw new IllegalArgumentException("Tried to add a rule after grammar was finalized");
        }
        if (!allRuleNames.add(ruleName)) {
            throw new IllegalArgumentException("More than one rule has the name \"" + ruleName + "\"");
        }
        unusedRuleNames.add(ruleName);
        ruleNameToExpr.put(ruleName, expr);
        return expr;
    }

    private Expr intern(Expr expr) {
        // Postorder traversal of subexprs
        for (int i = 0; i < expr.subExprs.size(); i++) {
            expr.subExprs.set(i, intern(expr.subExprs.get(i)));
        }

        // Intern this expr
        Expr internedExpr = internedExprs.get(expr.stringRep);
        if (internedExpr == null) {
            internedExprs.put(expr.stringRep, expr);
            internedExpr = expr;
        }
        return internedExpr;
    }

    // Resolve a potential chain of Rule references
    private Expr resolveRuleChain(Expr expr) {
        if (!(expr instanceof Rule)) {
            return expr;
        }

        String firstRuleName = ((Rule) expr).name;
        String currRuleName = firstRuleName;
        Expr currRule = expr;
        for (;;) {
            unusedRuleNames.remove(currRuleName);
            currRule = ruleNameToExpr.get(currRuleName);
            if (currRule == null) {
                throw new IllegalArgumentException("There is no rule defined with the same name as Rule \""
                        + currRuleName + "\"");
            }
            if (currRule instanceof Rule) {
                currRuleName = ((Rule) currRule).name;
                if (currRuleName.equals(firstRuleName)) {
                    throw new IllegalArgumentException("Found a cycle of Rule references involving rule \""
                            + currRuleName + "\"");
                }
            } else {
                break;
            }
        }
        return currRule;
    }

    private void resolveRuleSubExprs(Expr expr) {
        for (int i = 0; i < expr.subExprs.size(); i++) {
            expr.subExprs.set(i, resolveRuleChain(expr.subExprs.get(i)));
        }
    }

    /** Connect Rules to the rules they represent. */
    private void finalizeGrammar() {
        if (finalizedExprs != null) {
            return;
        }

        topExpr = ruleNameToExpr.get("TOP");
        if (topExpr == null) {
            throw new IllegalArgumentException("There is no rule defined with the name \"TOP\"");
        }

        // Intern subexpressions of each rule, so that common sub-expressions are reused.
        for (String ruleName : ruleNameToExpr.keySet()) {
            ruleNameToExpr.put(ruleName, intern(ruleNameToExpr.get(ruleName)));
        }

        // Resolve Rule references
        for (String stringRep : internedExprs.keySet()) {
            resolveRuleSubExprs(internedExprs.get(stringRep));
        }

        // Resolve Rule references in the mapping from rule name to expr
        for (String ruleName : ruleNameToExpr.keySet()) {
            Expr resolved = resolveRuleChain(ruleNameToExpr.get(ruleName));
            ruleNameToExpr.put(ruleName, resolved);
            resolved.ruleNames.add(ruleName);
        }

        // Re-get TOP in case the reference changed due to interning and/or resolving Rule references
        topExpr = ruleNameToExpr.get("TOP");

        // All rules but TOP must be used
        unusedRuleNames.remove("TOP");
        if (!unusedRuleNames.isEmpty()) {
            throw new IllegalArgumentException("Unused rules: " + unusedRuleNames);
        }

        // Create an indexed list of the resolved expressions.
        // (Put TOP first and terminals last, then sort by string rep in between, for prettyprinting.)
        ArrayList<Expr> resolvedExprs = new ArrayList<>();
        for (Expr expr : internedExprs.values()) {
            if (!(expr instanceof Rule)) {
                resolvedExprs.add(expr);
            }
        }
        Expr[] resolvedExprsArr = new Expr[resolvedExprs.size()];
        resolvedExprs.toArray(resolvedExprsArr);
        Arrays.sort(resolvedExprsArr, new Comparator<Expr>() {
            @Override
            public int compare(Expr a, Expr b) {
                if (a.ruleNames.contains("TOP")) {
                    return -1;
                } else if (b.ruleNames.contains("TOP")) {
                    return 1;
                } else if (a instanceof Char && !(b instanceof Char)) {
                    return 1;
                } else if (!(a instanceof Char) && b instanceof Char) {
                    return -1;
                } else {
                    return a.toString().compareTo(b.toString());
                }
            }
        });

        // Record index of each expr
        for (int i = 0; i < resolvedExprsArr.length; i++) {
            resolvedExprsArr[i].idx = i;
        }

        // Create links from the first subexprs of each expr back to the expr they are a subexpr of
        for (int i = 0; i < resolvedExprsArr.length; i++) {
            Expr expr = resolvedExprsArr[i];
            if (expr.subExprs.size() > 0) {
                // For FirstMatch, we need to add backlinks for all the sub-exprs, since any one of them can be in
                // the first position if previous subexprs fail
                int numSubExprsToBacklink = expr instanceof FirstMatch ? expr.subExprs.size() : 1;
                for (int j = 0; j < numSubExprsToBacklink; j++) {
                    Expr firstSubExpr = expr.subExprs.get(j);
                    firstSubExpr.superExprsWithThisAsFirstSubExpr.add(expr);
                }
            }
        }

        // Check recovery level expression is a child of a OneOrMore expr
        if (recoveryLevelExpr != null) {
            boolean hasOneOrMoreSuperExpr = false;
            for (Expr superExpr : recoveryLevelExpr.superExprsWithThisAsFirstSubExpr) {
                if (superExpr instanceof OneOrMore) {
                    hasOneOrMoreSuperExpr = true;
                    break;
                }
            }
            if (!hasOneOrMoreSuperExpr) {
                throw new IllegalArgumentException(
                        "Recovery level expression must be a child of a OneOrMore expression");
            }
        }

        // Can't add rules after grammar is finalized
        finalizedExprs = resolvedExprsArr;
    }

    public Expr[] getExprs() {
        finalizeGrammar();
        return finalizedExprs;
    }

    public Expr getTopExpr() {
        finalizeGrammar();
        return topExpr;
    }

    public void setRecoveryLevelExpr(Expr recoveryLevelExpr) {
        this.recoveryLevelExpr = recoveryLevelExpr;
    }

    public Expr getRecoveryLevelExpr() {
        return recoveryLevelExpr;
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr Nothing() {
        return new Nothing();
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr Seq(Expr... exprs) {
        // Nothing entries don't change a sequence match
        int numNothing = 0;
        for (Expr expr : exprs) {
            if (expr instanceof Nothing) {
                numNothing++;
            }
        }

        // If there are no parameters in the Seq, or only Nothing() params,
        // return a single Nothing(), not wrapped in a Seq
        if (exprs.length - numNothing == 0) {
            return Nothing();
        }

        // Strip out any Nothings from subexprs
        if (numNothing > 0) {
            Expr[] somethingExprs = new Expr[exprs.length - numNothing];
            for (int i = 0, j = 0; i < exprs.length; i++) {
                Expr expr = exprs[i];
                if (!(expr instanceof Nothing)) {
                    somethingExprs[j++] = expr;
                }
            }
            exprs = somethingExprs;
        }

        if (exprs.length == 1) {
            // Don't wrap a single expression
            return exprs[0];
        } else {
            return new Seq(exprs);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /** Return the first subexpression that matches. FirstMatch always requires at least one parameter. */
    public static Expr FirstMatch(Expr first, Expr... rest) {
        // Throw away any subexpressions after a Nothing entry
        int last = rest.length - 1;
        for (int i = -1; i < rest.length; i++) {
            Expr expr = i == -1 ? first : rest[i];
            if (expr instanceof Nothing) {
                last = i;
                break;
            }
        }
        int numExprs = last + 2;
        if (numExprs == 1) {
            // Don't wrap a single expression
            return first;
        } else {
            Expr[] allExprs = new Expr[numExprs];
            for (int i = -1; i < rest.length; i++) {
                Expr expr = i == -1 ? first : rest[i];
                allExprs[i + 1] = expr;
            }
            return new FirstMatch(allExprs);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr OneOrMore(Expr expr) {
        if (expr instanceof Nothing) {
            // Nothing always matches one or more times
            return Nothing();
        }
        return new OneOrMore(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr ZeroOrMore(Expr expr) {
        return FirstMatch(OneOrMore(expr), Nothing());
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr FollowedBy(Expr expr) {
        // FollowedBy(Nothing) is ill-defined (technically it means "always match") since Nothing will always match.
        if (expr instanceof Nothing) {
            // Nothing followed by Nothing is just Nothing
            return Nothing();
        }
        return new FollowedBy(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr NotFollowedBy(Expr expr) {
        // NotFollowedBy(Nothing) is ill-defined (it means "never match", since FollowedBy(Nothing) means
        // "always match") -- but there is no PEG equivalent to "match nothing", so we have to report an error.
        // TODO: Create a Fail type, and then drop it and everything after it in FirstMatch, and propagate it up through Seq? 
        if (expr instanceof Nothing) {
            // TDOO: report location of error
            throw new IllegalArgumentException("NotFollowedBy(Nothing) is not a valid construction");
        }
        return new NotFollowedBy(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr Optional(Expr expr) {
        return FirstMatch(expr, Nothing());
    }

    // -----------------------------------------------------------------------------------------------------------------

    /** If the expression and any subexpressions match, insert a single Drop node into the parse tree. */
    public static Expr Drop(Expr expr) {
        return new Drop(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /** If the expression and any subexpressions match, lift subexpressions to the level of the parent in the AST. */
    public static Expr Lift(Expr expr) {
        return new Lift(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /** Reduce all subexprs to a single token in the AST. */
    public static class Span extends Expr {
        private Span(Expr expr) {
            super(expr, "Span(" + expr + ")");
        }

        @Override
        public Memo match(DirectionalParser parser, MemoRef ref) {
            // Match if the subexprs matched
            Memo subExprMemo = parser.matchMemoized(ref, new MemoRef(subExprs.get(0), ref.pos));
            return subExprMemo.matches() ? Memo.matched(ref, subExprMemo) : Memo.DID_NOT_MATCH(ref);
        }
    }

    /** Reduce all subexprs to a single token in the AST. */
    public static Expr Span(Expr expr) {
        return new Span(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static String chrToString(char c) {
        if (c > 31) {
            return Character.toString(c);
        } else if (c == '\n') {
            return "\\n";
        } else if (c == '\r') {
            return "\\r";
        } else if (c == '\t') {
            return "\\t";
        } else {
            if (c < 16) {
                return "\\u000" + (c < 10 ? c + '0' : (char) (c - 10 + 'a'));
            } else {
                int u = (c & 0xf0) >> 4, l = c & 0xf;
                return "\\u00" + (u < 10 ? u + '0' : (char) (u - 10 + 'a'))
                        + (l < 10 ? l + '0' : (char) (l - 10 + 'a'));
            }
        }
    }

    private static final BitSet IS_UNICODE_WHITESPACE = new BitSet(1 << 16);

    static {
        // Valid unicode whitespace chars, see:
        // http://stackoverflow.com/questions/4731055/whitespace-matching-regex-java
        String wsChars = ""//
                + (char) 0x0009 // CHARACTER TABULATION
                + (char) 0x000A // LINE FEED (LF)
                + (char) 0x000B // LINE TABULATION
                + (char) 0x000C // FORM FEED (FF)
                + (char) 0x000D // CARRIAGE RETURN (CR)
                + (char) 0x0020 // SPACE
                + (char) 0x0085 // NEXT LINE (NEL) 
                + (char) 0x00A0 // NO-BREAK SPACE
                + (char) 0x1680 // OGHAM SPACE MARK
                + (char) 0x180E // MONGOLIAN VOWEL SEPARATOR
                + (char) 0x2000 // EN QUAD 
                + (char) 0x2001 // EM QUAD 
                + (char) 0x2002 // EN SPACE
                + (char) 0x2003 // EM SPACE
                + (char) 0x2004 // THREE-PER-EM SPACE
                + (char) 0x2005 // FOUR-PER-EM SPACE
                + (char) 0x2006 // SIX-PER-EM SPACE
                + (char) 0x2007 // FIGURE SPACE
                + (char) 0x2008 // PUNCTUATION SPACE
                + (char) 0x2009 // THIN SPACE
                + (char) 0x200A // HAIR SPACE
                + (char) 0x2028 // LINE SEPARATOR
                + (char) 0x2029 // PARAGRAPH SEPARATOR
                + (char) 0x202F // NARROW NO-BREAK SPACE
                + (char) 0x205F // MEDIUM MATHEMATICAL SPACE
                + (char) 0x3000; // IDEOGRAPHIC SPACE
        for (int i = 0; i < wsChars.length(); i++) {
            IS_UNICODE_WHITESPACE.set((int) wsChars.charAt(i));
        }
    }

    public static Expr Char(char chr) {
        return new Char("[" + chrToString(chr) + "]") {
            @Override
            public boolean matches(char c) {
                return c == chr;
            }
        };
    }

    public static Expr Letter() {
        return new Char("[a-zA-Z]") {
            @Override
            public boolean matches(char c) {
                return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            }
        };
    }

    public static Expr Digit() {
        return new Char("[0-9]") {
            @Override
            public boolean matches(char c) {
                return c >= '0' && c <= '9';
            }
        };
    }

    public static Expr Char(char lowInclusive, char highInclusive) {
        return new Char("[" + chrToString(lowInclusive) + "-" + chrToString(highInclusive) + "]") {
            @Override
            public boolean matches(char c) {
                return c >= lowInclusive && c <= highInclusive;
            }
        };
    }

    /** Match any Unicode whitespace character */
    public static Expr Whitespace() {
        return new Char("Whitespace()") {
            @Override
            public boolean matches(char c) {
                return IS_UNICODE_WHITESPACE.get(c);
            }
        };
    }

    /** Match any Unicode whitespace character except for '\n' and '\r' */
    public static Expr NonNewlineWhitespace() {
        return new Char("NonNewlineWhitespace()") {
            @Override
            public boolean matches(char c) {
                return c != '\n' && c != '\r' && IS_UNICODE_WHITESPACE.get(c);
            }
        };
    }

    /** Match a newline ("\n" or "\r\n"). */
    public static Expr Newline() {
        return FirstMatch(Char('\n'), Seq(Char('\r'), Char('\n')));
    }

    /** Match a sequence of characters. */
    public static Expr Tok(String token) {
        if (token.isEmpty()) {
            return Nothing();
        }
        Expr[] chrExprs = new Expr[token.length()];
        for (int i = 0; i < token.length(); i++) {
            chrExprs[i] = Char(token.charAt(i));
        }
        return Seq(chrExprs);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr Rule(String name) {
        return new Rule(name);
    }
}
