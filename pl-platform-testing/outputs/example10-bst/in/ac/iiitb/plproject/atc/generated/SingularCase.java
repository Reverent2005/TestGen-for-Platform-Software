package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: insert -> insert -> insert -> contains -> min -> delete
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : BinarySearchTreeDryRun");
        System.out.println("test string   : insert -> insert -> insert -> contains -> min -> delete");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        boolean found;
        int smallest;

        try {
            {   // ── block 0: insert ──────────────────────────────────────────────
                int key = 50; // CLIENT_INPUT
                require(0, "insert", "!Keys.contains(key)", !Helper.Keys.contains(key));
                int size_old = Helper.size;
                Helper.insert(key);
                ensure(0, "insert", "Keys.contains(key)", Helper.Keys.contains(key));
                ensure(0, "insert", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 1: insert ──────────────────────────────────────────────
                int key = 30; // CLIENT_INPUT
                require(1, "insert", "!Keys.contains(key)", !Helper.Keys.contains(key));
                int size_old = Helper.size;
                Helper.insert(key);
                ensure(1, "insert", "Keys.contains(key)", Helper.Keys.contains(key));
                ensure(1, "insert", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 2: insert ──────────────────────────────────────────────
                int key = 70; // CLIENT_INPUT
                require(2, "insert", "!Keys.contains(key)", !Helper.Keys.contains(key));
                int size_old = Helper.size;
                Helper.insert(key);
                ensure(2, "insert", "Keys.contains(key)", Helper.Keys.contains(key));
                ensure(2, "insert", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 3: contains ────────────────────────────────────────────
                int key = 30; // CLIENT_INPUT
                require(3, "contains", "size > 0", (Helper.size > 0));
                int size_old = Helper.size;
                found = Helper.contains(key); // SERVER_OUTPUT captured
                ensure(3, "contains", "found == Keys.contains(key)", (java.util.Objects.equals(found, Helper.Keys.contains(key))));
                ensure(3, "contains", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
            }

            {   // ── block 4: min ─────────────────────────────────────────────────
                require(4, "min", "size > 0", (Helper.size > 0));
                int size_old = Helper.size;
                smallest = Helper.min(); // SERVER_OUTPUT captured
                ensure(4, "min", "smallest == minKey", (java.util.Objects.equals(smallest, Helper.minKey)));
                ensure(4, "min", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
            }

            {   // ── block 5: delete ──────────────────────────────────────────────
                int key = 30; // CLIENT_INPUT
                require(5, "delete", "Keys.contains(key)", Helper.Keys.contains(key));
                int size_old = Helper.size;
                Helper.delete(key);
                ensure(5, "delete", "!Keys.contains(key)", !Helper.Keys.contains(key));
                ensure(5, "delete", "size == \\old(size) - 1", (java.util.Objects.equals(Helper.size, (size_old - 1))));
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
