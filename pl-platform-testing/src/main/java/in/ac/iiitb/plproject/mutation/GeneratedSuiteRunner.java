package in.ac.iiitb.plproject.mutation;

import in.ac.iiitb.plproject.atc.LibraryDryRunExamples;
import in.ac.iiitb.plproject.parser.SpecToAstConverter;
import in.ac.iiitb.plproject.parser.TestStringParser;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.verify.GeneratedSuiteHarness;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds and runs a generated test suite for every library, and measures it with
 * the generated mutation suite.
 *
 * <p>Three phases, in order, each answering a different question:
 *
 * <ol>
 *   <li><b>test strings</b> — generate a few hundred sequences per library and run
 *       each one against the real library. Each run is a test: it either satisfies
 *       every pre- and postcondition it reaches, or it stops because the sequence
 *       stepped outside the library's contract, or it fails a postcondition, which
 *       would be a genuine finding against the library.</li>
 *   <li><b>mutants</b> — generate the operator mutants and run each against the
 *       library's own checked-in test string. This is the per-operator score: what
 *       fraction of each KIND of fault does one hand-written sequence catch?</li>
 *   <li><b>the cross</b> — take every mutant that survived phase 2 and re-run it
 *       against the generated family from phase 1, stopping at the first sequence
 *       that kills it. This is the phase the other two exist for. Several mutation
 *       notes in {@code mutations/} claim a survivor would die "against a longer
 *       test string, with no change to the spec"; this measures how many.</li>
 * </ol>
 *
 * <pre>
 *   ./run-operator-suites.sh                  # every library, all three phases
 *   ./run-operator-suites.sh stack queue      # a subset
 * </pre>
 *
 * <p>It is slow — each of the thousands of runs forks a compiler and two JVMs — so
 * it is a script of its own rather than part of {@code mvn -o test}.
 */
public class GeneratedSuiteRunner {

    /** Everything this runner writes, relative to the repository root. */
    private static final Path SUITES      = Paths.get("..", "operator-suites");
    private static final Path TEST_STRINGS = SUITES.resolve("teststrings");
    private static final Path RESULTS     = SUITES.resolve("results");

    private static final Path MUTATIONS = Paths.get("..", "mutations");
    private static final Path STUBS     = Paths.get("..", "verify-stubs");
    private static final Path WORK      = Paths.get("target", "operator-suites");

    /** How many test strings to generate per library — the brief's 200 to 300. */
    public static final int TEST_STRINGS_PER_LIBRARY = 260;

    /** The cap on a library's mutation suite; the generator produces what the source admits. */
    public static final int MUTANTS_PER_LIBRARY = 300;

    /**
     * How many sequences of the family a surviving mutant is re-tried against.
     *
     * <p>The cross is quadratic — survivors times sequences — so it is bounded. The
     * sequences are taken in generation order, which is arbitrary and therefore
     * unbiased; a mutant that no sequence in the first {@value} of them kills is recorded as
     * surviving the family, which understates the family rather than overstating it.
     */
    public static final int CROSS_SEQUENCES = 20;

    /** What one generated test string did when it was run. */
    public enum TestStringStatus {
        /** Every condition it reached holds. */
        PASSED,
        /** A precondition failed: the sequence stepped outside the library's contract. */
        STOPPED,
        /** A postcondition failed — a finding against the library itself. */
        FAILED,
        /** The generated sources did not compile. */
        INVALID
    }

    /** One generated test string and what came of it. */
    public static class TestStringResult {
        final String name;
        final String sequence;
        final TestStringStatus status;
        final int conditions;
        final Path path;

        TestStringResult(String name, String sequence, TestStringStatus status,
                         int conditions, Path path) {
            this.name = name;
            this.sequence = sequence;
            this.status = status;
            this.conditions = conditions;
            this.path = path;
        }

        public TestStringStatus getStatus() { return status; }
        public int getConditions() { return conditions; }
        public Path getPath() { return path; }
    }

