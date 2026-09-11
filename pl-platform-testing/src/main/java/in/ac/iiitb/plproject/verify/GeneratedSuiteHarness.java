package in.ac.iiitb.plproject.verify;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles and executes what the pipeline generated for one library, and reports
 * the result condition by condition.
 *
 * <p>This is the executable half of {@code verify-generated.sh}, factored out so
 * two callers can share it: the JUnit suite that surfaces every generated test
 * case as a real JUnit test, and the mutation runner that replays the same suite
 * against a seeded fault.  Both need the same three steps —
 *
 * <ol>
 *   <li>compile the generated sources against the {@code verify-stubs}, which
 *       stand in for {@code gov.nasa.jpf.symbc.Debug} and {@code org.junit};</li>
 *   <li>run {@code SingularCase}, the concrete run of the test string that checks
 *       every precondition before its call and every postcondition after it;</li>
 *   <li>run {@code GeneratedATCs_JUnit} with {@code -ea}, so the generated
 *       {@code assert} statements are actually evaluated.</li>
 * </ol>
 *
 * <p>Each generated class calls {@code System.exit} and mutates static library
 * state, so both are executed in a fresh forked JVM rather than in this one.
 */
public class GeneratedSuiteHarness {

    /** Package the code generator emits into, and where the runnable classes land. */
    private static final String GENERATED_PACKAGE = "in.ac.iiitb.plproject.atc.generated";

    /**
     * The stub's marker for "the assumptions were not met, so the test did not run".
     *
     * <p>The generated JUnit flavour binds every CLIENT_INPUT to a dummy placeholder
     * rather than a value solved against the precondition, so a spec whose
     * precondition the dummy does not satisfy — {@code numSeats > 0} against the
     * placeholder {@code 0} — aborts on its first {@code assumeTrue}.  In JUnit
     * that is a skip, not a failure, and treating it as a failure would report a
     * defect in a library that was never called.
     */
    private static final String ASSUMPTION_NOT_MET = "org.junit.AssumptionViolatedException";

    /** One line of the SingularCase report: a single pre- or postcondition. */
    public static class Condition {
        private final int block;
        private final String function;
        private final boolean precondition;
        private final String expression;
        private final boolean holds;

        Condition(int block, String function, boolean precondition, String expression, boolean holds) {
            this.block = block;
            this.function = function;
            this.precondition = precondition;
            this.expression = expression;
            this.holds = holds;
        }

        public int getBlock() { return block; }
        public String getFunction() { return function; }
        public boolean isPrecondition() { return precondition; }
        public String getExpression() { return expression; }
        public boolean holds() { return holds; }

        /** A stable, human-readable name — used verbatim as a JUnit test name. */
        public String describe() {
            return "block " + block + "  " + function + "  "
                    + (precondition ? "requires" : "ensures") + "  " + expression;
        }

        @Override
        public String toString() {
            return describe() + " -> " + (holds ? "ok" : "FAIL");
        }
    }

    /** What one verification produced. */
    public static class Outcome {
        private final boolean compiled;
        private final String compileDiagnostics;
        private final List<Condition> conditions;
        private final int singularCaseExit;
        private final String singularCaseOutput;
        private final int junitExit;
        private final String junitOutput;
        private final boolean timedOut;

        Outcome(boolean compiled, String compileDiagnostics, List<Condition> conditions,
                int singularCaseExit, String singularCaseOutput,
                int junitExit, String junitOutput) {
            this(compiled, compileDiagnostics, conditions, singularCaseExit, singularCaseOutput,
                 junitExit, junitOutput, false);
        }

        Outcome(boolean compiled, String compileDiagnostics, List<Condition> conditions,
                int singularCaseExit, String singularCaseOutput,
                int junitExit, String junitOutput, boolean timedOut) {
            this.timedOut = timedOut;
            this.compiled = compiled;
            this.compileDiagnostics = compileDiagnostics;
            this.conditions = Collections.unmodifiableList(conditions);
            this.singularCaseExit = singularCaseExit;
            this.singularCaseOutput = singularCaseOutput;
            this.junitExit = junitExit;
            this.junitOutput = junitOutput;
        }

        public boolean compiled() { return compiled; }
        public String getCompileDiagnostics() { return compileDiagnostics; }

