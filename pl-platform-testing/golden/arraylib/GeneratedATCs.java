package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void set_helper() {
        int index = Debug.makeSymbolicInteger("index");
        int value = Debug.makeSymbolicInteger("value");
        Debug.assume(index >= 0 && index < Helper.length);
        int writes_old = Helper.writes;
        Helper.set(index, value);
        assert(java.util.Objects.equals(Helper.A[index], value) && java.util.Objects.equals(Helper.writes, (writes_old + 1)));
    }

    public int get_helper() {
        int index = Debug.makeSymbolicInteger("index");
        Debug.assume(index >= 0 && index < Helper.length);
        int writes_old = Helper.writes;
        int elem = Debug.makeSymbolicInteger("elem");
        elem = Helper.get(index);
        assert(java.util.Objects.equals(elem, Helper.A[index]) && java.util.Objects.equals(Helper.writes, writes_old));
        return elem;
    }

    public int sum_helper() {
        Debug.assume((Helper.length > 0));
        int writes_old = Helper.writes;
        int total = Debug.makeSymbolicInteger("total");
        total = Helper.sum();
        assert(total >= 0 && java.util.Objects.equals(Helper.writes, writes_old));
        return total;
    }

    public int indexOf_helper() {
        int value = Debug.makeSymbolicInteger("value");
        Debug.assume((Helper.length > 0));
        int writes_old = Helper.writes;
        int position = Debug.makeSymbolicInteger("position");
        position = Helper.indexOf(value);
        assert(position >= -1 && position < Helper.length && java.util.Objects.equals(Helper.writes, writes_old));
        return position;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.set_helper();
        instance.set_helper();
        int elem = instance.get_helper();
        int total = instance.sum_helper();
        int position = instance.indexOf_helper();
    }
}
