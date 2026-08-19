package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public Integer submit_helper() {
        String payload = "payload" /* CLIENT_INPUT: replace with the literal SPF solves for "payload" */;
        assumeTrue(payload != null);
        int nextId_old = Helper.nextId;
        Response taskIdResponse = executeApiCall(Helper.submit(payload));
        Integer taskId = extractFromResponse(taskIdResponse, "taskId");
        assert(Helper.Tasks.containsKey(taskId) && java.util.Objects.equals(Helper.nextId, (nextId_old + 1)));
        return taskId;
    }

    public String getResult_helper(Integer taskId) {
        // taskId is bound dynamically: it arrives as the SERVER_OUTPUT captured by submit (block 0)
        assumeTrue(Helper.Tasks.containsKey(taskId));
        Response resultResponse = executeApiCall(Helper.getResult(taskId));
        String result = extractFromResponse(resultResponse, "result");
        assert(Helper.Results.containsKey(taskId));
        return result;
    }

    public void cancelTask_helper(Integer taskId) {
        // taskId is bound dynamically: it arrives as the SERVER_OUTPUT captured by submit (block 0)
        assumeTrue(Helper.Tasks.containsKey(taskId));
        Helper.cancelTask(taskId);
        assert(!Helper.Tasks.containsKey(taskId));
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        Integer taskId = instance.submit_helper();
        String result = instance.getResult_helper(taskId);
        instance.cancelTask_helper(taskId);
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
