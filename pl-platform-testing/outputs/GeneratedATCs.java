package in.ac.iiitb.plproject.atc.generated;

import gov.nasa.jpf.symbc.Debug;

import org.junit.Test;
import java.util.*;

public class GeneratedATCs {

    @Test
    public void appendExclamation_helper() {
        String s = Debug.makeSymbolicString("s");
        Debug.assume(s != null);
        System.out.println(("Test Input: s = " + s));
        Helper.appendExclamation(s);
        assert(s != null);
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.appendExclamation_helper();
        instance.appendExclamation_helper();
    }
}
