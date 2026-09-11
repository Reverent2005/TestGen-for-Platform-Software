/*
 * EXAMPLE 7 — Queue<String>
 * Pattern: Example 1's shape with the ORDER pinned down, plus the first boolean
 * return.
 *
 * Test string: enqueue -> enqueue -> dequeue -> front -> isEmpty
 *
 * Stack.spec deliberately never says WHICH element pop() returns, and
 * mutations/stack.mutants records the price: STACK_POP_FROM_BOTTOM survives,
 * because `\result == poppedElem` is a capture rather than a claim.  A queue can
 * say it, because the element that must come back is the one the derived field
 * `head` already names BEFORE the call:
 *
 *     dequeuedElem == \old(head)
 *
 * That single conjunct is what makes QUEUE_DEQUEUE_FROM_BACK die here while its
 * mirror image survives in Example 1.  It is the clearest demonstration in the
 * set that a surviving mutant is a statement about the specification, not about
 * the runner.
 *
 * isEmpty is the first block whose SERVER_OUTPUT is a `boolean`: a Java
 * primitive that is neither numeric nor a reference, so TypeMapper routes it to
 * Debug.makeSymbolicBoolean.
 */

state {
    List<String> Q;
    int size;
    String head;
}

spec enqueue {
    signature: void enqueue(String elem);
    requires:  elem != null;
    ensures:   size == \old(size) + 1 && Q.contains(elem);
}

spec dequeue {
    signature: String dequeue();
    requires:  size > 0;
    ensures:   \result == dequeuedElem && dequeuedElem == \old(head) && size == \old(size) - 1;
}

spec front {
    signature: String front();
    requires:  size > 0;
    ensures:   \result == frontElem && frontElem == head && size == \old(size);
}

spec isEmpty {
    signature: boolean isEmpty();
    requires:  size >= 0;
    ensures:   \result == emptyFlag && emptyFlag == false && size == \old(size);
}
