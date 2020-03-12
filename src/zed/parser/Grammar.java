package zed.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class Grammar {

    private HashMap<String, Expr> internedExprs = new HashMap<>();

    private HashSet<String> allRuleNames = new HashSet<>();

    private ArrayList<String> unusedRuleNames = new ArrayList<>();

    private HashMap<String, Expr> ruleNameToExpr = new HashMap<>();

    private Expr[] finalizedExprs;

    private Expr topExpr;

    public void addRule(String ruleName, Expr expr) {
        if (finalizedExprs != null) {
            throw new IllegalArgumentException("Tried to add a rule after grammar was finalized");
        }
        if (!allRuleNames.add(ruleName)) {
            throw new IllegalArgumentException("More than one rule has the name \"" + ruleName + "\"");
        }
        unusedRuleNames.add(ruleName);
        ruleNameToExpr.put(ruleName, expr);
    }

    private Expr intern(Expr expr) {
        // Postorder traversal of subexprs
        for (int i = 0; i < expr.subExprs.size(); i++) {
            expr.subExprs.set(i, intern(expr.subExprs.get(i)));
        }

        // Intern this expr
        String stringRep = expr.getStringRep();
        Expr internedExpr = internedExprs.get(stringRep);
        if (internedExpr == null) {
            internedExprs.put(stringRep, expr);
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
            resolved.addRuleName(ruleName);
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
                if (a.getRuleNames().contains("TOP")) {
                    return -1;
                } else if (b.getRuleNames().contains("TOP")) {
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
            resolvedExprsArr[i].setIdx(i);
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

    // -----------------------------------------------------------------------------------------------------------------
    // Expr types: constructors are private so that the user is forced to intern exprs by calling factory methods
    // -----------------------------------------------------------------------------------------------------------------

    public static class Nothing extends Expr {
        private Nothing() {
            setStringRep("Nothing()");
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            // Successfully match, consuming no input
            return Memo.matchedEmpty(this, startIdx);
        }
    }

    public static Expr Nothing() {
        return new Nothing();
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static class Seq extends Expr {
        private Seq(Expr... exprs) {
            addSubExprs(exprs);

            StringBuilder buf = new StringBuilder();
            for (Expr e : exprs) {
                if (buf.length() > 0) {
                    buf.append(' ');
                }
                boolean addParens = e instanceof Seq || e instanceof FirstMatch;
                if (addParens) {
                    buf.append('(');
                }
                buf.append(e);
                if (addParens) {
                    buf.append(')');
                }
            }
            setStringRep(buf.toString());
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            ArrayList<Memo> subExprMemos = new ArrayList<>();
            for (int i = 0, currIdx = startIdx; i < subExprs.size(); i++) {
                // Look up memo for subexpr.
                Memo subExprMemo = parser.matchMemoized(subExprs.get(i), currIdx);

                if (subExprMemo.matches()) {
                    // If the subexpr matches the sequence at currIdx, record the memo for the subexpr
                    subExprMemos.add(subExprMemo);

                    // Consume the match
                    currIdx = subExprMemo.endIdx;

                } else {
                    // Bail on the first non-match in the sequence
                    return Memo.DID_NOT_MATCH;
                }
            }
            // The whole sequence matched
            return Memo.matched(this, subExprMemos);
        }
    }

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

    public static class FirstMatch extends Expr {
        private FirstMatch(Expr... exprs) {
            addSubExprs(exprs);

            StringBuilder buf = new StringBuilder();
            for (Expr e : exprs) {
                if (buf.length() > 1) {
                    buf.append(" / ");
                }
                boolean addParens = e instanceof Seq || e instanceof FirstMatch;
                if (addParens) {
                    buf.append('(');
                }
                buf.append(e);
                if (addParens) {
                    buf.append(')');
                }
            }
            setStringRep(buf.toString());
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            ArrayList<Memo> subExprMemos = new ArrayList<>();
            for (int i = 0; i < subExprs.size(); i++) {
                // Look up memo for subexpr.
                Memo subExprMemo = parser.matchMemoized(subExprs.get(i), startIdx);

                if (subExprMemo.matches()) {
                    // Return on first match
                    subExprMemos.add(subExprMemo);
                    return Memo.matched(this, subExprMemos);
                }
            }
            // None of the options matched
            return Memo.DID_NOT_MATCH;
        }
    }

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

    public static class OneOrMore extends Expr {
        private OneOrMore(Expr expr) {
            addSubExpr(expr);
            setStringRep("(" + expr + ")+");
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            Expr subExpr = subExprs.get(0);

            // Greedily match the subexpr as many times as possible
            ArrayList<Memo> subExprMemos = new ArrayList<>();
            int currIdx = startIdx;
            for (boolean done = false; !done;) {
                // Look up memo for subexpr at curr input index
                Memo subExprMemo = parser.matchMemoized(subExpr, currIdx);

                if (subExprMemo.matches()) {
                    // Consume the match
                    currIdx = subExprMemo.endIdx;
                    subExprMemos.add(subExprMemo);
                } else {
                    done = true;
                }

                if (subExprMemo.startIdx == subExprMemo.endIdx) {
                    // If the subexpr consumed no input, it can only match once, otherwise we end up in an infinite loop
                    done = true;
                }
            }

            if (subExprMemos.size() >= 1) {
                return Memo.matched(this, subExprMemos);
            } else {
                return Memo.DID_NOT_MATCH;
            }
        }
    }

    public static Expr OneOrMore(Expr expr) {
        return new OneOrMore(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr ZeroOrMore(Expr expr) {
        return FirstMatch(OneOrMore(expr), Nothing());
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static class FollowedBy extends Expr {
        private FollowedBy(Expr expr) {
            addSubExpr(expr);
            setStringRep("&(" + expr + ")");
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            Memo subExprMemo = parser.matchMemoized(subExprs.get(0), startIdx);
            // If the subexpr matches, return a match but consume no input, otherwise return a non-match
            return subExprMemo.matches() ? Memo.matchedEmpty(this, startIdx) : Memo.DID_NOT_MATCH;
        }
    }

    public static Expr FollowedBy(Expr expr) {
        return new FollowedBy(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static class NotFollowedBy extends Expr {
        private NotFollowedBy(Expr expr) {
            addSubExpr(expr);
            setStringRep("!(" + expr + ")");
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            Memo subExprMemo = parser.matchMemoized(subExprs.get(0), startIdx);
            // If the subexpr does not match, return a match but consume no input, otherwise return a non-match
            return !subExprMemo.matches() ? Memo.matchedEmpty(this, startIdx) : Memo.DID_NOT_MATCH;
        }
    }

    public static Expr NotFollowedBy(Expr expr) {
        return new NotFollowedBy(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static Expr Optional(Expr expr) {
        return FirstMatch(expr, Nothing());
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static class Drop extends Expr {
        private Drop(Expr expr) {
            addSubExpr(expr);
            setStringRep("Drop(" + expr + ")");
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            Memo subExprMemo = parser.matchMemoized(subExprs.get(0), startIdx);
            // If the subexpr matches, return a single drop node spanning the same input subsequence
            return subExprMemo.matches() ? Memo.matched(this, startIdx, subExprMemo.endIdx) : Memo.DID_NOT_MATCH;
        }
    }

    /** If the expression and any subexpressions match, insert a single Drop node into the parse tree. */
    public static Expr Drop(Expr expr) {
        return new Drop(expr);
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static class Lift extends Expr {
        private Lift(Expr expr) {
            addSubExpr(expr);
            setStringRep("Lift(" + expr + ")");
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            Memo subExprMemo = parser.matchMemoized(subExprs.get(0), startIdx);
            return subExprMemo.matches() ? Memo.matched(this, subExprMemo) : Memo.DID_NOT_MATCH;
        }
    }

    /** If the expression and any subexpressions match, lift subexpressions to the level of the parent in the AST. */
    public static Expr Lift(Expr expr) {
        return new Lift(expr);
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

    public static abstract class Char extends Expr {
        public abstract boolean matches(char c);

        @Override
        public Memo match(Parser parser, int startIdx) {
            if (startIdx >= parser.input.length()) {
                return Memo.DID_NOT_MATCH;
            }
            if (this.matches(parser.input.charAt(startIdx))) {
                return Memo.matchedTerminal(this, startIdx);
            } else {
                return Memo.DID_NOT_MATCH;
            }
        }
    }

    public static Expr Char(char chr) {
        return new Char() {
            {
                setStringRep("[" + chrToString(chr) + "]");
            }

            @Override
            public boolean matches(char c) {
                return c == chr;
            }
        };
    }

    public static Expr Letter() {
        return new Char() {
            {
                setStringRep("[a-zA-Z]");
            }

            @Override
            public boolean matches(char c) {
                return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            }
        };
    }

    public static Expr Digit() {
        return new Char() {
            {
                setStringRep("[0-9]");
            }

            @Override
            public boolean matches(char c) {
                return c >= '0' && c <= '9';
            }
        };
    }

    public static Expr Char(char lowInclusive, char highInclusive) {
        return new Char() {
            {
                setStringRep("[" + chrToString(lowInclusive) + "-" + chrToString(highInclusive) + "]");
            }

            @Override
            public boolean matches(char c) {
                return c >= lowInclusive && c <= highInclusive;
            }
        };
    }

    /** Match any Unicode whitespace character */
    public static Expr Whitespace() {
        return new Char() {
            {
                setStringRep("Whitespace()");
            }

            @Override
            public boolean matches(char c) {
                return IS_UNICODE_WHITESPACE.get(c);
            }
        };
    }

    /** Match any Unicode whitespace character except for '\n' and '\r' */
    public static Expr NonNewlineWhitespace() {
        return new Char() {
            {
                setStringRep("NonNewlineWhitespace()");
            }

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

    public static class Rule extends Expr {
        String name;

        private Rule(String name) {
            this.name = name;
            setStringRep(name);
        }

        @Override
        public Memo match(Parser parser, int startIdx) {
            // Rule references were already resolved, this will never get called
            throw new RuntimeException("Undefined");
        }
    }

    public static Expr Rule(String name) {
        return new Rule(name);
    }
}
