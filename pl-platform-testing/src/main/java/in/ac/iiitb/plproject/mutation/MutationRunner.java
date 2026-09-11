package in.ac.iiitb.plproject.mutation;

import in.ac.iiitb.plproject.atc.LibraryDryRunExamples;
import in.ac.iiitb.plproject.verify.GeneratedSuiteHarness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the whole pipeline against every mutant and reports what the generated
 * tests caught.
 *
 * <p>For each mutant the runner seeds the fault into a private copy of the
 * library and then repeats the entire chain — spec and test string to AST, to the
 * propagation scan, to the ATC IR, to {@code GeneratedATCs.java},
 * {@code GeneratedATCs_JUnit.java} and {@code SingularCase.java}, to javac, to a
 * forked JVM.  Nothing is stubbed out and no result is reused between mutants:
 * regenerating each time is what makes the outcome a statement about the
 * pipeline rather than about one cached suite.
 *
 * <p>A mutant is <strong>killed</strong> when the generated conditions reject it
 * and <strong>survives</strong> when every one of them still holds.  A survivor
 * is a measurement, not a bug in the runner: it says the spec and the test string
 * together never constrained the behaviour that mutant changed.
 *
 * <pre>
 *   cd pl-platform-testing
 *   mvn -o compile
 *   java -cp target/classes in.ac.iiitb.plproject.mutation.MutationRunner
 *   java -cp target/classes in.ac.iiitb.plproject.mutation.MutationRunner stack taskqueue
 * </pre>
 */
public class MutationRunner {

    /** Where a mutant ended up. */
    public enum Status {
        /** The generated tests rejected the seeded fault. */
        KILLED,
        /** Every generated condition still held: nothing constrains this behaviour. */
        SURVIVED,
        /** The mutant could not be seeded or the mutated library did not compile. */
        INVALID
    }

    /** The verdict on one mutant, with the evidence behind it. */
    public static class MutantResult {
        private final Mutant mutant;
        private final Status status;
        private final String detail;
        private final GeneratedSuiteHarness.Outcome outcome;

        MutantResult(Mutant mutant, Status status, String detail,
                     GeneratedSuiteHarness.Outcome outcome) {
            this.mutant = mutant;
            this.status = status;
            this.detail = detail;
            this.outcome = outcome;
        }

        public Mutant getMutant() { return mutant; }
        public Status getStatus() { return status; }

        /** Which condition killed it, or why it survived or could not be run. */
        public String getDetail() { return detail; }

        /** The full compile-and-run outcome, or null when the mutant was never runnable. */
        public GeneratedSuiteHarness.Outcome getOutcome() { return outcome; }

        public boolean isKilled()   { return status == Status.KILLED; }
        public boolean isSurvivor() { return status == Status.SURVIVED; }

        /** True when the run disagrees with what the mutation set declared. */
        public boolean isUnexpected() {
            if (status == Status.INVALID) return true;
            return isKilled() != mutant.isExpectedKilled();
        }

        /** What the mutation set said should happen, next to what did. */
        public String expectationMismatch() {
            if (status == Status.INVALID) return "could not be run: " + detail;
            return "declared expect: " + (mutant.isExpectedKilled() ? "killed" : "survives")
                    + ", but the run reports " + status;
        }
    }

    /** Every result, plus the counts and score derived from them. */
    public static class Report {
        private final List<MutantResult> results;

        Report(List<MutantResult> results) {
            this.results = Collections.unmodifiableList(results);
        }

        public List<MutantResult> getResults() { return results; }

        public int total()     { return results.size(); }
        public int killed()    { return count(Status.KILLED); }
        public int survived()  { return count(Status.SURVIVED); }
        public int invalid()   { return count(Status.INVALID); }

        private int count(Status status) {
            int found = 0;
            for (MutantResult result : results) if (result.getStatus() == status) found++;
            return found;
        }

        /** killed / (killed + survived) — invalid mutants are excluded, as they measure nothing. */
        public double mutationScore() {
            int scored = killed() + survived();
            return scored == 0 ? 0.0 : (100.0 * killed()) / scored;
        }

