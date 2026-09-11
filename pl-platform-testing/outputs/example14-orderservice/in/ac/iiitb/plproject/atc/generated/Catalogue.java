package in.ac.iiitb.plproject.atc.generated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * EXAMPLE 14, collaborator 1 of 3 — what the warehouse holds.
 *
 * <p>Owns the stock levels and nothing else.  It knows nothing about orders, about
 * the ledger, or about the audit trail; the only class that calls it is
 * {@link Helper}, the façade.
 *
 * <p>{@code units} is the total across every SKU, maintained here rather than
 * summed on demand, because the specification needs a scalar it can compare
 * against {@code \old(...)} — and because keeping it here is what lets a fault in
 * this class be seen as a violation of a conservation law the façade states.
 */
class Catalogue {

    static Map<String, Integer> Stock = new LinkedHashMap<String, Integer>();
    static int units = 0;

    /** The level held for a SKU, 0 for one never stocked. */
    static int level(String sku) {
        Integer held = Stock.get(sku);
        return held == null ? 0 : held;
    }

    /** Adds stock for a SKU. */
    static void restock(String sku, int quantity) {
        Stock.put(sku, level(sku) + quantity);
        units = units + quantity;
    }

    /** Removes stock committed to an order. */
    static void take(String sku, int quantity) {
        Stock.put(sku, level(sku) - quantity);
        units = units - quantity;
    }

    static void reset() {
        Stock = new LinkedHashMap<String, Integer>();
        units = 0;
    }
}
