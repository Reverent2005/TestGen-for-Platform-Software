package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.Expr;

/**
 * Represents: assert(result > old_x);
 */
public class AtcAssertStmt extends AtcStatement {
    public Expr condition;

    public AtcAssertStmt(Expr condition) {
        this.condition = condition;
    }

    public Expr getCondition() {
        return condition;
    }
}
