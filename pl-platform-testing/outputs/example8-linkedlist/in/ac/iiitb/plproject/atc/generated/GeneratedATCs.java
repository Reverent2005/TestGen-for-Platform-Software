package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void addFirst_helper() {
        Integer value = Debug.makeSymbolicInteger("value");
        Debug.assume((value != null));
        int size_old = Helper.size;
        Helper.addFirst(value);
        assert(java.util.Objects.equals(Helper.head, value) && Helper.L.contains(value) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public void addLast_helper() {
        Integer value = Debug.makeSymbolicInteger("value");
        Debug.assume((value != null));
        int size_old = Helper.size;
        Helper.addLast(value);
        assert(java.util.Objects.equals(Helper.tail, value) && Helper.L.contains(value) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public Integer removeFirst_helper() {
        Debug.assume((Helper.size > 0));
        Integer head_old = Helper.head;
        int size_old = Helper.size;
        Integer removedValue = Debug.makeSymbolicInteger("removedValue");
        removedValue = Helper.removeFirst();
        assert(java.util.Objects.equals(removedValue, head_old) && java.util.Objects.equals(Helper.size, (size_old - 1)));
        return removedValue;
    }

    public int indexOf_helper() {
        Integer value = Debug.makeSymbolicInteger("value");
        Debug.assume((Helper.size > 0));
        int position = Debug.makeSymbolicInteger("position");
        position = Helper.indexOf(value);
        assert(position >= -1 && position < Helper.size);
        return position;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.addFirst_helper();
        instance.addLast_helper();
        Integer removedValue = instance.removeFirst_helper();
        int position = instance.indexOf_helper();
    }
}
