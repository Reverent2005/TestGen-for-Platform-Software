package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.AstHelper;
import in.ac.iiitb.plproject.ast.Expr;
import in.ac.iiitb.plproject.ast.MethodCallExpr;
import in.ac.iiitb.plproject.parser.ast.Variable;
import in.ac.iiitb.plproject.symex.TypeMapper;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class AtcIrCodeGenerator {

    private StringBuilder stringBuilder;
    private static final String INDENT = "    ";
    private String lastReturnVariable = null;

    /**
     * Which of the two Step D flavours is being emitted.
     *
     * Both are produced from the same method-signature model and the same
     * assertions; only the mechanism for obtaining a SERVER_OUTPUT differs
     * (Section 5, invariant 5):
     *
     * <ul>
     *   <li>{@code SPF}   — symbolic placeholder + direct {@code Helper.m(...)} call</li>
     *   <li>{@code JUNIT} — placeholder discarded; {@code executeApiCall(...)} then
     *       {@code extractFromResponse(...)} binds the REAL value at runtime</li>
     * </ul>
     */
    public enum Flavour { SPF, JUNIT }

    private Flavour flavour = Flavour.SPF;
    private String classNameOverride = null;

    public AtcIrCodeGenerator() {
        this.stringBuilder = new StringBuilder();
    }

    public String generateJavaFile(AtcClass atc) {
        return generateJavaFileInternal(atc);
    }

    public String generateSymbolicJavaFile(AtcClass atc) {
        return generateJavaFileInternal(atc);
    }

    /**
     * Emits the JUnit flavour, with DYNAMIC BINDING for every SERVER_OUTPUT.
     *
     * <p>Feed it the ATC IR (not the symbolic IR): the capture nodes must still be
     * intact so the generator can replace them with response extraction instead of
     * with a symbolic placeholder, and {@code assume} must still be an assume so it
     * can become {@code assumeTrue} rather than {@code Debug.assume}.
     *
     * <p>Method signatures and assertions are byte-identical to the SPF flavour.
     */
    public String generateJUnitFile(AtcClass atc) {
        try {
            flavour = Flavour.JUNIT;
            classNameOverride = atc.getClassName() + "_JUnit";
            return generateJavaFileInternal(atc);
        } finally {
            flavour = Flavour.SPF;
            classNameOverride = null;
        }
    }

    private String generateJavaFileInternal(AtcClass atc) {
        stringBuilder = new StringBuilder();

        stringBuilder.append("package ").append(atc.getPackageName()).append(";\n\n");

        if (flavour == Flavour.JUNIT) {
            // The symbolic placeholders are gone in this flavour, so Debug is not
            // needed; JUnit's assumeTrue takes over the role of Debug.assume.
            stringBuilder.append("import java.util.*;\n");
            stringBuilder.append("import org.junit.Test;\n");
            stringBuilder.append("import static org.junit.Assume.assumeTrue;\n");
        } else {
            for (String anImport : atc.getImports()) {
                stringBuilder.append("import ").append(anImport).append(";\n");
            }
        }
        stringBuilder.append("\n");

        if (atc.getRunWithAnnotationClass() != null && !atc.getRunWithAnnotationClass().isEmpty()) {
            stringBuilder.append("@RunWith(").append(atc.getRunWithAnnotationClass()).append(")\n");
        }
        
        String className = (classNameOverride != null) ? classNameOverride : atc.getClassName();
        stringBuilder.append("public class ").append(className).append(" {\n");

        for (AtcTestMethod method : atc.getTestMethods()) {
            lastReturnVariable = null;
            visit(method);
        }

        generateMainMethod(atc);

        if (flavour == Flavour.JUNIT) {
            generateJUnitRuntimeSupport();
        }

        stringBuilder.append("}\n");

        return stringBuilder.toString();
    }

    private void visit(AtcTestMethod method) {
        stringBuilder.append("\n");
        stringBuilder.append(INDENT).append("public ").append(method.getReturnType()).append(" ")
                     .append(method.getMethodName()).append("(");
        List<Variable> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) stringBuilder.append(", ");
            stringBuilder.append(parameters.get(i).getTypeName()).append(" ")
                         .append(parameters.get(i).getName());
        }
        stringBuilder.append(") {\n");

        Set<String> declaredVars = new HashSet<>();
        for (Variable parameter : parameters) {
            declaredVars.add(parameter.getName()); // formal params are already in scope
        }
        
        for (AtcStatement stmt : method.getStatements()) {
            if (stmt instanceof AtcSymbolicVarDecl) {
                visit((AtcSymbolicVarDecl) stmt);
                declaredVars.add(((AtcSymbolicVarDecl) stmt).getVarName());
            } else if (stmt instanceof AtcVarDecl) {
                String varName = ((AtcVarDecl) stmt).getVarName();
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
                lastReturnVariable = visit((AtcMethodCallStmt) stmt);
            } else if (stmt instanceof AtcAssertStmt) {
                visit((AtcAssertStmt) stmt);
            } else if (stmt instanceof AtcIfStmt) {
                visit((AtcIfStmt) stmt, declaredVars);
            } else if (stmt instanceof AtcReturnCaptureStmt) {
                lastReturnVariable = visit((AtcReturnCaptureStmt) stmt);
                declaredVars.add(((AtcReturnCaptureStmt) stmt).getOutputVar());
            } else if (stmt instanceof AtcPropagatedInputStmt) {
                visit((AtcPropagatedInputStmt) stmt);
            } else if (stmt instanceof AtcReturnStmt) {
                visit((AtcReturnStmt) stmt);
            }
        }

        stringBuilder.append(INDENT).append("}\n");
    }
    
    private void visit(AtcIfStmt stmt, Set<String> declaredVars) {
        String condCode = AstHelper.exprToJavaCode(stmt.getCondition());
        stringBuilder.append(INDENT).append(INDENT)
                     .append("if (").append(condCode).append(") {\n");
        
        if (stmt.hasReturn()) {
            stringBuilder.append(INDENT).append(INDENT).append(INDENT)
                         .append("return;\n");
        } else {
            for (AtcStatement thenStmt : stmt.getThenStatements()) {
                if (thenStmt instanceof AtcSymbolicVarDecl) {
                    visitWithIndent((AtcSymbolicVarDecl) thenStmt, 3);
                    declaredVars.add(((AtcSymbolicVarDecl) thenStmt).getVarName());
                } else if (thenStmt instanceof AtcVarDecl) {
                    String varName = ((AtcVarDecl) thenStmt).getVarName();
                    if (declaredVars.contains(varName)) {
                        visitAsAssignmentWithIndent((AtcVarDecl) thenStmt, 3);
                    } else {
                        visitWithIndent((AtcVarDecl) thenStmt, 3);
                        declaredVars.add(varName);
                    }
                } else if (thenStmt instanceof AtcAssignStmt) {
                    visitWithIndent((AtcAssignStmt) thenStmt, 3);
                } else if (thenStmt instanceof AtcAssumeStmt) {
                    visitWithIndent((AtcAssumeStmt) thenStmt, 3);
                } else if (thenStmt instanceof AtcMethodCallStmt) {
                    lastReturnVariable = visitWithIndent((AtcMethodCallStmt) thenStmt, 3);
                } else if (thenStmt instanceof AtcAssertStmt) {
                    visitWithIndent((AtcAssertStmt) thenStmt, 3);
                }
            }
        }
        
        stringBuilder.append(INDENT).append(INDENT).append("}\n");
    }
    
    private String visitWithIndent(AtcMethodCallStmt stmt, int indentLevel) {
        MethodCallExpr callExpr = stmt.getCallExpr();
        String callCode = AstHelper.exprToJavaCode(callExpr);
        String methodName = callExpr.name.identifier;
        String returnType = getHelperMethodReturnType(methodName);
        
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(INDENT);
        }
        
        if (returnType != null && !returnType.equals("void")) {
            String resultVar = "result_ " + methodName;  // ← CHANGED: unique name per method
            stringBuilder.append(returnType).append(" ").append(resultVar)
                         .append(" = ").append(callCode).append(";\n");
            return resultVar;
        } else {
            stringBuilder.append(callCode).append(";\n");
            return null;
        }
    }
    
    private void visitWithIndent(AtcVarDecl stmt, int indentLevel) {
        String initCode = AstHelper.exprToJavaCode(stmt.getInitExpr());
        String typeName = stmt.getTypeName();
        String varName = stmt.getVarName();
        
        if (typeName.endsWith("[]") && initCode.startsWith("new ")) {
            String baseType = typeName.substring(0, typeName.length() - 2);
            if (initCode.contains("(") && initCode.contains(")")) {
                String args = initCode.substring(initCode.indexOf("(") + 1, initCode.indexOf(")"));
                initCode = "new " + baseType + "[]{" + args + "}";
            }
        }
        
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(INDENT);
        }
        stringBuilder.append(typeName).append(" ").append(varName)
                     .append(" = ").append(initCode).append(";\n");
    }
    
    private void visitAsAssignmentWithIndent(AtcVarDecl stmt, int indentLevel) {
        String valueCode = AstHelper.exprToJavaCode(stmt.getInitExpr());
        String varName = stmt.getVarName();
        
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(INDENT);
        }
        stringBuilder.append(varName).append(" = ").append(valueCode).append(";\n");
    }
    
    private void visitWithIndent(AtcAssignStmt stmt, int indentLevel) {
        String valueCode = AstHelper.exprToJavaCode(stmt.getValueExpr());
        String varName = stmt.getVarName();
        
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(INDENT);
        }
        stringBuilder.append(varName).append(" = ").append(valueCode).append(";\n");
    }
    
    private void visitWithIndent(AtcAssumeStmt stmt, int indentLevel) {
        String condCode = AstHelper.exprToJavaCode(stmt.getCondition());
        if (condCode.startsWith("(") && condCode.endsWith(")") && condCode.length() > 2) {
            String inner = condCode.substring(1, condCode.length() - 1);
            if (inner.contains("null") && !inner.contains("(")) {
                condCode = inner;
            }
        }
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(INDENT);
        }
        stringBuilder.append(assumeCall(condCode)).append(";\n");
    }
    
    private void visitWithIndent(AtcAssertStmt stmt, int indentLevel) {
        Expr condition = stmt.getCondition();
        Map<String, MethodCallExpr> methodCallMap = new HashMap<>();
        Expr processedCondition = extractMethodCallsFromAssertion(condition, methodCallMap);
        
        for (Map.Entry<String, MethodCallExpr> entry : methodCallMap.entrySet()) {
            String varName = entry.getKey();
            MethodCallExpr methodCall = entry.getValue();
            String methodCallCode = AstHelper.exprToJavaCode(methodCall);
            String returnType = inferReturnType(methodCall);
            
            for (int i = 0; i < indentLevel; i++) {
                stringBuilder.append(INDENT);
            }
            stringBuilder.append(returnType).append(" ").append(varName)
                         .append(" = ").append(methodCallCode).append(";\n");
        }
        
        String condCode = AstHelper.exprToJavaCode(processedCondition);
        // Replace \result with the actual return variable
        if (lastReturnVariable != null && condCode.contains("\\result")) {
            condCode = condCode.replace("\\result", lastReturnVariable);
        }
        // Strip outer parentheses only for null checks
        if (condCode.startsWith("(") && condCode.endsWith(")") && condCode.length() > 2) {
            String inner = condCode.substring(1, condCode.length() - 1);
            if (inner.contains("null") && !inner.contains("(")) {
                condCode = inner;
            }
        }
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(INDENT);
        }
        stringBuilder.append("assert(").append(condCode).append(");\n");
    }
    
    private void visitWithIndent(AtcSymbolicVarDecl stmt, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append(INDENT);
        }
        
        String typeName = stmt.getTypeName();
        String varName = stmt.getVarName();
        String debugCall = getDebugMakeSymbolicCall(typeName, varName);
        stringBuilder.append(debugCall).append(";\n");
    }

    private String visit(AtcMethodCallStmt stmt) {
        MethodCallExpr callExpr = stmt.getCallExpr();
        String callCode = AstHelper.exprToJavaCode(callExpr);
        String methodName = callExpr.name.identifier;
        String returnType = getHelperMethodReturnType(methodName);
        
        if (callCode.contains("System.out.println") && callCode.contains("Test Input:")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("System\\.out\\.println\\(\\(([^)]+)\\)\\)");
            java.util.regex.Matcher matcher = pattern.matcher(callCode);
            if (matcher.find()) {
                String inner = matcher.group(1);
                java.util.regex.Pattern varPattern = java.util.regex.Pattern.compile("\"Test Input: [^=]+= \" \\+ ([a-zA-Z_][a-zA-Z0-9_]*)");
                java.util.regex.Matcher varMatcher = varPattern.matcher(inner);
                if (varMatcher.find()) {
                    String varName = varMatcher.group(1);
                    if (varName.matches("(data|result|map|set|list|collection|queue|deque)")) {
                        callCode = "System.out.println(" + inner + ")";
                    }
                }
            }
        }
        
        stringBuilder.append(INDENT).append(INDENT);
        
        if (returnType != null && !returnType.equals("void")) {
            String resultVar = "result_" + methodName;  // ← CHANGED: unique name per method
            stringBuilder.append(returnType).append(" ").append(resultVar)
                        .append(" = ").append(callCode).append(";\n");
            return resultVar;
        } else {
            stringBuilder.append(callCode).append(";\n");
            return null;
        }
    }
    /**
     * STEP D, producing block.
     *
     * <p>SPF flavour — capture the value the library generated:
     * <pre>String poppedElem = Helper.pop();</pre>
     * (in the symbolic IR the transformer has already split this into a
     * placeholder declaration plus an assignment).
     *
     * <p>JUnit flavour — DYNAMIC BINDING: no placeholder at all, the real value is
     * pulled out of the response at runtime:
     * <pre>Response poppedElemResponse = executeApiCall(Helper.pop());
     *String poppedElem = extractFromResponse(poppedElemResponse, "poppedElem");</pre>
     *
     * Nothing is asserted about the captured value here — a nullable SERVER_OUTPUT
     * such as HashMap.put's previous value must not pick up a non-null assertion
     * the spec never asked for (Section 5, invariant 2).
     */
    private String visit(AtcReturnCaptureStmt stmt) {
        String callCode = AstHelper.exprToJavaCode(stmt.getCallExpr());
        String type = stmt.getTypeName();
        String var = stmt.getOutputVar();

        if (flavour == Flavour.JUNIT) {
            stringBuilder.append(INDENT).append(INDENT)
                         .append("Response ").append(var).append("Response = executeApiCall(")
                         .append(callCode).append(");\n");
            stringBuilder.append(INDENT).append(INDENT)
                         .append(type).append(" ").append(var)
                         .append(" = extractFromResponse(").append(var).append("Response, \"")
                         .append(var).append("\");\n");
        } else {
            stringBuilder.append(INDENT).append(INDENT)
                         .append(type).append(" ").append(var)
                         .append(" = ").append(callCode).append(";\n");
        }
        return var;
    }

    /**
     * STEP D, consuming block.  The propagated value is already a formal parameter
     * of this helper, so the SPF flavour only re-binds it to a fresh symbolic value
     * (that rebinding lives in the symbolic IR as an assignment); the JUnit flavour
     * uses the real argument the caller threaded in and emits nothing but a note.
     */
    private void visit(AtcPropagatedInputStmt stmt) {
        if (flavour == Flavour.JUNIT) {
            stringBuilder.append(INDENT).append(INDENT)
                         .append("// ").append(stmt.getVarName())
                         .append(" is bound dynamically: it arrives as the SERVER_OUTPUT captured by ")
                         .append(stmt.getSourceFunction()).append(" (block ")
                         .append(stmt.getSourceBlockIndex()).append(")\n");
        } else {
            // An ASSIGNMENT, never a declaration: the name is already a formal
            // parameter of this helper, so re-declaring it would not compile.
            String factory = TypeMapper.symbolicFactoryFor(stmt.getTypeName());
            stringBuilder.append(INDENT).append(INDENT).append(stmt.getVarName()).append(" = ");
            if (factory != null) {
                stringBuilder.append("Debug.").append(factory)
                             .append("(\"").append(stmt.getVarName()).append("\");\n");
            } else {
                // Section 6, limitation #2: no primitive factory for this type.
                stringBuilder.append("(").append(stmt.getTypeName())
                             .append(") Debug.makeSymbolicRef(\"")
                             .append(stmt.getVarName()).append("\", null);\n");
            }
        }
    }

    /** Emits {@code return poppedElem;} so main() can chain the value forward. */
    private void visit(AtcReturnStmt stmt) {
        stringBuilder.append(INDENT).append(INDENT).append("return");
        if (stmt.getVarName() != null) {
            stringBuilder.append(" ").append(stmt.getVarName());
        }
        stringBuilder.append(";\n");
    }

    /**
     * The handful of helpers the JUnit flavour needs so that dynamic binding is
     * self-contained: a response wrapper, the call executor, and the extractor.
     * In a REST setting these would be the HTTP client and a JSON path read; for a
     * library target the "response" is simply the value the call returned.
     */
    private void generateJUnitRuntimeSupport() {
        stringBuilder.append("\n");
        stringBuilder.append(INDENT).append("// ── Dynamic data binding support ──────────────────────────────────\n");
        stringBuilder.append(INDENT).append("// SERVER_OUTPUT values are read back from the call at RUNTIME rather\n");
        stringBuilder.append(INDENT).append("// than solved for, which is what separates this flavour from the SPF one.\n");
        stringBuilder.append(INDENT).append("static class Response {\n");
        stringBuilder.append(INDENT).append(INDENT).append("private final Object payload;\n");
        stringBuilder.append(INDENT).append(INDENT).append("Response(Object payload) { this.payload = payload; }\n");
        stringBuilder.append(INDENT).append(INDENT).append("Object payload() { return payload; }\n");
        stringBuilder.append(INDENT).append("}\n\n");
        stringBuilder.append(INDENT).append("static Response executeApiCall(Object returnedValue) {\n");
        stringBuilder.append(INDENT).append(INDENT).append("return new Response(returnedValue);\n");
        stringBuilder.append(INDENT).append("}\n\n");
        stringBuilder.append(INDENT).append("@SuppressWarnings(\"unchecked\")\n");
        stringBuilder.append(INDENT).append("static <T> T extractFromResponse(Response response, String name) {\n");
        stringBuilder.append(INDENT).append(INDENT).append("return (T) response.payload();\n");
        stringBuilder.append(INDENT).append("}\n\n");
        stringBuilder.append(INDENT).append("@Test\n");
        stringBuilder.append(INDENT).append("public void testSequence() {\n");
        stringBuilder.append(INDENT).append(INDENT).append("main(new String[0]);\n");
        stringBuilder.append(INDENT).append("}\n");
    }

    private void visit(AtcSymbolicVarDecl stmt) {
        String typeName = stmt.getTypeName();
        String varName = stmt.getVarName();
        
        stringBuilder.append(INDENT).append(INDENT);
        String debugCall = getDebugMakeSymbolicCall(typeName, varName);
        stringBuilder.append(debugCall).append(";\n");
    }

    private String getDebugMakeSymbolicCall(String typeName, String varName) {
        // In the JUnit flavour there is no solver, so a CLIENT_INPUT becomes a
        // concrete literal — the slot where the value SPF solved for is hardened
        // into the test (dry-run PDF §10).
        if (flavour == Flavour.JUNIT) {
            return typeName + " " + varName + " = " + clientInputLiteral(typeName, varName)
                 + " /* CLIENT_INPUT: replace with the literal SPF solves for \"" + varName + "\" */";
        }
        if (TypeMapper.isCollectionType(typeName)) {
            String genericType = TypeMapper.getGenericType(typeName);
            return genericType + " " + varName + " = (" + genericType + ") Debug.makeSymbolicObject(\"" + varName + "\")";
        }
        // Note the declared type is preserved: a boxed Integer stays an Integer so
        // that null remains representable, while still routing to the primitive
        // factory rather than makeSymbolicRef (Section 5, invariant 7).
        String factory = TypeMapper.symbolicFactoryFor(typeName);
        if (factory != null) {
            return typeName + " " + varName + " = Debug." + factory + "(\"" + varName + "\")";
        }
        // Section 6, limitation #2: a custom type falls back to the generic object
        // path, which may under-constrain its individual fields.
        String genericType = TypeMapper.getGenericType(typeName);
        return genericType + " " + varName + " = (" + genericType + ") Debug.makeSymbolicObject(\"" + varName + "\")";
    }

    /** A deterministic stand-in value for a CLIENT_INPUT in the JUnit flavour. */
    private String clientInputLiteral(String typeName, String varName) {
        if (typeName == null) return "null";
        switch (typeName) {
            case "String": case "java.lang.String":
                return "\"" + varName + "\"";
            case "int": case "Integer": case "long": case "Long":
            case "short": case "Short": case "byte": case "Byte":
                return "0";
            case "double": case "Double": case "float": case "Float":
                return "0.0";
            case "boolean": case "Boolean":
                return "true";
            case "char": case "Character":
                return "'a'";
            default:
                return "null";
        }
    }
    private void visit(AtcVarDecl stmt) {
        String initCode = AstHelper.exprToJavaCode(stmt.getInitExpr());
        String typeName = stmt.getTypeName();
        String varName = stmt.getVarName();
        
        if (typeName.endsWith("[]") && initCode.startsWith("new ") && 
            initCode.contains("(") && initCode.contains(")") && !initCode.contains("{")) {
            String baseType = typeName.substring(0, typeName.length() - 2);
            String args = initCode.substring(initCode.indexOf("(") + 1, initCode.indexOf(")"));
            initCode = "new " + baseType + "[]{" + args + "}";
        }
        
        stringBuilder.append(INDENT).append(INDENT)
                     .append(typeName).append(" ").append(varName)
                     .append(" = ").append(initCode).append(";\n");
    }
    
    private void visitAsAssignment(AtcVarDecl stmt) {
        String valueCode = AstHelper.exprToJavaCode(stmt.getInitExpr());
        String varName = stmt.getVarName();
        
        stringBuilder.append(INDENT).append(INDENT)
                     .append(varName).append(" = ").append(valueCode).append(";\n");
    }
    
    private void visit(AtcAssignStmt stmt) {
        String valueCode = AstHelper.exprToJavaCode(stmt.getValueExpr());
        String varName = stmt.getVarName();
        
        stringBuilder.append(INDENT).append(INDENT)
                     .append(varName).append(" = ").append(valueCode).append(";\n");
    }

    private void visit(AtcAssumeStmt stmt) {
        String condCode = AstHelper.exprToJavaCode(stmt.getCondition());
        if (condCode.startsWith("(") && condCode.endsWith(")") && condCode.length() > 2) {
            String inner = condCode.substring(1, condCode.length() - 1);
            if (inner.contains("null") && !inner.contains("(")) {
                condCode = inner;
            }
        }
        stringBuilder.append(INDENT).append(INDENT)
                     .append(assumeCall(condCode)).append(";\n");
    }

    // private String visit(AtcMethodCallStmt stmt) {
    //     MethodCallExpr callExpr = stmt.getCallExpr();
    //     String callCode = AstHelper.exprToJavaCode(callExpr);
    //     String methodName = callExpr.name.identifier;
    //     String returnType = getHelperMethodReturnType(methodName);
        
    //     if (callCode.contains("System.out.println") && callCode.contains("Test Input:")) {
    //         java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("System\\.out\\.println\\(\\(([^)]+)\\)\\)");
    //         java.util.regex.Matcher matcher = pattern.matcher(callCode);
    //         if (matcher.find()) {
    //             String inner = matcher.group(1);
    //             java.util.regex.Pattern varPattern = java.util.regex.Pattern.compile("\"Test Input: [^=]+= \" \\+ ([a-zA-Z_][a-zA-Z0-9_]*)");
    //             java.util.regex.Matcher varMatcher = varPattern.matcher(inner);
    //             if (varMatcher.find()) {
    //                 String varName = varMatcher.group(1);
    //                 if (varName.matches("(data|result|map|set|list|collection|queue|deque)")) {
    //                     callCode = "System.out.println(" + inner + ")";
    //                 }
    //             }
    //         }
    //     }
        
    //     stringBuilder.append(INDENT).append(INDENT);
        
    //     if (returnType != null && !returnType.equals("void")) {
    //         String resultVar = "result";
    //         stringBuilder.append(returnType).append(" ").append(resultVar)
    //                      .append(" = ").append(callCode).append(";\n");
    //         return resultVar;
    //     } else {
    //         stringBuilder.append(callCode).append(";\n");
    //         return null;
    //     }
    // }

    private void visit(AtcAssertStmt stmt) {
        Expr condition = stmt.getCondition();
        
        Map<String, MethodCallExpr> methodCallMap = new HashMap<>();
        Expr processedCondition = extractMethodCallsFromAssertion(condition, methodCallMap);
        
        if (methodCallMap.isEmpty()) {
            String originalCode = AstHelper.exprToJavaCode(condition);
            if (originalCode.contains("Helper.update(") && countOccurrences(originalCode, "Helper.update(") > 1) {
                extractMethodCallsFromString(originalCode, methodCallMap);
                if (!methodCallMap.isEmpty()) {
                    String processedCode = originalCode;
                    for (Map.Entry<String, MethodCallExpr> entry : methodCallMap.entrySet()) {
                        String varName = entry.getKey();
                        String methodCallCode = AstHelper.exprToJavaCode(entry.getValue());
                        processedCode = processedCode.replace(methodCallCode, varName);
                    }
                    
                    for (Map.Entry<String, MethodCallExpr> entry : methodCallMap.entrySet()) {
                        String varName = entry.getKey();
                        MethodCallExpr methodCall = entry.getValue();
                        String methodCallCode = AstHelper.exprToJavaCode(methodCall);
                        String returnType = inferReturnType(methodCall);
                        
                        stringBuilder.append(INDENT).append(INDENT)
                                     .append(returnType).append(" ").append(varName)
                                     .append(" = ").append(methodCallCode).append(";\n");
                    }
                    
                    stringBuilder.append(INDENT).append(INDENT)
                                 .append("assert(").append(processedCode).append(");\n");
                    return;
                }
            }
        }
        
        for (Map.Entry<String, MethodCallExpr> entry : methodCallMap.entrySet()) {
            String varName = entry.getKey();
            MethodCallExpr methodCall = entry.getValue();
            String methodCallCode = AstHelper.exprToJavaCode(methodCall);
            String returnType = inferReturnType(methodCall);
            
            stringBuilder.append(INDENT).append(INDENT)
                         .append(returnType).append(" ").append(varName)
                         .append(" = ").append(methodCallCode).append(";\n");
        }
        
        String condCode = AstHelper.exprToJavaCode(processedCondition);
        // Replace \result with the actual return variable
        if (lastReturnVariable != null && condCode.contains("\\result")) {
            condCode = condCode.replace("\\result", lastReturnVariable);
        }
        // Strip outer parentheses only for null checks
        if (condCode.startsWith("(") && condCode.endsWith(")") && condCode.length() > 2) {
            String inner = condCode.substring(1, condCode.length() - 1);
            if (inner.contains("null") && !inner.contains("(")) {
                condCode = inner;
            }
        }
        stringBuilder.append(INDENT).append(INDENT)
                     .append("assert(").append(condCode).append(");\n");
    }
    
    /**
     * A precondition is a Debug.assume for SPF and JUnit's assumeTrue for the
     * runtime flavour — the same "skip inputs the spec does not admit" semantics.
     */
    /**
     * main() instantiates the generated class itself, so the JUnit flavour has to
     * name ITS class rather than the SPF one.
     */
    private String retargetClassName(String code, AtcClass atc) {
        if (classNameOverride == null || code == null) return code;
        return code.replace(atc.getClassName(), classNameOverride);
    }

    private String assumeCall(String condCode) {
        return (flavour == Flavour.JUNIT ? "assumeTrue(" : "Debug.assume(") + condCode + ")";
    }

    private String getHelperMethodReturnType(String methodName) {
        switch (methodName.toLowerCase()) {
            case "sqrt":
            case "divide":
                return "double";
            case "abs":
            case "increment":
                return "int";
            case "appendexclamation":
                return "String";
            case "process":
                return "boolean";
            case "update":
                return "Map<?,?>";
            default:
                return null;
        }
    }
    
    private int countOccurrences(String str, String substr) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substr, index)) != -1) {
            count++;
            index += substr.length();
        }
        return count;
    }
    
    private void extractMethodCallsFromString(String code, Map<String, MethodCallExpr> methodCallMap) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Helper\\.update\\([^)]+\\)");
        java.util.regex.Matcher matcher = pattern.matcher(code);
        
        String firstMatch = null;
        while (matcher.find()) {
            String match = matcher.group();
            if (firstMatch == null) {
                firstMatch = match;
                java.util.regex.Pattern argPattern = java.util.regex.Pattern.compile("Helper\\.update\\(([^)]+)\\)");
                java.util.regex.Matcher argMatcher = argPattern.matcher(match);
                if (argMatcher.find()) {
                    String argsStr = argMatcher.group(1);
                    String[] args = argsStr.split(",");
                    List<Expr> argExprs = new ArrayList<>();
                    for (String arg : args) {
                        argExprs.add(AstHelper.createNameExpr(arg.trim()));
                    }
                    MethodCallExpr updateCall = AstHelper.createMethodCallExpr(
                        AstHelper.createNameExpr("Helper"), "update", argExprs);
                    methodCallMap.put("expectedResult", updateCall);
                    break;
                }
            }
        }
    }
    
    private Expr extractMethodCallsFromAssertion(Expr expr, Map<String, MethodCallExpr> methodCallMap) {
        if (expr == null) {
            return expr;
        }
        
        if (expr instanceof MethodCallExpr) {
            MethodCallExpr methodCall = (MethodCallExpr) expr;
            String varName = generateMethodCallVarName(methodCall, methodCallMap.size());
            methodCallMap.put(varName, methodCall);
            
            return AstHelper.createNameExpr(varName);
        }
        
        try {
            Class<?> binaryExprClass = Class.forName("in.ac.iiitb.plproject.ast.BinaryExpr");
            if (binaryExprClass.isInstance(expr)) {
                java.lang.reflect.Field leftField = binaryExprClass.getDeclaredField("left");
                java.lang.reflect.Field rightField = binaryExprClass.getDeclaredField("right");
                java.lang.reflect.Field opField = binaryExprClass.getDeclaredField("op");
                leftField.setAccessible(true);
                rightField.setAccessible(true);
                opField.setAccessible(true);
                
                Expr left = (Expr) leftField.get(expr);
                Expr right = (Expr) rightField.get(expr);
                Object op = opField.get(expr);
                
                Expr processedLeft = extractMethodCallsFromAssertion(left, methodCallMap);
                Expr processedRight = extractMethodCallsFromAssertion(right, methodCallMap);
                
                String opName = op.getClass().getMethod("name").invoke(op).toString();
                
                return AstHelper.createBinaryExpr(processedLeft, processedRight, opName);
            }
        } catch (Exception e) {
        }
        
        return expr;
    }
    
    private String generateMethodCallVarName(MethodCallExpr methodCall, int index) {
        String methodName = methodCall.name.identifier;
        if (methodName.equals("update")) {
            return "expectedResult";
        } else {
            return "temp" + index;
        }
    }
    
    private String inferReturnType(MethodCallExpr methodCall) {
        String methodName = methodCall.name.identifier;
        if (methodName.equals("update")) {
            return "Map<?,?>";
        }
        return "Object";
    }


    private void generateMainMethod(AtcClass atc) {
        stringBuilder.append("\n");
        stringBuilder.append(INDENT).append("public static void main(String[] args) {\n");

        for (AtcStatement statement : atc.getMainMethodStatements()) {
            if (statement instanceof AtcMethodCallStmt) {
                String callCode = AstHelper.exprToJavaCode(((AtcMethodCallStmt) statement).getCallExpr());
                stringBuilder.append(INDENT).append(INDENT)
                             .append(callCode).append(";\n");
            } else if (statement instanceof AtcVarDecl) {
                String initCode = AstHelper.exprToJavaCode(((AtcVarDecl) statement).getInitExpr());
                stringBuilder.append(INDENT).append(INDENT)
                             .append(retargetClassName(((AtcVarDecl) statement).getTypeName(), atc))
                             .append(" ")
                             .append(((AtcVarDecl) statement).getVarName()).append(" = ")
                             .append(retargetClassName(initCode, atc)).append(";\n");
            }
        }
        stringBuilder.append(INDENT).append("}\n");
    }
}