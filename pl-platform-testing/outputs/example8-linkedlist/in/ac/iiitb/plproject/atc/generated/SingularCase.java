package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: addFirst -> addLast -> removeFirst -> indexOf
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : LinkedListDryRun");
        System.out.println("test string   : addFirst -> addLast -> removeFirst -> indexOf");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        Integer removedValue;
        int position;

        try {
            {   // ── block 0: addFirst ────────────────────────────────────────────
                Integer value = 10; // CLIENT_INPUT
                require(0, "addFirst", "value != null", (value != null));
                int size_old = Helper.size;
                Helper.addFirst(value);
                ensure(0, "addFirst", "head == value", (java.util.Objects.equals(Helper.head, value)));
                ensure(0, "addFirst", "L.contains(value)", Helper.L.contains(value));
                ensure(0, "addFirst", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 1: addLast ─────────────────────────────────────────────
                Integer value = 20; // CLIENT_INPUT
                require(1, "addLast", "value != null", (value != null));
                int size_old = Helper.size;
                Helper.addLast(value);
                ensure(1, "addLast", "tail == value", (java.util.Objects.equals(Helper.tail, value)));
                ensure(1, "addLast", "L.contains(value)", Helper.L.contains(value));
                ensure(1, "addLast", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
            }

            {   // ── block 2: removeFirst ─────────────────────────────────────────
                require(2, "removeFirst", "size > 0", (Helper.size > 0));
                Integer head_old = Helper.head;
                int size_old = Helper.size;
                removedValue = Helper.removeFirst(); // SERVER_OUTPUT captured
                ensure(2, "removeFirst", "removedValue == \\old(head)", (java.util.Objects.equals(removedValue, head_old)));
                ensure(2, "removeFirst", "size == \\old(size) - 1", (java.util.Objects.equals(Helper.size, (size_old - 1))));
            }

            {   // ── block 3: indexOf ─────────────────────────────────────────────
                Integer value = 20; // CLIENT_INPUT
                require(3, "indexOf", "size > 0", (Helper.size > 0));
                position = Helper.indexOf(value); // SERVER_OUTPUT captured
                ensure(3, "indexOf", "position >= -1", (position >= -1));
                ensure(3, "indexOf", "position < size", (position < Helper.size));
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
