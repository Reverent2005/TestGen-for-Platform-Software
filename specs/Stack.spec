/*
 * EXAMPLE 1 — Stack<String>
 * Pattern: independent captures, no propagation.
 *
 * Test string: push -> push -> pop -> peek
 *
 * pop() and peek() each capture a SERVER_OUTPUT of their own (poppedElem,
 * peekedElem) and neither is threaded into the other: peek takes no parameters,
 * so the propagation scan must not try to forward poppedElem into it.  Both
 * helpers therefore have an EMPTY parameter list even though both return a value.
 */

state {
    List<String> S;
    int size;
    String top;
}

spec push {
    signature: void push(String elem);
    requires:  elem != null;
    ensures:   size == \old(size) + 1 && top == elem;
}

spec pop {
    signature: String pop();
    requires:  size > 0;
    ensures:   \result == poppedElem && size == \old(size) - 1;
}

spec peek {
    signature: String peek();
    requires:  size > 0;
    ensures:   \result == peekedElem && peekedElem == top && size == \old(size);
}
