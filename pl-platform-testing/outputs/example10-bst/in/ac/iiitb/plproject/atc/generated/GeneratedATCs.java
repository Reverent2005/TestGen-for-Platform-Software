package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void insert_helper() {
        int key = Debug.makeSymbolicInteger("key");
        Debug.assume(!Helper.Keys.contains(key));
        int size_old = Helper.size;
        Helper.insert(key);
        assert(Helper.Keys.contains(key) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public boolean contains_helper() {
        int key = Debug.makeSymbolicInteger("key");
        Debug.assume((Helper.size > 0));
        int size_old = Helper.size;
        boolean found = (!java.util.Objects.equals(Debug.makeSymbolicInteger("found"), 0));
        found = Helper.contains(key);
        assert(java.util.Objects.equals(found, Helper.Keys.contains(key)) && java.util.Objects.equals(Helper.size, size_old));
        return found;
    }

    public int min_helper() {
        Debug.assume((Helper.size > 0));
        int size_old = Helper.size;
        int smallest = Debug.makeSymbolicInteger("smallest");
        smallest = Helper.min();
        assert(java.util.Objects.equals(smallest, Helper.minKey) && java.util.Objects.equals(Helper.size, size_old));
        return smallest;
    }

    public void delete_helper() {
        int key = Debug.makeSymbolicInteger("key");
        Debug.assume(Helper.Keys.contains(key));
        int size_old = Helper.size;
        Helper.delete(key);
        assert(!Helper.Keys.contains(key) && java.util.Objects.equals(Helper.size, (size_old - 1)));
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.insert_helper();
        instance.insert_helper();
        instance.insert_helper();
        boolean found = instance.contains_helper();
        int smallest = instance.min_helper();
        instance.delete_helper();
    }
}
