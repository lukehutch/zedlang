package pikaparser.clause;

public class OneOrMoreSuffix extends OneOrMore {

    public OneOrMoreSuffix(Clause subClause) {
        super(subClause, /* suffixMatch = */ true);
    }

    @Override
    public String toStr() {
        // Special notification for "suffix match"
        return subClauses[0] + "++";
    }
}
