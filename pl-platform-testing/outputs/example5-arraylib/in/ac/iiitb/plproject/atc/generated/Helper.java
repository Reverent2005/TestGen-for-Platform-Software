package in.ac.iiitb.plproject.atc.generated;

/**
 * EXAMPLE 5 — IntArray, a fixed-size int array.
 *
 * The library under test for the set -&gt; set -&gt; get -&gt; sum -&gt; indexOf dry run.
 * Plain Java, no framework dependencies: the generated ATC calls it through the
 * {@code Helper.<method>(...)} convention the code generator emits.
 *
 * Global state (mirrors specs/IntArray.spec):
 * <pre>
 *   A      : int[] = [0, 0, 0, 0]   // the backing store
 *   length : int   = 4              // A.length, exposed as its own field
 *   writes : int   = 0              // how many times a cell has been written
 * </pre>
 *
 * {@code length} is a derived view of {@code A.length}, held as its own field
 * because the .spec grammar has no member access on an array.  {@code writes} is
 * what makes "this call did not touch the array" checkable: every read asserts
 * {@code writes == \old(writes)}, which no amount of reasoning about the returned
 * value alone would catch.
 */
public class Helper {

    public static int[] A = new int[] { 0, 0, 0, 0 };
    public static int length = 4;
    public static int writes = 0;

    /** pre: 0 &lt;= index &lt; length;  post: A'[index] = value, writes' = writes + 1. */
    public static void set(int index, int value) {
        A[index] = value;
        writes = writes + 1;
    }

    /**
     * pre: 0 &lt;= index &lt; length;  post: \result = A[index], A' = A.
     *
     * The element is a SERVER_OUTPUT: the caller knows the index it asked for, not
     * the value the array holds there.
     */
    public static int get(int index) {
        int elem = A[index];
        return elem;
    }

    /** pre: length &gt; 0;  post: \result = A[0] + ... + A[length-1], A' = A. */
    public static int sum() {
        int total = 0;
        for (int i = 0; i < length; i++) {
            total = total + A[i];
        }
        return total;
    }

    /** pre: length &gt; 0;  post: \result = the first index holding value, or -1. */
    public static int indexOf(int value) {
        for (int i = 0; i < length; i++) {
            if (A[i] == value) {
                return i;
            }
        }
        return -1;
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        A = new int[] { 0, 0, 0, 0 };
        length = 4;
        writes = 0;
    }
}
