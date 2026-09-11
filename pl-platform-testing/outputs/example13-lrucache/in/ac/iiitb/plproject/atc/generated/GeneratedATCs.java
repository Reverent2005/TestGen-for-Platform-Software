package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public String put_helper() {
        String key = Debug.makeSymbolicString("key");
        String value = Debug.makeSymbolicString("value");
        Debug.assume(key != null && value != null && Helper.capacity > 0);
        String evictedKey = Debug.makeSymbolicString("evictedKey");
        evictedKey = Helper.put(key, value);
        assert(java.util.Objects.equals(Helper.C.get(key), value) && java.util.Objects.equals(Helper.mostRecent, key) && Helper.size <= Helper.capacity);
        return evictedKey;
    }

    public String get_helper() {
        String key = Debug.makeSymbolicString("key");
        Debug.assume(Helper.C.containsKey(key));
        int size_old = Helper.size;
        String cachedValue = Debug.makeSymbolicString("cachedValue");
        cachedValue = Helper.get(key);
        assert(java.util.Objects.equals(cachedValue, Helper.C.get(key)) && java.util.Objects.equals(Helper.mostRecent, key) && java.util.Objects.equals(Helper.size, size_old));
        return cachedValue;
    }

    public void restore_helper(String evictedKey) {
        evictedKey = Debug.makeSymbolicString("evictedKey");
        String value = Debug.makeSymbolicString("value");
        Debug.assume(evictedKey != null && !Helper.C.containsKey(evictedKey));
        Helper.restore(evictedKey, value);
        assert(Helper.C.containsKey(evictedKey) && java.util.Objects.equals(Helper.mostRecent, evictedKey) && Helper.size <= Helper.capacity);
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        String evictedKey_0 = instance.put_helper();
        String evictedKey_1 = instance.put_helper();
        String evictedKey_2 = instance.put_helper();
        String cachedValue = instance.get_helper();
        instance.restore_helper(evictedKey_2);
    }
}
