package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: add -> add -> contains -> remove
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : StringSetDryRun");
        System.out.println("test string   : add -> add -> contains -> remove");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        boolean added_0;
        boolean added_1;
        boolean present;

        try {
            {   // ── block 0: add ─────────────────────────────────────────────────
                String elem = "alpha"; // CLIENT_INPUT
                require(0, "add", "elem != null", (elem != null));
                added_0 = Helper.add(elem); // SERVER_OUTPUT captured
                boolean added = added_0; // the name the postcondition uses
                ensure(0, "add", "E.contains(elem)", Helper.E.contains(elem));
            }

            {   // ── block 1: add ─────────────────────────────────────────────────
                String elem = "alpha"; // CLIENT_INPUT
                require(1, "add", "elem != null", (elem != null));
                added_1 = Helper.add(elem); // SERVER_OUTPUT captured
                boolean added = added_1; // the name the postcondition uses
                ensure(1, "add", "E.contains(elem)", Helper.E.contains(elem));
            }

            {   // ── block 2: contains ────────────────────────────────────────────
                String elem = "alpha"; // CLIENT_INPUT
                require(2, "contains", "elem != null", (elem != null));
                int size_old = Helper.size;
                present = Helper.contains(elem); // SERVER_OUTPUT captured
                ensure(2, "contains", "present == E.contains(elem)", (java.util.Objects.equals(present, Helper.E.contains(elem))));
                ensure(2, "contains", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
            }

            {   // ── block 3: remove ──────────────────────────────────────────────
                String elem = "alpha"; // CLIENT_INPUT
                require(3, "remove", "E.contains(elem)", Helper.E.contains(elem));
                int size_old = Helper.size;
                Helper.remove(elem);
                ensure(3, "remove", "!E.contains(elem)", !Helper.E.contains(elem));
                ensure(3, "remove", "size == \\old(size) - 1", (java.util.Objects.equals(Helper.size, (size_old - 1))));
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
