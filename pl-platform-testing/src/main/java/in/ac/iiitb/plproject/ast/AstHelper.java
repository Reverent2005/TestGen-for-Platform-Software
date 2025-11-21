package in.ac.iiitb.plproject.ast;

import java.util.*;
import in.ac.iiitb.plproject.parser.ast.Variable;

/**
 * Helper class for working with AST expressions.
 * This class is in the ast package so it can access package-private classes.
 * Other packages can use this class to work with AST expressions.
 */
public class AstHelper {
    
    /**
     * Extract variable name from a NameExpr.
     */
    public static String getNameFromExpr(Object expr) {
        if (expr instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) expr;
            if (nameExpr.name instanceof SimpleName) {
                return ((SimpleName) nameExpr.name).identifier;
            }
        }
        return null;
    }
    
    /**
     * Create a NameExpr from a string name.
     */
    public static NameExpr createNameExpr(String name) {
        return new NameExpr(new SimpleName(name));
    }
    
    /**
     * Transform post-condition expressions.
     * Handles post-state variables (x_post -> x, x -> x_old).
     */
    public static Object transformPostCondition(Object expr, String resultVarName, Map<String, String> oldStateMap, List<Variable> params) {
        if (expr == null) {
            return null;
        }
        if (!(expr instanceof Expr)) {
            return expr; // Return as is if not an AST expression
        }
        return transformPostConditionRecursive((Expr) expr, resultVarName, oldStateMap, params);
    }

    private static Expr transformPostConditionRecursive(Expr expr, String resultVarName, Map<String, String> oldStateMap, List<Variable> params) {
        if (expr == null) {
            return null;
        }

        if (expr instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) expr;
            String name = ((SimpleName) nameExpr.name).identifier;

            // Case 1: Variable needs to be replaced with its old state (e.g., x -> old_x)
            if (oldStateMap.containsKey(name)) {
                return createNameExpr(oldStateMap.get(name));
            }
            // Case 2: Post-state variable (e.g., x_post)
            else if (name.endsWith("_post")) {
                String baseName = name.substring(0, name.length() - "_post".length());
                // If there's a return value, x_post maps to resultVarName
                if (resultVarName != null) {
                    // Simplified: assume if resultVarName exists, post-state vars map to it
                    return createNameExpr(resultVarName);
                }
                // If void function, x_post maps to x (in-place modification)
                else {
                    return createNameExpr(baseName);
                }
            }
            // Case 3: Other NameExpr - return as is
            return nameExpr;
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) expr;
            Expr left = transformPostConditionRecursive(binExpr.left, resultVarName, oldStateMap, params);
            Expr right = transformPostConditionRecursive(binExpr.right, resultVarName, oldStateMap, params);
            return createBinaryExpr(left, right, binExpr.op.toString());
        } else if (expr instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) expr;
            Expr scope = transformPostConditionRecursive(methodCallExpr.scope, resultVarName, oldStateMap, params);
            List<Object> args = new ArrayList<>();
            for (Expr arg : methodCallExpr.args) {
                args.add(transformPostConditionRecursive(arg, resultVarName, oldStateMap, params));
            }
            return createMethodCallExpr(scope, methodCallExpr.name.identifier, args);
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) expr;
            Expr innerExpr = transformPostConditionRecursive(unaryExpr.expr, resultVarName, oldStateMap, params);
            return createUnaryExpr(innerExpr, unaryExpr.op.toString());
        } else if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) expr;
            Expr scope = transformPostConditionRecursive(fieldAccessExpr.scope, resultVarName, oldStateMap, params);
            return new FieldAccessExpr(scope, fieldAccessExpr.field);
        } else if (expr instanceof SetExpr) {
            SetExpr setExpr = (SetExpr) expr;
            List<Expr> elements = new ArrayList<>();
            for (Expr element : setExpr.elements) {
                elements.add(transformPostConditionRecursive(element, resultVarName, oldStateMap, params));
            }
            return new SetExpr(elements);
        } else if (expr instanceof MapExpr) {
            MapExpr mapExpr = (MapExpr) expr;
            List<Pair<NameExpr, Expr>> entries = new ArrayList<>();
            for (Pair<NameExpr, Expr> entry : mapExpr.entries) {
                // Key is NameExpr, Value is Expr
                NameExpr key = (NameExpr) transformPostConditionRecursive(entry.key, resultVarName, oldStateMap, params);
                Expr value = transformPostConditionRecursive(entry.value, resultVarName, oldStateMap, params);
                entries.add(new Pair<>(key, value));
            }
            return new MapExpr(entries);
        } else if (expr instanceof TupleExpr) {
            TupleExpr tupleExpr = (TupleExpr) expr;
            List<Expr> elements = new ArrayList<>();
            for (Expr element : tupleExpr.elements) {
                elements.add(transformPostConditionRecursive(element, resultVarName, oldStateMap, params));
            }
            return new TupleExpr(elements);
        } else if (expr instanceof ObjectCreationExpr) {
            ObjectCreationExpr objCreationExpr = (ObjectCreationExpr) expr;
            List<Object> args = new ArrayList<>();
            for (Expr arg : objCreationExpr.args) {
                args.add(transformPostConditionRecursive(arg, resultVarName, oldStateMap, params));
            }
            return createObjectCreationExpr(((ClassOrInterfaceType) objCreationExpr.type).name.identifier, args);
        }
        // For literals (IntegerLiteralExpr, DoubleLiteralExpr, StringLiteralExpr, BooleanLiteralExpr) and ThisExpr, return as is.
        return expr;
    }
    
    /**
     * Collect variables that appear in post-state.
     */
    public static Set<String> collectPostStateVariables(Object expr) {
        // TODO: Implement - collect variables that need old state saved
        // Similar to addthedashexpr from Version1
        Set<String> result = new HashSet<>();
        if (expr instanceof Expr) {
            // Expr e = (Expr) expr;
            // Implement collection logic here
        }
        return result;
    }

    public static Set<String> collectVarsToSnapshot(Object expr) {
        Set<String> result = new HashSet<>();
        if (!(expr instanceof Expr)) {
            return result;
        }
        collectVarsToSnapshotRecursive((Expr) expr, result);
        return result;
    }

    private static void collectVarsToSnapshotRecursive(Expr expr, Set<String> result) {
        if (expr == null) {
            return;
        }

        if (expr instanceof NameExpr) {
            String name = ((SimpleName) ((NameExpr) expr).name).identifier;
            if (!name.endsWith("_post")) {
                result.add(name);
            }
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) expr;
            collectVarsToSnapshotRecursive(binExpr.left, result);
            collectVarsToSnapshotRecursive(binExpr.right, result);
        } else if (expr instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) expr;
            if (methodCallExpr.scope != null) {
                collectVarsToSnapshotRecursive(methodCallExpr.scope, result);
            }
            for (Expr arg : methodCallExpr.args) {
                collectVarsToSnapshotRecursive(arg, result);
            }
        } else if (expr instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) expr;
            collectVarsToSnapshotRecursive(unaryExpr.expr, result);
        } else if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) expr;
            collectVarsToSnapshotRecursive(fieldAccessExpr.scope, result);
            // The field itself is a SimpleName, not an Expr, so no recursion on it directly.
        } else if (expr instanceof SetExpr) {
            SetExpr setExpr = (SetExpr) expr;
            for (Expr element : setExpr.elements) {
                collectVarsToSnapshotRecursive(element, result);
            }
        } else if (expr instanceof MapExpr) {
            MapExpr mapExpr = (MapExpr) expr;
            for (Pair<NameExpr, Expr> entry : mapExpr.entries) {
                collectVarsToSnapshotRecursive(entry.key, result);
                collectVarsToSnapshotRecursive(entry.value, result);
            }
        } else if (expr instanceof TupleExpr) {
            TupleExpr tupleExpr = (TupleExpr) expr;
            for (Expr element : tupleExpr.elements) {
                collectVarsToSnapshotRecursive(element, result);
            }
        }
        // Literals, ThisExpr, ObjectCreationExpr do not contain variables to snapshot directly
    }
    
    /**
     * Convert an AST expression to Java code string.
     */
    public static String exprToJavaCode(Object expr) {
        if (expr == null) {
            return "true"; // Represent null/empty expression as true in Java (e.g., for missing preconditions)
        }
        if (!(expr instanceof Expr)) {
            return expr.toString(); // Fallback for non-Expr objects (e.g., JmlFunctionSpec itself)
        }

        Expr e = (Expr) expr;

        if (e instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) e;
            String left = exprToJavaCode(binExpr.left);
            String right = exprToJavaCode(binExpr.right);
            String operator = "";
            BinaryExpr.Operator op = binExpr.op;
            if (op == BinaryExpr.Operator.AND) {
                operator = " && ";
            } else if (op == BinaryExpr.Operator.OR) {
                operator = " || ";
            } else if (op == BinaryExpr.Operator.EQUALS) {
                operator = " == ";
            } else if (op == BinaryExpr.Operator.NOT_EQUALS) {
                operator = " != ";
            } else if (op == BinaryExpr.Operator.LESS_THAN) {
                operator = " < ";
            } else if (op == BinaryExpr.Operator.LESS_THAN_OR_EQUAL) {
                operator = " <= ";
            } else if (op == BinaryExpr.Operator.GREATER_THAN) {
                operator = " > ";
            } else if (op == BinaryExpr.Operator.GREATER_THAN_OR_EQUAL) {
                operator = " >= ";
            } else if (op == BinaryExpr.Operator.PLUS) {
                operator = " + ";
            } else if (op == BinaryExpr.Operator.MINUS) {
                operator = " - ";
            } else if (op == BinaryExpr.Operator.MULTIPLY) {
                operator = " * ";
            } else if (op == BinaryExpr.Operator.DIVIDE) {
                operator = " / ";
            } else {
                operator = " " + binExpr.op.toString() + " "; // Fallback
            }
            return "(" + left + operator + right + ")";
        } else if (e instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) e;
            return ((SimpleName) nameExpr.name).identifier;
        } else if (e instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) e;
            StringBuilder sb = new StringBuilder();
            if (methodCallExpr.scope != null) {
                sb.append(exprToJavaCode(methodCallExpr.scope)).append(".");
            }
            sb.append(methodCallExpr.name.identifier).append("(");
            for (int i = 0; i < methodCallExpr.args.size(); i++) {
                sb.append(exprToJavaCode(methodCallExpr.args.get(i)));
                if (i < methodCallExpr.args.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        } else if (e instanceof IntegerLiteralExpr) {
            return String.valueOf(((IntegerLiteralExpr) e).value);
        } else if (e instanceof DoubleLiteralExpr) {
            return String.valueOf(((DoubleLiteralExpr) e).value);
        } else if (e instanceof StringLiteralExpr) {
            return "\"" + ((StringLiteralExpr) e).value + "\"";
        } else if (e instanceof BooleanLiteralExpr) { // Handle BooleanLiteralExpr
            return String.valueOf(((BooleanLiteralExpr) e).value);
        } else if (e instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) e;
            String expression = exprToJavaCode(unaryExpr.expr);
            String operator = "";
            UnaryExpr.Operator op = unaryExpr.op;
            if (op == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                operator = "!";
            } else if (op == UnaryExpr.Operator.MINUS) {
                operator = "-";
            } else if (op == UnaryExpr.Operator.PLUS) {
                operator = "+";
            } else {
                operator = unaryExpr.op.toString(); // Fallback
            }
            return operator + expression;
        } else if (e instanceof ObjectCreationExpr) {
            ObjectCreationExpr objCreationExpr = (ObjectCreationExpr) e;
            StringBuilder sb = new StringBuilder("new ");
            sb.append(((ClassOrInterfaceType) objCreationExpr.type).name.identifier).append("(");
            for (int i = 0; i < objCreationExpr.args.size(); i++) {
                sb.append(exprToJavaCode(objCreationExpr.args.get(i)));
                if (i < objCreationExpr.args.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        } else if (e instanceof ThisExpr) {
            return "this";
        } else if (e instanceof FieldAccessExpr) {
            FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) e;
            return exprToJavaCode(fieldAccessExpr.scope) + "." + fieldAccessExpr.field.identifier;
        } else if (e instanceof SetExpr) {
            SetExpr setExpr = (SetExpr) e;
            StringBuilder sb = new StringBuilder("new HashSet<>(Arrays.asList(");
            for (int i = 0; i < setExpr.elements.size(); i++) {
                sb.append(exprToJavaCode(setExpr.elements.get(i)));
                if (i < setExpr.elements.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("))");
            return sb.toString();
        } else if (e instanceof MapExpr) {
            MapExpr mapExpr = (MapExpr) e;
            StringBuilder sb = new StringBuilder("new HashMap<>() {{");
            for (Pair<NameExpr, Expr> entry : mapExpr.entries) {
                sb.append(" put(").append(exprToJavaCode(entry.key)).append(", ").append(exprToJavaCode(entry.value)).append(");");
            }
            sb.append("}}");
            return sb.toString();
        } else if (e instanceof TupleExpr) {
            TupleExpr tupleExpr = (TupleExpr) e;
            StringBuilder sb = new StringBuilder("new Object[] {");
            for (int i = 0; i < tupleExpr.elements.size(); i++) {
                sb.append(exprToJavaCode(tupleExpr.elements.get(i)));
                if (i < tupleExpr.elements.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            return sb.toString();
        }
        // Fallback for unhandled Expr types
        return e.toString();
    }
    
    // ===================================
    // Factory methods for creating expressions
    // ===================================
    
    /**
     * Create a BinaryExpr.
     */
    public static BinaryExpr createBinaryExpr(Object left, Object right, String operatorName) {
        BinaryExpr.Operator op = BinaryExpr.Operator.valueOf(operatorName);
        return new BinaryExpr((Expr) left, (Expr) right, op);
    }
    
    /**
     * Create a MethodCallExpr.
     */
    public static MethodCallExpr createMethodCallExpr(Object scope, String methodName, List<Object> args) {
        List<Expr> exprArgs = new ArrayList<>();
        for (Object arg : args) {
            exprArgs.add((Expr) arg);
        }
        return new MethodCallExpr(
            scope != null ? (Expr) scope : null,
            new SimpleName(methodName),
            exprArgs
        );
    }
   
    /**
     * Create an ObjectCreationExpr.
     */
    public static ObjectCreationExpr createObjectCreationExpr(String typeName, List<Object> args) {
        List<Expr> exprArgs = new ArrayList<>();
        for (Object arg : args) {
            exprArgs.add((Expr) arg);
        }
        return new ObjectCreationExpr(
            new ClassOrInterfaceType(new SimpleName(typeName)),
            exprArgs
        );
    }
   
    /**
     * Create an IntegerLiteralExpr.
     */
    public static IntegerLiteralExpr createIntegerLiteralExpr(int value) {
        return new IntegerLiteralExpr(value);
    }

    /**
     * Create a UnaryExpr.
     */
    public static UnaryExpr createUnaryExpr(Object expr, String operatorName) {
        UnaryExpr.Operator op = UnaryExpr.Operator.valueOf(operatorName);
        return new UnaryExpr((Expr) expr, op);
    }

    /**
     * Create a BooleanLiteralExpr.
     */
    public static BooleanLiteralExpr createBooleanLiteralExpr(boolean value) {
        return new BooleanLiteralExpr(value);
    }

    /**
     * Combine multiple expressions with AND operator.
     * Useful for combining multiple requires/ensures clauses.
     */
    public static Object combineExpressionsWithAnd(List<Object> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return null;
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        // Combine all expressions with AND: expr1 && expr2 && expr3 ...
        Object result = expressions.get(0);
        for (int i = 1; i < expressions.size(); i++) {
            result = createBinaryExpr(result, expressions.get(i), "AND");
        }
        return result;
    }

    /**
     * Combine two expressions with AND operator.
     */
    public static Object combineWithAnd(Object left, Object right) {
        if (left == null) return right;
        if (right == null) return left;
        return createBinaryExpr(left, right, "AND");
    }
}

