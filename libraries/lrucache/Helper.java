package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EXAMPLE 13 — LruCache, the custom library.
 *
 * Not a standard container and not a coding-challenge transcription: a
 * fixed-capacity cache that evicts its least recently used entry, written for
 * this example set because it is the smallest library in which the callee has to
 * INVENT a value the caller could not have known — the key it chose to throw away
 * — and the caller then has to do something with it.
 *
 * The library under test for the put -&gt; put -&gt; put -&gt; get -&gt; restore dry run.
 *
 * Global state (mirrors specs/LruCache.spec):
 * <pre>
 *   C          : Map&lt;String,String&gt; = {}     // the cached entries
 *   Recency    : List&lt;String&gt;       = []     // least recently used first
 *   mostRecent : String             = null   // Recency's last element
 *   capacity   : int                = 2      // how many entries fit
 *   size       : int                = 0
 * </pre>
 *
 * {@code Recency} is the LRU order itself and {@code mostRecent} is the derived
 * view of its last element, exposed as a field for the same reason Example 1
 * exposes {@code top}: the grammar cannot index a list by a computed index, so
 * "the entry just touched" needs a name of its own.
 *
 * The capacity is deliberately 2.  A cache large enough never to evict would make
 * every eviction clause vacuous, and the third put is what this example is about.
 */
public class Helper {

    public static Map<String, String> C = new LinkedHashMap<String, String>();
    public static List<String> Recency = new ArrayList<String>();
    public static String mostRecent = null;
    public static int capacity = 2;
    public static int size = 0;

    /**
     * pre: key != null &amp;&amp; value != null;  post: C'[key] = value, size' &lt;= capacity.
     *
     * @return the key evicted to make room, or null when nothing had to go.  That
     *         key is a SERVER_OUTPUT: the cache picks it by its own recency
     *         book-keeping, so the caller cannot predict it and must not be asked
     *         to supply it.
     */
    public static String put(String key, String value) {
        String evictedKey = null;
        if (!C.containsKey(key) && C.size() >= capacity) {
            evictedKey = Recency.remove(0);
            C.remove(evictedKey);
        }
        C.put(key, value);
        touch(key);
        refresh();
        return evictedKey;
    }

    /**
     * pre: key ∈ C;  post: \result = C[key], key becomes the most recently used.
     *
     * A read is not read-only here: it reorders Recency, which is what makes the
     * NEXT eviction pick a different victim.
     */
    public static String get(String key) {
        String cachedValue = C.get(key);
        touch(key);
        refresh();
        return cachedValue;
    }

    /**
     * pre: evictedKey != null &amp;&amp; evictedKey ∉ C;  post: evictedKey ∈ C'.
     *
     * Puts back an entry the cache evicted earlier — the operation a caller
     * performs on a miss, using the key {@link #put} handed it.  This is the block
     * that CONSUMES the propagated value.
     */
    public static void restore(String evictedKey, String value) {
        put(evictedKey, value);
    }

    /** Moves a key to the most-recently-used end of the recency order. */
    private static void touch(String key) {
        Recency.remove(key);
        Recency.add(key);
    }

    /** Rebuilds the derived views of the recency order. */
    private static void refresh() {
        size = C.size();
        mostRecent = Recency.isEmpty() ? null : Recency.get(Recency.size() - 1);
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        C = new LinkedHashMap<String, String>();
        Recency = new ArrayList<String>();
        mostRecent = null;
        capacity = 2;
        size = 0;
    }
}
