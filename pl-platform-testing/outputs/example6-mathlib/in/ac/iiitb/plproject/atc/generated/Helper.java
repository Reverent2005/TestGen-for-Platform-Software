package in.ac.iiitb.plproject.atc.generated;

/**
 * EXAMPLE 6 — MathLib, integer mathematics.
 *
 * The library under test for the abs -&gt; gcd -&gt; power -&gt; factorial dry run.
 *
 * Global state (mirrors specs/MathLib.spec):
 * <pre>
 *   calls      : int = 0   // how many library calls have been made
 *   lastResult : int = 0   // the value the most recent call returned
 * </pre>
 *
 * The four functions are pure: each depends only on its arguments.  The two state
 * fields are the book-keeping that makes purity CHECKABLE — {@code calls} pins
 * down that every call is one call, and {@code lastResult} pins down that what
 * came back is what the library believes it returned.  Without them a spec for a
 * pure function can only constrain the return value's range, which is far weaker
 * than what these postconditions say (gcd really does divide both arguments).
 */
public class Helper {

    public static int calls = 0;
    public static int lastResult = 0;

    /** pre: x &gt; Integer.MIN_VALUE;  post: \result = |x|. */
    public static int abs(int x) {
        int absValue = x;
        if (absValue < 0) {
            absValue = -absValue;
        }
        calls = calls + 1;
        lastResult = absValue;
        return absValue;
    }

    /** pre: a &gt; 0 &amp;&amp; b &gt; 0;  post: \result divides both a and b, and is the greatest that does. */
    public static int gcd(int a, int b) {
        int x = a;
        int y = b;
        while (y != 0) {
            int remainder = x % y;
            x = y;
            y = remainder;
        }
        calls = calls + 1;
        lastResult = x;
        return x;
    }

    /** pre: base &gt; 0 &amp;&amp; 0 &lt;= exponent &lt;= 30;  post: \result = base^exponent. */
    public static int power(int base, int exponent) {
        int powerValue = 1;
        for (int i = 0; i < exponent; i++) {
            powerValue = powerValue * base;
        }
        calls = calls + 1;
        lastResult = powerValue;
        return powerValue;
    }

    /** pre: 0 &lt;= n &lt;= 12;  post: \result = n!  (12! is the largest that fits in an int). */
    public static int factorial(int n) {
        int factValue = 1;
        for (int i = 2; i <= n; i++) {
            factValue = factValue * i;
        }
        calls = calls + 1;
        lastResult = factValue;
        return factValue;
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        calls = 0;
        lastResult = 0;
    }
}
