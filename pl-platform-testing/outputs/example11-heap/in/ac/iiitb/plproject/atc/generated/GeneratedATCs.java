package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void insert_helper() {
        Integer value = Debug.makeSymbolicInteger("value");
        Debug.assume((value != null));
        int size_old = Helper.size;
        Helper.insert(value);
        assert(Helper.H.contains(value) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public Integer peekMin_helper() {
        Debug.assume((Helper.size > 0));
        int size_old = Helper.size;
        Integer smallest = Debug.makeSymbolicInteger("smallest");
        smallest = Helper.peekMin();
        assert(java.util.Objects.equals(smallest, Helper.minValue) && java.util.Objects.equals(Helper.size, size_old));
        return smallest;
    }

    public Integer extractMin_helper() {
        Debug.assume((Helper.size > 0));
        Integer minValue_old = Helper.minValue;
        int size_old = Helper.size;
        Integer removedMin = Debug.makeSymbolicInteger("removedMin");
        removedMin = Helper.extractMin();
        assert(java.util.Objects.equals(removedMin, minValue_old) && java.util.Objects.equals(Helper.size, (size_old - 1)));
        return removedMin;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.insert_helper();
        instance.insert_helper();
        instance.insert_helper();
        Integer smallest = instance.peekMin_helper();
        Integer removedMin = instance.extractMin_helper();
    }
}
