package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.Expr;

/**
 * Represents an assignment statement: varName = expression;
 * Used when assigning to an already-declared variable.
 */
public class AtcAssignStmt extends AtcStatement {
    private String varName;
    private Expr valueExpr;
    
    public AtcAssignStmt(String varName, Expr valueExpr) {
        this.varName = varName;
        this.valueExpr = valueExpr;
    }
    
    public String getVarName() {
        return varName;
    }
    
    public Expr getValueExpr() {
        return valueExpr;
    }
}

