package pikaparser.clause;

import java.util.Collection;

import pikaparser.memotable.Match;
import pikaparser.memotable.MemoEntry;
import pikaparser.memotable.MemoKey;
import pikaparser.memotable.MemoTable;

public class CreateASTNode extends Clause {
    CreateASTNode(String astNodeLabel, Clause clause) {
        super(new Clause[] { clause });
        this.ruleASTNodeLabel = astNodeLabel;
    }

    @Override
    public Match match(MatchDirection matchDirection, MemoTable memoTable, MemoKey memoKey, String input,
            Collection<MemoEntry> updatedEntries) {
        throw new IllegalArgumentException(getClass().getSimpleName() + " node should not be in final grammar");
    }

    @Override
    public String toString() {
        if (toStringCached == null) {
            var buf = new StringBuilder();
            appendRulePrefix(buf);
            buf.append("(" + getClass().getSimpleName() + " " + ruleASTNodeLabel + ":" + subClauses[0] + ")");
            appendRuleSuffix(buf);
            toStringCached = buf.toString();
        }
        return toStringCached;
    }
}