        /**
         * The generated code was still running when the clock ran out and had to be
         * killed.
         *
         * <p>A hand-written mutant never does this; a mechanically generated one does
         * it regularly, because moving a loop bound or flipping a comparison is
         * exactly how a loop stops terminating. Mutation testing counts a hang as a
         * detection — the suite noticed, by not finishing — which is what
         * {@code TIMED_OUT} means in the literature.
         */
        public boolean timedOut() { return timedOut; }

        /** Every pre/postcondition the singular case reached, in execution order. */
        public List<Condition> getConditions() { return conditions; }

        public int getSingularCaseExit() { return singularCaseExit; }
        public String getSingularCaseOutput() { return singularCaseOutput; }
        public int getJunitExit() { return junitExit; }
        public String getJunitOutput() { return junitOutput; }

        /** The singular case passed: every condition it checked holds. */
        public boolean singularCasePassed() { return compiled && singularCaseExit == 0; }

        /**
         * The generated JUnit flavour never ran, because its placeholder inputs do
         * not satisfy the precondition. Skipped, not failed.
         */
        public boolean junitAssumptionNotMet() {
            return compiled && junitExit != 0 && junitOutput.contains(ASSUMPTION_NOT_MET);
        }

        /**
         * The generated JUnit sequence raised no assertion — either it ran clean, or
         * its assumptions were not met and it never ran at all.
         */
        public boolean junitSequencePassed() {
            return compiled && (junitExit == 0 || junitAssumptionNotMet());
        }

        /** Nothing anywhere objected — compile, singular case and JUnit sequence all clean. */
        public boolean passed() { return !timedOut && singularCasePassed() && junitSequencePassed(); }

        /** The first condition that did not hold, or null when they all did. */
        public Condition firstFailure() {
            for (Condition condition : conditions) {
                if (!condition.holds()) return condition;
            }
            return null;
        }
    }

    /**
     * How long a generated run may take before it is killed.
     *
     * <p>Ten seconds is two orders of magnitude more than any healthy run in this
     * project needs — they finish in about a tenth of a second — so a run that
     * reaches it is not slow, it is stuck.
     */
    private static final long RUN_TIMEOUT_SECONDS = 10;

    private final Path stubSourceDir;
    private final Path workDir;
    private Path stubClasses;

    /**
     * @param stubSourceDir the {@code verify-stubs} directory
     * @param workDir       scratch directory for compiled classes; contents are replaced
     */
    public GeneratedSuiteHarness(Path stubSourceDir, Path workDir) {
        this.stubSourceDir = stubSourceDir;
        this.workDir = workDir;
    }

    /**
     * Compiles the stubs once, into a directory every later verification reuses.
     * Safe to call repeatedly; the work happens on the first call.
     */
    public synchronized void prepareStubs() throws IOException {
        if (stubClasses != null) return;
        Path target = workDir.resolve("stubs");
        deleteRecursively(target);
        Files.createDirectories(target);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        if (!compile(javaSourcesIn(stubSourceDir), target, null, diagnostics)) {
            throw new IOException("the verification stubs do not compile:\n" + describe(diagnostics));
        }
        stubClasses = target;
    }

