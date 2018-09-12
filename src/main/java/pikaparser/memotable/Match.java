package pikaparser.memotable;

import java.util.List;

import pikaparser.clause.Clause;

/** A complete match of a {@link Clause} at a given start position. */  
public class Match {

    /** The matching {@link Clause}. */
    Clause clause;
    
    /** The start position of the match. */
    int startPos;
    
    /** The length of the match. */
    int len;
    
    /** The subclause matches. */ 
    List<Match> subClauseMatches;
    
}
