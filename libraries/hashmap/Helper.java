package in.ac.iiitb.plproject.atc.generated;

import java.util.HashMap;
import java.util.Map;

/**
 * EXAMPLE 2 — HashMap&lt;String,Integer&gt; wrapper.
 *
 * The library under test for the put -&gt; put -&gt; getOldValue -&gt; remove dry run
 * (both puts use the same key).
 *
 * Global state (mirrors specs/HashMapLib.spec):
 * <pre>
 *   M    : Map&lt;String,Integer&gt; = {}
 *   size : int                  = 0
 * </pre>
 */
public class Helper {

    public static Map<String, Integer> M = new HashMap<String, Integer>();
    public static int size = 0;

    /**
     * pre: key != null, value != null;
     * post: \result = oldVal, M'[key] = value,
     *       size' = size + (1 if key not in dom(M) else 0).
     *
     * oldVal is a NULLABLE SERVER_OUTPUT — null when the key was absent — which is
     * why the return type is the boxed Integer rather than int.
     */
    public static Integer put(String key, Integer value) {
        Integer oldVal = M.put(key, value);
        size = M.size();
        return oldVal;
    }

    /** pre: key in dom(M);  post: \result = M[key]. Read-only. */
    public static Integer getOldValue(String key) {
        return M.get(key);
    }

    /** pre: key in dom(M);  post: M' = M \ {key}, size' = size - 1. */
    public static void remove(String key) {
        M.remove(key);
        size = M.size();
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        M = new HashMap<String, Integer>();
        size = 0;
    }
}
