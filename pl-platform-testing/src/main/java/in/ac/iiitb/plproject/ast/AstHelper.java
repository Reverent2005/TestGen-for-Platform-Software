package in.ac.iiitb.plproject.ast;

import java.util.*;
import in.ac.iiitb.plproject.parser.ast.Variable;
import in.ac.iiitb.plproject.ast.NewGrammar; // Import NewGrammar

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
        if (expr instanceof NewGrammar.NameExpr) {
            NewGrammar.NameExpr nameExpr = (NewGrammar.NameExpr) expr;
            if (nameExpr.name instanceof NewGrammar.SimpleName) {
                return ((NewGrammar.SimpleName) nameExpr.name).identifier;
            }
        }
        return null;
    }
    
    /**
     * Create a NameExpr from a string name.
     */
    public static NewGrammar.NameExpr createNameExpr(String name) {
        return new NewGrammar.NameExpr(new NewGrammar.SimpleName(name));
    }
    
    /**
     * Transform post-condition expressions.
     * Handles post-state variables (x_post -> x, x -> x_old).
     */
    public static Object transformPostCondition(Object expr, String resultVarName, Map<String, String> oldStateMap, List<Variable> params) {
        if (expr == null) {
            return null;
        }
        if (!(expr instanceof NewGrammar.Expr)) {
            return expr; // Return as is if not an AST expression
        }
        return transformPostConditionRecursive((NewGrammar.Expr) expr, resultVarName, oldStateMap, params);
    }

    private static NewGrammar.Expr transformPostConditionRecursive(NewGrammar.Expr expr, String resultVarName, Map<String, String> oldStateMap, List<Variable> params) {
        if (expr == null) {
            return null;
        }

        if (expr instanceof NewGrammar.NameExpr) {
            NewGrammar.NameExpr nameExpr = (NewGrammar.NameExpr) expr;
            String name = ((NewGrammar.SimpleName) nameExpr.name).identifier;

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
        } else if (expr instanceof NewGrammar.BinaryExpr) {
            NewGrammar.BinaryExpr binExpr = (NewGrammar.BinaryExpr) expr;
            NewGrammar.Expr left = transformPostConditionRecursive(binExpr.left, resultVarName, oldStateMap, params);
            NewGrammar.Expr right = transformPostConditionRecursive(binExpr.right, resultVarName, oldStateMap, params);
            return createBinaryExpr(left, right, binExpr.op.toString());
        } else if (expr instanceof NewGrammar.MethodCallExpr) {
            NewGrammar.MethodCallExpr methodCallExpr = (NewGrammar.MethodCallExpr) expr;
            NewGrammar.Expr scope = transformPostConditionRecursive(methodCallExpr.scope, resultVarName, oldStateMap, params);
            List<Object> args = new ArrayList<>();
            for (NewGrammar.Expr arg : methodCallExpr.args) {
                args.add(transformPostConditionRecursive(arg, resultVarName, oldStateMap, params));
            }
            return createMethodCallExpr(scope, methodCallExpr.name.identifier, args);
        } else if (expr instanceof NewGrammar.UnaryExpr) {
            NewGrammar.UnaryExpr unaryExpr = (NewGrammar.UnaryExpr) expr;
            NewGrammar.Expr innerExpr = transformPostConditionRecursive(unaryExpr.expr, resultVarName, oldStateMap, params);
            return createUnaryExpr(innerExpr, unaryExpr.op.toString());
        } else if (expr instanceof NewGrammar.FieldAccessExpr) {
            NewGrammar.FieldAccessExpr fieldAccessExpr = (NewGrammar.FieldAccessExpr) expr;
            NewGrammar.Expr scope = transformPostConditionRecursive(fieldAccessExpr.scope, resultVarName, oldStateMap, params);
            return new NewGrammar.FieldAccessExpr(scope, fieldAccessExpr.field);
        } else if (expr instanceof NewGrammar.SetExpr) {
            NewGrammar.SetExpr setExpr = (NewGrammar.SetExpr) expr;
            List<NewGrammar.Expr> elements = new ArrayList<>();
            for (NewGrammar.Expr element : setExpr.elements) {
                elements.add(transformPostConditionRecursive(element, resultVarName, oldStateMap, params));
            }
            return new NewGrammar.SetExpr(elements);
        } else if (expr instanceof NewGrammar.MapExpr) {
            NewGrammar.MapExpr mapExpr = (NewGrammar.MapExpr) expr;
            List<NewGrammar.Pair<NewGrammar.NameExpr, NewGrammar.Expr>> entries = new ArrayList<>();
            for (NewGrammar.Pair<NewGrammar.NameExpr, NewGrammar.Expr> entry : mapExpr.entries) {
                // Key is NameExpr, Value is Expr
                NewGrammar.NameExpr key = (NewGrammar.NameExpr) transformPostConditionRecursive(entry.key, resultVarName, oldStateMap, params);
                NewGrammar.Expr value = transformPostConditionRecursive(entry.value, resultVarName, oldStateMap, params);
                entries.add(new NewGrammar.Pair<>(key, value));
            }
            return new NewGrammar.MapExpr(entries);
        } else if (expr instanceof NewGrammar.TupleExpr) {
            NewGrammar.TupleExpr tupleExpr = (NewGrammar.TupleExpr) expr;
            List<NewGrammar.Expr> elements = new ArrayList<>();
            for (NewGrammar.Expr element : tupleExpr.elements) {
                elements.add(transformPostConditionRecursive(element, resultVarName, oldStateMap, params));
            }
            return new NewGrammar.TupleExpr(elements);
        } else if (expr instanceof NewGrammar.ObjectCreationExpr) {
            NewGrammar.ObjectCreationExpr objCreationExpr = (NewGrammar.ObjectCreationExpr) expr;
            List<Object> args = new ArrayList<>();
            for (NewGrammar.Expr arg : objCreationExpr.args) {
                args.add(transformPostConditionRecursive(arg, resultVarName, oldStateMap, params));
            }
            return createObjectCreationExpr(((NewGrammar.ClassOrInterfaceType) objCreationExpr.type).name.identifier, args);
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
        if (expr instanceof NewGrammar.Expr) {
            // Expr e = (Expr) expr;
            // Implement collection logic here
        }
        return result;
    }

    public static Set<String> collectVarsToSnapshot(Object expr) {
        Set<String> result = new HashSet<>();
        if (!(expr instanceof NewGrammar.Expr)) {
            return result;
        }
        collectVarsToSnapshotRecursive((NewGrammar.Expr) expr, result);
        return result;
    }

    private static void collectVarsToSnapshotRecursive(NewGrammar.Expr expr, Set<String> result) {
        if (expr == null) {
            return;
        }

        if (expr instanceof NewGrammar.NameExpr) {
            String name = ((NewGrammar.SimpleName) ((NewGrammar.NameExpr) expr).name).identifier;
            if (!name.endsWith("_post")) {
                result.add(name);
            }
        } else if (expr instanceof NewGrammar.BinaryExpr) {
            NewGrammar.BinaryExpr binExpr = (NewGrammar.BinaryExpr) expr;
            collectVarsToSnapshotRecursive(binExpr.left, result);
            collectVarsToSnapshotRecursive(binExpr.right, result);
        } else if (expr instanceof NewGrammar.MethodCallExpr) {
            NewGrammar.MethodCallExpr methodCallExpr = (NewGrammar.MethodCallExpr) expr;
            if (methodCallExpr.scope != null) {
                collectVarsToSnapshotRecursive(methodCallExpr.scope, result);
            }
            for (NewGrammar.Expr arg : methodCallExpr.args) {
                collectVarsToSnapshotRecursive(arg, result);
            }
        } else if (expr instanceof NewGrammar.UnaryExpr) {
            NewGrammar.UnaryExpr unaryExpr = (NewGrammar.UnaryExpr) expr;
            collectVarsToSnapshotRecursive(unaryExpr.expr, result);
        } else if (expr instanceof NewGrammar.FieldAccessExpr) {
            NewGrammar.FieldAccessExpr fieldAccessExpr = (NewGrammar.FieldAccessExpr) expr;
            collectVarsToSnapshotRecursive(fieldAccessExpr.scope, result);
            // The field itself is a SimpleName, not an Expr, so no recursion on it directly.
        } else if (expr instanceof NewGrammar.SetExpr) {
            NewGrammar.SetExpr setExpr = (NewGrammar.SetExpr) expr;
            for (NewGrammar.Expr element : setExpr.elements) {
                collectVarsToSnapshotRecursive(element, result);
            }
        } else if (expr instanceof NewGrammar.MapExpr) {
            NewGrammar.MapExpr mapExpr = (NewGrammar.MapExpr) expr;
            for (NewGrammar.Pair<NewGrammar.NameExpr, NewGrammar.Expr> entry : mapExpr.entries) {
                collectVarsToSnapshotRecursive(entry.key, result);
                collectVarsToSnapshotRecursive(entry.value, result);
            }
        } else if (expr instanceof NewGrammar.TupleExpr) {
            NewGrammar.TupleExpr tupleExpr = (NewGrammar.TupleExpr) expr;
            for (NewGrammar.Expr element : tupleExpr.elements) {
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
        if (!(expr instanceof NewGrammar.Expr)) {
            return expr.toString(); // Fallback for non-Expr objects (e.g., JmlFunctionSpec itself)
        }

        NewGrammar.Expr e = (NewGrammar.Expr) expr;

        if (e instanceof NewGrammar.BinaryExpr) {
            NewGrammar.BinaryExpr binExpr = (NewGrammar.BinaryExpr) e;
            String left = exprToJavaCode(binExpr.left);
            String right = exprToJavaCode(binExpr.right);
            String operator = "";
            switch (binExpr.op) {
                case AND: operator = " && "; break;
                case OR: operator = " || "; break;
                case EQUALS: operator = " == "; break;
                case NOT_EQUALS: operator = " != "; break;
                case LESS_THAN: operator = " < "; break;
                case LESS_THAN_OR_EQUAL: operator = " <= "; break;
                case GREATER_THAN: operator = " > "; break;
                case GREATER_THAN_OR_EQUAL: operator = " >= "; break;
                case PLUS: operator = " + "; break;
                case MINUS: operator = " - "; break;
                case MULTIPLY: operator = " * "; break;
                case DIVIDE: operator = " / "; break;
                // Add other operators as needed
                default: operator = " " + binExpr.op.toString() + " "; // Fallback
            }
            return "(" + left + operator + right + ")";
        } else if (e instanceof NewGrammar.NameExpr) {
            NewGrammar.NameExpr nameExpr = (NewGrammar.NameExpr) e;
            return ((NewGrammar.SimpleName) nameExpr.name).identifier;
        } else if (e instanceof NewGrammar.MethodCallExpr) {
            NewGrammar.MethodCallExpr methodCallExpr = (NewGrammar.MethodCallExpr) e;
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
        } else if (e instanceof NewGrammar.IntegerLiteralExpr) {
            return String.valueOf(((NewGrammar.IntegerLiteralExpr) e).value);
        } else if (e instanceof NewGrammar.DoubleLiteralExpr) {
            return String.valueOf(((NewGrammar.DoubleLiteralExpr) e).value);
        } else if (e instanceof NewGrammar.StringLiteralExpr) {
            return "\"" + ((NewGrammar.StringLiteralExpr) e).value + "\"";
        } else if (e instanceof NewGrammar.BooleanLiteralExpr) { // Handle BooleanLiteralExpr
            return String.valueOf(((NewGrammar.BooleanLiteralExpr) e).value);
        } else if (e instanceof NewGrammar.UnaryExpr) {
            NewGrammar.UnaryExpr unaryExpr = (NewGrammar.UnaryExpr) e;
            String expression = exprToJavaCode(unaryExpr.expr);
            String operator = "";
            switch (unaryExpr.op) {
                case LOGICAL_COMPLEMENT: operator = "!"; break;
                case MINUS: operator = "-"; break;
                case PLUS: operator = "+"; break;
                // Add other unary operators as needed
                default: operator = unaryExpr.op.toString(); // Fallback
            }
            return operator + expression;
        } else if (e instanceof NewGrammar.ObjectCreationExpr) {
            NewGrammar.ObjectCreationExpr objCreationExpr = (NewGrammar.ObjectCreationExpr) e;
            StringBuilder sb = new StringBuilder("new ");
            sb.append(((NewGrammar.ClassOrInterfaceType) objCreationExpr.type).name.identifier).append("(");
            for (int i = 0; i < objCreationExpr.args.size(); i++) {
                sb.append(exprToJavaCode(objCreationExpr.args.get(i)));
                if (i < objCreationExpr.args.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        } else if (e instanceof NewGrammar.ThisExpr) {
            return "this";
        } else if (e instanceof NewGrammar.FieldAccessExpr) {
            NewGrammar.FieldAccessExpr fieldAccessExpr = (NewGrammar.FieldAccessExpr) e;
            return exprToJavaCode(fieldAccessExpr.scope) + "." + fieldAccessExpr.field.identifier;
        } else if (e instanceof NewGrammar.SetExpr) {
            NewGrammar.SetExpr setExpr = (NewGrammar.SetExpr) e;
            StringBuilder sb = new StringBuilder("new HashSet<>(Arrays.asList(");
            for (int i = 0; i < setExpr.elements.size(); i++) {
                sb.append(exprToJavaCode(setExpr.elements.get(i)));
                if (i < setExpr.elements.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("))");
            return sb.toString();
        } else if (e instanceof NewGrammar.MapExpr) {
            NewGrammar.MapExpr mapExpr = (NewGrammar.MapExpr) e;
            StringBuilder sb = new StringBuilder("new HashMap<>() {{");
            for (NewGrammar.Pair<NewGrammar.NameExpr, NewGrammar.Expr> entry : mapExpr.entries) {
                sb.append(" put(").append(exprToJavaCode(entry.key)).append(", ").append(exprToJavaCode(entry.value)).append(");");
            }
            sb.append("}}");
            return sb.toString();
        } else if (e instanceof NewGrammar.TupleExpr) {
            NewGrammar.TupleExpr tupleExpr = (NewGrammar.TupleExpr) e;
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
    public static NewGrammar.BinaryExpr createBinaryExpr(Object left, Object right, String operatorName) {
        NewGrammar.BinaryExpr.Operator op = NewGrammar.BinaryExpr.Operator.valueOf(operatorName);
        return new NewGrammar.BinaryExpr((NewGrammar.Expr) left, (NewGrammar.Expr) right, op);
    }
    
    /**
     * Create a MethodCallExpr.
     */
    public static NewGrammar.MethodCallExpr createMethodCallExpr(Object scope, String methodName, List<Object> args) {
        List<NewGrammar.Expr> exprArgs = new ArrayList<>();
        for (Object arg : args) {
            exprArgs.add((NewGrammar.Expr) arg);
        }
        return new NewGrammar.MethodCallExpr(
            scope != null ? (NewGrammar.Expr) scope : null,
            new NewGrammar.SimpleName(methodName),
            exprArgs
        );
    }
   
    /**
     * Create an ObjectCreationExpr.
     */
    public static NewGrammar.ObjectCreationExpr createObjectCreationExpr(String typeName, List<Object> args) {
        List<NewGrammar.Expr> exprArgs = new ArrayList<>();
        for (Object arg : args) {
            exprArgs.add((NewGrammar.Expr) arg);
        }
        return new NewGrammar.ObjectCreationExpr(
            new NewGrammar.ClassOrInterfaceType(new NewGrammar.SimpleName(typeName)),
            exprArgs
        );
    }
   
    /**
     * Create an IntegerLiteralExpr.
     */
    public static NewGrammar.IntegerLiteralExpr createIntegerLiteralExpr(int value) {
        return new NewGrammar.IntegerLiteralExpr(value);
    }

    /**
     * Create a UnaryExpr.
     */
    public static NewGrammar.UnaryExpr createUnaryExpr(Object expr, String operatorName) {
        NewGrammar.UnaryExpr.Operator op = NewGrammar.UnaryExpr.Operator.valueOf(operatorName);
        return new NewGrammar.UnaryExpr((NewGrammar.Expr) expr, op);
    }

    /**
     * Create a BooleanLiteralExpr.
     */
    public static NewGrammar.BooleanLiteralExpr createBooleanLiteralExpr(boolean value) {
        return new NewGrammar.BooleanLiteralExpr(value);
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

