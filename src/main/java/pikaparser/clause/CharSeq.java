package pikaparser.clause;

import java.util.Collections;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;
import pikaparser.parser.Parser;

public class CharSeq extends Clause {

    public final String str;
    public final boolean ignoreCase;

    public CharSeq(String str, boolean ignoreCase) {
        super();
        this.str = str;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public Match extendParsingContext(Parser parser, MemoEntry parentMemoEntryUnused,
            ParsingContext prevSubClauseParsingContextUnused, int startPos, Set<MemoEntry> visited) {
        return getCurrBestMatch(parser, prevSubClauseParsingContextUnused, startPos, visited);
    }

    @Override
    public Match getCurrBestMatch(Parser parser, ParsingContext prevSubClauseParsingContextUnused, int startPos,
            Set<MemoEntry> visited) {
        boolean match = startPos < parser.input.length() - str.length()
                && parser.input.regionMatches(ignoreCase, startPos, str, 0, str.length());
        return match ? new Match(this, startPos, str.length(), /* subClauseMatches = */ Collections.emptyList(),
                /* firstMatchingSubClauseIdx = */ 0) : null;
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
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
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
