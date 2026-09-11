package in.ac.iiitb.plproject.mutation;

import in.ac.iiitb.plproject.atc.ConcreteInput;
import in.ac.iiitb.plproject.atc.PropagationScan;
import in.ac.iiitb.plproject.parser.TestStringParser;
import in.ac.iiitb.plproject.parser.TestStringValidator;
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
import java.util.Random;
import java.util.Set;

/**
 * Builds a family of test strings for one library.
 *
 * <h3>Why</h3>
 *
 * Every library in this project ships exactly one test string, hand-written, and
 * a great many mutants survive for a reason that has nothing to do with the
 * specification: <em>the test string never asks</em>.  {@code specs/Stack.tests}
 * pushes twice and then pops, so by the time {@code peek} runs there is one
 * element left and "the top" and "the bottom" are the same element; a library
 * that peeked at the wrong end passes.  The mutation sets record several of these
 * and each note says the same thing — reorder the sequence and the fault dies,
 * with no change to the spec at all.
 *
 * <p>That is a claim worth testing rather than asserting.  This generator produces
 * a few hundred test strings per library instead of one, so the claim becomes a
 * measurement: how many of the survivors are survivors only because of the single
 * sequence they were run against?
 *
 * <h3>How a sequence is built</h3>
 *
 * <ol>
 *   <li>a length is drawn, then that many function names from the spec;</li>
 *   <li>the propagation scan runs over the draft sequence, which says which
 *       parameters arrive from an upstream SERVER_OUTPUT — those must NOT be
 *       bound, and are left alone;</li>
 *   <li>every remaining parameter is a CLIENT_INPUT and is bound to a literal
 *       drawn from the pool;</li>
 *   <li>the result goes through the same {@link TestStringValidator} the
 *       checked-in test strings do, and is discarded if it does not pass.</li>
 * </ol>
 *
 * <h3>Where the literals come from</h3>
 *
 * Mostly from the library's own checked-in {@code .tests} file.  A generated
 * sequence that invented its own strings would spend almost every run stopped at
 * the first precondition — {@code M.containsKey(key)} is only true for a key
 * something else put there — so the pool is seeded with the literals the author
 * already chose, per function and parameter, and only falls back to a generic
 * value by type.  Reusing them is what makes a generated sequence land inside the
 * library's contract often enough to be worth running.
 *
 * <p>Generation is seeded, so the same library always produces the same family:
 * a suite that changed between runs could not be a regression baseline.
 */
public class TestStringGenerator {

    /** Sequences shorter than this are not worth a run; longer ones rarely stay legal. */
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 7;

    /** Literals used when the checked-in test string offers nothing for a parameter. */
    private static final Map<String, String[]> FALLBACK_LITERALS = new LinkedHashMap<String, String[]>();
    static {
        FALLBACK_LITERALS.put("int",     new String[] { "0", "1", "2", "3", "7" });
        FALLBACK_LITERALS.put("Integer", new String[] { "0", "1", "2", "3", "7" });
        FALLBACK_LITERALS.put("long",    new String[] { "0", "1", "2" });
        FALLBACK_LITERALS.put("String",  new String[] { "\"alpha\"", "\"beta\"", "\"gamma\"" });
        FALLBACK_LITERALS.put("boolean", new String[] { "true", "false" });
        FALLBACK_LITERALS.put("Boolean", new String[] { "true", "false" });
        FALLBACK_LITERALS.put("double",  new String[] { "0.0", "1.0" });
        FALLBACK_LITERALS.put("char",    new String[] { "'a'", "'b'" });
    }

    /**
     * How often a sequence is built by editing the checked-in one rather than drawn
     * from scratch.
     *
     * <p>A sequence drawn at random is usually illegal: three quarters of them call
     * something outside its precondition — {@code pop} on an empty stack — and stop
     * at the first block. That is a legitimate test of the contract's boundary, and
     * it is useless as an oracle for a mutant, because a run that stops proves
     * nothing about the library.
     *
     * <p>Editing the checked-in sequence produces the family the mutation notes keep
     * describing: "reorder the test string to push -> push -> peek -> pop and this
     * fault dies". Those are the sequences worth having most of, so most of the
     * family is made that way, and the rest is drawn freely so the family is not
     * merely a neighbourhood of one point.
     */
    private static final double SEED_DERIVED_SHARE = 0.7;

