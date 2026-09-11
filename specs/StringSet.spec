/*
 * EXAMPLE 9 — StringSet
 * Pattern: Example 2's "the second call is the interesting one" with a boolean
 * return instead of a nullable one.
 *
 * Test string: add -> add -> contains -> remove
 *
 * Both adds use the SAME element on purpose.  The first inserts it and returns
 * true; the second changes nothing and returns false.  That is the whole point of
 * a set, and it is also why `add` has no size clause: `size' = size + 1` holds
 * only when the element was absent, and the grammar has no conditional
 * expression — exactly the simplification HashMapLib.spec records for `put`.
 * Asserting the unconditional version would make the second block fail against a
 * correct library, which is worse than saying nothing.
 *
 * `remove` CAN carry its size clause, because its precondition (elem ∈ E) rules
 * out the case that would make it conditional.  Strengthening a precondition to
 * buy a stronger postcondition is the move the grammar leaves open.
 */

state {
    Set<String> E;
    int size;
}

spec add {
    signature: boolean add(String elem);
    requires:  elem != null;
    ensures:   \result == added && E.contains(elem);
}

spec contains {
    signature: boolean contains(String elem);
    requires:  elem != null;
    ensures:   \result == present && present == E.contains(elem) && size == \old(size);
}

spec remove {
    signature: void remove(String elem);
    requires:  E.contains(elem);
    ensures:   !E.contains(elem) && size == \old(size) - 1;
}
