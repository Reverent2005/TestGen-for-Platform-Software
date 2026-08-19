package in.ac.iiitb.plproject.atc.generated;

import java.util.HashMap;
import java.util.Map;

/**
 * EXAMPLE 3 — TaskQueue.
 *
 * The library under test for the submit -&gt; getResult -&gt; cancelTask dry run, the
 * example that exercises full multi-hop propagation: the taskId submit() invents
 * is threaded through the two blocks that follow it.
 *
 * Global state (mirrors specs/TaskQueue.spec):
 * <pre>
 *   Tasks   : Map&lt;Integer,String&gt; = {}
 *   Results : Map&lt;Integer,String&gt; = {}
 *   nextId  : int                  = 1
 * </pre>
 *
 * Structurally this is the placeOrder -&gt; shipOrder REST pattern: the algorithm
 * does not care whether the callee is a Java library or an HTTP API.
 */
public class Helper {

    public static Map<Integer, String> Tasks = new HashMap<Integer, String>();
    public static Map<Integer, String> Results = new HashMap<Integer, String>();
    public static int nextId = 1;

    /**
     * pre: payload != null;
     * post: \result = taskId, Tasks'[taskId] = payload, nextId' = nextId + 1.
     *
     * taskId is the archetypal SERVER_OUTPUT: auto-assigned by the callee, and
     * unknowable to the caller before the call.
     */
    public static Integer submit(String payload) {
        Integer taskId = nextId;
        Tasks.put(taskId, payload);
        nextId = nextId + 1;
        return taskId;
    }

    /**
     * pre: taskId in dom(Tasks);  post: \result = result, Results'[taskId] = result.
     * taskId is a SERVER_OUTPUT propagated from submit.
     */
    public static String getResult(Integer taskId) {
        String result = "result-for-" + Tasks.get(taskId);
        Results.put(taskId, result);
        return result;
    }

    /**
     * pre: taskId in dom(Tasks);  post: Tasks' = Tasks \ {taskId}.
     * taskId is a SERVER_OUTPUT propagated from submit.
     */
    public static void cancelTask(Integer taskId) {
        Tasks.remove(taskId);
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        Tasks = new HashMap<Integer, String>();
        Results = new HashMap<Integer, String>();
        nextId = 1;
    }
}
