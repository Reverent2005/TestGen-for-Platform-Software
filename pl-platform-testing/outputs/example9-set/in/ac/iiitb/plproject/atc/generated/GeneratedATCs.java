package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public boolean add_helper() {
        String elem = Debug.makeSymbolicString("elem");
        Debug.assume((elem != null));
        boolean added = (!java.util.Objects.equals(Debug.makeSymbolicInteger("added"), 0));
        added = Helper.add(elem);
        assert(Helper.E.contains(elem));
        return added;
    }

    public boolean contains_helper() {
        String elem = Debug.makeSymbolicString("elem");
        Debug.assume((elem != null));
        int size_old = Helper.size;
        boolean present = (!java.util.Objects.equals(Debug.makeSymbolicInteger("present"), 0));
        present = Helper.contains(elem);
        assert(java.util.Objects.equals(present, Helper.E.contains(elem)) && java.util.Objects.equals(Helper.size, size_old));
        return present;
    }

    public void remove_helper() {
        String elem = Debug.makeSymbolicString("elem");
        Debug.assume(Helper.E.contains(elem));
        int size_old = Helper.size;
        Helper.remove(elem);
        assert(!Helper.E.contains(elem) && java.util.Objects.equals(Helper.size, (size_old - 1)));
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        boolean added_0 = instance.add_helper();
        boolean added_1 = instance.add_helper();
        boolean present = instance.contains_helper();
        instance.remove_helper();
    }
}
