package in.ac.iiitb.plproject.atc;

import in.ac.iiitb.plproject.parser.ast.FunctionSignature;
import in.ac.iiitb.plproject.parser.ast.JmlFunctionSpec;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.parser.ast.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * STEP A — the propagation scan.
 *
 * <p>Walks the test string block by block, in order, maintaining the running set
 * of SERVER_OUTPUT names produced so far ({@code availableServerOutputs}).  For
 * each block:
 *
 * <ul>
 *   <li>a formal parameter whose NAME matches an already-available SERVER_OUTPUT
 *       is marked <i>propagated</i> — the block must not re-declare it as a fresh
 *       symbolic CLIENT_INPUT; it becomes a formal parameter of the helper and
 *       main() threads the upstream value into it;</li>
 *   <li>if the block's postcondition binds {@code \result} to a name, that name
 *       is SERVER_OUTPUT and joins {@code availableServerOutputs} <b>after</b> the
 *       block (so a function cannot propagate into itself).</li>
 * </ul>
 *
 * <p><b>Section 6, limitation #3:</b> propagation is decided purely by variable
 * NAME.  If the spec author writes {@code taskId} upstream and {@code id}
 * downstream, no propagation is detected.  As the suggested mitigation, the scan
 * records a {@link #getWarnings() warning} whenever a parameter's TYPE matches an
 * available SERVER_OUTPUT but its name does not.
 */
public class PropagationScan {

    /** One row of the scan trace: what a single block saw and produced. */
    public static class Step {
        private final int index;
        private final String functionName;
        private final Set<String> propagatedParams;
        private final String producedVar;
        private final Set<String> availableAfter;

        Step(int index, String functionName, Set<String> propagatedParams,
             String producedVar, Set<String> availableAfter) {
            this.index = index;
            this.functionName = functionName;
            this.propagatedParams = Collections.unmodifiableSet(propagatedParams);
            this.producedVar = producedVar;
            this.availableAfter = Collections.unmodifiableSet(availableAfter);
        }

        public int getIndex() { return index; }
        public String getFunctionName() { return functionName; }
        /** Parameters of this block that arrived from an earlier SERVER_OUTPUT. */
        public Set<String> getPropagatedParams() { return propagatedParams; }
        /** SERVER_OUTPUT this block produces, or null. */
        public String getProducedVar() { return producedVar; }
        /** availableServerOutputs immediately AFTER this block. */
        public Set<String> getAvailableAfter() { return availableAfter; }

        @Override
        public String toString() {
            return "step " + (index + 1) + " " + functionName
                 + ":  availableServerOutputs(after) = " + availableAfter
                 + "  propagated = " + propagatedParams;
        }
    }

    private final List<Step> steps = new ArrayList<Step>();
    private final Map<String, Set<String>> propagatedParamsPerFunc =
            new LinkedHashMap<String, Set<String>>();
    private final Map<String, String> producerOfVar = new LinkedHashMap<String, String>();
    private final Map<String, String> typeOfVar = new LinkedHashMap<String, String>();
    private final List<String> warnings = new ArrayList<String>();

    private PropagationScan() { }

    /** Runs Step A over the test string. */
    public static PropagationScan scan(JmlSpecAst specAst, TestStringAst testString) {
        PropagationScan scan = new PropagationScan();
        Set<String> available = new LinkedHashSet<String>();

        List<String> calls = testString.getCalls();
        for (int i = 0; i < calls.size(); i++) {
            String functionName = calls.get(i);
            JmlFunctionSpec spec = specAst.findSpecFor(functionName);
            if (spec == null) continue;

            Set<String> propagated = new LinkedHashSet<String>();
            FunctionSignature sig = spec.getSignature();
            if (sig != null && sig.getParameters() != null) {
                for (Variable param : sig.getParameters()) {
                    if (available.contains(param.getName())) {
                        propagated.add(param.getName());
                        param.setOrigin(Variable.VariableOrigin.SERVER_OUTPUT);
                    } else {
                        scan.warnOnTypeOnlyMatch(functionName, param, available);
                    }
                }
            }

            Set<String> known = scan.propagatedParamsPerFunc.get(functionName);
            if (known == null) {
                known = new LinkedHashSet<String>();
                scan.propagatedParamsPerFunc.put(functionName, known);
            }
            known.addAll(propagated);

            // The \result binding becomes available only AFTER this block.
            String produced = spec.getResultBinding();
            if (produced != null) {
                available.add(produced);
                scan.producerOfVar.put(produced, functionName);
                scan.typeOfVar.put(produced, spec.getReturnTypeName());
            }

            scan.steps.add(new Step(i, functionName, propagated, produced,
                                    new LinkedHashSet<String>(available)));
        }
        return scan;
    }

    /**
     * Section 6, limitation #3 mitigation: flag a parameter whose type matches an
     * available SERVER_OUTPUT while its name does not, since that is the shape a
     * naming mismatch takes.
     */
    private void warnOnTypeOnlyMatch(String functionName, Variable param, Set<String> available) {
        for (String candidate : available) {
            String candidateType = typeOfVar.get(candidate);
            if (candidateType != null && candidateType.equals(param.getTypeName())) {
                warnings.add("propagation not detected: " + functionName + "("
                        + param.getTypeName() + " " + param.getName() + ") has the same type as the"
                        + " available SERVER_OUTPUT '" + candidate + "' but a different name."
                        + " Propagation is name-based (Section 6, limitation #3) —"
                        + " rename the parameter to '" + candidate + "' if they denote the same value.");
            }
        }
    }

    /** The per-block trace, in test-string order. */
    public List<Step> getSteps() { return Collections.unmodifiableList(steps); }

    /** function name -> parameter names it receives by propagation. */
    public Map<String, Set<String>> getPropagatedParamsPerFunc() {
        return Collections.unmodifiableMap(propagatedParamsPerFunc);
    }

    /** Parameters of {@code functionName} that arrive from an upstream SERVER_OUTPUT. */
    public Set<String> propagatedParamsOf(String functionName) {
        Set<String> params = propagatedParamsPerFunc.get(functionName);
        return params != null ? params : Collections.<String>emptySet();
    }

    /** All SERVER_OUTPUT names produced anywhere in the test string. */
    public Set<String> getAllServerOutputs() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(producerOfVar.keySet()));
    }

    /** Declared type of a produced SERVER_OUTPUT. */
    public String typeOf(String varName) { return typeOfVar.get(varName); }

    /** Function whose \result binding produced this SERVER_OUTPUT. */
    public String producerOf(String varName) { return producerOfVar.get(varName); }

    /** True when any block in the test string produces or consumes a SERVER_OUTPUT. */
    public boolean hasReturnValueHandling() {
        if (!producerOfVar.isEmpty()) return true;
        for (Set<String> p : propagatedParamsPerFunc.values()) {
            if (!p.isEmpty()) return true;
        }
        return false;
    }

    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }

    /** Human-readable trace, in the shape of the spec's expected trace table. */
    public String traceString() {
        StringBuilder sb = new StringBuilder();
        for (Step step : steps) sb.append(step).append('\n');
        return sb.toString();
    }
}
