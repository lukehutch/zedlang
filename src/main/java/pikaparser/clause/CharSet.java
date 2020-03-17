package pikaparser.clause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class CharSet extends Terminal {

    public final Set<Character> charSet = new HashSet<>();

    public boolean invertMatch = false;

    public CharSet(char c) {
        super();
        this.charSet.add(c);
    }

    public CharSet(String chars) {
        super();
        for (int i = 0; i < chars.length(); i++) {
            this.charSet.add(chars.charAt(i));
        }
    }

    public CharSet(char[] chars) {
        super();
        for (int i = 0; i < chars.length; i++) {
            this.charSet.add(chars[i]);
        }
    }

    public CharSet(char minChar, char maxChar) {
        super();
        for (char c = minChar; c <= maxChar; c++) {
            this.charSet.add(c);
        }
    }

    public CharSet(CharSet... charSets) {
        super();
        for (CharSet charSet : charSets) {
            this.charSet.addAll(charSet.charSet);
        }
    }

    public CharSet(Collection<CharSet> charSets) {
        super();
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
    public Match match(MemoTable memoTable, MemoKey memoKey, String input, Set<MemoEntry> newMatchMemoEntries) {
        if (memoKey.startPos < input.length() && (invertMatch ^ charSet.contains(input.charAt(memoKey.startPos)))) {
            // Because terminals are matched top-down, don't call MemoTable.addMatch for terminals
            return new Match(memoKey, /* firstMatchingSubClauseIdx = */ 0, /* len = */ 1,
                    Match.NO_SUBCLAUSE_MATCHES);
        }
        return null;
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
            toStringCached = (ruleNodeLabel != null ? ruleNodeLabel + ':' : "") //
                    + ((!invertMatch && s.length() == 1 && s.charAt(0) >= 32 && s.charAt(0) <= 126) //
                            ? "'" + s + "'" //
                            : "[" + s + "]");
        }
        return toStringCached;
    }
}
