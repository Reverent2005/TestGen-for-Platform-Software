package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.AstHelper;
import in.ac.iiitb.plproject.ast.Expr;
import in.ac.iiitb.plproject.ast.MethodCallExpr;

/**
 * A visitor class that walks the AtcClass IR tree and generates the final Java code string.
 */
public class AtcIrCodeGenerator {

    private StringBuilder stringBuilder;
    private static final String INDENT = "    "; // 4 spaces for indentation

    public AtcIrCodeGenerator() {
        this.stringBuilder = new StringBuilder();
    }

    public String generateJavaFile(AtcClass atc) {
        stringBuilder = new StringBuilder(); // Reset for each generation

        // Package declaration
        stringBuilder.append("package ").append(atc.getPackageName()).append(";\n\n");

        // Imports
        for (String anImport : atc.getImports()) {
            stringBuilder.append("import ").append(anImport).append(";\n");
        }
        stringBuilder.append("\n");

        // Class annotations (e.g., @RunWith)
        if (atc.getRunWithAnnotationClass() != null && !atc.getRunWithAnnotationClass().isEmpty()) {
            stringBuilder.append("@RunWith(").append(atc.getRunWithAnnotationClass()).append(")\n");
        }
        
        // Class header
        stringBuilder.append("public class ").append(atc.getClassName()).append(" {\n");

        // Test methods
        for (AtcTestMethod method : atc.getTestMethods()) {
            visit(method);
        }

        // Generate the main method
        generateMainMethod(atc);

        // Class closing brace
        stringBuilder.append("}\n");

        return stringBuilder.toString();
    }

    private void visit(AtcTestMethod method) {
        stringBuilder.append("\n");
        // Method annotations (e.g., @Test)
        if (method.isTestAnnotated()) {
            stringBuilder.append(INDENT).append("@Test\n");
        }
        // Method header
        stringBuilder.append(INDENT).append("public void ").append(method.getMethodName()).append("() {\n");

        // Statements in method body
        for (AtcStatement stmt : method.getStatements()) {
            // Polymorphic call to visit specific statement types
            if (stmt instanceof AtcSymbolicVarDecl) {
                visit((AtcSymbolicVarDecl) stmt);
            } else if (stmt instanceof AtcVarDecl) {
                visit((AtcVarDecl) stmt);
            } else if (stmt instanceof AtcAssumeStmt) {
                visit((AtcAssumeStmt) stmt);
            } else if (stmt instanceof AtcMethodCallStmt) {
                visit((AtcMethodCallStmt) stmt);
            } else if (stmt instanceof AtcAssertStmt) {
                visit((AtcAssertStmt) stmt);
            }
            // Add more else if for other AtcStatement types if needed
        }

        // Method closing brace
        stringBuilder.append(INDENT).append("}\n");
    }

    private void visit(AtcSymbolicVarDecl stmt) {
        stringBuilder.append(INDENT).append(INDENT)
                     .append(stmt.getTypeName()).append(" ").append(stmt.getVarName())
                     .append(" = Debug.makeSymbolic").append(capitalize(stmt.getTypeName()))
                     .append("(\"").append(stmt.getVarName()).append("\");\n");
    }

    private void visit(AtcVarDecl stmt) {
        String initCode = AstHelper.exprToJavaCode(stmt.getInitExpr());
        stringBuilder.append(INDENT).append(INDENT)
                     .append(stmt.getTypeName()).append(" ").append(stmt.getVarName())
                     .append(" = ").append(initCode).append(";\n");
    }

    private void visit(AtcAssumeStmt stmt) {
        String condCode = AstHelper.exprToJavaCode(stmt.getCondition());
        stringBuilder.append(INDENT).append(INDENT)
                     .append("Debug.assume(").append(condCode).append(");\n");
    }

    private void visit(AtcMethodCallStmt stmt) {
        String callCode = AstHelper.exprToJavaCode(stmt.getCallExpr());
        stringBuilder.append(INDENT).append(INDENT)
                     .append(callCode).append(";\n");
    }

    private void visit(AtcAssertStmt stmt) {
        String condCode = AstHelper.exprToJavaCode(stmt.getCondition());
        stringBuilder.append(INDENT).append(INDENT)
                     .append("assert(").append(condCode).append(");\n");
    }

    // Helper to capitalize the first letter for Debug.makeSymbolicX methods
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void generateMainMethod(AtcClass atc) {
        stringBuilder.append("\n");
        stringBuilder.append(INDENT).append("public static void main(String[] args) {\n");

        // Instantiate the generated class to call non-static test methods
        stringBuilder.append(INDENT).append(INDENT)
                     .append(atc.getClassName()).append(" instance = new ").append(atc.getClassName()).append("();\n");

        // Initialize Capture object
        stringBuilder.append(INDENT).append(INDENT).append("Capture C = new Capture();\n");

        for (AtcTestMethod method : atc.getTestMethods()) {
            stringBuilder.append(INDENT).append(INDENT)
                         .append("instance.").append(method.getMethodName()).append("();\n");
        }
        stringBuilder.append(INDENT).append("}\n");
    }
}
