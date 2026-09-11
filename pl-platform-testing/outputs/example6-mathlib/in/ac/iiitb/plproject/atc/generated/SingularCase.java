package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: abs -> gcd -> power -> factorial
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : MathLibDryRun");
        System.out.println("test string   : abs -> gcd -> power -> factorial");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        int absValue;
        int divisor;
        int powerValue;
        int factValue;

        try {
            {   // ── block 0: abs ─────────────────────────────────────────────────
                int x = -17; // CLIENT_INPUT
                require(0, "abs", "x >= -2147483647", (x >= -2147483647));
                int calls_old = Helper.calls;
                absValue = Helper.abs(x); // SERVER_OUTPUT captured
                ensure(0, "abs", "absValue >= 0", (absValue >= 0));
                ensure(0, "abs", "lastResult == absValue", (java.util.Objects.equals(Helper.lastResult, absValue)));
                ensure(0, "abs", "calls == \\old(calls) + 1", (java.util.Objects.equals(Helper.calls, (calls_old + 1))));
            }

            {   // ── block 1: gcd ─────────────────────────────────────────────────
                int a = 48; // CLIENT_INPUT
                int b = 18; // CLIENT_INPUT
                require(1, "gcd", "a > 0 && b > 0", a > 0 && b > 0);
                int calls_old = Helper.calls;
                divisor = Helper.gcd(a, b); // SERVER_OUTPUT captured
                ensure(1, "gcd", "divisor > 0", (divisor > 0));
                ensure(1, "gcd", "a % divisor == 0", (java.util.Objects.equals(a % divisor, 0)));
                ensure(1, "gcd", "b % divisor == 0", (java.util.Objects.equals(b % divisor, 0)));
                ensure(1, "gcd", "lastResult == divisor", (java.util.Objects.equals(Helper.lastResult, divisor)));
                ensure(1, "gcd", "calls == \\old(calls) + 1", (java.util.Objects.equals(Helper.calls, (calls_old + 1))));
            }

            {   // ── block 2: power ───────────────────────────────────────────────
                int base = 3; // CLIENT_INPUT
                int exponent = 4; // CLIENT_INPUT
                require(2, "power", "base > 0 && exponent >= 0 && exponent <= 30", base > 0 && exponent >= 0 && exponent <= 30);
                int calls_old = Helper.calls;
                powerValue = Helper.power(base, exponent); // SERVER_OUTPUT captured
                ensure(2, "power", "powerValue > 0", (powerValue > 0));
                ensure(2, "power", "lastResult == powerValue", (java.util.Objects.equals(Helper.lastResult, powerValue)));
                ensure(2, "power", "calls == \\old(calls) + 1", (java.util.Objects.equals(Helper.calls, (calls_old + 1))));
            }

            {   // ── block 3: factorial ───────────────────────────────────────────
                int n = 5; // CLIENT_INPUT
                require(3, "factorial", "n >= 0 && n <= 12", n >= 0 && n <= 12);
                int calls_old = Helper.calls;
                factValue = Helper.factorial(n); // SERVER_OUTPUT captured
                ensure(3, "factorial", "factValue >= 1", (factValue >= 1));
                ensure(3, "factorial", "lastResult == factValue", (java.util.Objects.equals(Helper.lastResult, factValue)));
                ensure(3, "factorial", "calls == \\old(calls) + 1", (java.util.Objects.equals(Helper.calls, (calls_old + 1))));
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
