package pikaparser.clause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

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
    protected int minMatchLen() {
        return 1;
    }

    @Override
    public Memo match(String input, MemoRef memoRef) {
        boolean match = memoRef.startPos < input.length() && charSet.contains(input.charAt(memoRef.startPos));
        return new Memo(memoRef, (invertMatch ? !match : match) ? 1 : -1);
    }

    @Override
    public boolean isFirstOfRun(String input, int startPos) {
        boolean match = startPos == 0 || !charSet.contains(input.charAt(startPos - 1));
        return invertMatch ? !match : match;
    }

    // TODO: fix the escaping
    @Override
    public String toStr() {
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
        return (!invertMatch && s.length() == 1 && s.charAt(0) >= 32 && s.charAt(0) <= 126) //
                ? "'" + s + "'" //
                : "[" + s + "]";
    }
}
