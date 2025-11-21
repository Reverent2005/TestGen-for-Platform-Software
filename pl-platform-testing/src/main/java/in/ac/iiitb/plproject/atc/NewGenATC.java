package in.ac.iiitb.plproject.atc;

import in.ac.iiitb.plproject.parser.ast.*; // JmlFunctionSpec, JmlSpecAst, TestStringAst, Variable, FunctionSignature
import in.ac.iiitb.plproject.ast.AstHelper;
import in.ac.iiitb.plproject.ast.Expr;
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
        imports.add("java.util.Arrays"); // Needed for Arrays.asList() when creating HashSet from Set literals // Needed for SetExpr conversion to Java code
        // TODO: Add imports for user-defined classes (e.g., com.example.lms.Stack) - need a mechanism to collect these

        List<AtcTestMethod> actualTestMethods = new ArrayList<>(); // This will hold our unique helper methods

        List<String> calls = testStringAst.getCalls();
        
        // NEW: Maps to store unique function specifications and their generated helper methods
        Map<String, JmlFunctionSpec> uniqueFunctionSpecs = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order
        Map<String, AtcTestMethod> generatedHelperMethods = new HashMap<>(); // Store generated AtcTestMethod objects

        // 1. Identify unique function specifications from the test string calls
        for (String functionName : calls) {
            JmlFunctionSpec spec = jmlSpecAst.findSpecFor(functionName);
            if (spec != null) {
                // Assuming functionName is unique enough for now.
                // For real overloading, you'd need to consider the full signature.
                uniqueFunctionSpecs.putIfAbsent(functionName, spec);
            }
        }
        
        // Use a for-i loop to get an index for unique function names
        // for (int i = 0; i < calls.size(); i++) {
        //     String functionName = calls.get(i);
            
        //     // Find the corresponding JML spec for this function
        //     JmlFunctionSpec spec = jmlSpecAst.findSpecFor(functionName);
            
        //     if (spec != null) {
        //         // Generate a self-contained test function for this spec
        //         // Pass the index 'i' to make the function name unique
        //         AtcTestMethod testMethod = generateTestFunction(spec, i); // Changed to AtcTestMethod
        //         testMethods.add(testMethod);
        //     }
        // }
        
        // 2. Generate a helper method for each unique function specification
        for (Map.Entry<String, JmlFunctionSpec> entry : uniqueFunctionSpecs.entrySet()) {
            String funcName = entry.getKey();
            JmlFunctionSpec spec = entry.getValue();
            // Generate helper method, e.g., "increment_helper"
            AtcTestMethod helperMethod = generateHelperFunction(spec); // Renamed and modified
            generatedHelperMethods.put(funcName, helperMethod);
            actualTestMethods.add(helperMethod); // Add to the list of methods in the class
        }

        // 3. Generate the main method calls based on the original test string order
        List<AtcStatement> mainMethodStatements = new ArrayList<>();
        // Assuming a constructor call for GeneratedATCs, adjust if AtcClass handles this differently
        mainMethodStatements.add(new AtcVarDecl("GeneratedATCs", "instance", AstHelper.createObjectCreationExpr("GeneratedATCs", new ArrayList<>())));

        for (String functionName : calls) { // Iterate through original calls to maintain order
            AtcTestMethod helperMethod = generatedHelperMethods.get(functionName);
            if (helperMethod != null) {
                // Create a method call statement for the helper
                in.ac.iiitb.plproject.ast.MethodCallExpr callExpr =
                    AstHelper.createMethodCallExpr(AstHelper.createNameExpr("instance"), helperMethod.getMethodName(), new ArrayList<>());
                mainMethodStatements.add(new AtcMethodCallStmt(callExpr));
            }
        }

        // Create the main method AtcTestMethod object
        // We'll need to define a way for AtcTestMethod to signify a static main method.
        // For now, let's assume a constructor like: public AtcTestMethod(String name, List<AtcStatement> statements, boolean isStatic, boolean isMain)
        // I will add a placeholder for isStatic and isMain, assuming they are part of the AtcTestMethod or a new AtcMainMethod class.
        // For simplicity now, let's assume AtcTestMethod has a constructor to signify a main method.
        // If it doesn't, we will need to explore the 'ir' package further.
        // AtcTestMethod mainMethod = new AtcTestMethod("main", mainMethodStatements, true, true); // true for static, true for main
        // actualTestMethods.add(mainMethod); // Removed to avoid duplicate main()

        String packageName = "in.ac.iiitb.plproject.atc.generated"; // Assuming a default generated package
        String className = "GeneratedATCs";
        String runWithAnnotation = null; // No @RunWith for now, can be added later if needed

        return new AtcClass(packageName, className, imports, actualTestMethods, mainMethodStatements, runWithAnnotation);
    }
  
    private AtcTestMethod generateHelperFunction(JmlFunctionSpec spec) {
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
        Expr post = spec.getPostcondition();
        
        // This helper finds all "pre-state" vars in the post-condition
        Set<String> varsToSnapshot = collectVarsToSnapshot(post);
        Map<String, String> oldStateMap = new HashMap<>(); // Maps "x" -> "x_old"
        
        for (String varName : varsToSnapshot) {
            String oldVarName = varName + "_old";  // Use _old suffix to match Version1.java
            oldStateMap.put(varName, oldVarName);
            
            // Find the type of this var from the params list
            String varType = "Object"; // default
            for (Variable p : params) {
                if (p.getName().equals(varName)) {
                    varType = p.getTypeName();
                    break;
                }
            }
            
            // For Set and Map types, create proper copies instead of aliases
            // Generate: Set<?> var_old = new HashSet<>(var); or Map<?,?> var_old = new HashMap<>(var);
            // Note: Null-safety is handled in AtcIrCodeGenerator which will generate:
            // var_old = (var != null) ? new HashSet<>(var) : new HashSet<>();
            if (varType.equals("Set")) {
                List<Object> constructorArgs = new ArrayList<>();
                constructorArgs.add(AstHelper.createNameExpr(varName));
                statements.add(new AtcVarDecl("Set<?>", oldVarName, 
                    AstHelper.createObjectCreationExpr("HashSet", constructorArgs)));
            } else if (varType.equals("Map")) {
                List<Object> constructorArgs = new ArrayList<>();
                constructorArgs.add(AstHelper.createNameExpr(varName));
                statements.add(new AtcVarDecl("Map<?,?>", oldVarName, 
                    AstHelper.createObjectCreationExpr("HashMap", constructorArgs)));
            } else {
                // For primitive types and other objects, use simple assignment
                statements.add(new AtcVarDecl(varType, oldVarName, AstHelper.createNameExpr(varName)));
            }
        }
        // --- 4. Translate Preconditions (Algorithm Step 3) ---
        // func.append("\n        // 3. Set JML preconditions\n");
        Expr pre = spec.getPrecondition();
        if (pre != null) {
            // Old string generation logic commented out
            // String preCode = exprToJavaCode(pre);
            // func.append("        Debug.assume(").append(preCode).append(");\n");
            
            // New IR statement
            statements.add(new AtcAssumeStmt(pre));
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
        
        // Add null checks for collection parameters before method call
        // This prevents NullPointerException during symbolic execution
        for (Variable param : params) {
            String paramType = param.getTypeName();
            String paramName = param.getName();
            // Check if parameter is a collection type
            if (paramType.equals("Set") || paramType.equals("Map") || 
                paramType.equals("Set<?>") || paramType.equals("Map<?,?>") ||
                paramType.startsWith("Set<") || paramType.startsWith("Map<")) {
                // Add assume statement to ensure collection is not null
                // This helps JPF avoid exploring null paths that would cause NPE
                statements.add(new AtcAssumeStmt(
                    AstHelper.createBinaryExpr(
                        AstHelper.createNameExpr(paramName),
                        AstHelper.createNameExpr("null"),
                        "NOT_EQUALS"
                    )
                ));
            }
        }
        
        // Check if postcondition references parameters with prime notation or _post suffix
        // This determines whether to assign return value back to a parameter
        String postStateParam = null;
        if (post != null) {
            // Use AstHelper which has direct access to AST classes (more reliable than reflection)
            List<String> paramNameList = new ArrayList<>();
            for (Variable p : params) {
                paramNameList.add(p.getName());
            }
            postStateParam = AstHelper.findPostStateParameter(post, paramNameList);
            
            // Fallback: try string-based detection if AST-based fails
            if (postStateParam == null) {
                postStateParam = findPostStateParameter(post, params);
            }
        }
        
        // New IR: Create MethodCallExpr
        List<Object> callArgs = new ArrayList<>();
        for (String pName : paramNames) {
            callArgs.add(AstHelper.createNameExpr(pName));
        }
        in.ac.iiitb.plproject.ast.MethodCallExpr callExpr = AstHelper.createMethodCallExpr(AstHelper.createNameExpr("Helper"), functionName, callArgs); // Use MethodCallExpr

        // Determine how to handle the method call result
        // If postcondition references a parameter in post-state, we should assign return value to that parameter
        // This works for both void and non-void methods (void methods that actually return values via postcondition)
        if (postStateParam != null && isPrimitiveType(getParamType(postStateParam, params))) {
            // Postcondition references this parameter in post-state (e.g., x' > x or x_post > x)
            // Assign return value back to the parameter: param = Helper.method(param);
            // This handles both cases:
            // 1. Method returns void but postcondition suggests it should return a value -> treat as returning that type
            // 2. Method returns a value and postcondition references parameter -> assign to parameter
            String paramType = getParamType(postStateParam, params);
            statements.add(new AtcVarDecl(paramType, postStateParam, callExpr));
            resultVarName = postStateParam; // Use the parameter name as result for postcondition transformation
        } else if (returnType.equals("void")) {
            // Regular void method call with no parameter mutation
            statements.add(new AtcMethodCallStmt(callExpr));
        } else {
            // Method returns a value and postcondition doesn't reference a parameter
            // Standard return value handling: result = Helper.method(...);
            resultVarName = "result";
            statements.add(new AtcVarDecl(returnType, resultVarName, callExpr));
        }
        
        // --- 6. Translate Postconditions (Algorithm Step 6) ---
        // func.append("\n        // 5. Assert JML postconditions\n");
        if (post != null) {
            // Transform the post-condition AST:
            Expr transformedPost = transformPostCondition(post, resultVarName, oldStateMap, params);
            
            // Old string generation logic commented out
            // String postCode = exprToJavaCode(transformedPost);
            // func.append("        assert(").append(postCode).append(");\n");
            
            // New IR statement
            statements.add(new AtcAssertStmt(transformedPost));
        }
        // func.append("    }\n");
        // return func.toString();
        
        String helperMethodName = spec.getName() + "_helper"; // e.g., "increment_helper"
        return new AtcTestMethod(helperMethodName, statements);
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
     * This finds all variables in the post-condition that *do not* end in "_post" or appear inside prime operator ('(x)).
     * Delegates to AstHelper.
     * Supports both '_post' suffix notation and prime operator ('(x)) notation.
     */
    private Set<String> collectVarsToSnapshot(Expr expr) {
        // For "x_post > x", this should return {"x"}
        // For "'(x) > x", this should return {"x"} (x from prime operator)
        // For "result_post == update(result, data)", it returns {"result", "data"}
        return AstHelper.collectVarsToSnapshot(expr);
    }
    /**
     * Helper method to transform post-condition expressions.
     * Handles replacing post-state (e.g., "x_post" or "'(x)") and pre-state (e.g., "x") vars.
     * Delegates to AstHelper.
     * Supports both '_post' suffix notation and prime operator ('(x)) notation.
     * 
     * @param expr The post-condition AST (e.g., "x_post > x" or "'(x) > x")
     * @param resultVarName The name of the variable holding the method's return value (e.g., "result"), or null if void.
     * @param oldStateMap A map from pre-state var names to their snapshot names (e.g., "x" -> "x_old")
     * @param params The list of function parameters (to find in-place modification targets)
     * @return A new, transformed AST
     */
    private Expr transformPostCondition(Expr expr, String resultVarName, Map<String, String> oldStateMap, List<Variable> params) {
        // This helper handles:
        // 1. If it sees "x_post" or "'(x)", and return is void, it replaces it with "x".
        // 2. If it sees "x_post" or "'(x)", and return is non-void, it replaces it with "result".
        // 3. If it sees "x" (and "x" is in oldStateMap), it replaces it with "x_old".
        return (Expr) AstHelper.transformPostCondition(expr, resultVarName, oldStateMap, params);
    }
    
    /**
     * Finds if a parameter is referenced in post-state (with prime notation or _post suffix).
     * This is used to determine if return value should be assigned back to a parameter.
     * 
     * @param post The postcondition expression
     * @param params List of function parameters
     * @return Parameter name if referenced in post-state, null otherwise
     */
    private String findPostStateParameter(Expr post, List<Variable> params) {
        if (post == null) {
            return null;
        }
        
        // Convert postcondition to string to check for patterns
        // This is a more reliable approach than reflection
        String postStr = AstHelper.exprToJavaCode(post);
        
        // Look for prime operator pattern: '([varName]) or varName_post
        for (Variable param : params) {
            String paramName = param.getName();
            // Check for prime notation patterns (flexible matching):
            // - '([x]) - with brackets and spaces
            // - '(x) - without brackets
            // - '([x]) - various bracket formats
            // Use regex-like pattern matching for flexibility
            String primePattern1 = "'([" + paramName + "])";  // '([x])
            String primePattern2 = "'(" + paramName + ")";    // '(x)
            String primePattern3 = "'(["+ paramName + "])";   // '([x]) no space
            
            if (postStr.contains(primePattern1) || 
                postStr.contains(primePattern2) ||
                postStr.contains(primePattern3) ||
                postStr.matches(".*'\\s*\\(\\s*\\[\\s*" + paramName + "\\s*\\]\\s*\\).*") ||
                postStr.matches(".*'\\s*\\(\\s*" + paramName + "\\s*\\).*")) {
                return paramName;
            }
            // Check for _post suffix: x_post (must be whole word, not part of another name)
            if (postStr.matches(".*\\b" + paramName + "_post\\b.*")) {
                return paramName;
            }
        }
        
        // Fallback: use recursive reflection-based approach for more complex cases
        return findPostStateParameterRecursive(post, params);
    }
    
    /**
     * Gets the type of a parameter by name.
     */
    private String getParamType(String paramName, List<Variable> params) {
        for (Variable p : params) {
            if (p.getName().equals(paramName)) {
                return p.getTypeName();
            }
        }
        return "int"; // default
    }
    
    /**
     * Recursive helper to find parameter referenced in post-state.
     */
    private String findPostStateParameterRecursive(Expr expr, List<Variable> params) {
        if (expr == null) {
            return null;
        }
        
        String className = expr.getClass().getSimpleName();
        
        // Check if this is a method call with prime operator
        if (className.equals("MethodCallExpr")) {
            try {
                java.lang.reflect.Field nameField = expr.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Object nameObj = nameField.get(expr);
                
                java.lang.reflect.Field identifierField = nameObj.getClass().getDeclaredField("identifier");
                identifierField.setAccessible(true);
                String identifier = (String) identifierField.get(nameObj);
                
                java.lang.reflect.Field argsField = expr.getClass().getDeclaredField("args");
                argsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Expr> args = (List<Expr>) argsField.get(expr);
                
                if ("'".equals(identifier) && !args.isEmpty()) {
                    // Prime operator found - check if argument is a parameter
                    Expr arg = args.get(0);
                    String varName = AstHelper.getNameFromExpr(arg);
                    if (varName != null) {
                        // Check if this is a parameter
                        for (Variable param : params) {
                            if (param.getName().equals(varName)) {
                                return varName;
                            }
                        }
                    }
                }
                
                // Recursively check arguments
                for (Expr arg : args) {
                    String result = findPostStateParameterRecursive(arg, params);
                    if (result != null) {
                        return result;
                    }
                }
            } catch (Exception e) {
                // If reflection fails, return null
                return null;
            }
        } else if (className.equals("BinaryExpr")) {
            try {
                java.lang.reflect.Field leftField = expr.getClass().getDeclaredField("left");
                leftField.setAccessible(true);
                Expr left = (Expr) leftField.get(expr);
                
                java.lang.reflect.Field rightField = expr.getClass().getDeclaredField("right");
                rightField.setAccessible(true);
                Expr right = (Expr) rightField.get(expr);
                
                String result = findPostStateParameterRecursive(left, params);
                if (result != null) {
                    return result;
                }
                return findPostStateParameterRecursive(right, params);
            } catch (Exception e) {
                return null;
            }
        } else if (className.equals("NameExpr")) {
            // Check for _post suffix
            try {
                java.lang.reflect.Field nameField = expr.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Object nameObj = nameField.get(expr);
                
                java.lang.reflect.Field identifierField = nameObj.getClass().getDeclaredField("identifier");
                identifierField.setAccessible(true);
                String identifier = (String) identifierField.get(nameObj);
                
                if (identifier != null && identifier.endsWith("_post")) {
                    String baseName = identifier.substring(0, identifier.length() - "_post".length());
                    // Check if this is a parameter
                    for (Variable param : params) {
                        if (param.getName().equals(baseName)) {
                            return baseName;
                        }
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Finds if a primitive parameter is mutated (referenced with prime notation in postcondition).
     * Returns the parameter name if found, null otherwise.
     * Kept for backward compatibility.
     * 
     * @param post The postcondition expression
     * @param params List of function parameters
     * @return Parameter name if a primitive is mutated, null otherwise
     */
    private String findMutatedPrimitiveParameter(Expr post, List<Variable> params) {
        if (post == null) {
            return null;
        }
        
        // Check if postcondition contains prime operator on a primitive parameter
        return findMutatedPrimitiveParameterRecursive(post, params);
    }
    
    /**
     * Recursive helper to find mutated primitive parameter.
     * Uses reflection/string-based approach to avoid package-private visibility issues.
     */
    private String findMutatedPrimitiveParameterRecursive(Expr expr, List<Variable> params) {
        if (expr == null) {
            return null;
        }
        
        String className = expr.getClass().getSimpleName();
        
        // Check if this is a method call with prime operator
        if (className.equals("MethodCallExpr")) {
            try {
                java.lang.reflect.Field nameField = expr.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Object nameObj = nameField.get(expr);
                
                java.lang.reflect.Field identifierField = nameObj.getClass().getDeclaredField("identifier");
                identifierField.setAccessible(true);
                String identifier = (String) identifierField.get(nameObj);
                
                java.lang.reflect.Field argsField = expr.getClass().getDeclaredField("args");
                argsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Expr> args = (List<Expr>) argsField.get(expr);
                
                if ("'".equals(identifier) && !args.isEmpty()) {
                    // Prime operator found - check if argument is a primitive parameter
                    Expr arg = args.get(0);
                    String varName = AstHelper.getNameFromExpr(arg);
                    if (varName != null) {
                        // Check if this is a primitive parameter
                        for (Variable param : params) {
                            if (param.getName().equals(varName)) {
                                String paramType = param.getTypeName();
                                // Check if it's a primitive type
                                if (isPrimitiveType(paramType)) {
                                    return varName;
                                }
                            }
                        }
                    }
                }
                
                // Recursively check arguments
                for (Expr arg : args) {
                    String result = findMutatedPrimitiveParameterRecursive(arg, params);
                    if (result != null) {
                        return result;
                    }
                }
            } catch (Exception e) {
                // If reflection fails, return null
                return null;
            }
        } else if (className.equals("BinaryExpr")) {
            try {
                java.lang.reflect.Field leftField = expr.getClass().getDeclaredField("left");
                leftField.setAccessible(true);
                Expr left = (Expr) leftField.get(expr);
                
                java.lang.reflect.Field rightField = expr.getClass().getDeclaredField("right");
                rightField.setAccessible(true);
                Expr right = (Expr) rightField.get(expr);
                
                String result = findMutatedPrimitiveParameterRecursive(left, params);
                if (result != null) {
                    return result;
                }
                return findMutatedPrimitiveParameterRecursive(right, params);
            } catch (Exception e) {
                // If reflection fails, return null
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Checks if a type is a primitive type.
     */
    private boolean isPrimitiveType(String typeName) {
        return typeName.equals("int") || typeName.equals("Integer") ||
               typeName.equals("double") || typeName.equals("Double") ||
               typeName.equals("float") || typeName.equals("Float") ||
               typeName.equals("long") || typeName.equals("Long") ||
               typeName.equals("short") || typeName.equals("Short") ||
               typeName.equals("byte") || typeName.equals("Byte") ||
               typeName.equals("boolean") || typeName.equals("Boolean") ||
               typeName.equals("char") || typeName.equals("Character");
    }
}

