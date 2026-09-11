package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void enqueue_helper() {
        String elem = Debug.makeSymbolicString("elem");
        Debug.assume((elem != null));
        int size_old = Helper.size;
        Helper.enqueue(elem);
        assert(java.util.Objects.equals(Helper.size, (size_old + 1)) && Helper.Q.contains(elem));
    }

    public String dequeue_helper() {
        Debug.assume((Helper.size > 0));
        String head_old = Helper.head;
        int size_old = Helper.size;
        String dequeuedElem = Debug.makeSymbolicString("dequeuedElem");
        dequeuedElem = Helper.dequeue();
        assert(java.util.Objects.equals(dequeuedElem, head_old) && java.util.Objects.equals(Helper.size, (size_old - 1)));
        return dequeuedElem;
    }

    public String front_helper() {
        Debug.assume((Helper.size > 0));
        int size_old = Helper.size;
        String frontElem = Debug.makeSymbolicString("frontElem");
        frontElem = Helper.front();
        assert(java.util.Objects.equals(frontElem, Helper.head) && java.util.Objects.equals(Helper.size, size_old));
        return frontElem;
    }

    public boolean isEmpty_helper() {
        Debug.assume((Helper.size >= 0));
        int size_old = Helper.size;
        boolean emptyFlag = (!java.util.Objects.equals(Debug.makeSymbolicInteger("emptyFlag"), 0));
        emptyFlag = Helper.isEmpty();
        assert(java.util.Objects.equals(emptyFlag, false) && java.util.Objects.equals(Helper.size, size_old));
        return emptyFlag;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.enqueue_helper();
        instance.enqueue_helper();
        String dequeuedElem = instance.dequeue_helper();
        String frontElem = instance.front_helper();
        boolean emptyFlag = instance.isEmpty_helper();
    }
}