    private final JmlSpecAst spec;
    private final List<String> functions;
    private final List<String> seedSequence;
    private final Map<String, List<String>> literalPool;
    private final Random random;

    /**
     * @param spec      the library's specification
     * @param seedTests the checked-in test string, mined for literals
     * @param seed      the RNG seed, so a family is reproducible
     */
    public TestStringGenerator(JmlSpecAst spec, TestStringAst seedTests, long seed) {
        this.spec = spec;
        this.random = new Random(seed);
        this.functions = new ArrayList<String>();
        for (JmlFunctionSpec function : spec.getSpecs()) {
            this.functions.add(function.getName());
        }
        this.seedSequence = seedTests == null
                ? new ArrayList<String>()
                : new ArrayList<String>(seedTests.getCalls());
        this.literalPool = buildLiteralPool(spec, seedTests);
    }

    /**
     * A pool of literals per {@code function.parameter}, from the checked-in test
     * string first and the type's fallbacks after it.
     */
    private static Map<String, List<String>> buildLiteralPool(JmlSpecAst spec,
                                                              TestStringAst seedTests) {
        Map<String, List<String>> pool = new LinkedHashMap<String, List<String>>();

        if (seedTests != null) {
            for (ConcreteInput input : seedTests.getConcreteInputs()) {
                String key = input.getFunctionName() + "." + input.getParamName();
                List<String> literals = pool.get(key);
                if (literals == null) {
                    literals = new ArrayList<String>();
                    pool.put(key, literals);
                }
                if (!literals.contains(input.getLiteral())) literals.add(input.getLiteral());
            }
        }

        // A literal chosen for one function's parameter is usually meaningful to
        // another's: the key hashmap.put stored is the key hashmap.remove needs.
        Set<String> byType = new LinkedHashSet<String>();
        for (JmlFunctionSpec function : spec.getSpecs()) {
            FunctionSignature signature = function.getSignature();
            if (signature == null || signature.getParameters() == null) continue;
            for (Variable parameter : signature.getParameters()) {
                String key = function.getName() + "." + parameter.getName();
                List<String> literals = pool.get(key);
                if (literals == null) {
                    literals = new ArrayList<String>();
                    pool.put(key, literals);
                }
                for (String shared : sharedLiteralsFor(seedTests, parameter.getTypeName())) {
                    if (!literals.contains(shared)) literals.add(shared);
                }
                String[] fallbacks = FALLBACK_LITERALS.get(parameter.getTypeName());
                if (fallbacks != null) {
                    for (String fallback : fallbacks) {
                        if (!literals.contains(fallback)) literals.add(fallback);
                    }
                }
                if (literals.isEmpty()) literals.add("null");
                byType.add(parameter.getTypeName());
            }
        }
        return pool;
    }

    /** Every literal the checked-in test string binds to a parameter of this type. */
    private static List<String> sharedLiteralsFor(TestStringAst seedTests, String typeName) {
        List<String> shared = new ArrayList<String>();
        if (seedTests == null) return shared;
        boolean textual = "String".equals(typeName);
        for (ConcreteInput input : seedTests.getConcreteInputs()) {
            boolean literalIsText = input.getLiteral().startsWith("\"");
            if (literalIsText == textual && !shared.contains(input.getLiteral())) {
                shared.add(input.getLiteral());
            }
        }
        return shared;
    }

