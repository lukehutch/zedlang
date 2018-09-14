package pikaparser.clause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.ParsingContext;

public class CharSet extends Clause {

    protected final Set<Character> charSet = new HashSet<>();

    protected boolean invertMatch = false;

    private CharSet() {
        super(new Clause[0]);
    }

    public CharSet(char c) {
        super(new Clause[0]);
        this.charSet.add(c);
    }

    public CharSet(String chars) {
        super(new Clause[0]);
        for (int i = 0; i < chars.length(); i++) {
            this.charSet.add(chars.charAt(i));
        }
    }

    public CharSet(char[] chars) {
        super(new Clause[0]);
        for (int i = 0; i < chars.length; i++) {
            this.charSet.add(chars[i]);
        }
    }

    public CharSet(char minChar, char maxChar) {
        super(new Clause[0]);
        for (char c = minChar; c <= maxChar; c++) {
            this.charSet.add(c);
        }
    }

    public CharSet(CharSet... charSets) {
        super(new Clause[0]);
        for (CharSet charSet : charSets) {
            this.charSet.addAll(charSet.charSet);
        }
    }

    public CharSet(Collection<CharSet> charSets) {
        super(new Clause[0]);
        for (CharSet charSet : charSets) {
            this.charSet.addAll(charSet.charSet);
        }
    }

    /** Invert in-place, and return this. */
    public CharSet invert() {
        invertMatch = !invertMatch;
        return this;
    }

    @Override
    public Match extendParsingContext(String input, MemoEntry parentMemoEntryUnused,
            ParsingContext prevSubClauseParsingContextUnused, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContexts) {
        return getCurrBestMatch(input, prevSubClauseParsingContextUnused, startPos, memoEntriesWithNewParsingContexts);
    }

    @Override
    public Match getCurrBestMatch(String input, ParsingContext prevSubClauseParsingContextUnused, int startPos,
            Set<MemoEntry> memoEntriesWithNewParsingContextsUnused) {
        boolean match = startPos < input.length() && charSet.contains(input.charAt(startPos));
        return match ? new Match(this, startPos, /* len = */ 1, /* subClauseMatches = */ Collections.emptyList(),
                /* firstMatchingSubClauseIdx = */ 0) : null;
    }

    // TODO: fix the escaping
    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            if (invertMatch) {
                buf.append('^');
            }
            var charsSorted = new ArrayList<>(charSet);
            Collections.sort(charsSorted);
            for (int i = 0; i < charsSorted.size(); i++) {
                char c = charsSorted.get(i);
                if (c >= 32 && c <= 126) {
                    if (c == '^' && i == 0 && charSet.size() > 1) {
                        // Escape '^' at beginning of non-inverted character set range
                        buf.append('\\');
                    } else if (c == ']' && charSet.size() > 1) {
                        // Escape ']' within char range
                        buf.append('\\');
                    } else if (c == '\\') {
                        buf.append("\\\\");
                    } else {
                        buf.append(c);
                    }
                } else if (c == '\n') {
                    buf.append("\\n");
                } else if (c == '\r') {
                    buf.append("\\r");
                } else if (c == '\t') {
                    buf.append("\\t");
                } else {
                    buf.append("\\u" + String.format("%04x", (int) c));
                }
                int j = i + 1;
                while (j < charsSorted.size() && charsSorted.get(j).charValue() == c + (j - i)) {
                    j++;
                }
                if (j > i + 2) {
                    buf.append("-");
                    i = j - 1;
                    buf.append(charsSorted.get(i));
                }
            }
            String s = buf.toString();
            toStringCached = (!invertMatch && s.length() == 1 && s.charAt(0) >= 32 && s.charAt(0) <= 126) //
                    ? "'" + s + "'" //
                    : "[" + s + "]";
        }
        return toStringCached;
    }
}
