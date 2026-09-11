package org.junit;

/**
 * COMPILE-TIME STUB ONLY — stands in for JUnit 4's AssumptionViolatedException.
 *
 * Carries the same meaning as the real one: the test's assumptions were not met,
 * so the test did not run.  That is <em>skipped</em>, not <em>failed</em> — a
 * distinction the generated JUnit flavour depends on, because its CLIENT_INPUT
 * placeholders are dummies rather than values solved against the precondition.
 * A spec whose precondition the dummy happens to satisfy (elem != null) runs;
 * one whose precondition it does not (numSeats &gt; 0, with the placeholder 0) is
 * skipped.  Neither is a defect in the library under test.
 */
public class AssumptionViolatedException extends RuntimeException {
    public AssumptionViolatedException(String message) { super(message); }
}
