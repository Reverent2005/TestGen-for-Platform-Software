package in.ac.iiitb.plproject.atc.generated;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
 * Helper class for the generated test cases.
 * This class contains the methods under test that are called by the generated ATC code.
 */
public class Helper {
    
    /**
     * Increments the given integer value and returns the result.
     * Since Java passes primitives by value, we return the incremented value.
     * 
     * @param x The integer to increment
     * @return The incremented value (x + 1)
     */
    public static int increment(int x) {
        return x + 1;
    }
    
    /**
     * Processes the given data set and updates the result map.
     * 
     * @param data The input data set
     * @param result The result map to be updated
     */
    public static void process(Set<?> data, Map<?,?> result) {
        // In a real implementation, this would process the data and update the result map.
        // For symbolic execution testing, this stub allows the test to verify
        // that the postcondition (result.equals(update(result_old, data_old))) holds.
        if (data != null && result != null) {
            // Stub implementation: add data elements to result map
            // In a real scenario, this would contain actual processing logic
            @SuppressWarnings("unchecked")
            Map<Object, Object> resultMap = (Map<Object, Object>) result;
            for (Object item : data) {
                resultMap.put(item, item);
            }
        }
    }
    
    /**
     * Helper method for the update function used in postconditions.
     * This simulates the update operation referenced in the JML specification.
     * 
     * @param result The original result map
     * @param data The data set to update with
     * @return A new map representing the updated result
     */
    public static Map<?,?> update(Map<?,?> result, Set<?> data) {
        Map<Object, Object> updated = new HashMap<>();
        if (result != null) {
            updated.putAll(result);
        }
        if (data != null) {
            for (Object item : data) {
                updated.put(item, item);
            }
        }
        return updated;
    }
}
