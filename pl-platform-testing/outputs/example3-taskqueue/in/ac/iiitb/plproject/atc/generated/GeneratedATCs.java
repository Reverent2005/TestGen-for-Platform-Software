package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public Integer submit_helper() {
        String payload = Debug.makeSymbolicString("payload");
        Debug.assume((payload != null));
        int nextId_old = Helper.nextId;
        Integer taskId = Debug.makeSymbolicInteger("taskId");
        taskId = Helper.submit(payload);
        assert(Helper.Tasks.containsKey(taskId) && java.util.Objects.equals(Helper.nextId, (nextId_old + 1)));
        return taskId;
    }

    public String getResult_helper(Integer taskId) {
        taskId = Debug.makeSymbolicInteger("taskId");
        Debug.assume(Helper.Tasks.containsKey(taskId));
        String result = Debug.makeSymbolicString("result");
        result = Helper.getResult(taskId);
        assert(Helper.Results.containsKey(taskId));
        return result;
    }

    public void cancelTask_helper(Integer taskId) {
        taskId = Debug.makeSymbolicInteger("taskId");
        Debug.assume(Helper.Tasks.containsKey(taskId));
        Helper.cancelTask(taskId);
        assert(!Helper.Tasks.containsKey(taskId));
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        Integer taskId = instance.submit_helper();
        String result = instance.getResult_helper(taskId);
        instance.cancelTask_helper(taskId);
    }
}