        /** Results that contradict the mutation set's declared expectation. */
        public List<MutantResult> unexpected() {
            List<MutantResult> mismatches = new ArrayList<MutantResult>();
            for (MutantResult result : results) if (result.isUnexpected()) mismatches.add(result);
            return mismatches;
        }

        public List<MutantResult> survivors() {
            List<MutantResult> survivors = new ArrayList<MutantResult>();
            for (MutantResult result : results) if (result.isSurvivor()) survivors.add(result);
            return survivors;
        }

        /** Results grouped by library, in the order the libraries were run. */
        /** The results grouped by mutation operator, in the order the operators are declared. */
        public Map<String, List<MutantResult>> byOperator() {
            Map<String, List<MutantResult>> grouped =
                    new java.util.LinkedHashMap<String, List<MutantResult>>();
            for (String operator : MutationOperator.names()) {
                for (MutantResult result : results) {
                    if (!result.getMutant().getOperator().equals(operator)) continue;
                    List<MutantResult> bucket = grouped.get(operator);
                    if (bucket == null) {
                        bucket = new ArrayList<MutantResult>();
                        grouped.put(operator, bucket);
                    }
                    bucket.add(result);
                }
            }
            return grouped;
        }

        public Map<String, List<MutantResult>> byLibrary() {
            Map<String, List<MutantResult>> grouped = new LinkedHashMap<String, List<MutantResult>>();
            for (MutantResult result : results) {
                String key = result.getMutant().getLibraryKey();
                if (!grouped.containsKey(key)) grouped.put(key, new ArrayList<MutantResult>());
                grouped.get(key).add(result);
            }
            return grouped;
        }
    }

    /** Default locations, relative to the {@code pl-platform-testing} working directory. */
    private static final Path DEFAULT_MUTATIONS = Paths.get("..", "mutations");
    private static final Path DEFAULT_STUBS     = Paths.get("..", "verify-stubs");
    private static final Path DEFAULT_WORK      = Paths.get("target", "mutation");

    private final Path mutationsDir;
    private final Path workDir;
    private final GeneratedSuiteHarness harness;

    public MutationRunner() {
        this(DEFAULT_MUTATIONS, DEFAULT_STUBS, DEFAULT_WORK);
    }

    public MutationRunner(Path mutationsDir, Path stubsDir, Path workDir) {
        this.mutationsDir = mutationsDir;
        this.workDir = workDir;
        this.harness = new GeneratedSuiteHarness(stubsDir, workDir);
    }

    /**
     * Verifies the unmutated library first.
     *
     * <p>A mutation score only means something if the suite passes on the original:
     * if a condition already fails there, every mutant would be reported killed for
     * a reason that has nothing to do with the mutation.
     */
    public GeneratedSuiteHarness.Outcome baseline(LibraryDryRunExamples.Example example)
            throws IOException {
        LibraryDryRunExamples.run(example, false);
        return harness.verify(generatedSourcesIn(Paths.get(example.outputDir)),
                              "baseline-" + example.key);
    }

    /** Runs every mutant of every library named in {@code libraryKeys} (empty means all). */
    public Report run(List<String> libraryKeys, boolean verbose) throws IOException {
        List<Mutant> mutants = MutantSetParser.parseDirectory(mutationsDir);
        List<MutantResult> results = new ArrayList<MutantResult>();

        for (Mutant mutant : mutants) {
            if (!libraryKeys.isEmpty() && !libraryKeys.contains(mutant.getLibraryKey())) continue;
            MutantResult result = run(mutant);
            results.add(result);
            if (verbose) {
                System.out.println(String.format("  %-9s %-34s %s%s",
                        result.getStatus(), mutant.getId(), result.getDetail(),
                        result.isUnexpected() ? "   <-- UNEXPECTED" : ""));
            }
        }
        return new Report(results);
    }

    /** Seeds one fault and runs it against the library's own checked-in test string. */
    public MutantResult run(Mutant mutant) throws IOException {
        return run(mutant, null);
    }

