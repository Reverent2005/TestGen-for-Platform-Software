/*
 * EXAMPLE 6 — MathLib (integer mathematics)
 * Pattern: pure functions, specified by a PROPERTY of the answer rather than by
 * recomputing it.
 *
 * Test string: abs -> gcd -> power -> factorial
 *
 * Nothing propagates here — four independent calls, four independent captures —
 * so what this example is for is the shape of the postconditions.  A pure
 * function has no interesting state to constrain, so a spec that only said
 * `\result == answer` would assert nothing at all: the binding is a capture, not
 * a claim.  Each block therefore adds a property the answer must have:
 *
 *   gcd:       a % divisor == 0 && b % divisor == 0   — it really divides both
 *   power:     powerValue > 0                          — no overflow to a negative
 *   factorial: factValue >= 1
 *   abs:       absValue >= 0
 *
 * `a % divisor` uses the same escape hatch as `Holds.containsKey(seatHoldId)` in
 * Example 4: the grammar has no % operator, so the whole term is read as one free
 * name and emitted verbatim.  Its leading identifier is a parameter rather than a
 * state variable, so it is passed through untouched, which is what compiles.
 *
 * calls and lastResult are the two state variables that make a pure function's
 * spec bite: `calls == \old(calls) + 1` says this was exactly one call, and
 * `lastResult == answer` says the library agrees with what it handed back.
 *
 * LIMITATION: gcd is specified as a common divisor, not as the GREATEST common
 * divisor — the grammar cannot quantify over the divisors it beats.  What that
 * costs is measured: MATH_GCD_RETURNS_ONE survives mutations/mathlib.mutants,
 * because 1 divides everything.
 *
 * Integer.MIN_VALUE is excluded from abs's domain because its absolute value does
 * not fit in an int; the literal is written -2147483647 rather than -2147483648
 * for the same reason on the parser's side.
 */

state {
    int calls;
    int lastResult;
}

spec abs {
    signature: int abs(int x);
    requires:  x >= -2147483647;
    ensures:   \result == absValue && absValue >= 0 && lastResult == absValue && calls == \old(calls) + 1;
}

spec gcd {
    signature: int gcd(int a, int b);
    requires:  a > 0 && b > 0;
    ensures:   \result == divisor && divisor > 0 && a % divisor == 0 && b % divisor == 0 && lastResult == divisor && calls == \old(calls) + 1;
}

spec power {
    signature: int power(int base, int exponent);
    requires:  base > 0 && exponent >= 0 && exponent <= 30;
    ensures:   \result == powerValue && powerValue > 0 && lastResult == powerValue && calls == \old(calls) + 1;
}

spec factorial {
    signature: int factorial(int n);
    requires:  n >= 0 && n <= 12;
    ensures:   \result == factValue && factValue >= 1 && lastResult == factValue && calls == \old(calls) + 1;
}
