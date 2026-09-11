package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public boolean add_helper() {
        String elem = "elem" /* CLIENT_INPUT: replace with the literal SPF solves for "elem" */;
        assumeTrue(elem != null);
        Response addedResponse = executeApiCall(Helper.add(elem));
        boolean added = extractFromResponse(addedResponse, "added");
        assert(Helper.E.contains(elem));
        return added;
    }

    public boolean contains_helper() {
        String elem = "elem" /* CLIENT_INPUT: replace with the literal SPF solves for "elem" */;
        assumeTrue(elem != null);
        int size_old = Helper.size;
        Response presentResponse = executeApiCall(Helper.contains(elem));
        boolean present = extractFromResponse(presentResponse, "present");
        assert(java.util.Objects.equals(present, Helper.E.contains(elem)) && java.util.Objects.equals(Helper.size, size_old));
        return present;
    }

    public void remove_helper() {
        String elem = "elem" /* CLIENT_INPUT: replace with the literal SPF solves for "elem" */;
        assumeTrue(Helper.E.contains(elem));
        int size_old = Helper.size;
        Helper.remove(elem);
        assert(!Helper.E.contains(elem) && java.util.Objects.equals(Helper.size, (size_old - 1)));
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        boolean added_0 = instance.add_helper();
        boolean added_1 = instance.add_helper();
        boolean present = instance.contains_helper();
        instance.remove_helper();
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