    /**
     * Seeds one fault, regenerates, compiles and executes; classifies what came back.
     *
     * @param testStringPath the sequence to run the mutant against, or null for the
     *        library's checked-in one. Passing a different sequence is how a mutant
     *        that survived the checked-in test string is re-tried against a family of
     *        others — a survivor is only a statement about the spec once some
     *        sequence has actually tried to reach it.
     */
    public MutantResult run(Mutant mutant, String testStringPath) throws IOException {
        LibraryDryRunExamples.Example base = LibraryDryRunExamples.byKey(mutant.getLibraryKey());
        if (base == null) {
            return new MutantResult(mutant, Status.INVALID,
                    "no library example named " + mutant.getLibraryKey(), null);
        }

        // The sandbox holds the mutated library and the sources regenerated from it;
        // compiled classes go elsewhere, since the harness clears its own class directory.
        Path sandbox = workDir.resolve("mutants").resolve(mutant.getId());
        GeneratedSuiteHarness.deleteRecursively(sandbox);
        Files.createDirectories(sandbox);

        // Every source of the library is copied into the sandbox, because a
        // multi-class library only compiles with all of its classes present; exactly
        // one of them — the one the mutant names, or the façade by default — is
        // rewritten on the way in.
        String target = mutant.getFile() != null ? mutant.getFile() : "Helper.java";
        List<String> sandboxSources = new ArrayList<String>();
        boolean seeded = false;

        for (String librarySourcePath : base.librarySourcePaths) {
            Path source = Paths.get(librarySourcePath);
            String fileName = source.getFileName().toString();
            String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

            if (fileName.equals(target)) {
                try {
                    text = mutant.applyTo(text);
                } catch (Mutant.MutationNotApplicable notApplicable) {
                    return new MutantResult(mutant, Status.INVALID, notApplicable.getMessage(), null);
                }
                seeded = true;
            }

            Path sandboxCopy = sandbox.resolve(fileName);
            Files.write(sandboxCopy, text.getBytes(StandardCharsets.UTF_8));
            sandboxSources.add(sandboxCopy.toString());
        }

        if (!seeded) {
            return new MutantResult(mutant, Status.INVALID,
                    "no library source named " + target + " in the " + mutant.getLibraryKey()
                    + " example, which is built from " + base.librarySourcePaths, null);
        }

        // Regenerate the whole suite against the mutated library, then compile and run it.
        Path generatedRoot = sandbox.resolve("generated");
        LibraryDryRunExamples.Example variant = LibraryDryRunExamples.variantOf(
                base, sandboxSources,
                testStringPath != null ? testStringPath : base.testStringPath,
                generatedRoot.toString());
        LibraryDryRunExamples.run(variant, false);

        GeneratedSuiteHarness.Outcome outcome =
                harness.verify(generatedSourcesIn(generatedRoot), "classes/" + mutant.getId());

        if (!outcome.compiled()) {
            return new MutantResult(mutant, Status.INVALID,
                    "the mutated library does not compile: " + firstLine(outcome.getCompileDiagnostics()),
                    outcome);
        }

        // A run that had to be killed is a detection: the suite noticed by never
        // finishing. Mutation testing calls this TIMED_OUT and counts it as killed,
        // and a mechanically generated suite produces them regularly — moving a loop
        // bound is exactly how a loop stops terminating.
        if (outcome.timedOut()) {
            return new MutantResult(mutant, Status.KILLED,
                    "killed by timeout — the generated run had to be stopped", outcome);
        }

        GeneratedSuiteHarness.Condition failure = outcome.firstFailure();
        if (failure != null) {
            return new MutantResult(mutant, Status.KILLED,
                    "killed by " + failure.describe(), outcome);
        }
        if (!outcome.singularCasePassed()) {
            return new MutantResult(mutant, Status.KILLED,
                    "killed by the singular case (exit " + outcome.getSingularCaseExit() + ")", outcome);
        }
        if (!outcome.junitSequencePassed()) {
            return new MutantResult(mutant, Status.KILLED,
                    "killed by a generated assertion in GeneratedATCs_JUnit", outcome);
        }
        return new MutantResult(mutant, Status.SURVIVED,
                "every generated condition still holds", outcome);
    }

