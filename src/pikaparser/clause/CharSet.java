package pikaparser.clause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import pikaparser.memo.Memo;
import pikaparser.memo.MemoRef;

public class CharSet extends Clause {

    private final Set<Character> charSet = new HashSet<>();

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

    @Override
    public Memo match(String input, MemoRef memoRef) {
        return new Memo(memoRef,
                memoRef.startPos >= input.length() || !charSet.contains(input.charAt(memoRef.startPos)) ? -1 : 1);
    }

    @Override
    public boolean isFirstOfRun(String input, int startPos) {
        return startPos == 0 || !charSet.contains(input.charAt(startPos - 1));
    }

    @Override
    public String toStr() {
        var buf = new StringBuilder();
        var charsSorted = new ArrayList<>(charSet);
        Collections.sort(charsSorted);
        for (int i = 0; i < charsSorted.size(); i++) {
            char c = charsSorted.get(i);
            if (c >= 32 && c <= 126) {
                buf.append(c);
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
        return buf.length() == 1 && buf.charAt(0) >= 32 && buf.charAt(0) <= 126 ? "'" + buf + "'" : "[" + buf + "]";
    }
}
