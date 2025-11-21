package in.ac.iiitb.plproject.ast;

import java.util.List;

/**
 * Public class for method call expressions.
 * This allows other packages (like IR classes) to reference MethodCallExpr types.
 */
public class MethodCallExpr extends Expr {
    public final Expr scope;
    public final SimpleName name;
    public final List<Expr> args;
    
    public MethodCallExpr(Expr scope, SimpleName name, List<Expr> args) {
        this.scope = scope;
        this.name = name;
        this.args = args;
    }
    
    @Override 
    public String toString() { 
        return (scope != null ? scope + "." : "") + name + "(" + args + ")"; 
    }
}

