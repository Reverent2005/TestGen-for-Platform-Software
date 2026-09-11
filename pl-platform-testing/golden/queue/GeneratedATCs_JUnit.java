package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void enqueue_helper() {
        String elem = "elem" /* CLIENT_INPUT: replace with the literal SPF solves for "elem" */;
        assumeTrue(elem != null);
        int size_old = Helper.size;
        Helper.enqueue(elem);
        assert(java.util.Objects.equals(Helper.size, (size_old + 1)) && Helper.Q.contains(elem));
    }

    public String dequeue_helper() {
        assumeTrue((Helper.size > 0));
        String head_old = Helper.head;
        int size_old = Helper.size;
        Response dequeuedElemResponse = executeApiCall(Helper.dequeue());
        String dequeuedElem = extractFromResponse(dequeuedElemResponse, "dequeuedElem");
        assert(java.util.Objects.equals(dequeuedElem, head_old) && java.util.Objects.equals(Helper.size, (size_old - 1)));
        return dequeuedElem;
    }

    public String front_helper() {
        assumeTrue((Helper.size > 0));
        int size_old = Helper.size;
        Response frontElemResponse = executeApiCall(Helper.front());
        String frontElem = extractFromResponse(frontElemResponse, "frontElem");
        assert(java.util.Objects.equals(frontElem, Helper.head) && java.util.Objects.equals(Helper.size, size_old));
        return frontElem;
    }

    public boolean isEmpty_helper() {
        assumeTrue((Helper.size >= 0));
        int size_old = Helper.size;
        Response emptyFlagResponse = executeApiCall(Helper.isEmpty());
        boolean emptyFlag = extractFromResponse(emptyFlagResponse, "emptyFlag");
        assert(java.util.Objects.equals(emptyFlag, false) && java.util.Objects.equals(Helper.size, size_old));
        return emptyFlag;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.enqueue_helper();
        instance.enqueue_helper();
        String dequeuedElem = instance.dequeue_helper();
        String frontElem = instance.front_helper();
        boolean emptyFlag = instance.isEmpty_helper();
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
