package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.MethodCallExpr;

/**
 * Represents: C.increment(x); (if it were void)
 */
public class AtcMethodCallStmt extends AtcStatement {
    public MethodCallExpr callExpr;

    public AtcMethodCallStmt(MethodCallExpr callExpr) {
        this.callExpr = callExpr;
    }

    public MethodCallExpr getCallExpr() {
        return callExpr;
    }
}
