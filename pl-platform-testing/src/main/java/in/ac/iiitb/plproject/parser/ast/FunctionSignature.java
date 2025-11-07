package in.ac.iiitb.plproject.parser.ast;

// Note: Type is in ast package but package-private
// For now, we'll use Object or create a wrapper
// TODO: When JML parser is implemented, it should provide its own type representation
import java.util.List;

/**
 * Represents a function signature (name, parameters, return type).
 */
public class FunctionSignature {
    private String name;
    private List<Variable> parameters;
    private String returnTypeName; // Using String for now, can be converted to Type when needed

    public FunctionSignature(String name, List<Variable> parameters, String returnTypeName) {
        this.name = name;
        this.parameters = parameters;
        this.returnTypeName = returnTypeName;
    }

    public String getName() {
        return name;
    }

    public List<Variable> getParameters() {
        return parameters;
    }

    public String getReturnTypeName() {
        return returnTypeName;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FunctionSignature(");
        sb.append("name: ").append(name);
        if (parameters != null && !parameters.isEmpty()) {
            sb.append(", params: [");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(parameters.get(i));
            }
            sb.append("]");
        }
        sb.append(", returnType: ").append(returnTypeName);
        sb.append(")");
        return sb.toString();
    }
}
