package zed.pikaparser.parser;

import zed.pikaparser.parser.memo.Memo;
import zed.pikaparser.parser.memo.MemoTable;
import zed.pikaparser.parser.memo.MemoTable.MemoRef;

public abstract class DirectionalParser {
    public MemoTable memoTable;

    public DirectionalParser(MemoTable memoTable) {
        this.memoTable = memoTable;
    }

    public abstract void parse();

    public abstract Memo matchMemoized(MemoRef superExprRef, MemoRef subExprRef);
}