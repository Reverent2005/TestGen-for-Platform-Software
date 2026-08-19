/*
 * EXAMPLE 3 — TaskQueue
 * Pattern: full multi-hop forward propagation.
 *
 * Test string: submit -> getResult -> cancelTask
 *
 * taskId, the SERVER_OUTPUT submit invents, is threaded across TWO hops:
 * getResult_helper(Integer taskId) and cancelTask_helper(Integer taskId) both
 * take it as a formal parameter, and main() passes it explicitly to each.
 * getResult's own SERVER_OUTPUT, result, enters availableServerOutputs but is
 * never consumed downstream — captured and returned, never forwarded.
 *
 * This is structurally the placeOrder -> shipOrder REST pattern, which is the
 * point: the algorithm is domain-agnostic.
 *
 * KNOWN LIMITATION (Section 6, #1): nextId is an auto-incrementing global.  The
 * solver cannot infer the concrete taskId unless nextId is itself modelled as a
 * symbolic integer, which is out of scope here.
 */

state {
    Map<Integer,String> Tasks;
    Map<Integer,String> Results;
    int nextId;
}

spec submit {
    signature: Integer submit(String payload);
    requires:  payload != null;
    ensures:   \result == taskId && Tasks.containsKey(taskId) && nextId == \old(nextId) + 1;
}

spec getResult {
    signature: String getResult(Integer taskId);
    requires:  Tasks.containsKey(taskId);
    ensures:   \result == result && Results.containsKey(taskId);
}

spec cancelTask {
    signature: void cancelTask(Integer taskId);
    requires:  Tasks.containsKey(taskId);
    ensures:   !Tasks.containsKey(taskId);
}
