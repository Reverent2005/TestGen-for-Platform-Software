package in.ac.iiitb.plproject.ast;

/**
 * Public base class for names (identifiers).
 * This allows other packages to reference Name types.
 */
public class Name extends Node {
    public final String identifier;
    
    public Name(String identifier) { 
        this.identifier = identifier; 
    }
    
    @Override 
    public String toString() { 
        return identifier; 
    }
}

