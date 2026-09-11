package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.List;

/**
 * EXAMPLE 7 — Queue&lt;String&gt;.
 *
 * The library under test for the enqueue -&gt; enqueue -&gt; dequeue -&gt; front -&gt;
 * isEmpty dry run.
 *
 * Global state (mirrors specs/Queue.spec):
 * <pre>
 *   Q     : List&lt;String&gt; = []
 *   size  : int          = 0
 *   head  : String       = null   // derived: Q[0], the element dequeue() removes next
 * </pre>
 *
 * This is Example 1's stack with the other discipline: the same three-field
 * shape, FIFO instead of LIFO.  {@code head} is the derived view of {@code Q[0]}
 * that lets the spec say which element must come back, which is the clause
 * Stack.spec cannot express about {@code pop}.
 */
public class Helper {

    public static List<String> Q = new ArrayList<String>();
    public static int size = 0;
    public static String head = null;

    /** pre: elem != null;  post: Q' = Q + [elem], size' = size + 1. */
    public static void enqueue(String elem) {
        Q.add(elem);
        size = Q.size();
        head = Q.get(0);
    }

    /**
     * pre: size &gt; 0;  post: \result = Q[0], Q' = Q[1..], size' = size - 1.
     *
     * The dequeued element is a SERVER_OUTPUT: which element leaves is the
     * queue's decision, not the caller's.
     */
    public static String dequeue() {
        String dequeuedElem = Q.remove(0);
        size = Q.size();
        head = Q.isEmpty() ? null : Q.get(0);
        return dequeuedElem;
    }

    /** pre: size &gt; 0;  post: \result = Q[0], Q' = Q, size' = size. */
    public static String front() {
        return Q.get(0);
    }

    /** pre: true;  post: \result = (size == 0), Q' = Q. */
    public static boolean isEmpty() {
        return Q.isEmpty();
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        Q = new ArrayList<String>();
        size = 0;
        head = null;
    }
}
