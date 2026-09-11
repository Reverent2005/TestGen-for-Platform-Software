/*
 * EXAMPLE 14 — OrderService — LIBRARIES THAT INTERACT
 * Pattern: one specification, written against a façade, constraining state that
 * lives in three other classes.
 *
 * Test string: restock -> placeOrder -> ship -> stockLevel
 *
 * Examples 1 to 13 each test a single class.  This one tests four:
 *
 *          Helper            the façade, and the only class the ATC calls
 *         /      \
 * Catalogue      Ledger      stock levels        orders and their ids
 *                   |
 *                 Audit      the append-only trail
 *
 * Nothing about the generated code changes.  The ATC still calls
 * Helper.<method>(...) and still reads Helper.<name>; what changes is that those
 * names now stand for state three other classes own.  Stock, Open, Shipped and
 * Trail are ALIASES of the collaborators' own collections rather than copies of
 * them, and the scalars are re-read after every call, so there is no mirror in
 * which a fault could hide.
 *
 * THE CLAUSE A SINGLE-CLASS EXAMPLE CANNOT HAVE.  stockUnits is the catalogue's
 * counter and orderedUnits is the ledger's, and neither class can see the other.
 * placeOrder therefore asserts a conservation law across the two:
 *
 *     stockUnits   == \old(stockUnits)   - quantity
 *     orderedUnits == \old(orderedUnits) + quantity
 *
 * Units move; they are never created or destroyed.  A fault in Catalogue.take, in
 * Ledger.open, or in the way the façade sequences them breaks one half of that
 * pair while leaving the other intact, and the conjunction sees it.
 *
 * events is the same idea one class further out: Helper never calls Audit, only
 * Ledger does.  Every operation that must leave a trace says events == \old(events) + 1
 * and every operation that must not says events == \old(events), so a fault two
 * calls away from the façade is still named by a postcondition written at it.
 *
 * A QUERY HAS TO REFRESH THE MIRRORS TOO.  stockLevel changes nothing, so it
 * looks as though it has no book-keeping to do — but events == \old(events) is
 * checked against Helper's copy of Audit's counter, and a copy that is never
 * re-read compares two snapshots of the same stale number and holds no matter
 * what the collaborators did. The mutant ORDER_STOCKLEVEL_LOGS survived on
 * exactly that until stockLevel was made to refresh like every other method.
 *
 * placeOrder's precondition tests Stock.containsKey(sku) BEFORE comparing
 * Stock.get(sku): && is short-circuiting in the emitted Java, and the JUnit
 * flavour binds sku to a placeholder for which the map holds nothing. Written the
 * other way round it would unbox a null rather than skip the run.
 */

state {
    Map<String,Integer> Stock;
    Map<Integer,String> Open;
    Map<Integer,String> Shipped;
    List<String> Trail;
    int stockUnits;
    int orderedUnits;
    int nextOrderId;
    int events;
}

spec restock {
    signature: void restock(String sku, int quantity);
    requires:  sku != null && quantity > 0;
    ensures:   Stock.containsKey(sku) && stockUnits == \old(stockUnits) + quantity && orderedUnits == \old(orderedUnits) && events == \old(events);
}

spec placeOrder {
    signature: Integer placeOrder(String sku, int quantity);
    requires:  quantity > 0 && Stock.containsKey(sku) && Stock.get(sku) >= quantity;
    ensures:   \result == orderId && Open.containsKey(orderId) && stockUnits == \old(stockUnits) - quantity && orderedUnits == \old(orderedUnits) + quantity && nextOrderId == \old(nextOrderId) + 1 && events == \old(events) + 1;
}

spec ship {
    signature: void ship(Integer orderId);
    requires:  Open.containsKey(orderId);
    ensures:   Shipped.containsKey(orderId) && !Open.containsKey(orderId) && stockUnits == \old(stockUnits) && orderedUnits == \old(orderedUnits) && events == \old(events) + 1;
}

spec stockLevel {
    signature: int stockLevel(String sku);
    requires:  Stock.containsKey(sku);
    ensures:   \result == level && level == Stock.get(sku) && level >= 0 && stockUnits == \old(stockUnits) && events == \old(events);
}
