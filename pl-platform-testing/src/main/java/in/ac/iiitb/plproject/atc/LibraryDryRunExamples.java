package in.ac.iiitb.plproject.atc;

import in.ac.iiitb.plproject.atc.ir.AtcClass;
import in.ac.iiitb.plproject.parser.SpecToAstConverter;
import in.ac.iiitb.plproject.parser.TestStringParser;
import in.ac.iiitb.plproject.parser.TestStringValidator;
import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;
import in.ac.iiitb.plproject.symex.SpfWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives the three library dry runs described in the implementation spec through
 * the existing pipeline: spec text -> AST -> propagation scan -> ATC IR ->
 * Symbolic IR -> SPF file + JUnit file.
 *
 * <pre>
 *   1. Stack&lt;String&gt;             push -&gt; push -&gt; pop -&gt; peek
 *   2. HashMap&lt;String,Integer&gt;   put -&gt; put -&gt; getOldValue -&gt; remove
 *   3. TaskQueue                  submit -&gt; getResult -&gt; cancelTask
 * </pre>
 *
 * Run it from the {@code pl-platform-testing} directory:
 * <pre>
 *   mvn -o exec:java -Dexec.mainClass=in.ac.iiitb.plproject.atc.LibraryDryRunExamples
 *   java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples stack
 * </pre>
 */
public class LibraryDryRunExamples {

    /** One example: its spec file, its library source and its test string. */
    public static class Example {
        public final String key;
        public final String title;
        public final String specPath;
        public final String testStringPath;
        public final String librarySourcePath;
        public final String outputDir;

        Example(String key, String title, String specPath, String testStringPath,
                String librarySourcePath, String outputDir) {
            this.key = key;
            this.title = title;
            this.specPath = specPath;
            this.testStringPath = testStringPath;
            this.librarySourcePath = librarySourcePath;
            this.outputDir = outputDir;
        }
    }

    /** Repository root relative to the pl-platform-testing working directory. */
    private static final String ROOT = "..";

    public static final Example STACK = new Example(
            "stack", "EXAMPLE 1 — Stack<String> (independent captures, no propagation)",
            ROOT + "/specs/Stack.spec", ROOT + "/specs/Stack.tests",
            ROOT + "/libraries/stack/Helper.java", "outputs/example1-stack");

    public static final Example HASHMAP = new Example(
            "hashmap", "EXAMPLE 2 — HashMap<String,Integer> (nullable, produced-but-unconsumed)",
            ROOT + "/specs/HashMapLib.spec", ROOT + "/specs/HashMapLib.tests",
            ROOT + "/libraries/hashmap/Helper.java", "outputs/example2-hashmap");

    public static final Example TASKQUEUE = new Example(
            "taskqueue", "EXAMPLE 3 — TaskQueue (full multi-hop forward propagation)",
            ROOT + "/specs/TaskQueue.spec", ROOT + "/specs/TaskQueue.tests",
            ROOT + "/libraries/taskqueue/Helper.java", "outputs/example3-taskqueue");

    public static final List<Example> ALL =
            Collections.unmodifiableList(Arrays.asList(STACK, HASHMAP, TASKQUEUE));

    /** What one run produced, kept so tests can assert on it without re-running. */
    public static class Result {
        public final Example example;
        public final JmlSpecAst specAst;
        public final TestStringAst testString;
        public final TestStringValidator.Report validation;
        public final AtcClass atcIr;
        public final PropagationScan scan;
        public final String spfCode;
        public final String junitCode;
        public final String singularCaseCode;

        Result(Example example, JmlSpecAst specAst, TestStringAst testString,
               TestStringValidator.Report validation, AtcClass atcIr, PropagationScan scan,
               String spfCode, String junitCode, String singularCaseCode) {
            this.example = example;
            this.specAst = specAst;
            this.testString = testString;
            this.validation = validation;
            this.atcIr = atcIr;
            this.scan = scan;
            this.spfCode = spfCode;
            this.junitCode = junitCode;
            this.singularCaseCode = singularCaseCode;
        }
    }

    public static void main(String[] args) throws IOException {
        List<Example> selected = ALL;
        if (args.length > 0) {
            selected = new java.util.ArrayList<Example>();
            for (String arg : args) {
                Example match = byKey(arg);
                if (match == null) {
                    System.err.println("Unknown example: " + arg + " (expected one of stack, hashmap, taskqueue)");
                    return;
                }
                selected.add(match);
            }
        }

        for (Example example : selected) {
            Result result = run(example, true);
            System.out.println("\n✓ Wrote " + example.outputDir + "/"
                    + result.atcIr.getPackageName().replace('.', '/') + "/{GeneratedATCs.java,"
                    + "GeneratedATCs_JUnit.java,SingularCase.java,Helper.java}\n");
        }
    }

    public static Example byKey(String key) {
        for (Example example : ALL) {
            if (example.key.equalsIgnoreCase(key)) return example;
        }
        return null;
    }

    /** Runs all three examples and returns their results keyed by example key. */
    public static Map<String, Result> runAll() throws IOException {
        Map<String, Result> results = new LinkedHashMap<String, Result>();
        for (Example example : ALL) {
            results.put(example.key, run(example, false));
        }
        return results;
    }

    /** Feeds one example through the whole pipeline and writes all three artifacts out. */
    public static Result run(Example example, boolean verbose) throws IOException {
        if (verbose) {
            System.out.println("================================================================");
            System.out.println(example.title);
            System.out.println("================================================================");
            System.out.println("spec        : " + example.specPath);
            System.out.println("test string : " + example.testStringPath);
            System.out.println("library     : " + example.librarySourcePath);
            System.out.println();
        }

        JmlSpecAst specAst = SpecToAstConverter.convertSpecToAst(read(example.specPath));
        TestStringAst testString = TestStringParser.parse(read(example.testStringPath));

        // Validate the test string against the spec BEFORE generating anything.
        TestStringValidator.Report validation =
                TestStringValidator.validate(specAst, testString, true);
        if (verbose) {
            System.out.println("--- test string: " + testString + " ---");
            System.out.print(validation.describe());
            System.out.println();
        }
        if (!validation.isValid()) {
            throw new IllegalStateException("invalid test string in " + example.testStringPath
                    + ":\n" + validation.describe());
        }

        NewGenATC generator = new NewGenATC();
        AtcClass atcIr = generator.generateAtcFile(specAst, testString);
        PropagationScan scan = generator.getLastPropagationScan();

        String singularCaseCode = new SingularCaseGenerator()
                .generate(specAst, testString, scan, atcIr.getPackageName());

        SpfWrapper wrapper = new SpfWrapper();
        SpfWrapper.LibraryRunResult files = wrapper.runLibraryExample(
                atcIr, example.outputDir, example.librarySourcePath, singularCaseCode);

        if (verbose) {
            System.out.println("--- STEP A: propagation scan trace ---");
            System.out.print(scan.traceString());
            System.out.println();
            System.out.println("--- STEP D(a): SPF file (GeneratedATCs.java) ---");
            System.out.println(files.getSpfCode());
            System.out.println("--- STEP D(b): JUnit file (GeneratedATCs_JUnit.java) ---");
            System.out.println(files.getJunitCode());
            System.out.println("--- SINGULAR CASE (SingularCase.java) ---");
            System.out.println(singularCaseCode);
        }

        return new Result(example, specAst, testString, validation, atcIr, scan,
                          files.getSpfCode(), files.getJunitCode(), singularCaseCode);
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
