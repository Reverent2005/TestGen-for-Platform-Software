package in.ac.iiitb.plproject.atc.generated;

import gov.nasa.jpf.symbc.Debug;

import org.junit.Test;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Arrays;

public class GeneratedATCs {

    @Test
    public void increment_helper() {
        int x = Debug.makeSymbolicInteger("x");
        int x_old = x;
        Debug.assume((x > 0));
        x = Helper.increment(x);
        assert((x > x_old));
    }

    @Test
    public void process_helper() {
        Set<?> data = (Set<?>) Debug.makeSymbolicRef("data", new HashSet<>());
        Map<?,?> result = (Map<?,?>) Debug.makeSymbolicRef("result", new HashMap<>());
        Map<?,?> result_old = (result != null) ? new HashMap(result) : new HashMap<>();
        Set<?> data_old = (data != null) ? new HashSet(data) : new HashSet<>();
        Debug.assume(new HashSet<>(Arrays.asList(1, 2, 3)).contains(2));
        Debug.assume((!java.util.Objects.equals(data, null)));
        Debug.assume((!java.util.Objects.equals(result, null)));
        Helper.process(data, result);
        Map<?,?> expectedResult = Helper.update(result_old, data_old);
        assert((result != null && expectedResult != null && result.equals(expectedResult)));
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.increment_helper();
        instance.increment_helper();
        instance.process_helper();
        instance.increment_helper();
    }
}
