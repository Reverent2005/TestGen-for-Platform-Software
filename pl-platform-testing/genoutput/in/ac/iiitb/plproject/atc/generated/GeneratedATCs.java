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
        Helper.increment(x);
        assert((x > x_old));
    }

    @Test
    public void process_helper() {
        Set data = (Set) Debug.makeSymbolicRef("data");
        Map result = (Map) Debug.makeSymbolicRef("result");
        Map result_old = result;
        Set data_old = data;
        Debug.assume(new Set(1, 2, 3).contains(2));
        Helper.process(data, result);
        assert((result == update(result_old, data_old)));
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.increment_helper();
        instance.increment_helper();
        instance.process_helper();
        instance.increment_helper();
    }
}
