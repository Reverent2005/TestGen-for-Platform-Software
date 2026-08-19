package in.ac.iiitb.plproject.atc;

/**
 * One concrete value bound to one CLIENT_INPUT of one block of a test string.
 *
 * <p>A test string alone says only WHICH calls happen and in what order; the
 * symbolic pipeline leaves their inputs to the solver.  A <b>singular case</b>
 * pins them down: it names a literal for every CLIENT_INPUT, so the sequence can
 * be executed once, concretely, and every precondition and postcondition checked
 * against the real library.
 *
 * <p>The binding is per BLOCK, not per function, because a function that appears
 * twice in a test string generally wants different inputs each time:
 *
 * <pre>
 *   push[0].elem = "alpha"    // ConcreteInput(0, "push", "elem", "\"alpha\"")
 *   push[1].elem = "beta"     // ConcreteInput(1, "push", "elem", "\"beta\"")
 * </pre>
 *
 * <p>SERVER_OUTPUT values are never bound this way: by definition the caller
 * cannot know them in advance, so they come from the call itself and are threaded
 * forward by the propagation scan.
 */
public class ConcreteInput {

    private final int blockIndex;
    private final String functionName;
    private final String paramName;
    private final String literal;

    /**
     * @param blockIndex   position of the block in the test string, 0-based
     * @param functionName function called by that block, checked against the sequence
     * @param paramName    the CLIENT_INPUT parameter being bound
     * @param literal      the value as Java source (e.g. {@code "alpha"} with quotes, or {@code 42})
     */
    public ConcreteInput(int blockIndex, String functionName, String paramName, String literal) {
        this.blockIndex = blockIndex;
        this.functionName = functionName;
        this.paramName = paramName;
        this.literal = literal;
    }

    public int getBlockIndex() { return blockIndex; }

    public String getFunctionName() { return functionName; }

    public String getParamName() { return paramName; }

    /** The bound value, already in Java source form, ready to emit verbatim. */
    public String getLiteral() { return literal; }

    @Override
    public String toString() {
        return functionName + "[" + blockIndex + "]." + paramName + " = " + literal;
    }
}
