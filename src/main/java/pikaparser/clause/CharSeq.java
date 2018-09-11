package pikaparser.clause;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class CharSeq extends Clause {

    private final String str;
    private final boolean ignoreCase;

    public CharSeq(String str, boolean ignoreCase) {
        super();
        this.str = str;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        return new Memo(memoRef,
                memoRef.startPos >= input.length()
                        || !input.regionMatches(ignoreCase, memoRef.startPos, str, 0, str.length()) ? -1
                                : str.length());
    }

    @Override
    public boolean isFirstOfRun(String input, int startPos) {
        return startPos < str.length()
                || !input.regionMatches(ignoreCase, startPos - str.length(), str, 0, str.length());
    }

    @Override
    protected int minMatchLen() {
        return str.length();
    }

    @Override
    public String toStr() {
        var buf = new StringBuilder();
        buf.append('"');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 32 && c <= 126) {
                buf.append(c);
            } else {
                switch (c) {
                case '\t':
                    buf.append("\\t");
                    break;
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\b':
                    buf.append("\\b");
                    break;
                case '\f':
                    buf.append("\\f");
                    break;
                case '\'':
                    buf.append("\\'");
                    break;
                case '\"':
                    buf.append("\\\"");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                default:
                    buf.append("\\u" + String.format("%04x", (int) c));
                }
            }
        }
        buf.append('"');
        return buf.toString();
    }
}
