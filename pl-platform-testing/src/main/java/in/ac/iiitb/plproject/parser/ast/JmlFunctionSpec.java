package in.ac.iiitb.plproject.parser.ast;

// Note: Expr is package-private in ast package
// We'll use Object for now and cast when needed, or create a wrapper
import java.util.List;

/**
 * Represents a JML function specification with preconditions, postconditions, and function signature.
 * 
 * This is a skeleton - you'll need to implement the methods based on your JML parser output.
 */
public class JmlFunctionSpec {
    private String name;
    private FunctionSignature signature;
    private Object precondition;  // Using AST Expr from new grammar (package-private, so using Object)
    private Object postcondition; // Using AST Expr from new grammar

    public JmlFunctionSpec(String name, FunctionSignature signature, Object precondition, Object postcondition) {
        this.name = name;
        this.signature = signature;
        this.precondition = precondition;
        this.postcondition = postcondition;
    }

    public String getName() {
        return name;
    }

    public FunctionSignature getSignature() {
        return signature;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPrecondition() {
        return (T) precondition;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPostcondition() {
        return (T) postcondition;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JmlFunctionSpec(");
        sb.append("Name: ").append(name);
        if (signature != null) {
            sb.append(", Signature: ").append(signature);
        }
        if (precondition != null) {
            sb.append(", Pre: ").append(precondition);
        } else {
            sb.append(", Pre: (none)");
        }
        if (postcondition != null) {
            sb.append(", Post: ").append(postcondition);
        } else {
            sb.append(", Post: (none)");
        }
        sb.append(")");
        return sb.toString();
    }
}
