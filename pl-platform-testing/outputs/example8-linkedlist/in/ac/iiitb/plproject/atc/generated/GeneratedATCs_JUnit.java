package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void addFirst_helper() {
        Integer value = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue(value != null);
        int size_old = Helper.size;
        Helper.addFirst(value);
        assert(java.util.Objects.equals(Helper.head, value) && Helper.L.contains(value) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public void addLast_helper() {
        Integer value = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue(value != null);
        int size_old = Helper.size;
        Helper.addLast(value);
        assert(java.util.Objects.equals(Helper.tail, value) && Helper.L.contains(value) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public Integer removeFirst_helper() {
        assumeTrue((Helper.size > 0));
        Integer head_old = Helper.head;
        int size_old = Helper.size;
        Response removedValueResponse = executeApiCall(Helper.removeFirst());
        Integer removedValue = extractFromResponse(removedValueResponse, "removedValue");
        assert(java.util.Objects.equals(removedValue, head_old) && java.util.Objects.equals(Helper.size, (size_old - 1)));
        return removedValue;
    }

    public int indexOf_helper() {
        Integer value = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue((Helper.size > 0));
        Response positionResponse = executeApiCall(Helper.indexOf(value));
        int position = extractFromResponse(positionResponse, "position");
        assert(position >= -1 && position < Helper.size);
        return position;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.addFirst_helper();
        instance.addLast_helper();
        Integer removedValue = instance.removeFirst_helper();
        int position = instance.indexOf_helper();
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
