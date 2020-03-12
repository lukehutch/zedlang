package zed.pikaparser.grammar.expr;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public class Nothing extends Expr {
    public Nothing() {
        super("Nothing()");
    }

    @Override
    public Memo match(DirectionalParser parser, MemoRef ref) {
        // Successfully match, consuming no input.
        // (Nothing never gets memoized, it simply matches every time.)
        return Memo.matchedEmpty(ref);
    }
}