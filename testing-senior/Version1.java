// Version1.java - Updated to use NewGrammar AST classes
import java.util.*;

/*
  Updated to use NewGrammar AST classes:
    - Var -> NameExpr(SimpleName)
    - Num -> IntegerLiteralExpr or DoubleLiteralExpr
    - FuncCall -> MethodCallExpr (for method calls) or BinaryExpr (for operators)
    - FuncCallStmt -> ExpressionStmt
    - SetExpr, TupleExpr, MapExpr -> Compatibility classes in Helper.java
*/

public class Version1 {
    
    // Helper methods to work with NameExpr
    private static String getNameFromExpr(Expr expr) {
        if (expr instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) expr;
            if (nameExpr.name instanceof SimpleName) {
                return ((SimpleName) nameExpr.name).identifier;
            }
        }
        return null;
    }
    
    private static NameExpr createNameExpr(String name) {
        return new NameExpr(new SimpleName(name));
    }

    // convert1: produce a renamed deep-copy of the expression.
    public static Expr convert1(Expr expr, SymbolTable symtable, String add) {
        if (expr == null) return null;

        if (expr instanceof NameExpr) {
            String name = getNameFromExpr(expr);
            if (name != null) {
                if (symtable != null && symtable.exists(name)) {
                    return createNameExpr(name + add);
                } else {
                    return createNameExpr(name);
                }
            }
        }

        if (expr instanceof MethodCallExpr) {
            MethodCallExpr m = (MethodCallExpr) expr;
            List<Expr> newArgs = new ArrayList<>();
            for (Expr arg : m.args) {
                newArgs.add(convert1(arg, symtable, add));
            }
            Expr newScope = m.scope != null ? convert1(m.scope, symtable, add) : null;
            return new MethodCallExpr(newScope, m.name, newArgs);
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            return new BinaryExpr(
                convert1(b.left, symtable, add),
                convert1(b.right, symtable, add),
                b.op
            );
        }

        if (expr instanceof IntegerLiteralExpr) {
            IntegerLiteralExpr n = (IntegerLiteralExpr) expr;
            return new IntegerLiteralExpr(n.value);
        }

        if (expr instanceof DoubleLiteralExpr) {
            DoubleLiteralExpr d = (DoubleLiteralExpr) expr;
            return new DoubleLiteralExpr(d.value);
        }

        if (expr instanceof SetExpr) {
            SetExpr s = (SetExpr) expr;
            List<Expr> newElements = new ArrayList<>();
            for (Expr e : s.elements) newElements.add(convert1(e, symtable, add));
            return new SetExpr(newElements);
        }

        if (expr instanceof MapExpr) {
            MapExpr m = (MapExpr) expr;
            List<Pair<NameExpr, Expr>> ret = new ArrayList<>();
            for (Pair<NameExpr, Expr> me : m.entries) {
                // keys are NameExpr for the AST
                NameExpr keyExpr = me.key;
                NameExpr newKey = null;
                if (keyExpr != null) {
                    String keyName = getNameFromExpr(keyExpr);
                    if (keyName != null) {
                        if (symtable != null && symtable.exists(keyName)) {
                            newKey = createNameExpr(keyName + add);
                        } else {
                            newKey = createNameExpr(keyName);
                        }
                    }
                }
                Expr newVal = convert1(me.value, symtable, add);
                ret.add(new Pair<>(newKey, newVal));
            }
            return new MapExpr(ret);
        }

        if (expr instanceof TupleExpr) {
            TupleExpr t = (TupleExpr) expr;
            List<Expr> newExprs = new ArrayList<>();
            for (Expr e : t.elements) newExprs.add(convert1(e, symtable, add));
            return new TupleExpr(newExprs);
        }

        if (expr instanceof ObjectCreationExpr) {
            ObjectCreationExpr o = (ObjectCreationExpr) expr;
            List<Expr> newArgs = new ArrayList<>();
            for (Expr arg : o.args) {
                newArgs.add(convert1(arg, symtable, add));
            }
            return new ObjectCreationExpr(o.type, newArgs);
        }

        if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr f = (FieldAccessExpr) expr;
            return new FieldAccessExpr(
                convert1(f.scope, symtable, add),
                f.field
            );
        }

        if (expr instanceof StringLiteralExpr) {
            StringLiteralExpr s = (StringLiteralExpr) expr;
            return new StringLiteralExpr(s.value);
        }

        if (expr instanceof ThisExpr) {
            return new ThisExpr();
        }

        if (expr instanceof AssignExpr) {
            AssignExpr a = (AssignExpr) expr;
            return new AssignExpr(
                convert1(a.target, symtable, add),
                convert1(a.value, symtable, add)
            );
        }

        // Unknown expression type
        throw new RuntimeException("Unknown expression type in convert1: " + expr.getClass().getName());
    }

    // addthedashexpr: collect variable NAMES that appear as argument of MethodCallExpr whose name is "'"
    // or in post-state expressions (represented as NameExpr with "_post" suffix in new grammar)
    public static void addthedashexpr(Expr expr, Set<String> res) {
        if (expr == null) return;

        if (expr instanceof NameExpr) return;

        if (expr instanceof MethodCallExpr) {
            MethodCallExpr m = (MethodCallExpr) expr;
            if (m.name instanceof SimpleName && "'".equals(((SimpleName) m.name).identifier)) {
                if (!m.args.isEmpty()) {
                    Expr a0 = m.args.get(0);
                    String name = getNameFromExpr(a0);
                    if (name != null) {
                        res.add(name);
                    }
                }
            } else {
                if (m.scope != null) addthedashexpr(m.scope, res);
                for (Expr a : m.args) addthedashexpr(a, res);
            }
            return;
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            addthedashexpr(b.left, res);
            addthedashexpr(b.right, res);
            return;
        }

        if (expr instanceof IntegerLiteralExpr || expr instanceof DoubleLiteralExpr || 
            expr instanceof StringLiteralExpr) return;

        if (expr instanceof ObjectCreationExpr) {
            ObjectCreationExpr o = (ObjectCreationExpr) expr;
            for (Expr arg : o.args) addthedashexpr(arg, res);
            return;
        }

        if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr f = (FieldAccessExpr) expr;
            addthedashexpr(f.scope, res);
            return;
        }

        if (expr instanceof ThisExpr) {
            return;
        }

        if (expr instanceof AssignExpr) {
            AssignExpr a = (AssignExpr) expr;
            addthedashexpr(a.target, res);
            addthedashexpr(a.value, res);
            return;
        }

        if (expr instanceof SetExpr) {
            for (Expr e : ((SetExpr) expr).elements) addthedashexpr(e, res);
            return;
        }

        if (expr instanceof MapExpr) {
            for (Pair<NameExpr, Expr> me : ((MapExpr) expr).entries) {
                // key is NameExpr
                if (me.key != null) addthedashexpr(me.key, res);
                addthedashexpr(me.value, res);
            }
            return;
        }

        if (expr instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) expr).elements) addthedashexpr(e, res);
            return;
        }
    }

    // removethedashexpr: remove ' nodes and replace vars with _old where appropriate.
    public static Expr removethedashexpr(Expr expr, Set<String> res) {
        return removethedashexpr(expr, res, false);
    }

    private static Expr removethedashexpr(Expr expr, Set<String> res, boolean flag) {
        if (expr == null) return null;

        if (expr instanceof NameExpr) {
            String name = getNameFromExpr(expr);
            if (name != null) {
                // Handle post-state variables (name_post -> name)
                if (name.endsWith("_post")) {
                    String baseName = name.substring(0, name.length() - 5);
                    return createNameExpr(baseName);
                }
                if (flag) {
                    return createNameExpr(name);
                } else if (!flag && res.contains(name)) {
                    return createNameExpr(name + "_old");
                } else {
                    return createNameExpr(name);
                }
            }
        }

        if (expr instanceof MethodCallExpr) {
            MethodCallExpr m = (MethodCallExpr) expr;
            if (m.name instanceof SimpleName && "'".equals(((SimpleName) m.name).identifier)) {
                // remove the quote node and mark nested as inside quoted expr
                if (!m.args.isEmpty()) {
                    return removethedashexpr(m.args.get(0), res, true);
                } else {
                    return null;
                }
            } else {
                List<Expr> newArgs = new ArrayList<>();
                for (Expr a : m.args) newArgs.add(removethedashexpr(a, res, false));
                Expr newScope = m.scope != null ? removethedashexpr(m.scope, res, false) : null;
                return new MethodCallExpr(newScope, m.name, newArgs);
            }
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            return new BinaryExpr(
                removethedashexpr(b.left, res, false),
                removethedashexpr(b.right, res, false),
                b.op
            );
        }

        if (expr instanceof IntegerLiteralExpr) {
            IntegerLiteralExpr n = (IntegerLiteralExpr) expr;
            return new IntegerLiteralExpr(n.value);
        }

        if (expr instanceof DoubleLiteralExpr) {
            DoubleLiteralExpr d = (DoubleLiteralExpr) expr;
            return new DoubleLiteralExpr(d.value);
        }

        if (expr instanceof StringLiteralExpr) {
            StringLiteralExpr s = (StringLiteralExpr) expr;
            return new StringLiteralExpr(s.value);
        }

        if (expr instanceof ObjectCreationExpr) {
            ObjectCreationExpr o = (ObjectCreationExpr) expr;
            List<Expr> newArgs = new ArrayList<>();
            for (Expr arg : o.args) {
                newArgs.add(removethedashexpr(arg, res, false));
            }
            return new ObjectCreationExpr(o.type, newArgs);
        }

        if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr f = (FieldAccessExpr) expr;
            return new FieldAccessExpr(
                removethedashexpr(f.scope, res, false),
                f.field
            );
        }

        if (expr instanceof ThisExpr) {
            return new ThisExpr();
        }

        if (expr instanceof AssignExpr) {
            AssignExpr a = (AssignExpr) expr;
            return new AssignExpr(
                removethedashexpr(a.target, res, false),
                removethedashexpr(a.value, res, false)
            );
        }

        if (expr instanceof SetExpr) {
            SetExpr s = (SetExpr) expr;
            List<Expr> newElements = new ArrayList<>();
            for (Expr e : s.elements) newElements.add(removethedashexpr(e, res, false));
            return new SetExpr(newElements);
        }

        if (expr instanceof MapExpr) {
            MapExpr m = (MapExpr) expr;
            List<Pair<NameExpr, Expr>> ret = new ArrayList<>();
            for (Pair<NameExpr, Expr> me : m.entries) {
                NameExpr key = me.key;
                NameExpr newKeyExpr = null;
                if (key != null) {
                    // use removethedashexpr on the key (as Expr) then cast back to NameExpr if possible
                    Expr ke = removethedashexpr(key, res, false);
                    if (ke instanceof NameExpr) newKeyExpr = (NameExpr) ke;
                }
                Expr newVal = removethedashexpr(me.value, res, false);
                ret.add(new Pair<>(newKeyExpr, newVal));
            }
            return new MapExpr(ret);
        }

        if (expr instanceof TupleExpr) {
            TupleExpr t = (TupleExpr) expr;
            List<Expr> newExprs = new ArrayList<>();
            for (Expr e : t.elements) newExprs.add(removethedashexpr(e, res, false));
            return new TupleExpr(newExprs);
        }

        throw new RuntimeException("Unknown expression type in removethedashexpr");
    }

    // makeStmt: given an Expr expected to be NameExpr, return ExpressionStmt with input(var) call
    public static ExpressionStmt makeStmt(Expr expr) {
        if (expr == null) return null;
        if (expr instanceof NameExpr) {
            String name = getNameFromExpr(expr);
            if (name != null) {
                List<Expr> args = new ArrayList<>();
                args.add(createNameExpr(name)); // pass a copy
                MethodCallExpr inputCall = new MethodCallExpr(null, new SimpleName("input"), args);
                return new ExpressionStmt(inputCall);
            }
        }
        return null;
    }

    // getInputVars with Option 2: always push converted variable into InputVariables,
    // but still add type info to final when available in typemap.
    public static void getInputVars(Expr expr,
                                    List<Expr> InputVariables,
                                    String toadd,
                                    SymbolTable symtable,
                                    TypeMap finalTm,
                                    TypeMap typemap) {
        if (expr == null) return;

        if (expr instanceof NameExpr) {
            String name = getNameFromExpr(expr);
            if (name != null) {
                // Option 2: always push a converted copy into InputVariables
                InputVariables.add(convert1(expr, symtable, toadd));
                // if typemap has a type, add to final
                if (typemap != null && typemap.mapping.containsKey(name)) {
                    finalTm.mapping.put(name + toadd, typemap.mapping.get(name));
                }
            }
            return;
        }

        if (expr instanceof MethodCallExpr) {
            MethodCallExpr m = (MethodCallExpr) expr;
            if (m.scope != null) getInputVars(m.scope, InputVariables, toadd, symtable, finalTm, typemap);
            for (Expr a : m.args) getInputVars(a, InputVariables, toadd, symtable, finalTm, typemap);
            return;
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            getInputVars(b.left, InputVariables, toadd, symtable, finalTm, typemap);
            getInputVars(b.right, InputVariables, toadd, symtable, finalTm, typemap);
            return;
        }

        if (expr instanceof ObjectCreationExpr) {
            ObjectCreationExpr o = (ObjectCreationExpr) expr;
            for (Expr arg : o.args) getInputVars(arg, InputVariables, toadd, symtable, finalTm, typemap);
            return;
        }

        if (expr instanceof FieldAccessExpr) {
            FieldAccessExpr f = (FieldAccessExpr) expr;
            getInputVars(f.scope, InputVariables, toadd, symtable, finalTm, typemap);
            return;
        }

        if (expr instanceof AssignExpr) {
            AssignExpr a = (AssignExpr) expr;
            getInputVars(a.target, InputVariables, toadd, symtable, finalTm, typemap);
            getInputVars(a.value, InputVariables, toadd, symtable, finalTm, typemap);
            return;
        }

        if (expr instanceof SetExpr) {
            for (Expr e : ((SetExpr) expr).elements) getInputVars(e, InputVariables, toadd, symtable, finalTm, typemap);
            return;
        }

        if (expr instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) expr).elements) getInputVars(e, InputVariables, toadd, symtable, finalTm, typemap);
            return;
        }

        if (expr instanceof MapExpr) {
            for (Pair<NameExpr, Expr> me : ((MapExpr) expr).entries) {
                if (me.key != null) getInputVars(me.key, InputVariables, toadd, symtable, finalTm, typemap);
                getInputVars(me.value, InputVariables, toadd, symtable, finalTm, typemap);
            }
            return;
        }

        if (expr instanceof IntegerLiteralExpr || expr instanceof DoubleLiteralExpr || 
            expr instanceof StringLiteralExpr || expr instanceof ThisExpr) {
            return;
        }
    }

    // convert: main transformation from Specification (collection of FunctionSpecs) to Program
    // NOTE: this method mutates no input 'Specification' object; it assumes Specification is read-only.
    public static Program convert(Specification funcsSpec, SymbolTable symtable, TypeMap typemap) {
        TypeMap finalTm = new TypeMap();
        List<Stmt> programStmts = new ArrayList<>();

        if (funcsSpec == null) return new Program(programStmts);
        for (int i = 0; i < funcsSpec.blocks.size(); ++i) {
            // use stack-allocated TypeMap for per-block info
            TypeMap itm = new TypeMap();
            SymbolTable currTable = (symtable != null && symtable.children != null && i < symtable.children.size())
                    ? symtable.children.get(i)
                    : null;

            FunctionSpec currBlock = funcsSpec.blocks.get(i);

            Expr pre = currBlock.pre;
            FuncCallSpec callWrapper = currBlock.call;
            Response response = currBlock.response;
            Expr post = (response != null) ? response.expr : null;

            // 1) gather input variables from call arguments
            List<Expr> inputVariables = new ArrayList<>();
            if (callWrapper != null && callWrapper.call != null) {
                MethodCallExpr callNode = callWrapper.call;
                if (callNode.scope != null) {
                    getInputVars(callNode.scope, inputVariables, Integer.toString(i), currTable, finalTm, itm);
                }
                for (int j = 0; j < callNode.args.size(); ++j) {
                    getInputVars(callNode.args.get(j), inputVariables, Integer.toString(i), currTable, finalTm, itm);
                }
            }

            // 2) make input() statements
            for (Expr e : inputVariables) {
                ExpressionStmt s = makeStmt(e); // may return null if not NameExpr
                if (s != null) programStmts.add(s);
            }

            // 3) rename pre/call/post for this block
            Expr pre1 = convert1(pre, currTable, Integer.toString(i));

            // convert call: must create a fresh MethodCallExpr from the original call's name and args
            MethodCallExpr call1 = null;
            if (callWrapper != null && callWrapper.call != null) {
                MethodCallExpr origCall = callWrapper.call;
                // We need to deep-copy args because origCall.args may be reused
                List<Expr> origArgsCopy = new ArrayList<>();
                for (Expr a : origCall.args) origArgsCopy.add(a); // shallow copy; convert1 will create deep converted call below
                Expr origScopeCopy = origCall.scope != null ? origCall.scope : null;
                MethodCallExpr callexpr = new MethodCallExpr(origScopeCopy, origCall.name, origArgsCopy);
                call1 = (MethodCallExpr) convert1(callexpr, currTable, Integer.toString(i));
            }

            Expr post1 = convert1(post, currTable, Integer.toString(i));

            // 4) collect dashed variables from post1
            Set<String> res = new HashSet<>();
            addthedashexpr(post1, res);

            // 5) assume(pre1)
            if (pre1 != null) {
                List<Expr> v1 = new ArrayList<>();
                v1.add(pre1);
                MethodCallExpr assumeCall = new MethodCallExpr(null, new SimpleName("assume"), v1);
                programStmts.add(new ExpressionStmt(assumeCall));
            }

            // 6) For each dashed variable s, add s_old = s assignment (represented as AssignExpr)
            for (String s : res) {
                // Optionally produce input(s_old) as in your C++ (the C++ created g but didn't push it)
                // If you want input(s_old) uncomment below:
                // ExpressionStmt g = makeStmt(createNameExpr(s + "_old"));
                // if (g != null) programStmts.add(g);

                AssignExpr eq = new AssignExpr(
                    createNameExpr(s + "_old"),
                    createNameExpr(s)
                );
                programStmts.add(new ExpressionStmt(eq));
            }

            // 7) add the call itself
            if (call1 != null) {
                programStmts.add(new ExpressionStmt(call1));
            }

            // 8) transform post1 by removing ' and replacing dashed vars with _old where necessary
            Expr postAfter = removethedashexpr(post1, res);

            // 9) assert(postAfter)
            if (postAfter != null) {
                List<Expr> v2 = new ArrayList<>();
                v2.add(postAfter);
                MethodCallExpr assertCall = new MethodCallExpr(null, new SimpleName("assert"), v2);
                programStmts.add(new ExpressionStmt(assertCall));
            }
        }

        return new Program(programStmts);
    }

}