    /** Everything one library produced across the three phases. */
    public static class LibraryReport {
        final String key;
        final List<TestStringResult> testStrings = new ArrayList<TestStringResult>();
        final List<MutationRunner.MutantResult> mutants = new ArrayList<MutationRunner.MutantResult>();
        /** mutant id -> the generated sequence that killed it, for phase-3 kills. */
        final Map<String, String> killedByFamily = new LinkedHashMap<String, String>();
        long testStringSeconds;
        long mutantSeconds;
        long crossSeconds;

        LibraryReport(String key) { this.key = key; }

        public String getKey() { return key; }
        public List<TestStringResult> getTestStrings() { return testStrings; }
        public List<MutationRunner.MutantResult> getMutants() { return mutants; }
        public Map<String, String> getKilledByFamily() { return killedByFamily; }

        int testStrings(TestStringStatus status) {
            int n = 0;
            for (TestStringResult result : testStrings) if (result.status == status) n++;
            return n;
        }

        int mutants(MutationRunner.Status status) {
            int n = 0;
            for (MutationRunner.MutantResult result : mutants) if (result.getStatus() == status) n++;
            return n;
        }

        /** killed / (killed + survived) against the checked-in test string alone. */
        double scoreAlone() {
            int killed = mutants(MutationRunner.Status.KILLED);
            int scored = killed + mutants(MutationRunner.Status.SURVIVED);
            return scored == 0 ? 0.0 : (100.0 * killed) / scored;
        }

        /** killed / (killed + survived) once the generated family has had its turn. */
        double scoreWithFamily() {
            int killed = mutants(MutationRunner.Status.KILLED) + killedByFamily.size();
            int scored = mutants(MutationRunner.Status.KILLED)
                       + mutants(MutationRunner.Status.SURVIVED);
            return scored == 0 ? 0.0 : (100.0 * killed) / scored;
        }
    }

