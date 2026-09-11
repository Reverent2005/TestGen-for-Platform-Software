package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: addVertex -> addVertex -> addEdge -> degree -> hasEdge
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : GraphDryRun");
        System.out.println("test string   : addVertex -> addVertex -> addEdge -> degree -> hasEdge");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        int vertexDegree;
        boolean connected;

        try {
            {   // ── block 0: addVertex ───────────────────────────────────────────
                String name = "A"; // CLIENT_INPUT
                require(0, "addVertex", "name != null", (name != null));
                int edgeCount_old = Helper.edgeCount;
                Helper.addVertex(name);
                ensure(0, "addVertex", "V.contains(name)", Helper.V.contains(name));
                ensure(0, "addVertex", "edgeCount == \\old(edgeCount)", (java.util.Objects.equals(Helper.edgeCount, edgeCount_old)));
            }

            {   // ── block 1: addVertex ───────────────────────────────────────────
                String name = "B"; // CLIENT_INPUT
                require(1, "addVertex", "name != null", (name != null));
                int edgeCount_old = Helper.edgeCount;
                Helper.addVertex(name);
                ensure(1, "addVertex", "V.contains(name)", Helper.V.contains(name));
                ensure(1, "addVertex", "edgeCount == \\old(edgeCount)", (java.util.Objects.equals(Helper.edgeCount, edgeCount_old)));
            }

            {   // ── block 2: addEdge ─────────────────────────────────────────────
                String from = "A"; // CLIENT_INPUT
                String to = "B"; // CLIENT_INPUT
                require(2, "addEdge", "V.contains(from) && V.contains(to)", Helper.V.contains(from) && Helper.V.contains(to));
                int edgeCount_old = Helper.edgeCount;
                Helper.addEdge(from, to);
                ensure(2, "addEdge", "Adj.get(from).contains(to)", Helper.Adj.get(from).contains(to));
                ensure(2, "addEdge", "Adj.get(to).contains(from)", Helper.Adj.get(to).contains(from));
                ensure(2, "addEdge", "edgeCount == \\old(edgeCount) + 1", (java.util.Objects.equals(Helper.edgeCount, (edgeCount_old + 1))));
            }

            {   // ── block 3: degree ──────────────────────────────────────────────
                String name = "A"; // CLIENT_INPUT
                require(3, "degree", "V.contains(name)", Helper.V.contains(name));
                int edgeCount_old = Helper.edgeCount;
                vertexDegree = Helper.degree(name); // SERVER_OUTPUT captured
                ensure(3, "degree", "vertexDegree == Adj.get(name).size()", (java.util.Objects.equals(vertexDegree, Helper.Adj.get(name).size())));
                ensure(3, "degree", "vertexDegree >= 0", (vertexDegree >= 0));
                ensure(3, "degree", "edgeCount == \\old(edgeCount)", (java.util.Objects.equals(Helper.edgeCount, edgeCount_old)));
            }

            {   // ── block 4: hasEdge ─────────────────────────────────────────────
                String from = "A"; // CLIENT_INPUT
                String to = "B"; // CLIENT_INPUT
                require(4, "hasEdge", "V.contains(from) && V.contains(to)", Helper.V.contains(from) && Helper.V.contains(to));
                int edgeCount_old = Helper.edgeCount;
                connected = Helper.hasEdge(from, to); // SERVER_OUTPUT captured
                ensure(4, "hasEdge", "connected == Adj.get(from).contains(to)", (java.util.Objects.equals(connected, Helper.Adj.get(from).contains(to))));
                ensure(4, "hasEdge", "edgeCount == \\old(edgeCount)", (java.util.Objects.equals(Helper.edgeCount, edgeCount_old)));
            }

        } catch (PreconditionViolated stop) {
            // Past a broken precondition the spec says nothing about the library's
            // behaviour, so there is nothing meaningful left to check.
            System.out.println();
            System.out.println("run stopped at " + stop.getMessage()
                    + " — the test string calls it outside its contract");
        }

        System.exit(report());
    }

    /** Raised when a precondition does not hold, to stop the run. */
    private static class PreconditionViolated extends RuntimeException {
        PreconditionViolated(String where) { super(where); }
    }

    /**
      * A precondition, checked immediately before its call.
      *
      * A failure stops the run: calling the library outside its contract would
      * measure behaviour the spec never promised, so nothing after it is evidence
      * of anything.  Postcondition failures do NOT stop the run — each one is a
      * real finding, and reporting them all is more useful than reporting one.
      */
    private static void require(int block, String function, String condition, boolean holds) {
        check(block, function, "PRE ", condition, holds);
        if (!holds) {
            throw new PreconditionViolated("block " + block + " (" + function + ")");
        }
    }

    /** A postcondition, checked immediately after its call. */
    private static void ensure(int block, String function, String condition, boolean holds) {
        check(block, function, "POST", condition, holds);
    }

    private static void check(int block, String function, String kind,
                              String condition, boolean holds) {
        checks++;
        if (!holds) failures++;
        System.out.println(String.format("block %d  %-12s %s  %-4s  %s",
                block, function, kind, holds ? "ok" : "FAIL", condition));
    }

    private static int report() {
        System.out.println();
        if (failures == 0) {
            System.out.println(checks
                    + " condition(s) checked, all hold —"
                    + " the test string runs and meets its pre/postconditions");
            return 0;
        }
        System.out.println(failures + " of " + checks + " condition(s) FAILED");
        return 1;
    }
}
