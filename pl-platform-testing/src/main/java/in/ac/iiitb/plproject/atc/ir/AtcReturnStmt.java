package in.ac.iiitb.plproject.atc.ir;

/**
 * Represents: {@code return poppedElem;}
 *
 * Every SERVER_OUTPUT-capturing helper ends with one of these so main() can
 * chain the produced value into the blocks that consume it.
 */
public class AtcReturnStmt extends AtcStatement {

    private final String varName;

    public AtcReturnStmt(String varName) {
        this.varName = varName;
    }

    /** Variable being returned; null for a bare {@code return;}. */
    public String getVarName() { return varName; }
}
