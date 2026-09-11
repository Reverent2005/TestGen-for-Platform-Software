/*
 * EXAMPLE 10 — BinarySearchTree of ints
 * Pattern: a precondition bought to pay for a stronger postcondition, and an
 * ordering property expressed through a derived field.
 *
 * Test string: insert -> insert -> insert -> contains -> min -> delete
 *
 * insert requires the key to be ABSENT.  That is not a limitation of the library
 * — it accepts duplicates quite happily — but a deliberate narrowing of the
 * contract, because it is what makes `size == \old(size) + 1` unconditionally
 * true.  Example 9's `add` shows the alternative: no precondition, and no size
 * clause either.  The two together are the same trade-off written both ways.
 *
 * min() is the reason a tree is worth including at all.  Its answer is an
 * ORDERING property: not "a key that is present" but "the smallest key present".
 * The grammar cannot quantify, so the library exposes minKey — the leftmost
 * node's key, refreshed from the tree after every change — and the spec says
 * `smallest == minKey`.  A search that walked right instead of left fails it.
 *
 * The mirror (Keys, size, minKey) is rebuilt FROM the tree after every mutation
 * of it, so a fault seeded into the tree's own pointers reaches the spec rather
 * than being masked by book-keeping kept in parallel.
 */

state {
    Set<Integer> Keys;
    int size;
    int minKey;
}

spec insert {
    signature: void insert(int key);
    requires:  !Keys.contains(key);
    ensures:   Keys.contains(key) && size == \old(size) + 1;
}

spec contains {
    signature: boolean contains(int key);
    requires:  size > 0;
    ensures:   \result == found && found == Keys.contains(key) && size == \old(size);
}

spec min {
    signature: int min();
    requires:  size > 0;
    ensures:   \result == smallest && smallest == minKey && size == \old(size);
}

spec delete {
    signature: void delete(int key);
    requires:  Keys.contains(key);
    ensures:   !Keys.contains(key) && size == \old(size) - 1;
}
