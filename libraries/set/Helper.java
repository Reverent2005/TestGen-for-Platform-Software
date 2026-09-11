package in.ac.iiitb.plproject.atc.generated;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * EXAMPLE 9 — StringSet, a set of strings.
 *
 * The library under test for the add -&gt; add -&gt; contains -&gt; remove dry run.
 *
 * Global state (mirrors specs/StringSet.spec):
 * <pre>
 *   E    : Set&lt;String&gt; = {}
 *   size : int         = 0
 * </pre>
 *
 * The interesting case is the SECOND add of the same element: it changes nothing
 * and returns false.  That makes {@code add} the boolean twin of Example 2's
 * {@code put} — a return value whose meaning is "was this new?", which the spec
 * captures but, as in Example 2, cannot use to make the size clause conditional.
 */
public class Helper {

    public static Set<String> E = new LinkedHashSet<String>();
    public static int size = 0;

    /**
     * pre: elem != null;  post: E' = E ∪ {elem}, \result = (elem ∉ E).
     *
     * Whether the element was new is a SERVER_OUTPUT: the caller does not know
     * what the set already held.
     */
    public static boolean add(String elem) {
        boolean added = E.add(elem);
        size = E.size();
        return added;
    }

    /** pre: elem != null;  post: \result = (elem ∈ E), E' = E. */
    public static boolean contains(String elem) {
        return E.contains(elem);
    }

    /** pre: elem ∈ E;  post: E' = E \ {elem}, size' = size - 1. */
    public static void remove(String elem) {
        E.remove(elem);
        size = E.size();
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        E = new LinkedHashSet<String>();
        size = 0;
    }
}
