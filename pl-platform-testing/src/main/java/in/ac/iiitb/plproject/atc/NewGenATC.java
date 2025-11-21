package in.ac.iiitb.plproject.atc;

import in.ac.iiitb.plproject.parser.ast.*; // JmlFunctionSpec, JmlSpecAst, TestStringAst, Variable, FunctionSignature
import in.ac.iiitb.plproject.ast.AstHelper;
import java.util.*;
import in.ac.iiitb.plproject.atc.ir.*; // Import all IR classes

/**
 * New implementation of GenATC using the new grammar AST classes.
 * * This is where you'll implement your algorithm incrementally.
 * The algorithm should:
 * 1. Take JML specs and test strings
 * 2. Generate Java test code (as a string in JavaFile)
 * 3. Use the AST classes from in.ac.iiitb.plproject.ast for working with expressions
 */
public class NewGenATC implements GenATC {

    @Override
    public AtcClass generateAtcFile(JmlSpecAst jmlSpecAst, TestStringAst testStringAst) {
        // StringBuilder atcFileContent = new StringBuilder(); // Comment out old StringBuilder

        // --- New IR Building Logic ---
        // Simple Java imports (no JPF-specific imports - those will be added by SpfWrapper)
        List<String> imports = new ArrayList<>();
        imports.add("org.junit.Test");
        imports.add("java.util.Set");
        imports.add("java.util.Map");
        imports.add("java.util.HashSet");
        imports.add("java.util.HashMap");
        imports.add("java.util.Arrays"); // Needed for SetExpr conversion to Java code
        // TODO: Add imports for user-defined classes (e.g., com.example.lms.Stack) - need a mechanism to collect these

        List<AtcTestMethod> testMethods = new ArrayList<>();

        List<String> calls = testStringAst.getCalls();
        
        // Use a for-i loop to get an index for unique function names
        for (int i = 0; i < calls.size(); i++) {
            String functionName = calls.get(i);
            
            // Find the corresponding JML spec for this function
            JmlFunctionSpec spec = jmlSpecAst.findSpecFor(functionName);
            
            if (spec != null) {
                // Generate a self-contained test function for this spec
                // Pass the index 'i' to make the function name unique
                AtcTestMethod testMethod = generateTestFunction(spec, i); // Changed to AtcTestMethod
                testMethods.add(testMethod);
            }
        }
        
        String packageName = "in.ac.iiitb.plproject.atc.generated"; // Assuming a default generated package
        String className = "GeneratedATCs";
        String runWithAnnotation = null; // No @RunWith for now, can be added later if needed

        return new AtcClass(packageName, className, imports, testMethods, runWithAnnotation);
    }
  
