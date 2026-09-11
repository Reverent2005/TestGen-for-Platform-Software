package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void insert_helper() {
        int key = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        assumeTrue(!Helper.Keys.contains(key));
        int size_old = Helper.size;
        Helper.insert(key);
        assert(Helper.Keys.contains(key) && java.util.Objects.equals(Helper.size, (size_old + 1)));
    }

    public boolean contains_helper() {
        int key = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        assumeTrue((Helper.size > 0));
        int size_old = Helper.size;
        Response foundResponse = executeApiCall(Helper.contains(key));
        boolean found = extractFromResponse(foundResponse, "found");
        assert(java.util.Objects.equals(found, Helper.Keys.contains(key)) && java.util.Objects.equals(Helper.size, size_old));
        return found;
    }

    public int min_helper() {
        assumeTrue((Helper.size > 0));
        int size_old = Helper.size;
        Response smallestResponse = executeApiCall(Helper.min());
        int smallest = extractFromResponse(smallestResponse, "smallest");
        assert(java.util.Objects.equals(smallest, Helper.minKey) && java.util.Objects.equals(Helper.size, size_old));
        return smallest;
    }

    public void delete_helper() {
        int key = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        assumeTrue(Helper.Keys.contains(key));
        int size_old = Helper.size;
        Helper.delete(key);
        assert(!Helper.Keys.contains(key) && java.util.Objects.equals(Helper.size, (size_old - 1)));
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.insert_helper();
        instance.insert_helper();
        instance.insert_helper();
        boolean found = instance.contains_helper();
        int smallest = instance.min_helper();
        instance.delete_helper();
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
