package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public Integer put_helper() {
        String key = Debug.makeSymbolicString("key");
        Integer value = Debug.makeSymbolicInteger("value");
        Debug.assume(key != null && value != null);
        Integer oldVal = Debug.makeSymbolicInteger("oldVal");
        oldVal = Helper.put(key, value);
        assert((java.util.Objects.equals(Helper.M.get(key), value)));
        return oldVal;
    }

    public Integer getOldValue_helper() {
        String key = Debug.makeSymbolicString("key");
        Debug.assume(Helper.M.containsKey(key));
        int size_old = Helper.size;
        Integer curVal = Debug.makeSymbolicInteger("curVal");
        curVal = Helper.getOldValue(key);
        assert(java.util.Objects.equals(curVal, Helper.M.get(key)) && java.util.Objects.equals(Helper.size, size_old));
        return curVal;
    }

    public void remove_helper() {
        String key = Debug.makeSymbolicString("key");
        Debug.assume(Helper.M.containsKey(key));
        int size_old = Helper.size;
        Helper.remove(key);
        assert(!Helper.M.containsKey(key) && java.util.Objects.equals(Helper.size, (size_old - 1)));
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        Integer oldVal_0 = instance.put_helper();
        Integer oldVal_1 = instance.put_helper();
        Integer curVal = instance.getOldValue_helper();
        instance.remove_helper();
    }
}
