/*
 * EXAMPLE 5 — IntArray (a fixed-size int array)
 * Pattern: the first library whose values are primitive `int` rather than a
 * boxed or reference type, and the first whose state is indexed.
 *
 * Test string: set -> set -> get -> sum -> indexOf
 *
 * Three of the five blocks return a value and none of them is threaded anywhere:
 * elem, total and position are each captured, returned and dropped, exactly as
 * poppedElem and peekedElem are in Example 1.  What is new here is the TYPE:
 * `int` is a Java primitive, so the capture cannot be null and TypeMapper routes
 * it to Debug.makeSymbolicInteger rather than to any reference path.
 *
 * A[index] is written as a whole name rather than as an indexing operator: the
 * grammar has no [] operator, and a free name whose leading identifier is a
 * state variable is emitted as Helper.A[index], which is the code we want.  The
 * index inside the brackets is a formal parameter and stays as it is.
 *
 * SIMPLIFICATION: `sum` and `indexOf` are constrained by their range
 * (total >= 0, -1 <= position < length) rather than by their exact value, since
 * the grammar cannot quantify over the elements of A.  What that silence costs
 * is measured in mutations/arraylib.mutants — ARRAY_SUM_SKIPS_FIRST survives it.
 */

state {
    int[] A;
    int length;
    int writes;
}

spec set {
    signature: void set(int index, int value);
    requires:  index >= 0 && index < length;
    ensures:   A[index] == value && writes == \old(writes) + 1;
}

spec get {
    signature: int get(int index);
    requires:  index >= 0 && index < length;
    ensures:   \result == elem && elem == A[index] && writes == \old(writes);
}

spec sum {
    signature: int sum();
    requires:  length > 0;
    ensures:   \result == total && total >= 0 && writes == \old(writes);
}

spec indexOf {
    signature: int indexOf(int value);
    requires:  length > 0;
    ensures:   \result == position && position >= -1 && position < length && writes == \old(writes);
}