    private AtcTestMethod generateTestFunction(JmlFunctionSpec spec, int index) {
        // StringBuilder func = new StringBuilder(); // Comment out old StringBuilder
        List<AtcStatement> statements = new ArrayList<>(); // New list to hold IR statements
        FunctionSignature signature = spec.getSignature();
        
        // --- 1. Function Header (handled by AtcTestMethod itself) ---
        // func.append("    @Test\n");
        // func.append("    public void test_").append(spec.getName()).append("_").append(index).append("() {\n");

        // --- 2. Create Symbolic Inputs (Algorithm Step 2) ---
        // func.append("\n        // 1. Create symbolic inputs\n");
        List<Variable> params = signature.getParameters();
        List<String> paramNames = new ArrayList<>();
        for (Variable param : params) {
            String name = param.getName();
            String type = param.getTypeName(); // "int", "Set", "Map"
            paramNames.add(name);
            
            // Old string generation logic commented out
            /*
            String initialization;
            // Map JML types to Java initializations and SPF methods
            switch (type.toLowerCase()) {
                case "int":
                    initialization = "int " + name + " = Debug.makeSymbolicInt(\"" + name + "\");\n";
                    break;
                case "set":
                    initialization = "Set " + name + " = new HashSet(); // TODO: Handle symbolic collections\n";
                    break;
                case "map":
                    initialization = "Map " + name + " = new HashMap(); // TODO: Handle symbolic collections\n";
                    break;
                default:
                    initialization = type + " " + name + " = Debug.makeSymbolicRef(\"" + name + "\"); // May need null check\n";
            }
            func.append("        ").append(initialization);
            */
            // New IR statement
            statements.add(new AtcSymbolicVarDecl(type, name));
        }
        // --- 3. Snapshot Old State (Algorithm Step 4) ---
        // func.append("\n        // 2. Snapshot 'old' state for postconditions\n");
        Object post = spec.getPostcondition();
        
        // This helper finds all "pre-state" vars in the post-condition
        Set<String> varsToSnapshot = collectVarsToSnapshot(post);
        Map<String, String> oldStateMap = new HashMap<>(); // Maps "x" -> "old_x"
        
        for (String varName : varsToSnapshot) {
            String oldVarName = "old_" + varName;
            oldStateMap.put(varName, oldVarName);
            
            // Find the type of this var from the params list
            String varType = "Object"; // default
            for (Variable p : params) {
                if (p.getName().equals(varName)) {
                    varType = p.getTypeName();
                    break;
                }
            }
            
            // Old string generation logic commented out
            // func.append("        ").append(varType).append(" ").append(oldVarName).append(" = ").append(varName).append("; // WARNING: Not a deep copy for objects\n");
            
            // New IR statement
            statements.add(new AtcVarDecl(varType, oldVarName, AstHelper.createNameExpr(varName)));
        }
        // --- 4. Translate Preconditions (Algorithm Step 3) ---
        // func.append("\n        // 3. Set JML preconditions\n");
        Object pre = spec.getPrecondition();
        if (pre != null) {
            // Old string generation logic commented out
            // String preCode = exprToJavaCode(pre);
            // func.append("        Debug.assume(").append(preCode).append(");\n");
            
            // New IR statement
            statements.add(new AtcAssumeStmt((in.ac.iiitb.plproject.ast.Expr)pre)); // Cast to Expr
        } else {
            // If no precondition, assume true
            // func.append("        Debug.assume(true);\n");
            statements.add(new AtcAssumeStmt(AstHelper.createBooleanLiteralExpr(true))); // Assume true as an Expr
        }
        // --- 5. Generate Method Call (Algorithm Step 5) ---
        // func.append("\n        // 4. Make the actual call\n");
        String returnType = signature.getReturnTypeName();
        String functionName = signature.getName();
        String resultVarName = null;
        
        // TODO: The target of the call is not specified in the spec.
        // Assuming a static call to a helper class for now.
        // String callString = "Helper." + functionName + "(" + String.join(", ", paramNames) + ");\n";
        
        // New IR: Create MethodCallExpr
        List<Object> callArgs = new ArrayList<>();
        for (String pName : paramNames) {
            callArgs.add(AstHelper.createNameExpr(pName));
        }
        in.ac.iiitb.plproject.ast.MethodCallExpr callExpr = AstHelper.createMethodCallExpr(AstHelper.createNameExpr("Helper"), functionName, callArgs); // Use MethodCallExpr


        if (returnType.equals("void")) {
            // func.append("        ").append(callString);
            statements.add(new AtcMethodCallStmt(callExpr));
        } else {
            resultVarName = "result"; // The variable name for the return value
            // func.append("        ").append(returnType).append(" ").append(resultVarName).append(" = ").append(callString);
            statements.add(new AtcVarDecl(returnType, resultVarName, callExpr));
        }
        
        // --- 6. Translate Postconditions (Algorithm Step 6) ---
        // func.append("\n        // 5. Assert JML postconditions\n");
        if (post != null) {
            // Transform the post-condition AST:
            Object transformedPost = transformPostCondition(post, resultVarName, oldStateMap, params);
            
            // Old string generation logic commented out
            // String postCode = exprToJavaCode(transformedPost);
            // func.append("        assert(").append(postCode).append(");\n");
            
            // New IR statement
            statements.add(new AtcAssertStmt((in.ac.iiitb.plproject.ast.Expr)transformedPost)); // Cast to Expr
        }
        // func.append("    }\n");
        // return func.toString();
        
        String testName = "test_" + spec.getName() + "_" + index;
        return new AtcTestMethod(testName, statements);
    }
    // ===================================
    // Helper Methods - Implement using AstHelper
    // ===================================
    /**
     * Helper method to convert an AST expression to Java code string.
     * Delegates to AstHelper.
     */
    // private String exprToJavaCode(Object expr) { // Comment out old method
    //     return AstHelper.exprToJavaCode(expr);
    // }
    /**
     * Helper method to collect "pre-state" variables that need to be snapshotted.
     * This finds all variables in the post-condition that *do not* end in "_post".
     * Delegates to AstHelper.
     */
    private Set<String> collectVarsToSnapshot(Object expr) {
        // For "x_post > x", this should return {"x"}
        // For "result_post == update(result, data)", it returns {"result", "data"}
        return AstHelper.collectVarsToSnapshot((in.ac.iiitb.plproject.ast.Expr)expr); // Cast to Expr
    }
    /**
     * Helper method to transform post-condition expressions.
     * Handles replacing post-state (e.g., "x_post") and pre-state (e.g., "x") vars.
     * Delegates to AstHelper.
     * * @param expr The post-condition AST (e.g., "x_post > x")
     * @param resultVarName The name of the variable holding the method's return value (e.g., "result"), or null if void.
     * @param oldStateMap A map from pre-state var names to their snapshot names (e.g., "x" -> "old_x")
     * @param params The list of function parameters (to find in-place modification targets)
     * @return A new, transformed AST
     */
    private Object transformPostCondition(Object expr, String resultVarName, Map<String, String> oldStateMap, List<Variable> params) {
        // This helper must be smart.
        // 1. If it sees "x_post", and return is void, it replaces it with "x".
        // 2. If it sees "x_post", and return is non-void, it replaces it with "result".
        // 3. If it sees "x" (and "x" is in oldStateMap), it replaces it with "old_x".
        return AstHelper.transformPostCondition((in.ac.iiitb.plproject.ast.Expr)expr, resultVarName, oldStateMap, params); // Cast to Expr
    }
}

