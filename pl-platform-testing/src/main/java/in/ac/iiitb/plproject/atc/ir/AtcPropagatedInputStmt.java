package in.ac.iiitb.plproject.atc.ir;

/**
 * Step B, consuming block: replaces what would have been a fresh CLIENT_INPUT
 * declaration for a parameter whose value actually arrived from an earlier
 * block's SERVER_OUTPUT (name-matched by the propagation scan).
 *
 * <p>The variable is a formal parameter of the generated helper method, so:
 * <ul>
 *   <li><b>SPF</b>  — the parameter is re-bound to a fresh symbolic value of the
 *       same name and type ({@code taskId = Debug.makeSymbolicInteger("taskId");}).
 *       Cross-block equality is left to the solver via the block's
 *       {@code assume(...)} constraints (e.g. {@code Tasks.containsKey(taskId)})
 *       rather than threading a literal object through symbolic execution.</li>
 *   <li><b>JUnit</b> — nothing is emitted: the real value flows in as the
 *       argument the caller extracted from the upstream response.</li>
 * </ul>
 */
public class AtcPropagatedInputStmt extends AtcStatement {

    private final String typeName;
    private final String varName;
    private final String sourceFunction;
    private final int sourceBlockIndex;

    public AtcPropagatedInputStmt(String typeName, String varName,
                                  String sourceFunction, int sourceBlockIndex) {
        this.typeName = typeName;
        this.varName = varName;
        this.sourceFunction = sourceFunction;
        this.sourceBlockIndex = sourceBlockIndex;
    }

    public String getTypeName() { return typeName; }

    public String getVarName() { return varName; }

    /** Name of the function whose \result binding produced this value. */
    public String getSourceFunction() { return sourceFunction; }

    /** Index of the producing block in the test string. */
    public int getSourceBlockIndex() { return sourceBlockIndex; }
}
