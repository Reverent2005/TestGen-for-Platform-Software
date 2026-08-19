package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void push_helper() {
        String elem = "elem" /* CLIENT_INPUT: replace with the literal SPF solves for "elem" */;
        assumeTrue(elem != null);
        int size_old = Helper.size;
        Helper.push(elem);
        assert(java.util.Objects.equals(Helper.size, (size_old + 1)) && java.util.Objects.equals(Helper.top, elem));
    }

    public String pop_helper() {
        assumeTrue((Helper.size > 0));
        int size_old = Helper.size;
        Response poppedElemResponse = executeApiCall(Helper.pop());
        String poppedElem = extractFromResponse(poppedElemResponse, "poppedElem");
        assert((java.util.Objects.equals(Helper.size, (size_old - 1))));
        return poppedElem;
    }

    public String peek_helper() {
        assumeTrue((Helper.size > 0));
        int size_old = Helper.size;
        Response peekedElemResponse = executeApiCall(Helper.peek());
        String peekedElem = extractFromResponse(peekedElemResponse, "peekedElem");
        assert(java.util.Objects.equals(peekedElem, Helper.top) && java.util.Objects.equals(Helper.size, size_old));
        return peekedElem;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.push_helper();
        instance.push_helper();
        String poppedElem = instance.pop_helper();
        String peekedElem = instance.peek_helper();
    }

    // ── Dynamic data binding support ──────────────────────────────────
    // SERVER_OUTPUT values are read back from the call at RUNTIME rather
    // than solved for, which is what separates this flavour from the SPF one.
    static class Response {
        private final Object payload;
        Response(Object payload) { this.payload = payload; }
        Object payload() { return payload; }
    }

    static Response executeApiCall(Object returnedValue) {
        return new Response(returnedValue);
    }

    @SuppressWarnings("unchecked")
    static <T> T extractFromResponse(Response response, String name) {
        return (T) response.payload();
    }

    @Test
    public void testSequence() {
        main(new String[0]);
    }
}
