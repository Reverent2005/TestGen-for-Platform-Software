package in.ac.iiitb.plproject.parser.ast;

import java.util.List;

/**
 * Represents a test string - a sequence of function calls to test.
 * Example: ["PUSH_OK", "POP_OK", "PUSH_OK"]
 */
public class TestStringAst {
    private List<String> calls;

    public TestStringAst(List<String> calls) {
        this.calls = calls;
    }

    public List<String> getCalls() {
        return calls;
    }
}

