/*
 * Spec file for MathUtils
 * Contract definitions for basic math operations
 */

spec sqrt {
    signature: double sqrt(double x);
    requires:  x >= 0.0;
    ensures:   \result >= 0.0 && \result * \result <= x;
}

spec divide {
    signature: double divide(double a, double b);
    requires:  b != 0.0;
    ensures:   \result == a / b;
}

spec abs {
    signature: int abs(int x);
    requires:  true;
    ensures:   (\result >= 0) && (\result == x || \result == -x);
}

spec power {
    signature: double power(double base, int exp);
    requires:  exp >= 0;
    ensures:   \result >= 0.0;
}
