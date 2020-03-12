package zed.parser;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class Expr {

    private String stringRep;

    protected int exprIdx;

    /** The name(s) of the rule name(s) for this expression. */
    private HashSet<String> ruleNames = new HashSet<>();

    protected ArrayList<Expr> subExprs = new ArrayList<>();

    protected void setStringRep(String stringRep) {
        this.stringRep = stringRep;
    }

    public String getStringRep() {
        return stringRep;
    }

    @Override
    public String toString() {
        return stringRep;
    }

    public String format() {
        StringBuilder buf = new StringBuilder();
        buf.append(exprIdx);
        if (!ruleNames.isEmpty()) {
            buf.append(" [");
            boolean first = true;
            for (String ruleName : ruleNames) {
                if (first) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(ruleName);
            }
            buf.append(']');
        }
        buf.append(":\t");
        buf.append(stringRep);
        return buf.toString();
    }

    protected void addSubExpr(Expr subExpr) {
        subExprs.add(subExpr);
    }

    protected void addSubExprs(Expr... subExprs) {
        for (Expr expr : subExprs) {
            this.subExprs.add(expr);
        }
    }

    public ArrayList<Expr> getSubExprs() {
        return subExprs;
    }

    public Expr getFirstSubExpr() {
        return subExprs.get(0);
    }

    public void addRuleName(String ruleName) {
        this.ruleNames.add(ruleName);
    }

    public HashSet<String> getRuleNames() {
        return ruleNames;
    }

    public void setIdx(int idx) {
        this.exprIdx = idx;
    }

    public int getIdx() {
        return exprIdx;
    }

    public abstract Memo match(Parser parser, int startIdx);
}
