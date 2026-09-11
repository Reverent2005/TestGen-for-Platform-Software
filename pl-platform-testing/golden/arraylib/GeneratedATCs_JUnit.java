package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void set_helper() {
        int index = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "index" */;
        int value = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue(index >= 0 && index < Helper.length);
        int writes_old = Helper.writes;
        Helper.set(index, value);
        assert(java.util.Objects.equals(Helper.A[index], value) && java.util.Objects.equals(Helper.writes, (writes_old + 1)));
    }

    public int get_helper() {
        int index = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "index" */;
        assumeTrue(index >= 0 && index < Helper.length);
        int writes_old = Helper.writes;
        Response elemResponse = executeApiCall(Helper.get(index));
        int elem = extractFromResponse(elemResponse, "elem");
        assert(java.util.Objects.equals(elem, Helper.A[index]) && java.util.Objects.equals(Helper.writes, writes_old));
        return elem;
    }

    public int sum_helper() {
        assumeTrue((Helper.length > 0));
        int writes_old = Helper.writes;
        Response totalResponse = executeApiCall(Helper.sum());
        int total = extractFromResponse(totalResponse, "total");
        assert(total >= 0 && java.util.Objects.equals(Helper.writes, writes_old));
        return total;
    }

    public int indexOf_helper() {
        int value = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue((Helper.length > 0));
        int writes_old = Helper.writes;
        Response positionResponse = executeApiCall(Helper.indexOf(value));
        int position = extractFromResponse(positionResponse, "position");
        assert(position >= -1 && position < Helper.length && java.util.Objects.equals(Helper.writes, writes_old));
        return position;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.set_helper();
        instance.set_helper();
        int elem = instance.get_helper();
        int total = instance.sum_helper();
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
