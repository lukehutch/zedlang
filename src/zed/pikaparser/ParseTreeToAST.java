package zed.pikaparser;

import java.util.ArrayList;

import zed.pikaparser.grammar.action.Drop;
import zed.pikaparser.grammar.action.Lift;
import zed.pikaparser.grammar.expr.Nothing;
import zed.pikaparser.parser.memo.Memo;

public class ParseTreeToAST {

    public static class ASTNode {
    }

    //    public static class ExprNode extends ASTNode {
    //    }
    //
    //    public static class BinOp extends ExprNode {
    //        char op;
    //        ExprNode l;
    //        ExprNode r;
    //
    //        public BinOp(char op, ExprNode l, ExprNode r) {
    //            this.op = op;
    //            this.l = l;
    //            this.r = r;
    //        }
    //        
    //        @Override
    //        public String toString() {
    //            return "(" + l + " " + op + " " + r + ")";
    //        }
    //    }
    //
    //    public static class Number extends ExprNode {
    //        int num;
    //
    //        public Number(int num) {
    //            this.num = num;
    //        }
    //
    //        @Override
    //        public String toString() {
    //            return Integer.toString(num);
    //        }
    //    }

    public static class IdentNode extends ASTNode {
        String ident;

        public IdentNode(String ident) {
            this.ident = ident;
        }

        @Override
        public String toString() {
            return ident;
        }
    }

    public static class GenericASTNode extends ASTNode {
        ArrayList<ASTNode> children;

        public GenericASTNode(ArrayList<ASTNode> children) {
            this.children = children;
        }

        @Override
        public String toString() {
            return children.toString();
        }
    }

    public static class RuleASTNode extends ASTNode {
        ASTNode ident;
        ASTNode x;

        public RuleASTNode(ASTNode ident, ASTNode x) {
            this.ident = ident;
            this.x = x;
        }

        @Override
        public String toString() {
            return ident + "::=" + x;
        }
    }

    public static class LiftASTNode extends ASTNode {
        ArrayList<ASTNode> children;

        public LiftASTNode(ArrayList<ASTNode> children) {
            this.children = children;
        }

        @Override
        public String toString() {
            return "Lift(" + children.toString() + ")";
        }
    }

    public static ASTNode convertToAST(Memo memo, String input) {
        if (!memo.matches()) {
            return null;
        }
        if (memo.isRule("Ident") || memo.isRule("Match")) {
            return new IdentNode(input.substring(memo.ref.pos, memo.endPos));

        } else if (memo.ref.expr instanceof Lift) {
            // TODO: make 2 passes: one for generic steps (Lift/Drop), second pass for grammar-specific things
            // TODO: also add support for Span

            // For any Lift nodes in the parse tree, traverse down the parse tree to the first sequence of 2 or more
            // nodes, then convert those nodes to AST nodes, and return a LiftASTNode that will be lifted by its parent.
            Memo curr = memo;
            while (curr != null) {
                if (curr.subExprMemos.size() == 0) {
                    return null;
                } else if (curr.subExprMemos.size() == 1) {
                    curr = curr.subExprMemos.get(0);
                } else {
                    break;
                }
            }
            ArrayList<ASTNode> childASTNodes = new ArrayList<>();
            for (Memo childMemo : curr.subExprMemos) {
                childASTNodes.add(convertToAST(childMemo, input));
            }
            return new LiftASTNode(childASTNodes);

        } else if (memo.ref.expr instanceof Drop || memo.ref.expr instanceof Nothing) {
            return null;
        }
        ArrayList<ASTNode> subExprASTNodes = new ArrayList<>();
        for (Memo subExprMemo : memo.subExprMemos) {
            ASTNode subExprASTNode = convertToAST(subExprMemo, input);
            if (subExprASTNode != null) {
                if (subExprASTNode instanceof LiftASTNode) {
                    subExprASTNodes.addAll(((LiftASTNode) subExprASTNode).children);
                } else {
                    subExprASTNodes.add(subExprASTNode);
                }
            }
        }
        if (memo.isRule("Rule")) {
            if (subExprASTNodes.size() != 2) {
                throw new RuntimeException("Wrong num of subnodes");
            }
            return new RuleASTNode(subExprASTNodes.get(0), subExprASTNodes.get(1));
        }

        if (subExprASTNodes.size() == 0) {
            return null;
        } else if (subExprASTNodes.size() == 1) {
            // TODO: don't strip out levels if the parse tree nodes have rule names?
            return subExprASTNodes.get(0);
        } else {
            return new GenericASTNode(subExprASTNodes);
        }
    }

}
