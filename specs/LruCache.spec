/*
 * EXAMPLE 13 — LruCache — THE CUSTOM LIBRARY
 * Pattern: everything the other twelve examples cover, in one library, plus the
 * case none of them has — a SERVER_OUTPUT that is NULLABLE and PROPAGATED.
 *
 * Test string: put -> put -> put -> get -> restore
 *
 * The other libraries are either textbook containers (Examples 1, 5-12) or a
 * transcription of a published coding challenge (Example 4).  This one was
 * written for the example set, to put the two hardest cases in the same value:
 *
 *   Example 2  oldVal      nullable, produced, never consumed
 *   Example 3  taskId      never null, produced, consumed twice
 *   Example 13 evictedKey  NULLABLE, produced three times, consumed once
 *
 * evictedKey is null for the first two puts (nothing has to go yet) and the key
 * the cache chose for the third.  restore then takes it as a formal parameter, so
 * the propagation scan threads the THIRD put's value into block 4 — the most
 * recent producing block, not the first.  Binding it in the .tests file would be
 * rejected: the cache picks the victim by recency, and the caller cannot know it.
 *
 * `mostRecent == key` is the LRU discipline itself, and it is asserted for get()
 * as well as for put(), because a get that did not reorder the recency list would
 * satisfy every clause about C and still evict the wrong entry next time.  The
 * run is built so that this matters: block 3 gets the key that was inserted
 * SECOND, which makes the entry inserted third the victim of block 4.
 *
 * SIMPLIFICATION: `size <= capacity` is asserted rather than
 * `size == min(size+1, capacity)`, because the grammar has neither min nor a
 * conditional.  What that silence costs is measured in mutations/lrucache.mutants
 * — LRU_PUT_NEVER_EVICTS dies on it, but LRU_EVICT_MOST_RECENT does not, since
 * evicting the wrong entry keeps the size right.
 */

state {
    Map<String,String> C;
    List<String> Recency;
    String mostRecent;
    int capacity;
    int size;
}

spec put {
    signature: String put(String key, String value);
    requires:  key != null && value != null && capacity > 0;
    ensures:   \result == evictedKey && C.get(key) == value && mostRecent == key && size <= capacity;
}

spec get {
    signature: String get(String key);
    requires:  C.containsKey(key);
    ensures:   \result == cachedValue && cachedValue == C.get(key) && mostRecent == key && size == \old(size);
}

spec restore {
    signature: void restore(String evictedKey, String value);
    requires:  evictedKey != null && !C.containsKey(evictedKey);
    ensures:   C.containsKey(evictedKey) && mostRecent == evictedKey && size <= capacity;
}