    /** The package directory the code generator writes its four files into. */
    private static Path generatedSourcesIn(Path outputDir) {
        return outputDir.resolve("in").resolve("ac").resolve("iiitb")
                        .resolve("plproject").resolve("atc").resolve("generated");
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // command line
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {
        List<String> libraryKeys = new ArrayList<String>();
        for (String arg : args) {
            if (LibraryDryRunExamples.byKey(arg) == null) {
                System.err.println("Unknown library: " + arg
                        + " (expected one of " + LibraryDryRunExamples.keys() + ")");
                System.exit(2);
            }
            libraryKeys.add(arg.toLowerCase());
        }

        MutationRunner runner = new MutationRunner();

        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println("BASELINE — the unmutated libraries must pass before anything else");
        System.out.println("════════════════════════════════════════════════════════════════");
        for (LibraryDryRunExamples.Example example : LibraryDryRunExamples.ALL) {
            if (!libraryKeys.isEmpty() && !libraryKeys.contains(example.key)) continue;
            GeneratedSuiteHarness.Outcome outcome = runner.baseline(example);
            System.out.println(String.format("  %-10s %s  (%d condition(s) checked)",
                    example.key, outcome.passed() ? "PASS" : "FAIL", outcome.getConditions().size()));
            if (!outcome.passed()) {
                System.err.println("baseline failed for " + example.key
                        + "; mutation scores would be meaningless:\n" + outcome.getSingularCaseOutput());
                System.exit(1);
            }
        }

        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println("MUTATION RUN — whole pipeline re-run per mutant");
        System.out.println("════════════════════════════════════════════════════════════════");
        Report report = runner.run(libraryKeys, true);

        System.out.println();
        printSummary(report, System.out);
        System.exit(report.unexpected().isEmpty() ? 0 : 1);
    }

    /** The counts the run exists to produce, per library and overall. */
    public static void printSummary(Report report, java.io.PrintStream out) {
        out.println("════════════════════════════════════════════════════════════════");
        out.println("SUMMARY");
        out.println("════════════════════════════════════════════════════════════════");
        out.println(String.format("%-12s %8s %8s %9s %8s %s",
                "library", "mutants", "killed", "survived", "invalid", "score"));
        for (Map.Entry<String, List<MutantResult>> entry : report.byLibrary().entrySet()) {
            Report perLibrary = new Report(entry.getValue());
            out.println(String.format("%-12s %8d %8d %9d %8d %5.1f%%",
                    entry.getKey(), perLibrary.total(), perLibrary.killed(),
                    perLibrary.survived(), perLibrary.invalid(), perLibrary.mutationScore()));
        }
        out.println(String.format("%-12s %8d %8d %9d %8d %5.1f%%",
                "ALL", report.total(), report.killed(), report.survived(),
                report.invalid(), report.mutationScore()));

        // The same mutants counted the other way.  "78% of faults caught" is a
        // number; "every relational fault caught, two in five return-value faults
        // missed" is a direction to work in.
        out.println();
        out.println(String.format("%-34s %-5s %8s %8s %9s %s",
                "operator", "abbr", "mutants", "killed", "survived", "score"));
        for (Map.Entry<String, List<MutantResult>> entry : report.byOperator().entrySet()) {
            Report perOperator = new Report(entry.getValue());
            out.println(String.format("%-34s %-5s %8d %8d %9d %5.1f%%",
                    entry.getKey(), MutationOperator.abbreviationOf(entry.getKey()),
                    perOperator.total(), perOperator.killed(), perOperator.survived(),
                    perOperator.mutationScore()));
        }

        if (!report.survivors().isEmpty()) {
            out.println();
            out.println("SURVIVORS — behaviour the spec and test string do not pin down:");
            for (MutantResult survivor : report.survivors()) {
                out.println("  " + survivor.getMutant().getId());
                out.println("      " + survivor.getMutant().getDescription());
                if (!survivor.getMutant().getNote().isEmpty()) {
                    out.println("      why: " + survivor.getMutant().getNote());
                }
            }
        }

        if (!report.unexpected().isEmpty()) {
            out.println();
            out.println("UNEXPECTED — the run disagrees with the mutation set's declaration:");
            for (MutantResult mismatch : report.unexpected()) {
                out.println("  " + mismatch.getMutant().getId() + ": " + mismatch.expectationMismatch());
            }
        }
    }
}
