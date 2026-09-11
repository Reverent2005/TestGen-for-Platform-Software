package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: enqueue -> enqueue -> dequeue -> front -> isEmpty
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : QueueDryRun");
        System.out.println("test string   : enqueue -> enqueue -> dequeue -> front -> isEmpty");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        String dequeuedElem;
        String frontElem;
        boolean emptyFlag;

        try {
            {   // ── block 0: enqueue ─────────────────────────────────────────────
                String elem = "first"; // CLIENT_INPUT
                require(0, "enqueue", "elem != null", (elem != null));
                int size_old = Helper.size;
                Helper.enqueue(elem);
                ensure(0, "enqueue", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
                ensure(0, "enqueue", "Q.contains(elem)", Helper.Q.contains(elem));
            }

            {   // ── block 1: enqueue ─────────────────────────────────────────────
                String elem = "second"; // CLIENT_INPUT
                require(1, "enqueue", "elem != null", (elem != null));
                int size_old = Helper.size;
                Helper.enqueue(elem);
                ensure(1, "enqueue", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
                ensure(1, "enqueue", "Q.contains(elem)", Helper.Q.contains(elem));
            }

            {   // ── block 2: dequeue ─────────────────────────────────────────────
                require(2, "dequeue", "size > 0", (Helper.size > 0));
                String head_old = Helper.head;
                int size_old = Helper.size;
                dequeuedElem = Helper.dequeue(); // SERVER_OUTPUT captured
                ensure(2, "dequeue", "dequeuedElem == \\old(head)", (java.util.Objects.equals(dequeuedElem, head_old)));
                ensure(2, "dequeue", "size == \\old(size) - 1", (java.util.Objects.equals(Helper.size, (size_old - 1))));
            }

            {   // ── block 3: front ───────────────────────────────────────────────
                require(3, "front", "size > 0", (Helper.size > 0));
                int size_old = Helper.size;
                frontElem = Helper.front(); // SERVER_OUTPUT captured
                ensure(3, "front", "frontElem == head", (java.util.Objects.equals(frontElem, Helper.head)));
                ensure(3, "front", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
            }

            {   // ── block 4: isEmpty ─────────────────────────────────────────────
                require(4, "isEmpty", "size >= 0", (Helper.size >= 0));
                int size_old = Helper.size;
                emptyFlag = Helper.isEmpty(); // SERVER_OUTPUT captured
                ensure(4, "isEmpty", "emptyFlag == false", (java.util.Objects.equals(emptyFlag, false)));
                ensure(4, "isEmpty", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
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
