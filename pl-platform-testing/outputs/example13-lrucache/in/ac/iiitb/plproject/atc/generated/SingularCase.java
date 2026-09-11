package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: put -> put -> put -> get -> restore
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : LruCacheDryRun");
        System.out.println("test string   : put -> put -> put -> get -> restore");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        String evictedKey_0;
        String evictedKey_1;
        String evictedKey_2;
        String cachedValue;

        try {
            {   // ── block 0: put ─────────────────────────────────────────────────
                String key = "alpha"; // CLIENT_INPUT
                String value = "1"; // CLIENT_INPUT
                require(0, "put", "key != null && value != null && capacity > 0", key != null && value != null && Helper.capacity > 0);
                evictedKey_0 = Helper.put(key, value); // SERVER_OUTPUT captured
                String evictedKey = evictedKey_0; // the name the postcondition uses
                ensure(0, "put", "C.get(key) == value", (java.util.Objects.equals(Helper.C.get(key), value)));
                ensure(0, "put", "mostRecent == key", (java.util.Objects.equals(Helper.mostRecent, key)));
                ensure(0, "put", "size <= capacity", (Helper.size <= Helper.capacity));
            }

            {   // ── block 1: put ─────────────────────────────────────────────────
                String key = "beta"; // CLIENT_INPUT
                String value = "2"; // CLIENT_INPUT
                require(1, "put", "key != null && value != null && capacity > 0", key != null && value != null && Helper.capacity > 0);
                evictedKey_1 = Helper.put(key, value); // SERVER_OUTPUT captured
                String evictedKey = evictedKey_1; // the name the postcondition uses
                ensure(1, "put", "C.get(key) == value", (java.util.Objects.equals(Helper.C.get(key), value)));
                ensure(1, "put", "mostRecent == key", (java.util.Objects.equals(Helper.mostRecent, key)));
                ensure(1, "put", "size <= capacity", (Helper.size <= Helper.capacity));
            }

            {   // ── block 2: put ─────────────────────────────────────────────────
                String key = "gamma"; // CLIENT_INPUT
                String value = "3"; // CLIENT_INPUT
                require(2, "put", "key != null && value != null && capacity > 0", key != null && value != null && Helper.capacity > 0);
                evictedKey_2 = Helper.put(key, value); // SERVER_OUTPUT captured
                String evictedKey = evictedKey_2; // the name the postcondition uses
                ensure(2, "put", "C.get(key) == value", (java.util.Objects.equals(Helper.C.get(key), value)));
                ensure(2, "put", "mostRecent == key", (java.util.Objects.equals(Helper.mostRecent, key)));
                ensure(2, "put", "size <= capacity", (Helper.size <= Helper.capacity));
            }

            {   // ── block 3: get ─────────────────────────────────────────────────
                String key = "beta"; // CLIENT_INPUT
                require(3, "get", "C.containsKey(key)", Helper.C.containsKey(key));
                int size_old = Helper.size;
                cachedValue = Helper.get(key); // SERVER_OUTPUT captured
                ensure(3, "get", "cachedValue == C.get(key)", (java.util.Objects.equals(cachedValue, Helper.C.get(key))));
                ensure(3, "get", "mostRecent == key", (java.util.Objects.equals(Helper.mostRecent, key)));
                ensure(3, "get", "size == \\old(size)", (java.util.Objects.equals(Helper.size, size_old)));
            }

            {   // ── block 4: restore ─────────────────────────────────────────────
                String evictedKey = evictedKey_2; // SERVER_OUTPUT propagated from put
                String value = "1"; // CLIENT_INPUT
                require(4, "restore", "evictedKey != null && !C.containsKey(evictedKey)", evictedKey != null && !Helper.C.containsKey(evictedKey));
                Helper.restore(evictedKey, value);
                ensure(4, "restore", "C.containsKey(evictedKey)", Helper.C.containsKey(evictedKey));
                ensure(4, "restore", "mostRecent == evictedKey", (java.util.Objects.equals(Helper.mostRecent, evictedKey)));
                ensure(4, "restore", "size <= capacity", (Helper.size <= Helper.capacity));
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
