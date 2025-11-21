package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.AstHelper;
import in.ac.iiitb.plproject.ast.Expr;
import in.ac.iiitb.plproject.ast.MethodCallExpr;
import in.ac.iiitb.plproject.symex.TypeMapper;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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

        // Track declared variables to detect reassignments
        Set<String> declaredVars = new HashSet<>();
        
        // Statements in method body
        for (AtcStatement stmt : method.getStatements()) {
            // Polymorphic call to visit specific statement types
            if (stmt instanceof AtcSymbolicVarDecl) {
                visit((AtcSymbolicVarDecl) stmt);
                declaredVars.add(((AtcSymbolicVarDecl) stmt).getVarName());
            } else if (stmt instanceof AtcVarDecl) {
                String varName = ((AtcVarDecl) stmt).getVarName();
                // If variable already declared, generate assignment instead of declaration
                if (declaredVars.contains(varName)) {
                    visitAsAssignment((AtcVarDecl) stmt);
                } else {
                    visit((AtcVarDecl) stmt);
                    declaredVars.add(varName);
                }
            } else if (stmt instanceof AtcAssignStmt) {
                visit((AtcAssignStmt) stmt);
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
        String typeName = stmt.getTypeName();
        String varName = stmt.getVarName();
        
        // Use TypeMapper to determine the appropriate initialization strategy
        if (TypeMapper.isCollectionType(typeName)) {
            // For collections, use symbolic initialization with empty collection as default
            // This allows JPF to explore different collection states symbolically
            String genericType = TypeMapper.getGenericType(typeName);
            String initCode = TypeMapper.getCollectionInitCode(typeName, varName);
            stringBuilder.append(INDENT).append(INDENT)
                         .append(genericType).append(" ").append(varName)
                         .append(" = ").append(initCode).append(";\n");
        } else if (typeName.equalsIgnoreCase("int") || typeName.equals("Integer")) {
            // Generate Symbolic.input() for primitives - SpfWrapper will transform it
            stringBuilder.append(INDENT).append(INDENT)
                         .append("int ").append(varName)
                         .append(" = Symbolic.input(\"").append(varName).append("\");\n");
        } else if (typeName.equalsIgnoreCase("double") || typeName.equals("Double")) {
            stringBuilder.append(INDENT).append(INDENT)
                         .append("double ").append(varName)
                         .append(" = Symbolic.input(\"").append(varName).append("\");\n");
        } else if (typeName.equalsIgnoreCase("String")) {
            stringBuilder.append(INDENT).append(INDENT)
                         .append("String ").append(varName)
                         .append(" = Symbolic.input(\"").append(varName).append("\");\n");
        } else if (typeName.equalsIgnoreCase("boolean") || typeName.equals("Boolean")) {
            stringBuilder.append(INDENT).append(INDENT)
                         .append("boolean ").append(varName)
                         .append(" = Symbolic.input(\"").append(varName).append("\");\n");
        } else {
            // For custom classes (not collections), use Symbolic.input with cast
            // SpfWrapper will transform it to makeSymbolicRef with proper handling
            String genericType = TypeMapper.getGenericType(typeName);
            stringBuilder.append(INDENT).append(INDENT)
                         .append(genericType).append(" ").append(varName)
                         .append(" = (").append(genericType).append(") Symbolic.input(\"").append(varName).append("\");\n");
        }
    }

    private void visit(AtcVarDecl stmt) {
        String initCode = AstHelper.exprToJavaCode(stmt.getInitExpr());
        String typeName = stmt.getTypeName();
        String varName = stmt.getVarName();
        
        // Add null-safety for collection copies (e.g., new HashSet<>(var) where var might be null)
        // Pattern: If initCode contains "new HashSet(" or "new HashMap(" with a variable argument,
        // and the variable name ends with "_old", it's likely a copy operation that needs null safety
        if ((typeName.equals("Set<?>") || typeName.equals("Map<?,?>")) && 
            varName.endsWith("_old") && 
            (initCode.contains("new HashSet(") || initCode.contains("new HashMap("))) {
            // Extract the source variable name from initCode (e.g., "new HashSet<>(data)" -> "data")
            // Generate null-safe version: var_old = (source != null) ? new HashSet<>(source) : new HashSet<>();
            String sourceVar = extractSourceVarFromInit(initCode);
            if (sourceVar != null) {
                String emptyInit = typeName.equals("Set<?>") ? "new HashSet<>()" : "new HashMap<>()";
                initCode = "(" + sourceVar + " != null) ? " + initCode + " : " + emptyInit;
            }
        }
        
        stringBuilder.append(INDENT).append(INDENT)
                     .append(typeName).append(" ").append(varName)
                     .append(" = ").append(initCode).append(";\n");
    }
    
    /**
     * Generates an assignment statement for an already-declared variable.
     */
    private void visitAsAssignment(AtcVarDecl stmt) {
        String valueCode = AstHelper.exprToJavaCode(stmt.getInitExpr());
        String varName = stmt.getVarName();
        
        stringBuilder.append(INDENT).append(INDENT)
                     .append(varName).append(" = ").append(valueCode).append(";\n");
    }
    
    /**
     * Generates code for an explicit assignment statement.
     */
    private void visit(AtcAssignStmt stmt) {
        String valueCode = AstHelper.exprToJavaCode(stmt.getValueExpr());
        String varName = stmt.getVarName();
        
        stringBuilder.append(INDENT).append(INDENT)
                     .append(varName).append(" = ").append(valueCode).append(";\n");
    }
    
    /**
     * Extracts the source variable name from initialization code like "new HashSet<>(varName)".
     * Returns null if pattern doesn't match.
     */
    private String extractSourceVarFromInit(String initCode) {
        // Pattern: new HashSet<>(varName) or new HashMap<>(varName)
        // Handle both with and without generics: new HashSet<>(var) or new HashSet(var)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("new (HashSet|HashMap)(<.*>)?\\((\\w+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(initCode);
        if (matcher.find()) {
            return matcher.group(3); // Group 3 is the variable name
        }
        return null;
    }

    private void visit(AtcAssumeStmt stmt) {
        String condCode = AstHelper.exprToJavaCode(stmt.getCondition());
        // Generate simple assume - SpfWrapper will convert to Debug.assume()
        stringBuilder.append(INDENT).append(INDENT)
                     .append("assume(").append(condCode).append(");\n");
    }

    private void visit(AtcMethodCallStmt stmt) {
        String callCode = AstHelper.exprToJavaCode(stmt.getCallExpr());
        stringBuilder.append(INDENT).append(INDENT)
                     .append(callCode).append(";\n");
    }

    private void visit(AtcAssertStmt stmt) {
        Expr condition = stmt.getCondition();
        
        // Extract method calls from the assertion condition and store them in variables
        // This prevents calling methods multiple times and improves code quality
        Map<String, MethodCallExpr> methodCallMap = new HashMap<>();
        Expr processedCondition = extractMethodCallsFromAssertion(condition, methodCallMap);
        
        // If extraction didn't work (map is empty), try string-based extraction as fallback
        if (methodCallMap.isEmpty()) {
            String originalCode = AstHelper.exprToJavaCode(condition);
            // Check if the code contains method calls that appear multiple times
            // Pattern: Helper.update(...) appears more than once
            if (originalCode.contains("Helper.update(") && countOccurrences(originalCode, "Helper.update(") > 1) {
                // Extract using string manipulation as fallback
                extractMethodCallsFromString(originalCode, methodCallMap);
                if (!methodCallMap.isEmpty()) {
                    // Replace method calls in the string
                    String processedCode = originalCode;
                    for (Map.Entry<String, MethodCallExpr> entry : methodCallMap.entrySet()) {
                        String varName = entry.getKey();
                        String methodCallCode = AstHelper.exprToJavaCode(entry.getValue());
                        // Replace all occurrences of the method call with the variable name
                        processedCode = processedCode.replace(methodCallCode, varName);
                    }
                    
                    // Generate variable declarations
                    for (Map.Entry<String, MethodCallExpr> entry : methodCallMap.entrySet()) {
                        String varName = entry.getKey();
                        MethodCallExpr methodCall = entry.getValue();
                        String methodCallCode = AstHelper.exprToJavaCode(methodCall);
                        String returnType = inferReturnType(methodCall);
                        
                        stringBuilder.append(INDENT).append(INDENT)
                                     .append(returnType).append(" ").append(varName)
                                     .append(" = ").append(methodCallCode).append(";\n");
                    }
                    
                    // Generate assertion with processed code
                    stringBuilder.append(INDENT).append(INDENT)
                                 .append("assert(").append(processedCode).append(");\n");
                    return;
                }
            }
        }
        
        // Generate variable declarations for extracted method calls
        for (Map.Entry<String, MethodCallExpr> entry : methodCallMap.entrySet()) {
            String varName = entry.getKey();
            MethodCallExpr methodCall = entry.getValue();
            String methodCallCode = AstHelper.exprToJavaCode(methodCall);
            
            // Determine the return type - for now, assume Map<?,?> for Helper.update calls
            // This could be improved with type inference
            String returnType = inferReturnType(methodCall);
            
            stringBuilder.append(INDENT).append(INDENT)
                         .append(returnType).append(" ").append(varName)
                         .append(" = ").append(methodCallCode).append(";\n");
        }
        
        // Generate the assertion with the processed condition
        String condCode = AstHelper.exprToJavaCode(processedCondition);
        stringBuilder.append(INDENT).append(INDENT)
                     .append("assert(").append(condCode).append(");\n");
    }
    
    /**
     * Counts occurrences of a substring in a string.
     */
    private int countOccurrences(String str, String substr) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substr, index)) != -1) {
            count++;
            index += substr.length();
        }
        return count;
    }
    
    /**
     * Fallback method to extract method calls from string representation.
     * This is used when AST-based extraction fails.
     */
    private void extractMethodCallsFromString(String code, Map<String, MethodCallExpr> methodCallMap) {
        // Find Helper.update(...) pattern
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Helper\\.update\\([^)]+\\)");
        java.util.regex.Matcher matcher = pattern.matcher(code);
        
        String firstMatch = null;
        while (matcher.find()) {
            String match = matcher.group();
            if (firstMatch == null) {
                firstMatch = match;
                // Create a MethodCallExpr for Helper.update
                // We'll reconstruct it - this is a simplified approach
                // Extract arguments from the match: Helper.update(result_old, data_old)
                java.util.regex.Pattern argPattern = java.util.regex.Pattern.compile("Helper\\.update\\(([^)]+)\\)");
                java.util.regex.Matcher argMatcher = argPattern.matcher(match);
                if (argMatcher.find()) {
                    String argsStr = argMatcher.group(1);
                    String[] args = argsStr.split(",");
                    List<Object> argExprs = new ArrayList<>();
                    for (String arg : args) {
                        argExprs.add(AstHelper.createNameExpr(arg.trim()));
                    }
                    MethodCallExpr updateCall = AstHelper.createMethodCallExpr(
                        AstHelper.createNameExpr("Helper"), "update", argExprs);
                    methodCallMap.put("expectedResult", updateCall);
                    break; // Only extract once
                }
            }
        }
    }
    
    /**
     * Extracts method calls from an assertion expression and replaces them with variable references.
     * Returns a new expression with method calls replaced by variable names.
     * Uses reflection/instanceof to work with package-private classes.
     * 
     * @param expr The assertion condition expression
     * @param methodCallMap Map to store extracted method calls (varName -> MethodCallExpr)
     * @return A new expression with method calls replaced by variable references
     */
    private Expr extractMethodCallsFromAssertion(Expr expr, Map<String, MethodCallExpr> methodCallMap) {
        if (expr == null) {
            return expr;
        }
        
        if (expr instanceof MethodCallExpr) {
            // Found a method call - create a variable for it
            MethodCallExpr methodCall = (MethodCallExpr) expr;
            String varName = generateMethodCallVarName(methodCall, methodCallMap.size());
            methodCallMap.put(varName, methodCall);
            
            // Replace with a NameExpr referencing the variable
            return AstHelper.createNameExpr(varName);
        }
        
        // Check if it's a BinaryExpr using reflection to access package-private class
        // BinaryExpr is a top-level package-private class in NewGrammar.java
        try {
            Class<?> binaryExprClass = Class.forName("in.ac.iiitb.plproject.ast.BinaryExpr");
            if (binaryExprClass.isInstance(expr)) {
                // Use reflection to access fields
                java.lang.reflect.Field leftField = binaryExprClass.getDeclaredField("left");
                java.lang.reflect.Field rightField = binaryExprClass.getDeclaredField("right");
                java.lang.reflect.Field opField = binaryExprClass.getDeclaredField("op");
                leftField.setAccessible(true);
                rightField.setAccessible(true);
                opField.setAccessible(true);
                
                Expr left = (Expr) leftField.get(expr);
                Expr right = (Expr) rightField.get(expr);
                Object op = opField.get(expr);
                
                // Recursively process left and right sides
                Expr processedLeft = extractMethodCallsFromAssertion(left, methodCallMap);
                Expr processedRight = extractMethodCallsFromAssertion(right, methodCallMap);
                
                // Get operator name from enum
                String opName = op.getClass().getMethod("name").invoke(op).toString();
                
                // Create a new BinaryExpr with processed sides
                return AstHelper.createBinaryExpr(processedLeft, processedRight, opName);
            }
        } catch (Exception e) {
            // If reflection fails, fall through to return expr as-is
        }
        
        // For leaf nodes (NameExpr, literals) or other types, return as is
        return expr;
    }
    
    /**
     * Generates a unique variable name for a method call.
     */
    private String generateMethodCallVarName(MethodCallExpr methodCall, int index) {
        String methodName = methodCall.name.identifier;
        // Generate a descriptive variable name based on method name
        if (methodName.equals("update")) {
            return "expectedResult";
        } else {
            return "temp" + index;
        }
    }
    
    /**
     * Infers the return type of a method call.
     * This is a heuristic - ideally we'd have type information.
     */
    private String inferReturnType(MethodCallExpr methodCall) {
        String methodName = methodCall.name.identifier;
        // Heuristic: Helper.update returns Map<?,?>
        if (methodName.equals("update")) {
            return "Map<?,?>";
        }
        // Default to Object for unknown methods
        return "Object";
    }


    private void generateMainMethod(AtcClass atc) {
        stringBuilder.append("\n");
        stringBuilder.append(INDENT).append("public static void main(String[] args) {\n");

        // The instantiation of 'instance' is now handled as part of mainMethodStatements
        // from NewGenATC.java. Removing this line to prevent duplication.
        // stringBuilder.append(INDENT).append(INDENT)
        //              .append(atc.getClassName()).append(" instance = new ").append(atc.getClassName()).append("();\n");

        // Use the pre-constructed main method statements
        for (AtcStatement statement : atc.getMainMethodStatements()) {
            // Assuming AtcMethodCallStmt is the only type of statement here for simplicity
            // A more robust solution might use a visitor pattern for different statement types
            if (statement instanceof AtcMethodCallStmt) {
                String callCode = AstHelper.exprToJavaCode(((AtcMethodCallStmt) statement).getCallExpr());
                stringBuilder.append(INDENT).append(INDENT)
                             .append(callCode).append(";\n"); // MethodCallExpr already includes 'instance.' and method name
            } else if (statement instanceof AtcVarDecl) {
                String initCode = AstHelper.exprToJavaCode(((AtcVarDecl) statement).getInitExpr());
                stringBuilder.append(INDENT).append(INDENT)
                             .append(((AtcVarDecl) statement).getTypeName()).append(" ")
                             .append(((AtcVarDecl) statement).getVarName()).append(" = ")
                             .append(initCode).append(";\n");
            }
        }
        stringBuilder.append(INDENT).append("}\n");
    }
}
