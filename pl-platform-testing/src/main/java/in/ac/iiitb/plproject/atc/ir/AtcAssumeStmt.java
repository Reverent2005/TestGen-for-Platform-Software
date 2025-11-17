package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.Expr;

/**
 * Represents: Debug.assume(x > 0);
 */
public class AtcAssumeStmt extends AtcStatement {
    public Expr condition;

    public AtcAssumeStmt(Expr condition) {
        this.condition = condition;
    }

    public Expr getCondition() {
        return condition;
    }
}
