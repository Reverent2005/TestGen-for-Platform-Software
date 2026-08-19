package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: submit -> getResult -> cancelTask
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : TaskQueueDryRun");
        System.out.println("test string   : submit -> getResult -> cancelTask");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        Integer taskId;
        String result;

        try {
            {   // ── block 0: submit ──────────────────────────────────────────────
                String payload = "render-report"; // CLIENT_INPUT
                require(0, "submit", "payload != null", (payload != null));
                int nextId_old = Helper.nextId;
                taskId = Helper.submit(payload); // SERVER_OUTPUT captured
                ensure(0, "submit", "Tasks.containsKey(taskId)", Helper.Tasks.containsKey(taskId));
                ensure(0, "submit", "nextId == \\old(nextId) + 1", (java.util.Objects.equals(Helper.nextId, (nextId_old + 1))));
            }

            {   // ── block 1: getResult ───────────────────────────────────────────
                // taskId is the SERVER_OUTPUT captured by submit, propagated into this block
                require(1, "getResult", "Tasks.containsKey(taskId)", Helper.Tasks.containsKey(taskId));
                result = Helper.getResult(taskId); // SERVER_OUTPUT captured
                ensure(1, "getResult", "Results.containsKey(taskId)", Helper.Results.containsKey(taskId));
            }

            {   // ── block 2: cancelTask ──────────────────────────────────────────
                // taskId is the SERVER_OUTPUT captured by submit, propagated into this block
                require(2, "cancelTask", "Tasks.containsKey(taskId)", Helper.Tasks.containsKey(taskId));
                Helper.cancelTask(taskId);
                ensure(2, "cancelTask", "!Tasks.containsKey(taskId)", !Helper.Tasks.containsKey(taskId));
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
