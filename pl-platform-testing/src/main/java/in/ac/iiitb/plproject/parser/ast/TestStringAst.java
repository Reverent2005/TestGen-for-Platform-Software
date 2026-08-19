package in.ac.iiitb.plproject.parser.ast;

import in.ac.iiitb.plproject.atc.ConcreteInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a test string - a sequence of function calls to test.
 * Example: ["push", "push", "pop", "peek"]
 *
 * <p>A test string may also carry a name and a set of {@link ConcreteInput}
 * bindings.  With bindings it describes a SINGULAR CASE: one concrete execution
 * of the sequence whose preconditions and postconditions can be checked against
 * the real library.  Without them it is just the call order, which is all the
 * symbolic pipeline needs.
 */
public class TestStringAst {

    private final List<String> calls;
    private final String name;
    private final List<ConcreteInput> concreteInputs;

    public TestStringAst(List<String> calls) {
        this(null, calls, null);
    }

    public TestStringAst(String name, List<String> calls, List<ConcreteInput> concreteInputs) {
        this.calls = (calls != null) ? new ArrayList<String>(calls) : new ArrayList<String>();
        this.name = name;
        this.concreteInputs = (concreteInputs != null)
                ? new ArrayList<ConcreteInput>(concreteInputs) : new ArrayList<ConcreteInput>();
    }

    public List<String> getCalls() {
        return calls;
    }

    /** Name from the test string file, or null when the sequence was built in code. */
    public String getName() {
        return name;
    }

    /** True when this test string pins down every value needed for one concrete run. */
    public boolean isSingularCase() {
        return !concreteInputs.isEmpty();
    }

    public List<ConcreteInput> getConcreteInputs() {
        return Collections.unmodifiableList(concreteInputs);
    }

    /** Concrete inputs of one block, keyed by parameter name. */
    public Map<String, ConcreteInput> inputsForBlock(int blockIndex) {
        Map<String, ConcreteInput> byParam = new LinkedHashMap<String, ConcreteInput>();
        for (ConcreteInput input : concreteInputs) {
            if (input.getBlockIndex() == blockIndex) {
                byParam.put(input.getParamName(), input);
            }
        }
        return byParam;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name).append(": ");
        sb.append(String.join(" -> ", calls));
        if (isSingularCase()) sb.append(" ").append(concreteInputs);
        return sb.toString();
    }
}
