package in.ac.iiitb.plproject.parser;

import in.ac.iiitb.plproject.atc.ConcreteInput;
import in.ac.iiitb.plproject.atc.PropagationScan;
import in.ac.iiitb.plproject.parser.ast.FunctionSignature;
import in.ac.iiitb.plproject.parser.ast.JmlFunctionSpec;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.parser.ast.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks a test string against the specification it will run against, BEFORE any
 * code is generated.
 *
 * <p>This is the static half of "validating a test string": everything that can
 * be decided by reading the sequence and the spec together.  It answers
 *
 * <ul>
 *   <li>does every block name a function the spec actually declares?</li>
 *   <li>is every CLIENT_INPUT of every block given a concrete value, so the
 *       sequence can be run as a singular case?</li>
 *   <li>does each input binding address a real block and a real parameter of the
 *       function at that position?</li>
 *   <li>does a binding try to pin down a SERVER_OUTPUT, which the caller cannot
 *       know in advance?</li>
 *   <li>does a consuming block expect a value nothing upstream produces?</li>
 * </ul>
 *
 * <p>What it deliberately does NOT answer is whether the preconditions actually
 * hold at each step: that depends on the library's state as the sequence runs, so
 * it is checked by executing the generated singular case, not by reading the spec.
 */
public class TestStringValidator {

    /** One problem found in a test string, with the block it belongs to. */
    public static class Problem {
        private final int blockIndex;
        private final String message;

        Problem(int blockIndex, String message) {
            this.blockIndex = blockIndex;
            this.message = message;
        }

        /** Block the problem concerns, or -1 when it is about the test string as a whole. */
        public int getBlockIndex() { return blockIndex; }

        public String getMessage() { return message; }

        @Override
        public String toString() {
            return (blockIndex >= 0 ? "block " + blockIndex + ": " : "") + message;
        }
    }

    /** Outcome of validating one test string. */
    public static class Report {
        private final List<Problem> errors;
        private final List<Problem> warnings;

        Report(List<Problem> errors, List<Problem> warnings) {
            this.errors = Collections.unmodifiableList(errors);
            this.warnings = Collections.unmodifiableList(warnings);
        }

        /** Problems that make the test string unusable. */
        public List<Problem> getErrors() { return errors; }

        /** Problems worth flagging that still leave the test string runnable. */
        public List<Problem> getWarnings() { return warnings; }

        public boolean isValid() { return errors.isEmpty(); }

        /** Human-readable summary, one problem per line. */
        public String describe() {
            StringBuilder sb = new StringBuilder();
            for (Problem error : errors)     sb.append("  ERROR   ").append(error).append('\n');
            for (Problem warning : warnings) sb.append("  WARNING ").append(warning).append('\n');
            if (errors.isEmpty() && warnings.isEmpty()) {
                sb.append("  the test string is valid against the specification\n");
            }
            return sb.toString();
        }
    }

    /**
     * Validates the sequence and, when it carries concrete inputs, the bindings too.
     *
     * @param requireConcreteInputs true when the test string is meant to be a
     *        singular case, so a CLIENT_INPUT left unbound is an error rather than
     *        something the solver would have supplied
     */
    public static Report validate(JmlSpecAst specAst, TestStringAst testString,
                                  boolean requireConcreteInputs) {
        List<Problem> errors = new ArrayList<Problem>();
        List<Problem> warnings = new ArrayList<Problem>();

        List<String> calls = testString.getCalls();
        if (calls.isEmpty()) {
            errors.add(new Problem(-1, "the test string is empty"));
            return new Report(errors, warnings);
        }

        // Every block must name a function the spec declares.
        for (int i = 0; i < calls.size(); i++) {
            if (specAst.findSpecFor(calls.get(i)) == null) {
                errors.add(new Problem(i, "no spec declares a function named '" + calls.get(i) + "'"));
            }
        }
        if (!errors.isEmpty()) return new Report(errors, warnings);

        PropagationScan scan = PropagationScan.scan(specAst, testString);
        Set<String> serverOutputs = scan.getAllServerOutputs();

        // Each block: which parameters must the caller supply, and are they supplied?
        for (int i = 0; i < calls.size(); i++) {
            JmlFunctionSpec spec = specAst.findSpecFor(calls.get(i));
            FunctionSignature signature = spec.getSignature();
            if (signature == null || signature.getParameters() == null) continue;

            Set<String> propagated = scan.getSteps().get(i).getPropagatedParams();
            Map<String, ConcreteInput> bound = testString.inputsForBlock(i);

            for (Variable param : signature.getParameters()) {
                boolean isPropagated = propagated.contains(param.getName());

                if (isPropagated && bound.containsKey(param.getName())) {
                    errors.add(new Problem(i, calls.get(i) + "." + param.getName()
                            + " is a SERVER_OUTPUT propagated from '"
                            + scan.producerOf(param.getName())
                            + "', so it cannot be given a concrete value — remove the binding"));
                } else if (!isPropagated && requireConcreteInputs
                        && !bound.containsKey(param.getName())) {
                    errors.add(new Problem(i, "CLIENT_INPUT " + calls.get(i) + "["
                            + i + "]." + param.getName() + " has no value; a singular case must bind"
                            + " every input the caller supplies"));
                }
            }

            // A binding that names a parameter the function does not have.
            Set<String> paramNames = new LinkedHashSet<String>();
            for (Variable param : signature.getParameters()) paramNames.add(param.getName());
            for (ConcreteInput input : bound.values()) {
                if (!paramNames.contains(input.getParamName())) {
                    errors.add(new Problem(i, calls.get(i) + " has no parameter named '"
                            + input.getParamName() + "'; it takes " + paramNames));
                }
            }
        }

        // Bindings that address a block that does not exist, or the wrong function.
        for (ConcreteInput input : testString.getConcreteInputs()) {
            int index = input.getBlockIndex();
            if (index < 0 || index >= calls.size()) {
                errors.add(new Problem(-1, "input binding " + input + " addresses block "
                        + index + ", but the test string has " + calls.size() + " blocks"));
            } else if (!calls.get(index).equals(input.getFunctionName())) {
                errors.add(new Problem(index, "input binding " + input + " names '"
                        + input.getFunctionName() + "' but block " + index + " calls '"
                        + calls.get(index) + "'"));
            }
        }

        // A produced value nobody consumes is fine, and worth saying out loud.
        for (String produced : serverOutputs) {
            boolean consumed = false;
            for (PropagationScan.Step step : scan.getSteps()) {
                if (step.getPropagatedParams().contains(produced)) { consumed = true; break; }
            }
            if (!consumed) {
                warnings.add(new Problem(-1, "SERVER_OUTPUT '" + produced + "' (from "
                        + scan.producerOf(produced) + ") is produced but never consumed downstream;"
                        + " it is captured and returned, which is valid"));
            }
        }

        // The propagation scan's own naming-mismatch hints belong in this report too.
        for (String warning : scan.getWarnings()) {
            warnings.add(new Problem(-1, warning));
        }

        return new Report(errors, warnings);
    }
}
