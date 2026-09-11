package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: insert -> insert -> insert -> peekMin -> extractMin
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : MinHeapDryRun");
        System.out.println("test string   : insert -> insert -> insert -> peekMin -> extractMin");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        Integer smallest;
        Integer removedMin;

        try {
            {   // ── block 0: insert ──────────────────────────────────────────────
                Integer value = 9; // CLIENT_INPUT
                require(0, "insert", "value != null", (value != null));
                int size_old = Helper.size;
                Helper.insert(value);
                ensure(0, "insert", "H.contains(value)", Helper.H.contains(value));
                ensure(0, "insert", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 1: insert ──────────────────────────────────────────────
                Integer value = 4; // CLIENT_INPUT
                require(1, "insert", "value != null", (value != null));
                int size_old = Helper.size;
                Helper.insert(value);
                ensure(1, "insert", "H.contains(value)", Helper.H.contains(value));
                ensure(1, "insert", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 2: insert ──────────────────────────────────────────────
                Integer value = 7; // CLIENT_INPUT
                require(2, "insert", "value != null", (value != null));
                int size_old = Helper.size;
                Helper.insert(value);
                ensure(2, "insert", "H.contains(value)", Helper.H.contains(value));
                ensure(2, "insert", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 3: peekMin ─────────────────────────────────────────────
                require(3, "peekMin", "size > 0", (Helper.size > 0));
                int size_old = Helper.size;
                smallest = Helper.peekMin(); // SERVER_OUTPUT captured
                ensure(3, "peekMin", "smallest == minValue", (java.util.Objects.equals(smallest, Helper.minValue)));
                ensure(3, "peekMin", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
            }

            {   // ── block 4: extractMin ──────────────────────────────────────────
                require(4, "extractMin", "size > 0", (Helper.size > 0));
                Integer minValue_old = Helper.minValue;
                int size_old = Helper.size;
                removedMin = Helper.extractMin(); // SERVER_OUTPUT captured
                ensure(4, "extractMin", "removedMin == \\old(minValue)", (java.util.Objects.equals(removedMin, minValue_old)));
                ensure(4, "extractMin", "size == \\old(size) - 1", (java.util.Objects.equals(Helper.size, (size_old - 1))));
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
