package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public String put_helper() {
        String key = "key" /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        String value = "value" /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue(key != null && value != null && Helper.capacity > 0);
        Response evictedKeyResponse = executeApiCall(Helper.put(key, value));
        String evictedKey = extractFromResponse(evictedKeyResponse, "evictedKey");
        assert(java.util.Objects.equals(Helper.C.get(key), value) && java.util.Objects.equals(Helper.mostRecent, key) && Helper.size <= Helper.capacity);
        return evictedKey;
    }

    public String get_helper() {
        String key = "key" /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        assumeTrue(Helper.C.containsKey(key));
        int size_old = Helper.size;
        Response cachedValueResponse = executeApiCall(Helper.get(key));
        String cachedValue = extractFromResponse(cachedValueResponse, "cachedValue");
        assert(java.util.Objects.equals(cachedValue, Helper.C.get(key)) && java.util.Objects.equals(Helper.mostRecent, key) && java.util.Objects.equals(Helper.size, size_old));
        return cachedValue;
    }

    public void restore_helper(String evictedKey) {
        // evictedKey is bound dynamically: it arrives as the SERVER_OUTPUT captured by put (block 0)
        String value = "value" /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue(evictedKey != null && !Helper.C.containsKey(evictedKey));
        Helper.restore(evictedKey, value);
        assert(Helper.C.containsKey(evictedKey) && java.util.Objects.equals(Helper.mostRecent, evictedKey) && Helper.size <= Helper.capacity);
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        String evictedKey_0 = instance.put_helper();
        String evictedKey_1 = instance.put_helper();
        String evictedKey_2 = instance.put_helper();
        String cachedValue = instance.get_helper();
        instance.restore_helper(evictedKey_2);
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
