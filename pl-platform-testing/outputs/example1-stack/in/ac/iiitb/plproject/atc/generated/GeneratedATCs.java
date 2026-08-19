package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void push_helper() {
        String elem = Debug.makeSymbolicString("elem");
        Debug.assume((elem != null));
        int size_old = Helper.size;
        Helper.push(elem);
        assert(java.util.Objects.equals(Helper.size, (size_old + 1)) && java.util.Objects.equals(Helper.top, elem));
    }

    public String pop_helper() {
        Debug.assume((Helper.size > 0));
        int size_old = Helper.size;
        String poppedElem = Debug.makeSymbolicString("poppedElem");
        poppedElem = Helper.pop();
        assert((java.util.Objects.equals(Helper.size, (size_old - 1))));
        return poppedElem;
    }

    public String peek_helper() {
        Debug.assume((Helper.size > 0));
        int size_old = Helper.size;
        String peekedElem = Debug.makeSymbolicString("peekedElem");
        peekedElem = Helper.peek();
        assert(java.util.Objects.equals(peekedElem, Helper.top) && java.util.Objects.equals(Helper.size, size_old));
        return peekedElem;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.push_helper();
        instance.push_helper();
        String poppedElem = instance.pop_helper();
        String peekedElem = instance.peek_helper();
    }
}
