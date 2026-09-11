package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public int abs_helper() {
        int x = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "x" */;
        assumeTrue((x >= -2147483647));
        int calls_old = Helper.calls;
        Response absValueResponse = executeApiCall(Helper.abs(x));
        int absValue = extractFromResponse(absValueResponse, "absValue");
        assert(absValue >= 0 && java.util.Objects.equals(Helper.lastResult, absValue) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return absValue;
    }

    public int gcd_helper() {
        int a = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "a" */;
        int b = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "b" */;
        assumeTrue(a > 0 && b > 0);
        int calls_old = Helper.calls;
        Response divisorResponse = executeApiCall(Helper.gcd(a, b));
        int divisor = extractFromResponse(divisorResponse, "divisor");
        assert(divisor > 0 && java.util.Objects.equals(a % divisor, 0) && java.util.Objects.equals(b % divisor, 0) && java.util.Objects.equals(Helper.lastResult, divisor) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return divisor;
    }

    public int power_helper() {
        int base = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "base" */;
        int exponent = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "exponent" */;
        assumeTrue(base > 0 && exponent >= 0 && exponent <= 30);
        int calls_old = Helper.calls;
        Response powerValueResponse = executeApiCall(Helper.power(base, exponent));
        int powerValue = extractFromResponse(powerValueResponse, "powerValue");
        assert(powerValue > 0 && java.util.Objects.equals(Helper.lastResult, powerValue) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return powerValue;
    }

    public int factorial_helper() {
        int n = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "n" */;
        assumeTrue(n >= 0 && n <= 12);
        int calls_old = Helper.calls;
        Response factValueResponse = executeApiCall(Helper.factorial(n));
        int factValue = extractFromResponse(factValueResponse, "factValue");
        assert(factValue >= 1 && java.util.Objects.equals(Helper.lastResult, factValue) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return factValue;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        int absValue = instance.abs_helper();
        int divisor = instance.gcd_helper();
        int powerValue = instance.power_helper();
        int factValue = instance.factorial_helper();
    }

    // ── Dynamic data binding support ──────────────────────────────────
    // SERVER_OUTPUT values are read back from the call at RUNTIME rather
    // than solved for, which is what separates this flavour from the SPF one.
    static class Response {
        private final Object payload;
        Response(Object payload) { this.payload = payload; }
        Object payload() { return payload; }
    }

    static Response executeApiCall(Object returnedValue) {
        return new Response(returnedValue);
    }

    @SuppressWarnings("unchecked")
    static <T> T extractFromResponse(Response response, String name) {
        return (T) response.payload();
    }

    @Test
    public void testSequence() {
        main(new String[0]);
    }
}
