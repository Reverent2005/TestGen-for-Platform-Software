package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void sqrt_helper() {
        double x = Debug.makeSymbolicReal("x");
        Debug.assume((x >= 0.0));
        double x_old = x;
        System.out.println(("Test Input: x = " + x));
        double result_sqrt = Helper.sqrt(x);
        assert(result_sqrt >= 0.0 && (result_sqrt * result_sqrt) <= x_old);
    }

    public void divide_helper() {
        double a = Debug.makeSymbolicReal("a");
        double b = Debug.makeSymbolicReal("b");
        Debug.assume((!java.util.Objects.equals(b, 0.0)));
        double a_old = a;
        double b_old = b;
        System.out.println(("Test Input: a = " + a));
        double result_divide = Helper.divide(a, b);
        assert((java.util.Objects.equals(result_divide, (a_old / b_old))));
    }

    public void abs_helper() {
        int x = Debug.makeSymbolicInteger("x");
        Debug.assume(true);
        int x_old = x;
        System.out.println(("Test Input: x = " + x));
        int result_abs = Helper.abs(x);
        assert(result_abs >= 0 && java.util.Objects.equals(result_abs, x_old) || java.util.Objects.equals(result_abs, -x_old));
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.sqrt_helper();
        instance.divide_helper();
        instance.abs_helper();
    }
}