    /**
     * Generates up to {@code count} distinct, valid test strings.
     *
     * <p>Distinct by rendered text, so two draws that produce the same sequence with
     * the same inputs count once. The walk gives up after a generous number of
     * attempts: a small library has only so many sequences of this length, and
     * spinning forever to reach a target the spec cannot supply would be worse than
     * returning what there is.
     */
    public List<String> generate(String namePrefix, int count) {
        List<String> generated = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        int attempts = 0;
        int maxAttempts = count * 60;

        while (generated.size() < count && attempts++ < maxAttempts) {
            List<String> sequence = drawSequence();
            String body = renderInputs(sequence);
            if (body == null) continue;

            String signature = String.join(",", sequence) + "|" + body;
            if (!seen.add(signature)) continue;

            String text = "test " + namePrefix + (generated.size() + 1) + " {\n"
                        + "    sequence: " + String.join(" -> ", sequence) + ";\n"
                        + "\n"
                        + "    inputs {\n" + body + "    }\n"
                        + "}\n";

            // The same gate the checked-in test strings pass through.
            TestStringAst parsed;
            try {
                parsed = TestStringParser.parse(text);
            } catch (RuntimeException notATestString) {
                continue;
            }
            if (!TestStringValidator.validate(spec, parsed, true).isValid()) continue;

            generated.add(text);
        }
        return generated;
    }

    private List<String> drawSequence() {
        if (!seedSequence.isEmpty() && random.nextDouble() < SEED_DERIVED_SHARE) {
            return editSeedSequence();
        }
        int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
        List<String> sequence = new ArrayList<String>();
        for (int index = 0; index < length; index++) {
            sequence.add(functions.get(random.nextInt(functions.size())));
        }
        return sequence;
    }

    /**
     * The checked-in sequence with one to three small edits.
     *
     * <p>The four edits are the four ways a reviewer describes a different test:
     * "what if these two were the other way round", "what if it happened twice",
     * "what if that step were missing", "what if this were called as well".
     */
    private List<String> editSeedSequence() {
        List<String> sequence = new ArrayList<String>(seedSequence);
        int edits = 1 + random.nextInt(3);

        for (int edit = 0; edit < edits && !sequence.isEmpty(); edit++) {
            int at = random.nextInt(sequence.size());
            switch (random.nextInt(4)) {
                case 0:                                          // swap with a neighbour
                    if (sequence.size() > 1) {
                        int other = random.nextInt(sequence.size());
                        String held = sequence.get(at);
                        sequence.set(at, sequence.get(other));
                        sequence.set(other, held);
                    }
                    break;
                case 1:                                          // do it twice
                    if (sequence.size() < MAX_LENGTH) sequence.add(at, sequence.get(at));
                    break;
                case 2:                                          // leave it out
                    if (sequence.size() > MIN_LENGTH) sequence.remove(at);
                    break;
                default:                                         // call something else too
                    if (sequence.size() < MAX_LENGTH) {
                        sequence.add(at, functions.get(random.nextInt(functions.size())));
                    }
                    break;
            }
        }
        return sequence;
    }

    /**
     * The {@code inputs} block for a sequence, or null when a parameter has no
     * literal to bind.
     */
    private String renderInputs(List<String> sequence) {
        TestStringAst draft = new TestStringAst("draft", sequence,
                                                new ArrayList<ConcreteInput>());
        PropagationScan scan = PropagationScan.scan(spec, draft);

        StringBuilder out = new StringBuilder();
        for (int block = 0; block < sequence.size(); block++) {
            String functionName = sequence.get(block);
            JmlFunctionSpec function = spec.findSpecFor(functionName);
            if (function == null) return null;

            FunctionSignature signature = function.getSignature();
            if (signature == null || signature.getParameters() == null) continue;

            Set<String> propagated = scan.getSteps().get(block).getPropagatedParams();
            for (Variable parameter : signature.getParameters()) {
                // A propagated value is the callee's to invent; binding one is exactly
                // what TestStringValidator rejects.
                if (propagated.contains(parameter.getName())) continue;

                List<String> literals = literalPool.get(functionName + "." + parameter.getName());
                if (literals == null || literals.isEmpty()) return null;

                out.append("        ").append(functionName).append('[').append(block)
                   .append("].").append(parameter.getName()).append(" = ")
                   .append(literals.get(random.nextInt(literals.size()))).append(";\n");
            }
        }
        return out.toString();
    }

    /** The functions this library declares, in spec order. */
    public List<String> getFunctions() {
        return Collections.unmodifiableList(functions);
    }
}
