package in.ac.iiitb.plproject;

import in.ac.iiitb.plproject.atc.LibraryDryRunExamples;
import in.ac.iiitb.plproject.atc.PropagationScan;
import in.ac.iiitb.plproject.ast.AstHelper;
import in.ac.iiitb.plproject.ast.Expr;
import in.ac.iiitb.plproject.atc.ir.AtcTestMethod;
import in.ac.iiitb.plproject.atc.ConcreteInput;
import in.ac.iiitb.plproject.parser.SpecToAstConverter;
import in.ac.iiitb.plproject.parser.TestStringParser;
import in.ac.iiitb.plproject.parser.TestStringValidator;
import in.ac.iiitb.plproject.parser.ast.JmlFunctionSpec;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.parser.ast.Variable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the library dry runs.
 *
 * <p>They encode the cross-example invariants of the implementation spec's
 * Section 5, the per-example expected behaviours of Section 4, and a guard that
 * the ordinary functional pipeline is unaffected by the return-value handling.
 *
 * <p>The Section 4 tests name their example; the Section 5 ones walk
 * {@link LibraryDryRunExamples#ALL}, so a newly registered library is held to
 * every cross-example invariant — and to the golden-file comparison — the moment
 * it is added.
 */
@DisplayName("Library dry runs with return value handling")
class LibraryDryRunInvariantsTest {

    private static Map<String, LibraryDryRunExamples.Result> results;

    @BeforeAll
    static void generateAllExamples() throws IOException {
        results = LibraryDryRunExamples.runAll();
    }

    private static LibraryDryRunExamples.Result stack()     { return results.get("stack"); }
    private static LibraryDryRunExamples.Result hashMap()   { return results.get("hashmap"); }
    private static LibraryDryRunExamples.Result taskQueue() { return results.get("taskqueue"); }
    private static LibraryDryRunExamples.Result ticketService() { return results.get("ticketservice"); }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 4 — per-example expected behaviour
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Example 1: pop and peek capture independently, with no propagation between them")
    void stackCapturesAreIndependent() {
        PropagationScan scan = stack().scan;

        assertEquals(Arrays.asList("push", "push", "pop", "peek"), functionNames(scan));
        for (PropagationScan.Step step : scan.getSteps()) {
            assertTrue(step.getPropagatedParams().isEmpty(),
                    "no value propagates anywhere in the Stack example, but " + step.getFunctionName()
                            + " received " + step.getPropagatedParams());
        }
        assertEquals(new LinkedHashSet<>(Arrays.asList("poppedElem", "peekedElem")),
                scan.getAllServerOutputs());

        // peek takes no parameters, so the scan must not try to forward poppedElem into it.
        assertTrue(helper(stack(), "peek_helper").getParameters().isEmpty());
        assertTrue(helper(stack(), "pop_helper").getParameters().isEmpty());

        // main() stores both results independently.
        assertTrue(stack().spfCode.contains("String poppedElem = instance.pop_helper();"));
        assertTrue(stack().spfCode.contains("String peekedElem = instance.peek_helper();"));

        // Both placeholders are Strings.
        assertTrue(stack().spfCode.contains("String poppedElem = Debug.makeSymbolicString(\"poppedElem\")"));
        assertTrue(stack().spfCode.contains("String peekedElem = Debug.makeSymbolicString(\"peekedElem\")"));
    }

    @Test
    @DisplayName("Example 2: oldVal is nullable, produced twice and never consumed")
    void hashMapProducesValuesNobodyConsumes() {
        PropagationScan scan = hashMap().scan;

        assertEquals(Arrays.asList("put", "put", "getOldValue", "remove"), functionNames(scan));
        assertEquals(new LinkedHashSet<>(Arrays.asList("oldVal", "curVal")), scan.getAllServerOutputs());

        // Produced but not consumed: no helper takes either value as a parameter…
        for (AtcTestMethod helperMethod : hashMap().atcIr.getTestMethods()) {
            for (Variable parameter : helperMethod.getParameters()) {
                assertFalse(scan.getAllServerOutputs().contains(parameter.getName()),
                        helperMethod.getMethodName() + " must not take " + parameter.getName()
                                + " as a parameter: nothing downstream names it");
            }
        }
        // …yet both are still returned, one local per producing block.
        assertTrue(hashMap().spfCode.contains("Integer oldVal_0 = instance.put_helper();"));
        assertTrue(hashMap().spfCode.contains("Integer oldVal_1 = instance.put_helper();"));
        assertTrue(hashMap().spfCode.contains("Integer curVal = instance.getOldValue_helper();"));

        // key is CLIENT_INPUT, declared afresh in every block that names it.
        assertEquals(3, countOccurrences(hashMap().spfCode, "String key = Debug.makeSymbolicString(\"key\")"),
                "put, getOldValue and remove must each declare their own symbolic key");
    }

    @Test
    @DisplayName("Example 3: the propagation-scan trace matches the spec's table exactly")
    void taskQueueTraceMatchesSpec() {
        List<PropagationScan.Step> steps = taskQueue().scan.getSteps();
        assertEquals(3, steps.size());

        assertEquals("submit", steps.get(0).getFunctionName());
        assertEquals(setOf("taskId"), steps.get(0).getAvailableAfter());
        assertEquals(setOf(), steps.get(0).getPropagatedParams());

        assertEquals("getResult", steps.get(1).getFunctionName());
        assertEquals(setOf("taskId", "result"), steps.get(1).getAvailableAfter());
        assertEquals(setOf("taskId"), steps.get(1).getPropagatedParams());

        assertEquals("cancelTask", steps.get(2).getFunctionName());
        assertEquals(setOf("taskId", "result"), steps.get(2).getAvailableAfter());
        assertEquals(setOf("taskId"), steps.get(2).getPropagatedParams());
    }

    @Test
    @DisplayName("Example 3: main() threads taskId explicitly through both consuming blocks")
    void taskQueueMainThreadsTaskId() {
        for (String code : Arrays.asList(taskQueue().spfCode, taskQueue().junitCode)) {
            assertTrue(code.contains("Integer taskId = instance.submit_helper();"), code);
            assertTrue(code.contains("String result = instance.getResult_helper(taskId);"), code);
            assertTrue(code.contains("instance.cancelTask_helper(taskId);"), code);
        }
        // result enters availableServerOutputs but is never forwarded onward.
        assertTrue(helper(taskQueue(), "cancelTask_helper").getParameters().stream()
                        .noneMatch(v -> v.getName().equals("result")));
    }

    @Test
    @DisplayName("Example 4: a repeated read-only query captures independently in each block")
    void ticketServiceRepeatedQueryCapturesIndependently() {
        PropagationScan scan = ticketService().scan;

        assertEquals(Arrays.asList("numSeatsAvailable", "findAndHoldSeats",
                                   "reserveSeats", "numSeatsAvailable"), functionNames(scan));
        assertEquals(new LinkedHashSet<>(Arrays.asList("freeCount", "seatHoldId", "confirmationCode")),
                scan.getAllServerOutputs());

        // numSeatsAvailable runs twice and neither capture feeds the other: it takes
        // no parameters, so the scan must not forward the first freeCount into the second.
        assertTrue(helper(ticketService(), "numSeatsAvailable_helper").getParameters().isEmpty());
        assertTrue(ticketService().spfCode.contains(
                "Integer freeCount_0 = instance.numSeatsAvailable_helper();"));
        assertTrue(ticketService().spfCode.contains(
                "Integer freeCount_3 = instance.numSeatsAvailable_helper();"));

        // seatHoldId is the one value that does propagate, one hop, into reserveSeats.
        assertEquals(setOf("seatHoldId"), scan.propagatedParamsOf("reserveSeats"));
        assertSignature(ticketService(), "reserveSeats_helper",
                "String reserveSeats_helper(Integer seatHoldId)");
        for (String code : Arrays.asList(ticketService().spfCode, ticketService().junitCode)) {
            assertTrue(code.contains("Integer seatHoldId = instance.findAndHoldSeats_helper();"), code);
            assertTrue(code.contains(
                    "String confirmationCode = instance.reserveSeats_helper(seatHoldId);"), code);
        }
    }

    @Test
    @DisplayName("Example 4: customerEmail is a CLIENT_INPUT in two blocks, not a propagated value")
    void ticketServiceDoesNotPropagateASharedParameterName() {
        // findAndHoldSeats and reserveSeats both declare a parameter called
        // customerEmail. They are independent CLIENT_INPUTs that happen to share a
        // name, so neither helper may take it as a parameter and the test string
        // must bind it once per block.
        for (AtcTestMethod helperMethod : ticketService().atcIr.getTestMethods()) {
            for (Variable parameter : helperMethod.getParameters()) {
                assertFalse(parameter.getName().equals("customerEmail"),
                        helperMethod.getMethodName() + " must not take customerEmail as a parameter:"
                                + " it is a CLIENT_INPUT, not a SERVER_OUTPUT");
            }
        }
        assertEquals(2, countOccurrences(ticketService().singularCaseCode,
                        "String customerEmail = \"alice@example.com\";"),
                "findAndHoldSeats and reserveSeats must each declare their own customerEmail,"
                        + " bound separately by the test string");
        assertEquals(2, countOccurrences(ticketService().spfCode,
                        "String customerEmail = Debug.makeSymbolicString(\"customerEmail\")"),
                "the symbolic flavour declares a fresh customerEmail in each block that names it");
    }

    @Test
    @DisplayName("Example 4: a postcondition may offset old state by a CLIENT_INPUT, not just a literal")
    void ticketServicePostconditionUsesAClientInputInItsArithmetic() {
        // Examples 1 to 3 only ever offset old state by a constant. Here the
        // subtrahend is numSeats, a CLIENT_INPUT declared in the same block.
        for (String code : Arrays.asList(ticketService().spfCode, ticketService().junitCode,
                                         ticketService().singularCaseCode)) {
            assertTrue(code.contains("int available_old = Helper.available;"), code);
            assertTrue(code.contains(
                    "java.util.Objects.equals(Helper.available, (available_old - numSeats))"), code);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 5 — cross-example invariants
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Invariant 1: a no-parameter function that returns a value gets an empty parameter list")
    void invariant1_noParameterServerOutputFunctions() {
        assertSignature(stack(), "pop_helper", "String pop_helper()");
        assertSignature(stack(), "peek_helper", "String peek_helper()");
        assertSignature(taskQueue(), "submit_helper", "Integer submit_helper()");
        assertSignature(hashMap(), "put_helper", "Integer put_helper()");
    }

    @Test
    @DisplayName("Invariant 2: a nullable SERVER_OUTPUT is boxed and attracts no non-null assertion")
    void invariant2_nullableServerOutputHasNoSpuriousAssertion() {
        AtcTestMethod put = helper(hashMap(), "put_helper");
        assertEquals("Integer", put.getReturnType(),
                "put's previous value is null when the key was absent, so it must be boxed");

        for (String code : Arrays.asList(hashMap().spfCode, hashMap().junitCode)) {
            for (String assertion : assertionsIn(code)) {
                assertFalse(assertion.contains("oldVal"),
                        "the spec constrains nothing about oldVal, so no assertion may mention it: " + assertion);
            }
        }
    }

    @Test
    @DisplayName("Invariant 3: produced-but-unconsumed values stay valid — captured, returned, never forced into a parameter")
    void invariant3_producedButUnconsumedValues() {
        for (LibraryDryRunExamples.Result result : results.values()) {
            Set<String> serverOutputs = result.scan.getAllServerOutputs();
            for (AtcTestMethod helperMethod : result.atcIr.getTestMethods()) {
                for (Variable parameter : helperMethod.getParameters()) {
                    assertTrue(result.scan.propagatedParamsOf(
                                    helperMethod.getMethodName().replace("_helper", ""))
                                    .contains(parameter.getName()),
                            helperMethod.getMethodName() + " has a parameter the scan never marked propagated: "
                                    + parameter.getName());
                }
            }
            for (String produced : serverOutputs) {
                assertTrue(result.spfCode.contains("return " + produced + ";"),
                        produced + " is captured, so its helper must return it");
            }
        }
    }

    @Test
    @DisplayName("Invariant 4: a SERVER_OUTPUT threads correctly through two consuming blocks")
    void invariant4_multiHopPropagation() {
        assertSignature(taskQueue(), "getResult_helper", "String getResult_helper(Integer taskId)");
        assertSignature(taskQueue(), "cancelTask_helper", "void cancelTask_helper(Integer taskId)");
    }

    @Test
    @DisplayName("Invariant 5: the SPF and JUnit files share signatures and assertions; only bodies differ")
    void invariant5_spfAndJUnitAgree() {
        for (LibraryDryRunExamples.Result result : results.values()) {
            assertLinesMatch(helperSignaturesIn(result.spfCode), helperSignaturesIn(result.junitCode),
                    result.example.key + ": helper signatures must be identical in both flavours");
            assertLinesMatch(assertionsIn(result.spfCode), assertionsIn(result.junitCode),
                    result.example.key + ": assertions must be identical in both flavours");

            // …and the bodies really do differ, in exactly the documented way.
            assertTrue(result.spfCode.contains("Debug.assume("));
            assertTrue(result.junitCode.contains("assumeTrue("));
            assertFalse(result.junitCode.contains("Debug."),
                    "the JUnit flavour must discard every symbolic placeholder");
            for (String produced : result.scan.getAllServerOutputs()) {
                assertTrue(result.junitCode.contains(
                        "Response " + produced + "Response = executeApiCall("));
                assertTrue(result.junitCode.contains(
                        "extractFromResponse(" + produced + "Response, \"" + produced + "\")"));
            }
        }
    }

    @Test
    @DisplayName("Invariant 6: old-state snapshots are ordinary locals of the declared state type")
    void invariant6_oldStateSnapshotsAreOrdinaryLocals() {
        assertTrue(stack().spfCode.contains("int size_old = Helper.size;"));
        assertTrue(hashMap().spfCode.contains("int size_old = Helper.size;"));
        assertTrue(taskQueue().spfCode.contains("int nextId_old = Helper.nextId;"));

        // A snapshot is a plain read, never a symbolic value or a special-cased node.
        for (LibraryDryRunExamples.Result result : results.values()) {
            for (String line : result.spfCode.split("\n")) {
                if (line.contains("_old =")) {
                    assertFalse(line.contains("Debug."), "snapshot must be a plain read: " + line);
                }
            }
        }
    }

    @Test
    @DisplayName("Invariant 7: a boxed Integer SERVER_OUTPUT routes to makeSymbolicInteger, never makeSymbolicRef")
    void invariant7_boxedPrimitivesUsePrimitiveFactory() {
        assertTrue(hashMap().spfCode.contains("Integer oldVal = Debug.makeSymbolicInteger(\"oldVal\")"));
        assertTrue(hashMap().spfCode.contains("Integer curVal = Debug.makeSymbolicInteger(\"curVal\")"));
        assertTrue(taskQueue().spfCode.contains("Integer taskId = Debug.makeSymbolicInteger(\"taskId\")"));
        assertTrue(taskQueue().spfCode.contains("taskId = Debug.makeSymbolicInteger(\"taskId\");"),
                "a propagated Integer is re-bound through the primitive factory too");

        for (LibraryDryRunExamples.Result result : results.values()) {
            assertFalse(result.spfCode.contains("makeSymbolicRef"),
                    result.example.key + " has no custom types, so nothing may hit the makeSymbolicRef fallback");
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Test strings as files, and the singular case they enable
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A test string file parses into its sequence and its concrete inputs")
    void testStringParsing() {
        TestStringAst parsed = TestStringParser.parse(
                "test Demo {\n"
              + "    sequence: push -> push -> pop;\n"
              + "    inputs {\n"
              + "        push[0].elem = \"alpha\";\n"
              + "        push[1].elem = \"beta\";\n"
              + "    }\n"
              + "}\n");

        assertEquals("Demo", parsed.getName());
        assertEquals(Arrays.asList("push", "push", "pop"), parsed.getCalls());
        assertTrue(parsed.isSingularCase());
        assertEquals("\"alpha\"", parsed.inputsForBlock(0).get("elem").getLiteral());
        assertEquals("\"beta\"", parsed.inputsForBlock(1).get("elem").getLiteral());
        assertTrue(parsed.inputsForBlock(2).isEmpty(), "pop takes no CLIENT_INPUT");

        // Comments are ignored, and the bare one-liner form works too.
        TestStringAst bare = TestStringParser.parse("/* c */ submit -> getResult // trailing\n");
        assertEquals(Arrays.asList("submit", "getResult"), bare.getCalls());
        assertFalse(bare.isSingularCase());

        assertThrows(TestStringParser.TestStringSyntaxException.class,
                () -> TestStringParser.parse("test Broken { inputs { } }"),
                "a test with no sequence clause is not a test string");
    }

    @Test
    @DisplayName("The three checked-in test strings are valid against their specs")
    void checkedInTestStringsAreValid() {
        for (LibraryDryRunExamples.Result result : results.values()) {
            assertTrue(result.validation.isValid(),
                    result.example.key + " test string is invalid:\n" + result.validation.describe());
            assertTrue(result.testString.isSingularCase(),
                    result.example.key + " must bind every CLIENT_INPUT to be a singular case");
        }
        assertEquals(Arrays.asList("push", "push", "pop", "peek"), stack().testString.getCalls());
        assertEquals(Arrays.asList("put", "put", "getOldValue", "remove"), hashMap().testString.getCalls());
        assertEquals(Arrays.asList("submit", "getResult", "cancelTask"), taskQueue().testString.getCalls());
    }

    @Test
    @DisplayName("Validation rejects an unknown call, an unbound input and a bound SERVER_OUTPUT")
    void validationCatchesBrokenTestStrings() {
        JmlSpecAst spec = taskQueue().specAst;

        // A block naming a function the spec does not declare.
        assertFalse(TestStringValidator.validate(spec,
                TestStringParser.parse("submit -> notAFunction"), false).isValid());

        // A CLIENT_INPUT with no value cannot be run as a singular case…
        TestStringAst unbound = TestStringParser.parse("test T { sequence: submit; }");
        assertFalse(TestStringValidator.validate(spec, unbound, true).isValid());
        // …but is fine for the symbolic pipeline, where the solver supplies it.
        assertTrue(TestStringValidator.validate(spec, unbound, false).isValid());

        // taskId is a SERVER_OUTPUT: the caller cannot know it in advance.
        TestStringAst boundServerOutput = TestStringParser.parse(
                "test T {\n"
              + "    sequence: submit -> getResult;\n"
              + "    inputs {\n"
              + "        submit[0].payload = \"p\";\n"
              + "        getResult[1].taskId = 7;\n"
              + "    }\n"
              + "}\n");
        TestStringValidator.Report report = TestStringValidator.validate(spec, boundServerOutput, true);
        assertFalse(report.isValid());
        assertTrue(report.getErrors().stream()
                        .anyMatch(problem -> problem.getMessage().contains("SERVER_OUTPUT")),
                "binding a propagated value must be rejected: " + report.describe());

        // A binding that names the wrong function for its block index.
        TestStringAst misaddressed = TestStringParser.parse(
                "test T { sequence: submit -> getResult; inputs { getResult[0].payload = \"p\"; } }");
        assertFalse(TestStringValidator.validate(spec, misaddressed, true).isValid());
    }

    @Test
    @DisplayName("The singular case checks every precondition before its call and every postcondition after")
    void singularCaseChecksEveryCondition() {
        for (LibraryDryRunExamples.Result result : results.values()) {
            String code = result.singularCaseCode;

            int expectedPre = 0;
            int expectedPost = 0;
            for (String call : result.testString.getCalls()) {
                JmlFunctionSpec spec = result.specAst.findSpecFor(call);
                if (spec.getPrecondition() != null) expectedPre++;
                for (Expr conjunct : AstHelper.splitConjuncts(spec.getPostcondition())) {
                    if (!AstHelper.isResultBindingConjunct(conjunct, spec.getResultBinding())) expectedPost++;
                }
            }
            // Count call sites only — the footer also *declares* require/ensure.
            assertEquals(expectedPre, countMatches(code, "\\brequire\\(\\d"),
                    result.example.key + ": one require() per block with a precondition");
            assertEquals(expectedPost, countMatches(code, "\\bensure\\(\\d"),
                    result.example.key + ": one ensure() per postcondition conjunct");

            // Concrete inputs are bound; nothing is left to a solver.
            for (ConcreteInput input : result.testString.getConcreteInputs()) {
                assertTrue(code.contains(input.getParamName() + " = " + input.getLiteral() + ";"),
                        result.example.key + ": missing concrete input " + input);
            }
            assertFalse(code.contains("Debug."),
                    result.example.key + ": a singular case is concrete, with no symbolic values");
            assertTrue(code.contains("Helper.reset();"),
                    result.example.key + ": the case must start from the declared initial state");
        }
    }

    @Test
    @DisplayName("A propagated value is threaded into the singular case, never re-declared from itself")
    void singularCaseThreadsPropagatedValues() {
        String code = taskQueue().singularCaseCode;
        assertTrue(code.contains("taskId = Helper.submit(payload);"));
        assertTrue(code.contains("Helper.getResult(taskId)"));
        assertTrue(code.contains("Helper.cancelTask(taskId)"));
        assertFalse(code.contains("Integer taskId = taskId;"),
                "a value already in scope under that name must not be re-declared from itself");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Guards on the machinery the library examples lean on
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A \\result binding is detected from the AST, and a parameter is never mistaken for one")
    void resultBindingDetection() throws IOException {
        JmlSpecAst taskQueueSpec = taskQueue().specAst;
        assertEquals("taskId", taskQueueSpec.findSpecFor("submit").getResultBinding());
        assertEquals("result", taskQueueSpec.findSpecFor("getResult").getResultBinding());
        assertNull(taskQueueSpec.findSpecFor("cancelTask").getResultBinding());

        // A purely functional spec binds nothing: `\result == x` names this
        // function's OWN parameter, which is CLIENT_INPUT, not SERVER_OUTPUT.
        JmlSpecAst mathUtils = SpecToAstConverter.convertSpecToAst(read("../jml_parser/examples/MathUtils.spec"));
        for (String function : Arrays.asList("sqrt", "divide", "abs", "power")) {
            JmlFunctionSpec spec = mathUtils.findSpecFor(function);
            assertNull(spec.getResultBinding(),
                    function + " has no \\result binding, so the original generation path must still apply");
        }
    }

    @Test
    @DisplayName("The state block gives every library its declared global state")
    void stateBlockIsParsed() {
        assertEquals("int", stack().specAst.getStateVarType("size"));
        assertEquals("List<String>", stack().specAst.getStateVarType("S"));
        assertEquals("Map<String,Integer>", hashMap().specAst.getStateVarType("M"));
        assertEquals("int", taskQueue().specAst.getStateVarType("nextId"));
        assertTrue(SpecToAstConverter.convertSpecToAst("spec f { signature: void f(); requires: true; ensures: true; }")
                        .getStateVars().isEmpty(),
                "a spec file without a state block must behave exactly as before");
    }

    @Test
    @DisplayName("A state block routes to the library path even when nothing returns a value")
    void stateBlockAloneSelectsTheLibraryPath() throws IOException {
        // Found by the generated test-string families: a sequence of nothing but
        // void calls has no return-value handling, and the generator used to read
        // that as "this is a purely functional spec" and take the original path —
        // which does not qualify state names or snapshot \old(...). The result was
        // `assert(java.util.Objects.equals(size, (\old(size) + 1)))` emitted into
        // Java, which does not compile. Every checked-in test string happens to call
        // something that returns a value, so nothing caught it until sequences were
        // generated.
        JmlSpecAst spec = stack().specAst;
        TestStringAst onlyVoidCalls = TestStringParser.parse(
                "test AllPushes {\n"
              + "    sequence: push -> push;\n"
              + "    inputs {\n"
              + "        push[0].elem = \"alpha\";\n"
              + "        push[1].elem = \"beta\";\n"
              + "    }\n"
              + "}\n");

        PropagationScan scan = PropagationScan.scan(spec, onlyVoidCalls);
        assertFalse(scan.hasReturnValueHandling(),
                "a sequence of pushes produces and consumes no SERVER_OUTPUT");

        in.ac.iiitb.plproject.atc.ir.AtcClass atc = new in.ac.iiitb.plproject.atc.NewGenATC()
                .generateAtcFile(spec, onlyVoidCalls);
        String spf = new in.ac.iiitb.plproject.atc.ir.AtcIrCodeGenerator()
                .generateSymbolicJavaFile(atc);

        // The library path is what qualifies state as Helper.<name> and turns an
        // \old(...) into a snapshot taken before the call.
        assertTrue(spf.contains("int size_old = Helper.size;"),
                "the state snapshot must be a plain read of the library's field:\n" + spf);
        assertFalse(spf.contains("\\old("),
                "no JML token may survive into the emitted Java:\n" + spf);
    }

    @Test
    @DisplayName("Generated files match the checked-in golden output")
    void goldenFilesAreUnchanged() throws IOException {
        for (LibraryDryRunExamples.Result result : results.values()) {
            assertEquals(read("golden/" + result.example.key + "/GeneratedATCs.java"),
                         result.spfCode,
                         result.example.key + ": SPF output drifted from its golden file");
            assertEquals(read("golden/" + result.example.key + "/GeneratedATCs_JUnit.java"),
                         result.junitCode,
                         result.example.key + ": JUnit output drifted from its golden file");
            assertEquals(read("golden/" + result.example.key + "/SingularCase.java"),
                         result.singularCaseCode,
                         result.example.key + ": singular case drifted from its golden file");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void assertNull(Object value, String message) {
        assertTrue(value == null, message + " (was " + value + ")");
    }

    private static void assertNull(Object value) {
        assertTrue(value == null, "expected null but was " + value);
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static Set<String> setOf(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    private static List<String> functionNames(PropagationScan scan) {
        List<String> names = new ArrayList<>();
        for (PropagationScan.Step step : scan.getSteps()) names.add(step.getFunctionName());
        return names;
    }

    private static AtcTestMethod helper(LibraryDryRunExamples.Result result, String methodName) {
        for (AtcTestMethod method : result.atcIr.getTestMethods()) {
            if (method.getMethodName().equals(methodName)) return method;
        }
        throw new AssertionError("no helper named " + methodName + " in " + result.example.key);
    }

    private static void assertSignature(LibraryDryRunExamples.Result result, String methodName,
                                        String expected) {
        assertEquals(expected, helper(result, methodName).signatureString());
        assertTrue(result.spfCode.contains("public " + expected + " {"),
                "SPF file must declare: public " + expected);
        assertTrue(result.junitCode.contains("public " + expected + " {"),
                "JUnit file must declare: public " + expected);
    }

    /** The `<returnType> <name>(<params>)` of every generated helper, in order. */
    private static List<String> helperSignaturesIn(String code) {
        List<String> signatures = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?m)^ {4}public (?!static )([^\\n{]+)\\{").matcher(code);
        while (matcher.find()) {
            String signature = matcher.group(1).trim();
            if (!signature.startsWith("void testSequence")) { // JUnit's harness entry point
                signatures.add(signature);
            }
        }
        return signatures;
    }

    /** Every generated assertion, in order. */
    private static List<String> assertionsIn(String code) {
        List<String> assertions = new ArrayList<>();
        for (String line : code.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("assert(")) assertions.add(trimmed);
        }
        return assertions;
    }

    private static int countMatches(String haystack, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(haystack);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
