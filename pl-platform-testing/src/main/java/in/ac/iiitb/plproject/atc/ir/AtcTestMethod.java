package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.parser.ast.Variable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single test method (e.g., "test_increment_0").
 */
public class AtcTestMethod {
    public String methodName;
    public List<AtcStatement> statements;
    public boolean isTestAnnotated = true; // All generated methods are @Test
    public boolean isStatic = false;
    public boolean isMain = false;

    /**
     * Declared return type of the helper.  "void" (or null) when the block has no
     * \result binding.  A block that captures a SERVER_OUTPUT returns that value
     * so main() can thread it into the blocks that consume it.
     */
    private String returnType = "void";

    /**
     * Formal parameters of the helper.  Per Step D these are ONLY the propagated
     * vars — values that arrived from an earlier block's SERVER_OUTPUT.  Freshly
     * declared CLIENT_INPUTs stay local to the body, so a no-parameter function
     * such as pop()/peek() yields an empty parameter list even though it returns
     * a value (Section 5, invariant 1).
     */
    private List<Variable> parameters = new ArrayList<Variable>();

    public AtcTestMethod(String methodName, List<AtcStatement> statements) {
        this.methodName = methodName;
        this.statements = statements;
    }

    public AtcTestMethod(String methodName, List<AtcStatement> statements, boolean isStatic, boolean isMain) {
        this.methodName = methodName;
        this.statements = statements;
        this.isStatic = isStatic;
        this.isMain = isMain;
        this.isTestAnnotated = !isMain; // Main method is not @Test annotated
    }

    public AtcTestMethod(String methodName, List<AtcStatement> statements,
                         String returnType, List<Variable> parameters) {
        this.methodName = methodName;
        this.statements = statements;
        this.returnType = (returnType == null || returnType.isEmpty()) ? "void" : returnType;
        this.parameters = (parameters != null) ? new ArrayList<Variable>(parameters)
                                               : new ArrayList<Variable>();
        this.isTestAnnotated = false; // library helpers are driven from main(), not by JUnit discovery
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = (returnType == null || returnType.isEmpty()) ? "void" : returnType;
    }

    public List<Variable> getParameters() {
        return new ArrayList<Variable>(parameters);
    }

    public void setParameters(List<Variable> parameters) {
        this.parameters = (parameters != null) ? new ArrayList<Variable>(parameters)
                                               : new ArrayList<Variable>();
    }

    /** True when this helper hands a captured SERVER_OUTPUT back to its caller. */
    public boolean hasReturnValue() {
        return returnType != null && !returnType.equals("void");
    }

    /**
     * Renders the signature as it must appear in BOTH generated files.
     * Section 5, invariant 5: the SPF and JUnit flavours differ only in the body.
     */
    public String signatureString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType).append(' ').append(methodName).append('(');
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).getTypeName()).append(' ')
              .append(parameters.get(i).getName());
        }
        return sb.append(')').toString();
    }

    public String getMethodName() {
        return methodName;
    }

    public List<AtcStatement> getStatements() {
        return statements;
    }

    public boolean isTestAnnotated() {
        return isTestAnnotated;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isMain() {
        return isMain;
    }
}
