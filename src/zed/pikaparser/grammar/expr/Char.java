package zed.pikaparser.grammar.expr;

import zed.pikaparser.parser.DirectionalParser;
import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public abstract class Char extends Expr {
    public Char(String stringRep) {
        super(stringRep);
    }

    public abstract boolean matches(char c);

    @Override
    public Memo match(DirectionalParser parser, MemoRef ref) {
        if (ref.pos >= parser.memoTable.input.length()) {
            return Memo.DID_NOT_MATCH(ref);
        }
        if (this.matches(parser.memoTable.input.charAt(ref.pos))) {
            return Memo.matchedChar(ref);
        } else {
            return Memo.DID_NOT_MATCH(ref);
        }
    }
}