package in.ac.iiitb.plproject.atc;

import in.ac.iiitb.plproject.parser.ast.JmlSpecAst;
import in.ac.iiitb.plproject.parser.ast.TestStringAst;

public interface GenATC {

    /**
     * Generates a complete, runnable Java test file (the ATC)
     * from JML specs and a test string.
     *
     * @param jmlSpecAst The full set of parsed JML specs (e.g., STACK specs).
     * @param testStringAst The sequence of operations to test (e.g., PUSH_OK, POP_OK).
     * @return A JavaFile object (or String) containing the generated test code.
     */
    JavaFile generateAtcFile(JmlSpecAst jmlSpecAst, TestStringAst testStringAst);
}

