package in.ac.iiitb.plproject.atc.ir;

/**
 * Represents: int x = Debug.makeSymbolicInt("x");
 */
public class AtcSymbolicVarDecl extends AtcStatement {
    public String typeName;
    public String varName;

    public AtcSymbolicVarDecl(String typeName, String varName) {
        this.typeName = typeName;
        this.varName = varName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getVarName() {
        return varName;
    }
}
