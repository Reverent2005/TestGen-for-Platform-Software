package in.ac.iiitb.plproject.atc.ir;

import in.ac.iiitb.plproject.ast.MethodCallExpr;

/**
 * Step B, producing block: replaces the plain method call with a capture of the
 * callee's return value into a named SERVER_OUTPUT variable.
 *
 * <pre>
 *   poppedElem := execute(Helper.pop())
 * </pre>
 *
 * The two back-ends render it differently, which is the whole point of the
 * dry-run design:
 *
 * <ul>
 *   <li><b>SPF</b>  — a symbolic placeholder of {@link #getTypeName()} is emitted
 *       just before the call, then the real call overwrites it:
 *       {@code String poppedElem = Debug.makeSymbolicString("poppedElem");
 *              poppedElem = Helper.pop();}</li>
 *   <li><b>JUnit</b> — the placeholder is discarded entirely and the value is
 *       bound dynamically from the response:
 *       {@code Response poppedElemResponse = executeApiCall(Helper.pop());
 *              String poppedElem = extractFromResponse(poppedElemResponse, "poppedElem");}</li>
 * </ul>
 *
 * Section 6, limitation #4: a capture holds exactly one bound name, so
 * multi-valued returns (tuples/records) are out of scope.
 */
public class AtcReturnCaptureStmt extends AtcStatement {

    private final String typeName;
    private final String outputVar;
    private final MethodCallExpr callExpr;
    private final boolean nullable;

    public AtcReturnCaptureStmt(String typeName, String outputVar, MethodCallExpr callExpr) {
        this(typeName, outputVar, callExpr, false);
    }

    public AtcReturnCaptureStmt(String typeName, String outputVar, MethodCallExpr callExpr,
                                boolean nullable) {
        this.typeName = typeName;
        this.outputVar = outputVar;
        this.callExpr = callExpr;
        this.nullable = nullable;
    }

    public String getTypeName() { return typeName; }

    public String getOutputVar() { return outputVar; }

    public MethodCallExpr getCallExpr() { return callExpr; }

    /**
     * True when the spec allows the captured value to be null (e.g. the previous
     * value returned by {@code HashMap.put} for a key that was absent).  Nullable
     * captures must use a boxed type and must never attract a synthesised
     * non-null assertion — only what the postcondition actually constrains.
     */
    public boolean isNullable() { return nullable; }
}
