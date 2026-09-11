package in.ac.iiitb.plproject;

import in.ac.iiitb.plproject.atc.LibraryDryRunExamples;
import in.ac.iiitb.plproject.verify.GeneratedSuiteHarness;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Surfaces the checked-in test cases as JUnit tests.
 *
 * <p>The test cases are the {@code .tests} files in {@code specs/} — one test
 * string per library, with its CLIENT_INPUTs bound.  Running them used to mean
 * running {@code verify-generated.sh} and reading its output; here each one is
 * fed through the whole pipeline, compiled, executed, and then reported to JUnit
 * <em>condition by condition</em>, so the surefire report names every
 * precondition and postcondition the generated singular case actually checked:
 *
 * <pre>
 *   StackDryRun : push -&gt; push -&gt; pop -&gt; peek
 *     ├─ the generated sources compile
 *     ├─ block 0  push  requires  elem != null
 *     ├─ block 0  push  ensures   size == \old(size) + 1
 *     ├─ …
 *     └─ GeneratedATCs_JUnit replays the sequence with assertions enabled
 * </pre>
 *
 * <p>{@link LibraryDryRunInvariantsTest} asserts on the generated <em>code</em>;
 * this class asserts on what that code does when it runs against the real
 * library, which is the part a reader of the surefire report actually wants.
 */
@DisplayName("Generated test cases (specs/*.tests)")
class GeneratedTestCasesTest {

    private static final Path STUBS = Paths.get("..", "verify-stubs");
    private static final Path WORK  = Paths.get("target", "junit-testcases");

    /** Where the code generator puts its four files under an example's output directory. */
    private static final String GENERATED_PACKAGE_PATH =
            "in/ac/iiitb/plproject/atc/generated";

    private static Map<LibraryDryRunExamples.Result, GeneratedSuiteHarness.Outcome> outcomes;

    @BeforeAll
    static void generateCompileAndRunEveryTestCase() throws IOException {
        GeneratedSuiteHarness harness = new GeneratedSuiteHarness(STUBS, WORK);
        outcomes = new LinkedHashMap<LibraryDryRunExamples.Result, GeneratedSuiteHarness.Outcome>();
        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            LibraryDryRunExamples.Result generated = LibraryDryRunExamples.run(example, false);
            outcomes.put(generated, harness.verify(
                    Paths.get(example.outputDir, GENERATED_PACKAGE_PATH), example.key));
        }
    }

    @TestFactory
    @DisplayName("conditions")
    Stream<DynamicNode> testCases() {
        List<DynamicNode> containers = new ArrayList<DynamicNode>();
        for (Map.Entry<LibraryDryRunExamples.Result, GeneratedSuiteHarness.Outcome> entry
                : outcomes.entrySet()) {
            containers.add(containerFor(entry.getKey(), entry.getValue()));
        }
        return containers.stream();
    }

    private static DynamicContainer containerFor(LibraryDryRunExamples.Result generated,
                                                 GeneratedSuiteHarness.Outcome outcome) {
        final LibraryDryRunExamples.Example example = generated.example;
        List<DynamicNode> children = new ArrayList<DynamicNode>();

        children.add(DynamicTest.dynamicTest("the generated sources compile", () ->
                assertTrue(outcome.compiled(),
                        () -> "generated sources for " + example.key + " do not compile:\n"
                                + outcome.getCompileDiagnostics())));

        if (!outcome.compiled()) {
            // Nothing ran, so there are no conditions to report; the compile test above
            // is the whole finding.
            return DynamicContainer.dynamicContainer(title(generated), children);
        }

        // One JUnit test per pre/postcondition the singular case reached.
        for (final GeneratedSuiteHarness.Condition condition : outcome.getConditions()) {
            children.add(DynamicTest.dynamicTest(condition.describe(), () ->
                    assertTrue(condition.holds(), () ->
                            example.key + ": " + condition.describe() + " does not hold")));
        }

        // A precondition failure stops the singular case, so conditions after it are
        // never reached. Comparing the exit code against the conditions we did see is
        // what catches a run that stopped early.
        children.add(DynamicTest.dynamicTest(
                "the singular case runs to the end of the test string", () -> {
            assertTrue(!outcome.getConditions().isEmpty(),
                    () -> example.key + ": the singular case reported no conditions at all:\n"
                            + outcome.getSingularCaseOutput());
            assertEquals(0, outcome.getSingularCaseExit(),
                    () -> example.key + ": singular case exited non-zero:\n"
                            + outcome.getSingularCaseOutput());
        }));

        children.add(DynamicTest.dynamicTest(
                "GeneratedATCs_JUnit replays the sequence with assertions enabled", () -> {
            // The JUnit flavour binds CLIENT_INPUTs to dummy placeholders rather than
            // values solved against the precondition, so a spec the dummies do not
            // satisfy aborts on its first assumeTrue. JUnit calls that skipped, and
            // reporting it as skipped here is what keeps it distinct from a real failure.
            Assumptions.assumeFalse(outcome.junitAssumptionNotMet(),
                    () -> example.key + ": the JUnit flavour's placeholder CLIENT_INPUTs do not"
                            + " satisfy the precondition, so the sequence never ran."
                            + " The singular case above is the oracle for this example.");
            if (outcome.getJunitExit() != 0) {
                fail(example.key + ": the generated JUnit flavour failed:\n"
                        + outcome.getJunitOutput());
            }
        }));

        return DynamicContainer.dynamicContainer(title(generated), children);
    }

    /** The test case's own name and sequence, so the report names what the author wrote. */
    private static String title(LibraryDryRunExamples.Result generated) {
        return generated.testString.getName() + " : "
                + String.join(" -> ", generated.testString.getCalls())
                + "   [" + generated.example.testStringPath.replace("../", "") + "]";
    }
}
