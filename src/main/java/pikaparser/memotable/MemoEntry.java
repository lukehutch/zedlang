package pikaparser.memotable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import pikaparser.clause.Clause;

/** A memo entry for a specific {@link Clause} at a specific start position. */
public class MemoEntry {

    /** The {@link Clause}. */
    Clause clause;
    
    /** The start position. */
    int startPos;

    /** The {@link ParsingContext} for partial matches that referenced this clause at this position. */
    Queue<ParsingContext> parsingContexts = new ConcurrentLinkedQueue<>();

    /** The current best {@link Match} for this {@link Clause} at this start position. */
    Match bestMatch;
    
    /** New {@link Match} objects to be compared to the current {@link #bestMatch}. */ 
    Queue<Match> newMatches = new ConcurrentLinkedQueue<>();
    
    // TODO: Need a concurrent Set<MemoEntry> in the Parser for any MemoEntry objects that have had newMatches added 
}
