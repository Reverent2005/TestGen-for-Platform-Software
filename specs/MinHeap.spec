/*
 * EXAMPLE 11 — MinHeap<Integer>
 * Pattern: a value the caller cannot predict even in principle, named twice —
 * once before the call and once after.
 *
 * Test string: insert -> insert -> insert -> peekMin -> extractMin
 *
 * A heap reorders itself on every insert, so which element sits at the root after
 * three insertions is a property of the algorithm, not of the call.  peekMin and
 * extractMin therefore both produce a SERVER_OUTPUT, and both are checked against
 * the same derived field:
 *
 *   peekMin:    smallest   == minValue          — the root right now
 *   extractMin: removedMin == \old(minValue)    — the root just before the call
 *
 * The \old on the second one is what makes it a claim rather than a tautology:
 * after the call minValue has moved on to the next-smallest element, so
 * comparing against its CURRENT value would be false for a correct heap.
 *
 * minValue is found by scanning H rather than by reading H[0].  A mirror that
 * echoed the root would turn `smallest == minValue` into a comparison of
 * peekMin's answer with itself; scanning makes it the heap invariant — the root
 * really is the smallest element — which is what kills HEAP_NO_SIFT_UP.
 *
 * LIMITATION: the heap ORDER invariant (every parent <= its children) is not
 * asserted, because the grammar cannot quantify over the array.  What the spec
 * does assert — that the root is what comes back and that the size moves by one —
 * is enough to kill a sift-down that stops early only because the run's three
 * values make the root visibly wrong; see mutations/heap.mutants.
 */

state {
    List<Integer> H;
    int size;
    Integer minValue;
}

spec insert {
    signature: void insert(Integer value);
    requires:  value != null;
    ensures:   H.contains(value) && size == \old(size) + 1;
}

spec peekMin {
    signature: Integer peekMin();
    requires:  size > 0;
    ensures:   \result == smallest && smallest == minValue && size == \old(size);
}

spec extractMin {
    signature: Integer extractMin();
    requires:  size > 0;
    ensures:   \result == removedMin && removedMin == \old(minValue) && size == \old(size) - 1;
}
