package pikaparser.parser;

import java.util.ArrayList;
import java.util.List;

import pikaparser.clause.Clause;

public class ASTNode {

    public final String astLabel;
    public final int startPos;
    public final int len;
    public final List<ASTNode> children;
    public final Clause nodeType;

    public ASTNode(String nodeName, Clause nodeType, int startPos, int len) {
        this.astLabel = nodeName;
        this.nodeType = nodeType;
        this.startPos = startPos;
        this.len = len;
        this.children = new ArrayList<>();
    }

    public void addChild(ASTNode child) {
        children.add(child);
    }

    public void printTree(String input, String indentStr, boolean isLastChild) {
        int inpLen = 80;
        String inp = input.substring(startPos, Math.min(input.length(), startPos + Math.min(len, inpLen)));
        if (inp.length() == inpLen) {
            inp += "...";
        }
        inp = inp.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
        System.out.println(indentStr + "|   ");
        System.out.println(indentStr + "+-- " + astLabel + " " + startPos + "+" + len + " : \"" + inp + "\"");
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                var subClauseMatch = children.get(i);
                subClauseMatch.printTree(input, indentStr + (isLastChild ? "    " : "|   "),
                        i == children.size() - 1);
            }
        }
    }

    public void printTree(String input) {
        printTree(input, "", true);
    }

    private void getAllDescendantsNamed(String name, List<ASTNode> termsOut) {
        if (astLabel.equals(name)) {
            termsOut.add(this);
        } else {
            for (ASTNode child : children) {
                child.getAllDescendantsNamed(name, termsOut);
            }
        }
    }

    public List<ASTNode> getAllDescendantsNamed(String name) {
        List<ASTNode> terms = new ArrayList<>();
        getAllDescendantsNamed(name, terms);
        return terms;
    }

    public ASTNode getFirstDescendantNamed(String name) {
        if (astLabel.equals(name)) {
            return this;
        } else {
            for (ASTNode child : children) {
                return child.getFirstDescendantNamed(name);
            }
        }
        throw new IllegalArgumentException("Node not found: \"" + name + "\"");
    }

    public ASTNode getOnlyChild() {
        if (children.size() != 1) {
            throw new IllegalArgumentException("Expected one child, got " + children.size());
        }
        return children.get(0);
    }

    public ASTNode getFirstChild() {
        if (children.size() < 1) {
            throw new IllegalArgumentException("No first child");
        }
        return children.get(0);
    }

    public ASTNode getSecondChild() {
        if (children.size() < 2) {
            throw new IllegalArgumentException("No second child");
        }
        return children.get(1);
    }

    public ASTNode getChild(int i) {
        return children.get(i);
    }

    public String getText(String input) {
        return input.substring(startPos, startPos + len);
    }
}
