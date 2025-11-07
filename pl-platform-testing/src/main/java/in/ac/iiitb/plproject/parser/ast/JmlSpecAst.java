package in.ac.iiitb.plproject.parser.ast;

import java.util.List;

public class JmlSpecAst {
    private List<JmlFunctionSpec> specs;

    public JmlSpecAst(List<JmlFunctionSpec> specs) {
        this.specs = specs;
    }

    public JmlFunctionSpec findSpecFor(String functionName) {
        // Placeholder implementation
        for (JmlFunctionSpec spec : specs) {
            if (spec.getName().equals(functionName)) {
                return spec;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JmlSpecAst[");
        if (specs != null) {
            for (int i = 0; i < specs.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(specs.get(i));
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
