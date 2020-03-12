package zed.pikaparser.grammar.expr;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class Rule extends Expr {
    public String name;

    public Rule(String name) {
        super(name);
        this.name = name;
    }

    @Override
    public Memo match(DirectionalParser parser, MemoRef ref) {
        // Rule references were already resolved, this will never get called
        throw new RuntimeException("Undefined");
    }
}