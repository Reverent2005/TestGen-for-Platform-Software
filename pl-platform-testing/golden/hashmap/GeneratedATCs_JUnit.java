package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public Integer put_helper() {
        String key = "key" /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        Integer value = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "value" */;
        assumeTrue(key != null && value != null);
        Response oldValResponse = executeApiCall(Helper.put(key, value));
        Integer oldVal = extractFromResponse(oldValResponse, "oldVal");
        assert((java.util.Objects.equals(Helper.M.get(key), value)));
        return oldVal;
    }

    public Integer getOldValue_helper() {
        String key = "key" /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        assumeTrue(Helper.M.containsKey(key));
        int size_old = Helper.size;
        Response curValResponse = executeApiCall(Helper.getOldValue(key));
        Integer curVal = extractFromResponse(curValResponse, "curVal");
        assert(java.util.Objects.equals(curVal, Helper.M.get(key)) && java.util.Objects.equals(Helper.size, size_old));
        return curVal;
    }

    public void remove_helper() {
        String key = "key" /* CLIENT_INPUT: replace with the literal SPF solves for "key" */;
        assumeTrue(Helper.M.containsKey(key));
        int size_old = Helper.size;
        Helper.remove(key);
        assert(!Helper.M.containsKey(key) && java.util.Objects.equals(Helper.size, (size_old - 1)));
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        Integer oldVal_0 = instance.put_helper();
        Integer oldVal_1 = instance.put_helper();
        Integer curVal = instance.getOldValue_helper();
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
