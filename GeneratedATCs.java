package in.ac.iiitb.plproject.atc.generated;
import java.util.*;

test string = ["increment" , "increment", "process", "increment"]

IR-> sym-wrapper (IR) -> (.Java, .JPF)
IR-> Java Code (string) -> sym-wrapper

public class GeneratedATCs {

    public void increment_helper() {
        int x = Symbolic.input("x");
        assume((x > 0));
        int x_old = x;
        System.out.println(("Test Input: x = " + x));
        int[] xRef = new int[]{x};
        Helper.increment(xRef);
        x = xRef[0];
        int r = Helper.increment(x); "\result"
        assert((xRef[0] > x_old));
    }

    public void process_helper() {
        Set<Integer> data = (Set<Integer>) Symbolic.input("data");
        Map<Integer, Integer> result = (Map<Integer, Integer>) Symbolic.input("result");
        if (data == null || result == null) {
            return;
        }
        assume(data != null);
        assume(result != null);
        assume(new HashSet<>(Arrays.asList(1, 2, 3)).contains(2));
        System.out.println("Test Input: data = " + data);
        if (data != null && result != null) {
            Helper.process(data, result);
        }
        System.out.println("Test Input: Helper.process completed");
        assert(result != null);
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.increment_helper();
        instance.increment_helper();
        instance.process_helper();
        instance.increment_helper();
    }
}
