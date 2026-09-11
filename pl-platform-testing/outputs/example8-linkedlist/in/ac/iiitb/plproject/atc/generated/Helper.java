package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.List;

/**
 * EXAMPLE 8 — SinglyLinkedList&lt;Integer&gt;.
 *
 * The library under test for the addFirst -&gt; addLast -&gt; removeFirst -&gt; indexOf
 * dry run.  A real node chain, not a wrapper around a list: {@link Node} is the
 * data structure and {@code L} is a mirror of it maintained for the spec.
 *
 * Global state (mirrors specs/LinkedList.spec):
 * <pre>
 *   L    : List&lt;Integer&gt; = []     // mirror of the chain, in order
 *   size : int            = 0
 *   head : Integer        = null   // value in the first node
 *   tail : Integer        = null   // value in the last node
 * </pre>
 *
 * The mirror exists because the .spec grammar can talk about membership and
 * position in a list but cannot follow a {@code next} pointer.  Every mutation in
 * mutations/linkedlist.mutants is seeded into the CHAIN, so the mirror never
 * hides a fault: it is rebuilt from the chain after every operation.
 */
public class Helper {

    /** One link of the chain. */
    static class Node {
        Integer value;
        Node next;
        Node(Integer value) { this.value = value; }
    }

    static Node first = null;

    public static List<Integer> L = new ArrayList<Integer>();
    public static int size = 0;
    public static Integer head = null;
    public static Integer tail = null;

    /** pre: value != null;  post: L' = [value] + L, head' = value, size' = size + 1. */
    public static void addFirst(Integer value) {
        Node node = new Node(value);
        node.next = first;
        first = node;
        refresh();
    }

    /** pre: value != null;  post: L' = L + [value], tail' = value, size' = size + 1. */
    public static void addLast(Integer value) {
        Node node = new Node(value);
        if (first == null) {
            first = node;
        } else {
            Node cursor = first;
            while (cursor.next != null) {
                cursor = cursor.next;
            }
            cursor.next = node;
        }
        refresh();
    }

    /**
     * pre: size &gt; 0;  post: \result = old head, L' = L[1..], size' = size - 1.
     *
     * The removed value is a SERVER_OUTPUT: the caller says only "drop the front",
     * and the list decides what that was.
     */
    public static Integer removeFirst() {
        Integer removedValue = first.value;
        first = first.next;
        refresh();
        return removedValue;
    }

    /** pre: size &gt; 0;  post: \result = first position holding value, or -1. */
    public static int indexOf(Integer value) {
        int position = 0;
        Node cursor = first;
        while (cursor != null) {
            if (cursor.value.equals(value)) {
                return position;
            }
            position = position + 1;
            cursor = cursor.next;
        }
        return -1;
    }

    /** Rebuilds the spec-visible mirror from the chain, which is the real structure. */
    private static void refresh() {
        L = new ArrayList<Integer>();
        for (Node cursor = first; cursor != null; cursor = cursor.next) {
            L.add(cursor.value);
        }
        size = L.size();
        head = L.isEmpty() ? null : L.get(0);
        tail = L.isEmpty() ? null : L.get(L.size() - 1);
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        first = null;
        L = new ArrayList<Integer>();
        size = 0;
        head = null;
        tail = null;
    }
}
