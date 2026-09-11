package in.ac.iiitb.plproject;

import in.ac.iiitb.plproject.atc.LibraryDryRunExamples;
import in.ac.iiitb.plproject.mutation.Mutant;
import in.ac.iiitb.plproject.mutation.MutationOperator;
import in.ac.iiitb.plproject.mutation.OperatorMutantGenerator;
import in.ac.iiitb.plproject.mutation.TestStringGenerator;
import in.ac.iiitb.plproject.parser.SpecToAstConverter;
import in.ac.iiitb.plproject.parser.TestStringParser;
import in.ac.iiitb.plproject.parser.TestStringValidator;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards on the two generators behind {@code ./run-operator-suites.sh}.
 *
 * <p>The suites themselves take the better part of an hour to run, so they are a
 * script rather than a test. What is checked here is everything that can be
 * checked without executing anything: that a generated mutant can actually be
 * seeded into the source it names, and that a generated test string is legal in
 * exactly the sense a hand-written one is.
 *
 * <p>Those two properties are what make the generated numbers mean anything. A
 * mutant that cannot be seeded is an {@code INVALID} row, and a test string the
 * validator would reject is not a test at all.
 */
@DisplayName("Generated suites (operator-suites/)")
class GeneratedSuitesTest {

    @Test
    @DisplayName("every generated mutant can be seeded into the file it names")
    void generatedMutantsAreApplicable() throws IOException {
        List<String> problems = new ArrayList<String>();

        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            List<Mutant> mutants = OperatorMutantGenerator.generate(
                    example.key, example.librarySourcePaths, 400);

            assertTrue(mutants.size() > 10,
                    example.key + " generated only " + mutants.size()
                            + " mutants; the generator has stopped matching its source");

            for (Mutant mutant : mutants) {
                String targetFile = mutant.getFile();
                String source = null;
                for (String path : example.librarySourcePaths) {
                    if (Paths.get(path).getFileName().toString().equals(targetFile)) {
                        source = new String(Files.readAllBytes(Paths.get(path)),
                                            StandardCharsets.UTF_8);
                    }
                }
                if (source == null) {
                    problems.add(mutant.getId() + " names a file the library does not have: "
                            + targetFile);
                    continue;
                }
                try {
                    // The seeding itself: `find` must match exactly one line of the
                    // method named, or the mutant measures nothing when it is run.
                    mutant.applyTo(source);
                } catch (Mutant.MutationNotApplicable notApplicable) {
                    problems.add(notApplicable.getMessage());
                }
            }
        }

        assertTrue(problems.isEmpty(),
                () -> problems.size() + " generated mutants cannot be seeded:\n  "
                        + String.join("\n  ", problems.subList(0, Math.min(10, problems.size()))));
    }

    @Test
    @DisplayName("generated mutants declare only operators the vocabulary knows")
    void generatedMutantsUseTheVocabulary() throws IOException {
        Set<String> used = new LinkedHashSet<String>();
        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            for (Mutant mutant : OperatorMutantGenerator.generate(
                    example.key, example.librarySourcePaths, 400)) {
                assertTrue(MutationOperator.isDeclared(mutant.getOperator()),
                        mutant.getId() + " declares an unknown operator: " + mutant.getOperator());
                used.add(mutant.getOperator());
            }
        }
        // Not every operator applies to every library, but each should apply somewhere:
        // one that matches nothing anywhere is a rule that has silently stopped working.
        List<String> unused = new ArrayList<String>();
        for (String operator : MutationOperator.names()) {
            if (!used.contains(operator)) unused.add(operator);
        }
        assertTrue(unused.isEmpty(),
                () -> "operators the generator never produces: " + String.join(", ", unused));
    }

    @Test
    @DisplayName("a generated mutant never touches reset(), which every dry run depends on")
    void generatedMutantsLeaveTheHarnessAlone() throws IOException {
        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            for (Mutant mutant : OperatorMutantGenerator.generate(
                    example.key, example.librarySourcePaths, 400)) {
                assertFalse("reset".equals(mutant.getMethod()),
                        mutant.getId() + " seeds a fault into reset(): a run that cannot reach"
                                + " its initial state measures nothing");
            }
        }
    }

    @Test
    @DisplayName("every generated test string passes the same validator the checked-in ones do")
    void generatedTestStringsAreValid() throws IOException {
        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            JmlSpecAst spec = SpecToAstConverter.convertSpecToAst(read(example.specPath));
            TestStringAst seed = TestStringParser.parse(read(example.testStringPath));

            TestStringGenerator generator =
                    new TestStringGenerator(spec, seed, example.key.hashCode());
            List<String> family = generator.generate("Check", 40);

            assertTrue(family.size() >= 20,
                    example.key + " produced only " + family.size()
                            + " test strings out of 40; the generator is not finding legal"
                            + " sequences for this spec");

            for (String text : family) {
                TestStringAst parsed = TestStringParser.parse(text);
                TestStringValidator.Report report =
                        TestStringValidator.validate(spec, parsed, true);
                // Validated as a SINGULAR CASE: the flag that makes an unbound
                // CLIENT_INPUT an error rather than something a solver would supply.
                assertTrue(report.isValid(),
                        () -> example.key + ": a generated test string is invalid:\n"
                                + text + report.describe());
                // isSingularCase() only asks whether anything was bound at all, and a
                // sequence of parameterless calls — `pop -> peek` — binds nothing and
                // is complete all the same. The validator above is the real check.
                if (!parsed.getConcreteInputs().isEmpty()) {
                    assertTrue(parsed.isSingularCase());
                }
            }
        }
    }

    @Test
    @DisplayName("the same seed produces the same family, so a generated suite is a baseline")
    void generationIsReproducible() throws IOException {
        LibraryDryRunExamples.Example example = LibraryDryRunExamples.byKey("stack");
        JmlSpecAst spec = SpecToAstConverter.convertSpecToAst(read(example.specPath));
        TestStringAst seed = TestStringParser.parse(read(example.testStringPath));

        List<String> first = new TestStringGenerator(spec, seed, 42).generate("Repeat", 25);
        List<String> second = new TestStringGenerator(spec, seed, 42).generate("Repeat", 25);
        assertEquals(first, second, "the same seed must produce the same family");

        List<Mutant> mutantsOnce = OperatorMutantGenerator.generate(
                example.key, example.librarySourcePaths, 200);
        List<Mutant> mutantsTwice = OperatorMutantGenerator.generate(
                example.key, example.librarySourcePaths, 200);
        assertEquals(mutantsOnce.size(), mutantsTwice.size());
        for (int index = 0; index < mutantsOnce.size(); index++) {
            assertEquals(mutantsOnce.get(index).getId(), mutantsTwice.get(index).getId());
            assertEquals(mutantsOnce.get(index).getReplacement(),
                         mutantsTwice.get(index).getReplacement());
        }
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
