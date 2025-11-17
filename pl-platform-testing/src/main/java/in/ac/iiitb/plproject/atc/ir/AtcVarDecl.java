package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.Expr;

/**
 * Represents: int old_x = x;
 *       OR:     int result = C.increment(x);
 */
public class AtcVarDecl extends AtcStatement {
    public String typeName;
    public String varName;
    public Expr initExpr;

    public AtcVarDecl(String typeName, String varName, Expr initExpr) {
        this.typeName = typeName;
        this.varName = varName;
        this.initExpr = initExpr;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getVarName() {
        return varName;
    }

    public Expr getInitExpr() {
        return initExpr;
    }
}
