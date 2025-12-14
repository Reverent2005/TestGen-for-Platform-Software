package in.ac.iiitb.plproject.ast;

/**
 * Public class for simple names (identifiers).
 * This allows other packages to reference SimpleName types.
 */
public class SimpleName extends Name {
    public SimpleName(String identifier) { 
        super(identifier); 
    }
}

