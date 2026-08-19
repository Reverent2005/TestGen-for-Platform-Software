package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: push -> push -> pop -> peek
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : StackDryRun");
        System.out.println("test string   : push -> push -> pop -> peek");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        String poppedElem;
        String peekedElem;

        try {
            {   // ── block 0: push ────────────────────────────────────────────────
                String elem = "alpha"; // CLIENT_INPUT
                require(0, "push", "elem != null", (elem != null));
                int size_old = Helper.size;
                Helper.push(elem);
                ensure(0, "push", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
                ensure(0, "push", "top == elem", (java.util.Objects.equals(Helper.top, elem)));
            }

            {   // ── block 1: push ────────────────────────────────────────────────
                String elem = "beta"; // CLIENT_INPUT
                require(1, "push", "elem != null", (elem != null));
                int size_old = Helper.size;
                Helper.push(elem);
                ensure(1, "push", "size == \\old(size) + 1", (java.util.Objects.equals(Helper.size, (size_old + 1))));
                ensure(1, "push", "top == elem", (java.util.Objects.equals(Helper.top, elem)));
            }

            {   // ── block 2: pop ─────────────────────────────────────────────────
                require(2, "pop", "size > 0", (Helper.size > 0));
                int size_old = Helper.size;
                poppedElem = Helper.pop(); // SERVER_OUTPUT captured
                ensure(2, "pop", "size == \\old(size) - 1", (java.util.Objects.equals(Helper.size, (size_old - 1))));
            }

            {   // ── block 3: peek ────────────────────────────────────────────────
                require(3, "peek", "size > 0", (Helper.size > 0));
                int size_old = Helper.size;
                peekedElem = Helper.peek(); // SERVER_OUTPUT captured
                ensure(3, "peek", "peekedElem == top", (java.util.Objects.equals(peekedElem, Helper.top)));
                ensure(3, "peek", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
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
