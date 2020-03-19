package pikaparser.clause;

import java.util.Collection;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class CharSeq extends Terminal {

    public final String str;
    public final boolean ignoreCase;

    CharSeq(String str, boolean ignoreCase) {
        super();
        this.str = str;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Collection<MemoEntry> updatedEntries) {
        // Terminals are always matched top-down
        if (memoKey.startPos < input.length() - str.length()
                && input.regionMatches(ignoreCase, memoKey.startPos, str, 0, str.length())) {
            // Don't call MemoTable.addMatch for terminals, to limit size of memo table
            memoTable.numMatchObjectsCreated.incrementAndGet();
            return new Match(memoKey, /* firstMatchingSubClauseIdx = */ 0, str.length(),
                    Match.NO_SUBCLAUSE_MATCHES);
        }
        return null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            appendRulePrefix(buf);
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
            appendRuleSuffix(buf);
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
