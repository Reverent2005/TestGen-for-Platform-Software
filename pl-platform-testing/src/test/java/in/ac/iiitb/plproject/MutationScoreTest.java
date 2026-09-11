package in.ac.iiitb.plproject;

import in.ac.iiitb.plproject.atc.LibraryDryRunExamples;
import in.ac.iiitb.plproject.mutation.Mutant;
import in.ac.iiitb.plproject.mutation.MutantSetParser;
import in.ac.iiitb.plproject.mutation.MutationOperator;
import in.ac.iiitb.plproject.mutation.MutationRunner;
import in.ac.iiitb.plproject.verify.GeneratedSuiteHarness;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs every mutation set through the whole pipeline and reports the result to
 * JUnit, one test per mutant.
 *
 * <p>Each mutant seeds a single fault into one library and the pipeline is
 * re-run from the spec file onwards, so what is measured is the generated test
 * suite's ability to catch faults, not a cached artifact's.  A mutant passes its
 * JUnit test when the run agrees with what {@code mutations/*.mutants} declared:
 * {@code expect: killed} for a fault the generated conditions should reject, and
 * {@code expect: survives} for one the spec deliberately says nothing about.
 *
 * <p>That is the point of declaring the expectation rather than simply failing on
 * every survivor.  A permanent set of red tests for known, documented spec gaps
 * teaches people to ignore red.  Declaring them instead makes the file a
 * two-way regression check: tighten a postcondition and a declared survivor
 * starts dying — which fails here until the declaration is updated — and weaken
 * one and a mutant that used to die starts surviving, which fails here too.
 *
 * <p>This is the slowest suite in the project: it forks a compiler and two JVMs
 * per mutant.  Skip it with {@code -Dmutation.skip=true} when iterating on
 * something else.
 */
@DisplayName("Mutation testing (mutations/*.mutants)")
@DisabledIfSystemProperty(named = "mutation.skip", matches = "true")
class MutationScoreTest {

    private static final Path MUTATIONS = Paths.get("..", "mutations");
    private static final Path STUBS     = Paths.get("..", "verify-stubs");
    private static final Path WORK      = Paths.get("target", "mutation-junit");

    private static MutationRunner.Report report;

    @BeforeAll
    static void runEveryMutantThroughThePipeline() throws IOException {
        MutationRunner runner = new MutationRunner(MUTATIONS, STUBS, WORK);

        // A mutation score only means something if the unmutated libraries pass:
        // otherwise every mutant is "killed" for a reason unrelated to its fault.
        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            GeneratedSuiteHarness.Outcome baseline = runner.baseline(example);
            assertTrue(baseline.passed(),
                    () -> "baseline failed for " + example.key
                            + ", so no mutation score below it can be trusted:\n"
                            + baseline.getSingularCaseOutput());
        }

        report = runner.run(Collections.<String>emptyList(), false);
        MutationRunner.printSummary(report, System.out);
    }

    @Test
    @DisplayName("the mutation sets parse and cover every library")
    void mutationSetsAreWellFormed() throws IOException {
        List<Mutant> mutants = MutantSetParser.parseDirectory(MUTATIONS);
        assertTrue(!mutants.isEmpty(), "no mutants found in " + MUTATIONS.toAbsolutePath());
        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            boolean covered = false;
            for (Mutant mutant : mutants) {
                if (mutant.getLibraryKey().equals(example.key)) { covered = true; break; }
            }
            assertTrue(covered, example.key + " has no mutation set; every library needs one");
        }
        for (Mutant mutant : mutants) {
            for (String source : LibraryDryRunExamples.byKey(mutant.getLibraryKey())
                                                      .librarySourcePaths) {
                assertTrue(Files.isRegularFile(Paths.get(source)),
                        mutant.getId() + " targets a library whose source " + source
                                + " does not exist");
            }
        }
    }

    @Test
    @DisplayName("every mutation operator is exercised by at least one mutant")
    void everyOperatorIsExercised() throws IOException {
        List<Mutant> mutants = MutantSetParser.parseDirectory(MUTATIONS);
        List<String> unexercised = new ArrayList<String>();
        for (String operator : MutationOperator.names()) {
            boolean exercised = false;
            for (Mutant mutant : mutants) {
                if (mutant.getOperator().equals(operator)) { exercised = true; break; }
            }
            if (!exercised) unexercised.add(operator);
        }
        // The per-operator score is only a measurement if every operator has
        // something in it: an operator with no mutants is an untested KIND of
        // fault, and it would read as a blank row rather than as a gap.
        assertTrue(unexercised.isEmpty(),
                () -> "operators declared in MutationOperator but exercised by no mutant: "
                        + String.join(", ", unexercised)
                        + ". Either write a mutant of that kind or stop declaring the operator.");
    }

    @Test
    @DisplayName("a fault seeded in a collaborator class is still reported against its library")
    void multiClassMutantsAreSeededIntoTheNamedFile() throws IOException {
        // Example 14 is the only multi-class library, and the `file:` field is the
        // only way to reach a class the generated ATC never calls. If seeding
        // silently fell back to the façade, every one of these would report a
        // "no line matches" INVALID rather than a result — so their status is the
        // evidence that the fault really landed where the mutant said.
        int collaboratorMutants = 0;
        for (Mutant mutant : MutantSetParser.parseDirectory(MUTATIONS)) {
            if (mutant.getFile() == null || "Helper.java".equals(mutant.getFile())) continue;
            collaboratorMutants++;
            for (MutationRunner.MutantResult result : report.getResults()) {
                if (!result.getMutant().getId().equals(mutant.getId())) continue;
                assertTrue(result.getStatus() != MutationRunner.Status.INVALID,
                        () -> mutant.getId() + " names " + mutant.getFile()
                                + " but could not be seeded there: " + result.getDetail());
            }
        }
        assertTrue(collaboratorMutants > 0,
                "no mutant seeds a fault into a collaborator class, so the multi-class"
                        + " library is not actually being measured through its façade");
    }

    @TestFactory
    @DisplayName("mutants")
    Stream<DynamicNode> mutants() {
        List<DynamicNode> containers = new ArrayList<DynamicNode>();
        for (Map.Entry<String, List<MutationRunner.MutantResult>> entry
                : report.byLibrary().entrySet()) {
            List<DynamicNode> children = new ArrayList<DynamicNode>();
            for (final MutationRunner.MutantResult result : entry.getValue()) {
                children.add(DynamicTest.dynamicTest(nameOf(result), () -> {
                    assertTrue(result.getStatus() != MutationRunner.Status.INVALID,
                            () -> result.getMutant().getId() + " could not be run: "
                                    + result.getDetail());
                    assertEquals(result.getMutant().isExpectedKilled(), result.isKilled(),
                            () -> result.getMutant().getId() + ": "
                                    + result.expectationMismatch() + "\n  fault: "
                                    + result.getMutant().getDescription()
                                    + "\n  run   : " + result.getDetail()
                                    + (result.getMutant().getNote().isEmpty() ? ""
                                       : "\n  note  : " + result.getMutant().getNote()));
                }));
            }
            containers.add(DynamicContainer.dynamicContainer(
                    entry.getKey() + " (" + entry.getValue().size() + " mutants)", children));
        }
        return containers.stream();
    }

    @Test
    @DisplayName("every mutant is applicable — no mutation set has drifted from its library")
    void noInvalidMutants() {
        List<String> invalid = new ArrayList<String>();
        for (MutationRunner.MutantResult result : report.getResults()) {
            if (result.getStatus() == MutationRunner.Status.INVALID) {
                invalid.add(result.getMutant().getId() + " — " + result.getDetail());
            }
        }
        assertTrue(invalid.isEmpty(),
                () -> "mutants that could not be seeded or compiled:\n  "
                        + String.join("\n  ", invalid));
    }

    private static String nameOf(MutationRunner.MutantResult result) {
        return result.getMutant().getId() + "  [" + result.getStatus() + "]  "
                + result.getMutant().getDescription();
    }
}
