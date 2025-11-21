package in.ac.iiitb.plproject.atc.ir;

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
