package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.List;

/**
 * EXAMPLE 11 — MinHeap of Integers.
 *
 * The library under test for the insert -&gt; insert -&gt; insert -&gt; peekMin -&gt;
 * extractMin dry run.  A real binary heap in an array, with sift-up and
 * sift-down; {@code H} and {@code minValue} are the spec-visible mirror.
 *
 * Global state (mirrors specs/MinHeap.spec):
 * <pre>
 *   H        : List&lt;Integer&gt; = []     // the heap array, in heap order
 *   size     : int            = 0
 *   minValue : Integer        = null   // the smallest element, found by scanning H
 * </pre>
 *
 * {@code minValue} is the smallest element of H, computed by scanning rather than
 * by reading the root.  That is deliberate: it lets the spec say that the root IS
 * the smallest — which is the heap invariant — and it lets extractMin's
 * postcondition name the value that must come back BEFORE the call happens.
 */
public class Helper {

    public static List<Integer> H = new ArrayList<Integer>();
    public static int size = 0;
    public static Integer minValue = null;

    /** pre: value != null;  post: value ∈ H, size' = size + 1, heap order restored. */
    public static void insert(Integer value) {
        H.add(value);
        siftUp(H.size() - 1);
        refresh();
    }

    /** pre: size &gt; 0;  post: \result = H[0], heap unchanged. */
    public static Integer peekMin() {
        return H.get(0);
    }

    /**
     * pre: size &gt; 0;  post: \result = old H[0], size' = size - 1, heap order restored.
     *
     * Which value leaves is the heap's decision, so it is a SERVER_OUTPUT.
     */
    public static Integer extractMin() {
        Integer removedMin = H.get(0);
        Integer last = H.remove(H.size() - 1);
        if (!H.isEmpty()) {
            H.set(0, last);
            siftDown(0);
        }
        refresh();
        return removedMin;
    }

    private static void siftUp(int index) {
        int child = index;
        while (child > 0) {
            int parent = (child - 1) / 2;
            if (H.get(child) >= H.get(parent)) {
                return;
            }
            swap(child, parent);
            child = parent;
        }
    }

    private static void siftDown(int index) {
        int parent = index;
        while (true) {
            int left = 2 * parent + 1;
            int right = left + 1;
            int smallest = parent;
            if (left < H.size() && H.get(left) < H.get(smallest)) {
                smallest = left;
            }
            if (right < H.size() && H.get(right) < H.get(smallest)) {
                smallest = right;
            }
            if (smallest == parent) {
                return;
            }
            swap(parent, smallest);
            parent = smallest;
        }
    }

    private static void swap(int i, int j) {
        Integer held = H.get(i);
        H.set(i, H.get(j));
        H.set(j, held);
    }

    /**
     * Rebuilds the spec-visible mirror from the heap array.
     *
     * {@code minValue} is found by SCANNING H, not by reading H[0].  That
     * distinction is the whole oracle: if the mirror simply echoed the root, then
     * `smallest == minValue` would compare peekMin's answer with itself and hold
     * for any heap at all, however badly ordered.  Scanning makes it a claim about
     * the heap invariant — the root really is the smallest element.
     */
    private static void refresh() {
        size = H.size();
        minValue = null;
        for (Integer value : H) {
            if (minValue == null || value < minValue) {
                minValue = value;
            }
        }
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        H = new ArrayList<Integer>();
        size = 0;
        minValue = null;
    }
}
