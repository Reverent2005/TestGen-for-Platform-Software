package in.ac.iiitb.plproject.atc.generated;

import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    // Existing string helper
    public void appendExclamation_helper() {
        String s = Debug.makeSymbolicString("s");
        Debug.assume(s != null);
        System.out.println("Test Input: s = " + s);
        Helper.appendExclamation(s);
        assert s != null;
    }

    // New age helper for symbolic execution
    public void ageCategory_helper() {
        int age = Debug.makeSymbolicInteger("age");
        // Example precondition: age must be non-negative and not absurdly high
        Debug.assume(age >= 0 && age <= 120);

        System.out.println("Test Input: age = " + age);
        Helper.ageCategoryPrint(age);

        // Example postcondition: just to show symbolic assertions, could check category
        assert age >= 0;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.ageCategory_helper();
    }
}
