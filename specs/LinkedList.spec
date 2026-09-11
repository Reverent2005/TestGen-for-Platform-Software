/*
 * EXAMPLE 8 — SinglyLinkedList<Integer>
 * Pattern: both ends of the same structure, and a mirror standing in for a
 * pointer chain.
 *
 * Test string: addFirst -> addLast -> removeFirst -> indexOf
 *
 * The library is a real node chain.  The grammar cannot follow a `next` pointer,
 * so the spec talks about a mirror list L that the library rebuilds from the
 * chain after every operation, exactly as Example 1's `top` mirrors S[size-1].
 * A mutation seeded into the chain therefore still shows up here — the mirror is
 * derived from the mutated chain, not maintained beside it.
 *
 * addFirst and addLast have postconditions of the SAME shape distinguished only
 * by which end they name (head' == value versus tail' == value).  That is what
 * makes the pair worth having: a library that confused its two ends satisfies
 * every size clause and still fails one of the two.
 *
 * removeFirst binds \old(head): the value that must come back is the one the
 * mirror names before the call, the same trick Example 7 uses for dequeue.
 */

state {
    List<Integer> L;
    int size;
    Integer head;
    Integer tail;
}

spec addFirst {
    signature: void addFirst(Integer value);
    requires:  value != null;
    ensures:   head == value && L.contains(value) && size == \old(size) + 1;
}

spec addLast {
    signature: void addLast(Integer value);
    requires:  value != null;
    ensures:   tail == value && L.contains(value) && size == \old(size) + 1;
}

spec removeFirst {
    signature: Integer removeFirst();
    requires:  size > 0;
    ensures:   \result == removedValue && removedValue == \old(head) && size == \old(size) - 1;
}

spec indexOf {
    signature: int indexOf(Integer value);
    requires:  size > 0;
    ensures:   \result == position && position >= -1 && position < size;
}
