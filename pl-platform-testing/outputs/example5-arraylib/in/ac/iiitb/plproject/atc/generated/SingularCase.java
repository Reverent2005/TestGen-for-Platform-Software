package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: set -> set -> get -> sum -> indexOf
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : IntArrayDryRun");
        System.out.println("test string   : set -> set -> get -> sum -> indexOf");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        int elem;
        int total;
        int position;

        try {
            {   // ── block 0: set ─────────────────────────────────────────────────
                int index = 0; // CLIENT_INPUT
                int value = 7; // CLIENT_INPUT
                require(0, "set", "index >= 0 && index < length", index >= 0 && index < Helper.length);
                int writes_old = Helper.writes;
                Helper.set(index, value);
                ensure(0, "set", "A[index] == value", (java.util.Objects.equals(Helper.A[index], value)));
                ensure(0, "set", "writes == \\old(writes) + 1", (java.util.Objects.equals(Helper.writes, (writes_old + 1))));
            }

            {   // ── block 1: set ─────────────────────────────────────────────────
                int index = 1; // CLIENT_INPUT
                int value = 11; // CLIENT_INPUT
                require(1, "set", "index >= 0 && index < length", index >= 0 && index < Helper.length);
                int writes_old = Helper.writes;
                Helper.set(index, value);
                ensure(1, "set", "A[index] == value", (java.util.Objects.equals(Helper.A[index], value)));
                ensure(1, "set", "writes == \\old(writes) + 1", (java.util.Objects.equals(Helper.writes, (writes_old + 1))));
            }

            {   // ── block 2: get ─────────────────────────────────────────────────
                int index = 1; // CLIENT_INPUT
                require(2, "get", "index >= 0 && index < length", index >= 0 && index < Helper.length);
                int writes_old = Helper.writes;
                elem = Helper.get(index); // SERVER_OUTPUT captured
                ensure(2, "get", "elem == A[index]", (java.util.Objects.equals(elem, Helper.A[index])));
                ensure(2, "get", "writes == \\old(writes)", (java.util.Objects.equals(Helper.writes, writes_old)));
            }

            {   // ── block 3: sum ─────────────────────────────────────────────────
                require(3, "sum", "length > 0", (Helper.length > 0));
                int writes_old = Helper.writes;
                total = Helper.sum(); // SERVER_OUTPUT captured
                ensure(3, "sum", "total >= 0", (total >= 0));
                ensure(3, "sum", "writes == \\old(writes)", (java.util.Objects.equals(Helper.writes, writes_old)));
            }

            {   // ── block 4: indexOf ─────────────────────────────────────────────
                int value = 11; // CLIENT_INPUT
                require(4, "indexOf", "length > 0", (Helper.length > 0));
                int writes_old = Helper.writes;
                position = Helper.indexOf(value); // SERVER_OUTPUT captured
                ensure(4, "indexOf", "position >= -1", (position >= -1));
                ensure(4, "indexOf", "position < length", (position < Helper.length));
                ensure(4, "indexOf", "writes == \\old(writes)", (java.util.Objects.equals(Helper.writes, writes_old)));
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
