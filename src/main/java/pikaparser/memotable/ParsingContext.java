package pikaparser.memotable;

import pikaparser.clause.Clause;

/** The context for an incomplete match that referenced a given memo entry. */
public class ParsingContext {

    /** Reference to parent MemoEntry */
    Clause parentClause;

    /** Reference to prev sibling {@link ParsingContext}, or null if none. */
    ParsingContext prevSiblingContext;
    
    /** The index of the previous sibling within parent subclauses. */
    int prevSiblingSubClauseIdx;

    /** The startPos of the previous sibling. */
    int prevSiblingStartPos;
}
