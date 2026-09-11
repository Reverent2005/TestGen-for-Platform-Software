package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void insert_helper() {
        Integer value = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue(value != null);
        int size_old = Helper.size;
        Helper.insert(value);
        assert(Helper.H.contains(value) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public Integer peekMin_helper() {
        assumeTrue((Helper.size > 0));
        int size_old = Helper.size;
        Response smallestResponse = executeApiCall(Helper.peekMin());
        Integer smallest = extractFromResponse(smallestResponse, "smallest");
        assert(java.util.Objects.equals(smallest, Helper.minValue) && java.util.Objects.equals(Helper.size, size_old));
        return smallest;
    }

    public Integer extractMin_helper() {
        assumeTrue((Helper.size > 0));
        Integer minValue_old = Helper.minValue;
        int size_old = Helper.size;
        Response removedMinResponse = executeApiCall(Helper.extractMin());
        Integer removedMin = extractFromResponse(removedMinResponse, "removedMin");
        assert(java.util.Objects.equals(removedMin, minValue_old) && java.util.Objects.equals(Helper.size, (size_old - 1)));
        return removedMin;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.insert_helper();
        instance.insert_helper();
        instance.insert_helper();
        Integer smallest = instance.peekMin_helper();
        Integer removedMin = instance.extractMin_helper();
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
