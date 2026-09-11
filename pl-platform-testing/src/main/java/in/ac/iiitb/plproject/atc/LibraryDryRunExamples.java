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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives every library dry run through the existing pipeline: spec text -&gt; AST
 * -&gt; propagation scan -&gt; ATC IR -&gt; Symbolic IR -&gt; SPF file + JUnit file.
 *
 * <pre>
 *    1. Stack&lt;String&gt;             push -&gt; push -&gt; pop -&gt; peek
 *    2. HashMap&lt;String,Integer&gt;   put -&gt; put -&gt; getOldValue -&gt; remove
 *    3. TaskQueue                  submit -&gt; getResult -&gt; cancelTask
 *    4. TicketService              numSeatsAvailable -&gt; findAndHoldSeats
 *                                    -&gt; reserveSeats -&gt; numSeatsAvailable
 *    5. IntArray                   set -&gt; set -&gt; get -&gt; sum -&gt; indexOf
 *    6. MathLib                    abs -&gt; gcd -&gt; power -&gt; factorial
 *    7. Queue&lt;String&gt;             enqueue -&gt; enqueue -&gt; dequeue -&gt; front
 *                                    -&gt; isEmpty
 *    8. SinglyLinkedList           addFirst -&gt; addLast -&gt; removeFirst -&gt; indexOf
 *    9. StringSet                  add -&gt; add -&gt; contains -&gt; remove
 *   10. BinarySearchTree           insert -&gt; insert -&gt; insert -&gt; contains
 *                                    -&gt; min -&gt; delete
 *   11. MinHeap&lt;Integer&gt;         insert -&gt; insert -&gt; insert -&gt; peekMin
 *                                    -&gt; extractMin
 *   12. Graph                      addVertex -&gt; addVertex -&gt; addEdge -&gt; degree
 *                                    -&gt; hasEdge
 *   13. LruCache (custom)          put -&gt; put -&gt; put -&gt; get -&gt; restore
 *   14. OrderService               restock -&gt; placeOrder -&gt; ship -&gt; stockLevel
 * </pre>
 *
 * Example 14 is the only one whose library is several classes calling each other;
 * every other is a single {@code Helper}.
 *
 * Adding one means adding an {@link Example} constant here and to {@link #ALL};
 * every script and test that walks the examples reads that list rather than a
 * copy of it.
 *
 * Run it from the {@code pl-platform-testing} directory:
 * <pre>
 *   mvn -o exec:java -Dexec.mainClass=in.ac.iiitb.plproject.atc.LibraryDryRunExamples
 *   java -cp target/classes in.ac.iiitb.plproject.atc.LibraryDryRunExamples stack
 * </pre>
 */
public class LibraryDryRunExamples {

    /** One example: its spec file, its library sources and its test string. */
    public static class Example {
        public final String key;
        public final String title;
        public final String specPath;
        public final String testStringPath;
        /**
         * The library under test, as one or more {@code .java} files copied in
         * beside the generated code.
         *
         * <p>Most examples are a single {@code Helper.java}.  An example whose
         * library is several classes that call each other lists them all, with the
         * façade the generated ATC calls — {@code Helper.java} — first; the files
         * keep their own names in the output directory, so they compile as the
         * multi-class library they are.
         */
        public final List<String> librarySourcePaths;
        public final String outputDir;

        Example(String key, String title, String specPath, String testStringPath,
                String librarySourcePath, String outputDir) {
            this(key, title, specPath, testStringPath,
                 Collections.singletonList(librarySourcePath), outputDir);
        }

        Example(String key, String title, String specPath, String testStringPath,
                List<String> librarySourcePaths, String outputDir) {
            this.key = key;
            this.title = title;
            this.specPath = specPath;
            this.testStringPath = testStringPath;
            this.librarySourcePaths =
                    Collections.unmodifiableList(new ArrayList<String>(librarySourcePaths));
            this.outputDir = outputDir;
        }

        /** The façade the generated code calls: the first library source. */
        public String librarySourcePath() {
            return librarySourcePaths.get(0);
        }

        /** True when the library under test is more than one class. */
        public boolean isMultiClass() {
            return librarySourcePaths.size() > 1;
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

    public static final Example TICKETSERVICE = new Example(
            "ticketservice", "EXAMPLE 4 — TicketService (propagation beside a repeated read-only query)",
            ROOT + "/specs/TicketService.spec", ROOT + "/specs/TicketService.tests",
            ROOT + "/libraries/ticketservice/Helper.java", "outputs/example4-ticketservice");

    public static final Example ARRAYLIB = new Example(
            "arraylib", "EXAMPLE 5 — IntArray (primitive int captures over indexed state)",
            ROOT + "/specs/IntArray.spec", ROOT + "/specs/IntArray.tests",
            ROOT + "/libraries/arraylib/Helper.java", "outputs/example5-arraylib");

    public static final Example MATHLIB = new Example(
            "mathlib", "EXAMPLE 6 — MathLib (pure functions specified by a property of the answer)",
            ROOT + "/specs/MathLib.spec", ROOT + "/specs/MathLib.tests",
            ROOT + "/libraries/mathlib/Helper.java", "outputs/example6-mathlib");

    public static final Example QUEUE = new Example(
            "queue", "EXAMPLE 7 — Queue<String> (FIFO order pinned down, and a boolean capture)",
            ROOT + "/specs/Queue.spec", ROOT + "/specs/Queue.tests",
            ROOT + "/libraries/queue/Helper.java", "outputs/example7-queue");

    public static final Example LINKEDLIST = new Example(
            "linkedlist", "EXAMPLE 8 — SinglyLinkedList<Integer> (both ends of one structure)",
            ROOT + "/specs/LinkedList.spec", ROOT + "/specs/LinkedList.tests",
            ROOT + "/libraries/linkedlist/Helper.java", "outputs/example8-linkedlist");

    public static final Example SET = new Example(
            "set", "EXAMPLE 9 — StringSet (an idempotent call, and a boolean SERVER_OUTPUT)",
            ROOT + "/specs/StringSet.spec", ROOT + "/specs/StringSet.tests",
            ROOT + "/libraries/set/Helper.java", "outputs/example9-set");

    public static final Example BST = new Example(
            "bst", "EXAMPLE 10 — BinarySearchTree (a precondition bought to strengthen a postcondition)",
            ROOT + "/specs/BinarySearchTree.spec", ROOT + "/specs/BinarySearchTree.tests",
            ROOT + "/libraries/bst/Helper.java", "outputs/example10-bst");

    public static final Example HEAP = new Example(
            "heap", "EXAMPLE 11 — MinHeap<Integer> (a value named before the call and after it)",
            ROOT + "/specs/MinHeap.spec", ROOT + "/specs/MinHeap.tests",
            ROOT + "/libraries/heap/Helper.java", "outputs/example11-heap");

    public static final Example GRAPH = new Example(
            "graph", "EXAMPLE 12 — Graph (two parameters against one piece of state)",
            ROOT + "/specs/Graph.spec", ROOT + "/specs/Graph.tests",
            ROOT + "/libraries/graph/Helper.java", "outputs/example12-graph");

    public static final Example LRUCACHE = new Example(
            "lrucache", "EXAMPLE 13 — LruCache, the custom library (a nullable SERVER_OUTPUT, propagated)",
            ROOT + "/specs/LruCache.spec", ROOT + "/specs/LruCache.tests",
            ROOT + "/libraries/lrucache/Helper.java", "outputs/example13-lrucache");

    /**
     * The only example whose library is more than one class: a façade over a
     * catalogue, a ledger and an audit trail.  Every source is listed, façade
     * first, and all of them are copied in beside the generated code.
     */
    public static final Example ORDERSERVICE = new Example(
            "orderservice", "EXAMPLE 14 — OrderService (four classes interacting behind one façade)",
            ROOT + "/specs/OrderService.spec", ROOT + "/specs/OrderService.tests",
            Arrays.asList(ROOT + "/libraries/orderservice/Helper.java",
                          ROOT + "/libraries/orderservice/Catalogue.java",
                          ROOT + "/libraries/orderservice/Ledger.java",
                          ROOT + "/libraries/orderservice/Audit.java"),
            "outputs/example14-orderservice");

    public static final List<Example> ALL =
            Collections.unmodifiableList(Arrays.asList(
                    STACK, HASHMAP, TASKQUEUE, TICKETSERVICE,
                    ARRAYLIB, MATHLIB, QUEUE, LINKEDLIST, SET, BST, HEAP, GRAPH, LRUCACHE,
                    ORDERSERVICE));

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
                    System.err.println("Unknown example: " + arg
                            + " (expected one of " + keys() + ")");
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

    /**
     * The same example, but generated from a different copy of the library into a
     * different directory — how the mutation runner replays an example against a
     * seeded fault without disturbing the checked-in outputs.
     */
    public static Example variantOf(Example base, String librarySourcePath, String outputDir) {
        return variantOf(base, Collections.singletonList(librarySourcePath), outputDir);
    }

    /** The multi-class form: every library source replaced, in the same order. */
    public static Example variantOf(Example base, List<String> librarySourcePaths,
                                    String outputDir) {
        return variantOf(base, librarySourcePaths, base.testStringPath, outputDir);
    }

    /**
     * The same spec and library, run against a DIFFERENT test string.
     *
     * <p>This is what lets one library be exercised by a whole family of sequences
     * rather than by the single one checked in beside its spec: the spec is fixed,
     * the library is fixed, and the sequence is the variable.
     */
    public static Example variantOf(Example base, List<String> librarySourcePaths,
                                    String testStringPath, String outputDir) {
        return new Example(base.key, base.title, base.specPath, testStringPath,
                           librarySourcePaths, outputDir);
    }

    /** The keys of every registered example, in order, for usage messages. */
    public static String keys() {
        StringBuilder sb = new StringBuilder();
        for (Example example : ALL) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(example.key);
        }
        return sb.toString();
    }

    public static Example byKey(String key) {
        for (Example example : ALL) {
            if (example.key.equalsIgnoreCase(key)) return example;
        }
        return null;
    }

    /** Runs every registered example and returns the results keyed by example key. */
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
            System.out.println("library     : " + String.join(", ", example.librarySourcePaths));
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
                atcIr, example.outputDir, example.librarySourcePaths, singularCaseCode);

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
