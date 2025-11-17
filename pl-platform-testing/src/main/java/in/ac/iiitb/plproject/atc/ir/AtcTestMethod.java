package in.ac.iiitb.plproject.atc.ir;

import java.util.List;

/**
 * Represents a single test method (e.g., "test_increment_0").
 */
public class AtcTestMethod {
    public String methodName;
    public List<AtcStatement> statements;
    public boolean isTestAnnotated = true; // All generated methods are @Test

    public AtcTestMethod(String methodName, List<AtcStatement> statements) {
        this.methodName = methodName;
        this.statements = statements;
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
}
