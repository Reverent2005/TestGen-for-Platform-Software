package org.junit;

/** COMPILE-TIME STUB ONLY — stands in for JUnit 4's Assume during verification. */
public class Assume {
    public static void assumeTrue(boolean condition) {
        if (!condition) {
            // Skipped, not failed: see AssumptionViolatedException.
            throw new AssumptionViolatedException("assumption violated");
        }
    }
}
