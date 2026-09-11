package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public int abs_helper() {
        int x = Debug.makeSymbolicInteger("x");
        Debug.assume((x >= -2147483647));
        int calls_old = Helper.calls;
        int absValue = Debug.makeSymbolicInteger("absValue");
        absValue = Helper.abs(x);
        assert(absValue >= 0 && java.util.Objects.equals(Helper.lastResult, absValue) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return absValue;
    }

    public int gcd_helper() {
        int a = Debug.makeSymbolicInteger("a");
        int b = Debug.makeSymbolicInteger("b");
        Debug.assume(a > 0 && b > 0);
        int calls_old = Helper.calls;
        int divisor = Debug.makeSymbolicInteger("divisor");
        divisor = Helper.gcd(a, b);
        assert(divisor > 0 && java.util.Objects.equals(a % divisor, 0) && java.util.Objects.equals(b % divisor, 0) && java.util.Objects.equals(Helper.lastResult, divisor) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return divisor;
    }

    public int power_helper() {
        int base = Debug.makeSymbolicInteger("base");
        int exponent = Debug.makeSymbolicInteger("exponent");
        Debug.assume(base > 0 && exponent >= 0 && exponent <= 30);
        int calls_old = Helper.calls;
        int powerValue = Debug.makeSymbolicInteger("powerValue");
        powerValue = Helper.power(base, exponent);
        assert(powerValue > 0 && java.util.Objects.equals(Helper.lastResult, powerValue) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return powerValue;
    }

    public int factorial_helper() {
        int n = Debug.makeSymbolicInteger("n");
        Debug.assume(n >= 0 && n <= 12);
        int calls_old = Helper.calls;
        int factValue = Debug.makeSymbolicInteger("factValue");
        factValue = Helper.factorial(n);
        assert(factValue >= 1 && java.util.Objects.equals(Helper.lastResult, factValue) && java.util.Objects.equals(Helper.calls, (calls_old + 1)));
        return factValue;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        int absValue = instance.abs_helper();
        int divisor = instance.gcd_helper();
        int powerValue = instance.power_helper();
        int factValue = instance.factorial_helper();
    }
}
