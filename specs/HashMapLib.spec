/*
 * EXAMPLE 2 — HashMap<String,Integer>
 * Pattern: nullable SERVER_OUTPUT, produced but never consumed.
 *
 * Test string: put -> put -> getOldValue -> remove   (both puts use the same key)
 *
 * put's previous value is NULLABLE — null when the key was absent — so it is
 * carried in the boxed Integer and the postcondition places no non-null
 * constraint on it.  Nothing downstream has a parameter named oldVal, so the two
 * captured values are returned from their helper but never threaded onward.
 *
 * SIMPLIFICATION: the size clause of put's postcondition is conditional
 * (size' = size + 1 only when the key was absent).  The .spec grammar has no
 * conditional expression, so that clause is omitted here rather than asserted
 * wrongly; the M'[key] = value part is kept.
 */

state {
    Map<String,Integer> M;
    int size;
}

spec put {
    signature: Integer put(String key, Integer value);
    requires:  key != null && value != null;
    ensures:   \result == oldVal && M.get(key) == value;
}

spec getOldValue {
    signature: Integer getOldValue(String key);
    requires:  M.containsKey(key);
    ensures:   \result == curVal && curVal == M.get(key) && size == \old(size);
}

spec remove {
    signature: void remove(String key);
    requires:  M.containsKey(key);
    ensures:   !M.containsKey(key) && size == \old(size) - 1;
}