    /**
     * Compiles and runs everything the pipeline wrote into {@code generatedSourceDir}.
     *
     * @param generatedSourceDir directory holding GeneratedATCs.java, GeneratedATCs_JUnit.java,
     *                           SingularCase.java and the library's Helper.java
     * @param runKey             a name for this run; its classes go to {@code <workDir>/<runKey>}
     */
    public Outcome verify(Path generatedSourceDir, String runKey) throws IOException {
        prepareStubs();

        Path classes = workDir.resolve(runKey);
        deleteRecursively(classes);
        Files.createDirectories(classes);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<File> sources = javaSourcesIn(generatedSourceDir);
        if (sources.isEmpty()) {
            throw new IOException("no generated sources in " + generatedSourceDir.toAbsolutePath());
        }
        if (!compile(sources, classes, stubClasses, diagnostics)) {
            return new Outcome(false, describe(diagnostics), new ArrayList<Condition>(),
                    -1, "", -1, "");
        }

        String classpath = stubClasses + File.pathSeparator + classes;

        // The singular case is the primary oracle: it names every condition it checks.
        Execution singular = execute(classpath, GENERATED_PACKAGE + ".SingularCase", false);
        // The JUnit flavour is the second oracle: same assertions, dynamic binding,
        // and -ea so its generated `assert` statements are live.
        Execution junit = execute(classpath, GENERATED_PACKAGE + ".GeneratedATCs_JUnit", true);

        return new Outcome(true, "", parseConditions(singular.output),
                singular.exitCode, singular.output, junit.exitCode, junit.output,
                singular.timedOut || junit.timedOut);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compilation
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean compile(List<File> sources, Path outputDir, Path classpath,
                                   DiagnosticCollector<JavaFileObject> diagnostics)
            throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("no javac available — this needs a JDK, not a JRE");
        }
        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        try {
            List<String> options = new ArrayList<String>();
            options.add("-nowarn");
            options.add("-d");
            options.add(outputDir.toString());
            if (classpath != null) {
                options.add("-cp");
                options.add(classpath.toString());
            }
            Iterable<? extends JavaFileObject> units =
                    fileManager.getJavaFileObjectsFromFiles(sources);
            StringWriter sink = new StringWriter();
            return compiler.getTask(sink, fileManager, diagnostics, options, null, units).call();
        } finally {
            fileManager.close();
        }
    }

    private static String describe(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder text = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) continue;
            JavaFileObject source = diagnostic.getSource();
            text.append(source == null ? "?" : new File(source.getName()).getName())
                .append(':').append(diagnostic.getLineNumber()).append(": ")
                .append(diagnostic.getMessage(null)).append('\n');
        }
        return text.toString();
    }

    private static List<File> javaSourcesIn(Path directory) throws IOException {
        final List<File> sources = new ArrayList<File>();
        if (!Files.isDirectory(directory)) return sources;
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (file.toString().endsWith(".java")) sources.add(file.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(sources);
        return sources;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // execution
    // ─────────────────────────────────────────────────────────────────────────

    private static class Execution {
        final int exitCode;
        final String output;
        final boolean timedOut;
        Execution(int exitCode, String output, boolean timedOut) {
            this.exitCode = exitCode;
            this.output = output;
            this.timedOut = timedOut;
        }
    }

    private static Execution execute(String classpath, String mainClass, boolean assertionsEnabled)
            throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        if (assertionsEnabled) command.add("-ea");
        command.add("-cp");
        command.add(classpath);
        command.add(mainClass);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        final Process process = builder.start();

        // The output is drained on this thread, so a run that never terminates would
        // block here forever waiting for an end-of-stream that never comes. A
        // watchdog kills the process instead, which closes the stream and lets the
        // drain finish — a mutant that hangs has to be a result, not a hung suite.
        final java.util.concurrent.atomic.AtomicBoolean killed =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread watchdog = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!process.waitFor(RUN_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                        killed.set(true);
                        process.destroyForcibly();
                    }
                } catch (InterruptedException expected) {
                    // The run finished first; nothing to kill.
                }
            }
        }, "generated-run-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();

        try {
            String output = drain(process.getInputStream());
            int exitCode = process.waitFor();
            return new Execution(exitCode, output, killed.get());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("interrupted while running " + mainClass, interrupted);
        } finally {
            watchdog.interrupt();
        }
    }

    private static String drain(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = stream.read(chunk)) >= 0) buffer.write(chunk, 0, read);
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reading the singular case's report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The line SingularCase prints per condition, from its own format string
     * {@code "block %d  %-12s %s  %-4s  %s"}.
     */
    private static final Pattern CONDITION_LINE =
            Pattern.compile("^block (\\d+)\\s+(\\S+)\\s+(PRE|POST)\\s+(ok|FAIL)\\s+(.*?)\\s*$");

    static List<Condition> parseConditions(String singularCaseOutput) {
        List<Condition> conditions = new ArrayList<Condition>();
        for (String line : singularCaseOutput.split("\n")) {
            Matcher matcher = CONDITION_LINE.matcher(line.trim());
            if (!matcher.matches()) continue;
            conditions.add(new Condition(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2),
                    "PRE".equals(matcher.group(3)),
                    matcher.group(5),
                    "ok".equals(matcher.group(4))));
        }
        return conditions;
    }

    /** Empties a scratch directory so a run never sees a previous run's classes. */
    public static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure)
                    throws IOException {
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