    public static void main(String[] args) throws IOException {
        List<LibraryDryRunExamples.Example> selected = new ArrayList<LibraryDryRunExamples.Example>();
        for (String arg : args) {
            LibraryDryRunExamples.Example example = LibraryDryRunExamples.byKey(arg);
            if (example == null) {
                System.err.println("Unknown library: " + arg
                        + " (expected one of " + LibraryDryRunExamples.keys() + ")");
                System.exit(2);
            }
            selected.add(example);
        }
        if (selected.isEmpty()) selected.addAll(LibraryDryRunExamples.ALL);

        Files.createDirectories(SUITES);
        Files.createDirectories(TEST_STRINGS);
        Files.createDirectories(RESULTS);

        MutationRunner runner = new MutationRunner(MUTATIONS, STUBS, WORK);
        GeneratedSuiteHarness harness = new GeneratedSuiteHarness(STUBS, WORK.resolve("classes"));
        Map<String, LibraryReport> reports = new LinkedHashMap<String, LibraryReport>();

        for (LibraryDryRunExamples.Example example : selected) {
            System.out.println();
            System.out.println("══ " + example.key + " "
                    + dashes(58 - example.key.length()));
            LibraryReport report = new LibraryReport(example.key);
            reports.put(example.key, report);

            runTestStrings(example, harness, report);
            runMutants(example, runner, report);
            crossFamilyAgainstSurvivors(example, runner, report);

            writeTestStringResults(report);
            writeMutantResults(report);
        }

        printSummary(reports, System.out);
        writeSummary(reports);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // phase 1 — the generated test suite
    // ─────────────────────────────────────────────────────────────────────────

    private static void runTestStrings(LibraryDryRunExamples.Example example,
                                       GeneratedSuiteHarness harness,
                                       LibraryReport report) throws IOException {
        JmlSpecAst spec = SpecToAstConverter.convertSpecToAst(read(example.specPath));
        TestStringAst seed = TestStringParser.parse(read(example.testStringPath));

        // The seed is the library key, so a family is the same on every machine and
        // in every run: a suite that changed between runs could not be a baseline.
        TestStringGenerator generator = new TestStringGenerator(spec, seed,
                example.key.hashCode());
        List<String> texts = generator.generate(
                capitalise(example.key) + "Generated", TEST_STRINGS_PER_LIBRARY);

        Path libraryDir = TEST_STRINGS.resolve(example.key);
        deleteDirectory(libraryDir);
        Files.createDirectories(libraryDir);

        System.out.println("  phase 1 — " + texts.size() + " generated test strings");
        long startedAt = System.currentTimeMillis();

        for (int index = 0; index < texts.size(); index++) {
            String text = texts.get(index);
            String name = String.format("%s-%03d", example.key, index + 1);
            Path testStringPath = libraryDir.resolve(name + ".tests");
            Files.write(testStringPath, text.getBytes(StandardCharsets.UTF_8));

            Path outputDir = WORK.resolve("teststrings").resolve(example.key).resolve(name);
            LibraryDryRunExamples.Example variant = LibraryDryRunExamples.variantOf(
                    example, example.librarySourcePaths, testStringPath.toString(),
                    outputDir.toString());

            TestStringStatus status;
            int conditions = 0;
            try {
                LibraryDryRunExamples.run(variant, false);
                GeneratedSuiteHarness.Outcome outcome = harness.verify(
                        outputDir.resolve("in/ac/iiitb/plproject/atc/generated"),
                        "teststring-" + name);
                conditions = outcome.getConditions().size();
                if (!outcome.compiled()) {
                    status = TestStringStatus.INVALID;
                } else {
                    GeneratedSuiteHarness.Condition failure = outcome.firstFailure();
                    if (failure == null) status = TestStringStatus.PASSED;
                    else if (failure.isPrecondition()) status = TestStringStatus.STOPPED;
                    else status = TestStringStatus.FAILED;
                }
            } catch (RuntimeException | IOException problem) {
                status = TestStringStatus.INVALID;
            }

            report.testStrings.add(new TestStringResult(
                    name, sequenceOf(text), status, conditions, testStringPath));
        }
        report.testStringSeconds = (System.currentTimeMillis() - startedAt) / 1000;

        System.out.println(String.format(
                "            %d passed, %d stopped at a precondition, %d failed, %d invalid  (%ds)",
                report.testStrings(TestStringStatus.PASSED),
                report.testStrings(TestStringStatus.STOPPED),
                report.testStrings(TestStringStatus.FAILED),
                report.testStrings(TestStringStatus.INVALID),
                report.testStringSeconds));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // phase 2 — the generated mutation suite, against the checked-in sequence
    // ─────────────────────────────────────────────────────────────────────────

    private static void runMutants(LibraryDryRunExamples.Example example,
                                   MutationRunner runner,
                                   LibraryReport report) throws IOException {
        List<Mutant> mutants = OperatorMutantGenerator.generate(
                example.key, example.librarySourcePaths, MUTANTS_PER_LIBRARY);

        Files.write(SUITES.resolve(example.key + ".mutants"),
                OperatorMutantGenerator.render(example.key, example.librarySourcePaths, mutants)
                        .getBytes(StandardCharsets.UTF_8));

        System.out.println("  phase 2 — " + mutants.size() + " generated mutants");
        long startedAt = System.currentTimeMillis();
        for (Mutant mutant : mutants) {
            report.mutants.add(runner.run(mutant));
        }
        report.mutantSeconds = (System.currentTimeMillis() - startedAt) / 1000;

        System.out.println(String.format("            %d killed, %d survived, %d invalid — %.1f%%  (%ds)",
                report.mutants(MutationRunner.Status.KILLED),
                report.mutants(MutationRunner.Status.SURVIVED),
                report.mutants(MutationRunner.Status.INVALID),
                report.scoreAlone(), report.mutantSeconds));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // phase 3 — the survivors, against the family
    // ─────────────────────────────────────────────────────────────────────────

    private static void crossFamilyAgainstSurvivors(LibraryDryRunExamples.Example example,
                                                    MutationRunner runner,
                                                    LibraryReport report) throws IOException {
        // Only a sequence that passes against the healthy library is usable as an
        // oracle: one that stops at a precondition proves nothing about a mutant,
        // because it proves nothing about the library either.
        List<TestStringResult> usable = new ArrayList<TestStringResult>();
        for (TestStringResult result : report.testStrings) {
            if (result.status == TestStringStatus.PASSED) usable.add(result);
            if (usable.size() >= CROSS_SEQUENCES) break;
        }

        List<MutationRunner.MutantResult> survivors = new ArrayList<MutationRunner.MutantResult>();
        for (MutationRunner.MutantResult result : report.mutants) {
            if (result.getStatus() == MutationRunner.Status.SURVIVED) survivors.add(result);
        }

        System.out.println("  phase 3 — " + survivors.size() + " survivors against "
                + usable.size() + " passing sequences");
        long startedAt = System.currentTimeMillis();

        for (MutationRunner.MutantResult survivor : survivors) {
            for (TestStringResult sequence : usable) {
                MutationRunner.MutantResult retried =
                        runner.run(survivor.getMutant(), sequence.path.toString());
                if (retried.getStatus() == MutationRunner.Status.KILLED) {
                    report.killedByFamily.put(survivor.getMutant().getId(),
                            sequence.name + ": " + sequence.sequence);
                    break;
                }
            }
        }
        report.crossSeconds = (System.currentTimeMillis() - startedAt) / 1000;

        System.out.println(String.format(
                "            the family kills %d of %d survivors — %.1f%% becomes %.1f%%  (%ds)",
                report.killedByFamily.size(), survivors.size(),
                report.scoreAlone(), report.scoreWithFamily(), report.crossSeconds));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reports
    // ─────────────────────────────────────────────────────────────────────────

    private static void writeTestStringResults(LibraryReport report) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("# generated test strings — ").append(report.getKey()).append('\n');
        out.append("# name\tstatus\tconditions\tsequence\n");
        for (TestStringResult result : report.testStrings) {
            out.append(result.name).append('\t').append(result.status).append('\t')
               .append(result.conditions).append('\t').append(result.sequence).append('\n');
        }
        Files.write(RESULTS.resolve(report.getKey() + ".teststrings.tsv"),
                    out.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeMutantResults(LibraryReport report) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("# generated mutants — ").append(report.getKey()).append('\n');
        out.append("# id\tstatus\toperator\tmethod\tfile\tkilled-by-family\tdetail\n");
        for (MutationRunner.MutantResult result : report.mutants) {
            Mutant mutant = result.getMutant();
            String byFamily = report.killedByFamily.get(mutant.getId());
            out.append(mutant.getId()).append('\t')
               .append(result.getStatus()).append('\t')
               .append(mutant.getOperator()).append('\t')
               .append(mutant.getMethod() == null ? "-" : mutant.getMethod()).append('\t')
               .append(mutant.getFile() == null ? "Helper.java" : mutant.getFile()).append('\t')
               .append(byFamily == null ? "-" : byFamily).append('\t')
               .append(result.getDetail().replace('\t', ' ')).append('\n');
        }
        Files.write(RESULTS.resolve(report.getKey() + ".mutants.tsv"),
                    out.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeSummary(Map<String, LibraryReport> reports) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("# generated suites — summary\n\n");
        appendTestStringTable(reports, out);
        out.append('\n');
        appendMutantTable(reports, out);
        out.append('\n');
        appendOperatorTable(reports, out);
        Files.write(SUITES.resolve("SUMMARY.txt"), out.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void printSummary(Map<String, LibraryReport> reports, PrintStream out) {
        StringBuilder text = new StringBuilder();
        text.append('\n');
        text.append("════════════════════════════════════════════════════════════════\n");
        text.append("GENERATED SUITES — SUMMARY\n");
        text.append("════════════════════════════════════════════════════════════════\n\n");
        appendTestStringTable(reports, text);
        text.append('\n');
        appendMutantTable(reports, text);
        text.append('\n');
        appendOperatorTable(reports, text);
        out.print(text);
    }

    private static void appendTestStringTable(Map<String, LibraryReport> reports, StringBuilder out) {
        out.append("PHASE 1 — generated test strings\n");
        out.append(String.format("%-14s %6s %8s %9s %8s %9s%n",
                "library", "tests", "passed", "stopped", "failed", "invalid"));
        int total = 0, passed = 0, stopped = 0, failed = 0, invalid = 0;
        for (LibraryReport report : reports.values()) {
            out.append(String.format("%-14s %6d %8d %9d %8d %9d%n",
                    report.getKey(), report.testStrings.size(),
                    report.testStrings(TestStringStatus.PASSED),
                    report.testStrings(TestStringStatus.STOPPED),
                    report.testStrings(TestStringStatus.FAILED),
                    report.testStrings(TestStringStatus.INVALID)));
            total += report.testStrings.size();
            passed += report.testStrings(TestStringStatus.PASSED);
            stopped += report.testStrings(TestStringStatus.STOPPED);
            failed += report.testStrings(TestStringStatus.FAILED);
            invalid += report.testStrings(TestStringStatus.INVALID);
        }
        out.append(String.format("%-14s %6d %8d %9d %8d %9d%n",
                "ALL", total, passed, stopped, failed, invalid));
    }

    private static void appendMutantTable(Map<String, LibraryReport> reports, StringBuilder out) {
        out.append("PHASE 2 and 3 — generated mutants, alone and against the family\n");
        out.append(String.format("%-14s %8s %8s %9s %8s %8s %9s %s%n",
                "library", "mutants", "killed", "survived", "invalid", "alone", "by family", "with family"));
        int total = 0, killed = 0, survived = 0, invalid = 0, byFamily = 0;
        for (LibraryReport report : reports.values()) {
            out.append(String.format("%-14s %8d %8d %9d %8d %7.1f%% %9d %10.1f%%%n",
                    report.getKey(), report.mutants.size(),
                    report.mutants(MutationRunner.Status.KILLED),
                    report.mutants(MutationRunner.Status.SURVIVED),
                    report.mutants(MutationRunner.Status.INVALID),
                    report.scoreAlone(), report.killedByFamily.size(),
                    report.scoreWithFamily()));
            total += report.mutants.size();
            killed += report.mutants(MutationRunner.Status.KILLED);
            survived += report.mutants(MutationRunner.Status.SURVIVED);
            invalid += report.mutants(MutationRunner.Status.INVALID);
            byFamily += report.killedByFamily.size();
        }
        int scored = killed + survived;
        out.append(String.format("%-14s %8d %8d %9d %8d %7.1f%% %9d %10.1f%%%n",
                "ALL", total, killed, survived, invalid,
                scored == 0 ? 0.0 : (100.0 * killed) / scored, byFamily,
                scored == 0 ? 0.0 : (100.0 * (killed + byFamily)) / scored));
    }

    private static void appendOperatorTable(Map<String, LibraryReport> reports, StringBuilder out) {
        out.append("BY OPERATOR\n");
        out.append(String.format("%-34s %-5s %8s %8s %9s %8s %8s %s%n",
                "operator", "abbr", "mutants", "killed", "survived", "invalid", "alone", "with family"));
        for (String operator : MutationOperator.names()) {
            int total = 0, killed = 0, survived = 0, invalid = 0, byFamily = 0;
            for (LibraryReport report : reports.values()) {
                for (MutationRunner.MutantResult result : report.mutants) {
                    if (!result.getMutant().getOperator().equals(operator)) continue;
                    total++;
                    switch (result.getStatus()) {
                        case KILLED:   killed++; break;
                        case SURVIVED:
                            survived++;
                            if (report.killedByFamily.containsKey(result.getMutant().getId())) {
                                byFamily++;
                            }
                            break;
                        default:       invalid++; break;
                    }
                }
            }
            if (total == 0) continue;
            int scored = killed + survived;
            out.append(String.format("%-34s %-5s %8d %8d %9d %8d %7.1f%% %10.1f%%%n",
                    operator, MutationOperator.abbreviationOf(operator),
                    total, killed, survived, invalid,
                    scored == 0 ? 0.0 : (100.0 * killed) / scored,
                    scored == 0 ? 0.0 : (100.0 * (killed + byFamily)) / scored));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // small helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    /** The sequence line of a generated test string, for the report. */
    private static String sequenceOf(String text) {
        int at = text.indexOf("sequence:");
        if (at < 0) return "?";
        int end = text.indexOf(';', at);
        return text.substring(at + "sequence:".length(), end < 0 ? text.length() : end).trim();
    }

    private static String capitalise(String key) {
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    private static void deleteDirectory(Path directory) throws IOException {
        GeneratedSuiteHarness.deleteRecursively(directory);
    }

    private static String dashes(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(3, count); i++) sb.append('═');
        return sb.toString();
    }
}
