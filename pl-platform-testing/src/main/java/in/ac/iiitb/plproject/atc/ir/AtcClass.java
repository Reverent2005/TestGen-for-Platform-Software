package in.ac.iiitb.plproject.atc.ir;

import java.util.List;

/**
 * Represents the entire test file (e.g., "GeneratedATCs.java").
 * This is the root of your new IR.
 */
public class AtcClass {
    public String packageName;
    public String className;
    public List<String> imports;
    public List<AtcTestMethod> testMethods;
    public String runWithAnnotationClass; // e.g., "gov.nasa.jpf.symbc.SymbolicClasspathRunner.class"

    public AtcClass(String packageName, String className, List<String> imports, List<AtcTestMethod> testMethods, String runWithAnnotationClass) {
        this.packageName = packageName;
        this.className = className;
        this.imports = imports;
        this.testMethods = testMethods;
        this.runWithAnnotationClass = runWithAnnotationClass;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<AtcTestMethod> getTestMethods() {
        return testMethods;
    }

    public String getRunWithAnnotationClass() {
        return runWithAnnotationClass;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AtcClass(");
        sb.append("package: ").append(packageName).append(", ");
        sb.append("class: ").append(className).append(", ");
        sb.append("imports: ").append(imports).append(", ");
        sb.append("testMethods: ").append(testMethods.size()).append(" methods, ");
        if (runWithAnnotationClass != null && !runWithAnnotationClass.isEmpty()) {
            sb.append("@RunWith: ").append(runWithAnnotationClass);
        }
        sb.append(")");
        return sb.toString();
    }
}
