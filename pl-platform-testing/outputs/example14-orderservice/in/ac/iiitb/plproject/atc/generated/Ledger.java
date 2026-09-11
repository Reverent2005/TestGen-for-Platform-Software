package in.ac.iiitb.plproject.atc.generated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * EXAMPLE 14, collaborator 2 of 3 — what has been ordered.
 *
 * <p>Owns the orders and the order-id counter, and is the one collaborator that
 * calls another: every state change it makes is recorded through {@link Audit}.
 * It knows nothing about stock levels — a ledger that could reach into the
 * catalogue would make the conservation law the façade states trivially true.
 *
 * <p>{@code committed} is the total number of units the orders account for,
 * whether still open or already shipped.  It is the other half of the invariant:
 * a unit is either in {@link Catalogue#units} or in here, never both and never
 * neither.
 */
class Ledger {

    static Map<Integer, String> Open = new LinkedHashMap<Integer, String>();
    static Map<Integer, String> Shipped = new LinkedHashMap<Integer, String>();
    static int committed = 0;
    static int nextOrderId = 1;

    /**
     * Records a new order and returns the id it was given.
     *
     * The id is the ledger's to choose — the caller cannot know it in advance —
     * which is what makes it the SERVER_OUTPUT of the whole example.
     */
    static int open(String sku, int quantity) {
        int orderId = nextOrderId;
        nextOrderId = nextOrderId + 1;
        Open.put(orderId, sku);
        committed = committed + quantity;
        Audit.record("open " + orderId + " " + sku + " x" + quantity);
        return orderId;
    }

    /** Moves an open order to shipped.  Units stay committed: shipping moves nothing. */
    static void ship(int orderId) {
        String sku = Open.remove(orderId);
        Shipped.put(orderId, sku);
        Audit.record("ship " + orderId + " " + sku);
    }

    static void reset() {
        Open = new LinkedHashMap<Integer, String>();
        Shipped = new LinkedHashMap<Integer, String>();
        committed = 0;
        nextOrderId = 1;
    }
}
