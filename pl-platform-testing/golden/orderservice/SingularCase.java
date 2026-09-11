package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: restock -> placeOrder -> ship -> stockLevel
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : OrderServiceDryRun");
        System.out.println("test string   : restock -> placeOrder -> ship -> stockLevel");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        Integer orderId;
        int level;

        try {
            {   // ── block 0: restock ─────────────────────────────────────────────
                String sku = "widget"; // CLIENT_INPUT
                int quantity = 10; // CLIENT_INPUT
                require(0, "restock", "sku != null && quantity > 0", sku != null && quantity > 0);
                int stockUnits_old = Helper.stockUnits;
                int orderedUnits_old = Helper.orderedUnits;
                int events_old = Helper.events;
                Helper.restock(sku, quantity);
                ensure(0, "restock", "Stock.containsKey(sku)", Helper.Stock.containsKey(sku));
                ensure(0, "restock", "stockUnits == \\old(stockUnits) + quantity", (java.util.Objects.equals(Helper.stockUnits, (stockUnits_old + quantity))));
                ensure(0, "restock", "orderedUnits == \\old(orderedUnits)", (java.util.Objects.equals(Helper.orderedUnits, orderedUnits_old)));
                ensure(0, "restock", "events == \\old(events)", (java.util.Objects.equals(Helper.events, events_old)));
            }

            {   // ── block 1: placeOrder ──────────────────────────────────────────
                String sku = "widget"; // CLIENT_INPUT
                int quantity = 4; // CLIENT_INPUT
                require(1, "placeOrder", "quantity > 0 && Stock.containsKey(sku) && Stock.get(sku) >= quantity", quantity > 0 && Helper.Stock.containsKey(sku) && Helper.Stock.get(sku) >= quantity);
                int stockUnits_old = Helper.stockUnits;
                int orderedUnits_old = Helper.orderedUnits;
                int nextOrderId_old = Helper.nextOrderId;
                int events_old = Helper.events;
                orderId = Helper.placeOrder(sku, quantity); // SERVER_OUTPUT captured
                ensure(1, "placeOrder", "Open.containsKey(orderId)", Helper.Open.containsKey(orderId));
                ensure(1, "placeOrder", "stockUnits == \\old(stockUnits) - quantity", (java.util.Objects.equals(Helper.stockUnits, (stockUnits_old - quantity))));
                ensure(1, "placeOrder", "orderedUnits == \\old(orderedUnits) + quantity", (java.util.Objects.equals(Helper.orderedUnits, (orderedUnits_old + quantity))));
                ensure(1, "placeOrder", "nextOrderId == \\old(nextOrderId) + 1", (java.util.Objects.equals(Helper.nextOrderId, (nextOrderId_old + 1))));
                ensure(1, "placeOrder", "events == \\old(events) + 1", (java.util.Objects.equals(Helper.events, (events_old + 1))));
            }

            {   // ── block 2: ship ────────────────────────────────────────────────
                // orderId is the SERVER_OUTPUT captured by placeOrder, propagated into this block
                require(2, "ship", "Open.containsKey(orderId)", Helper.Open.containsKey(orderId));
                int stockUnits_old = Helper.stockUnits;
                int orderedUnits_old = Helper.orderedUnits;
                int events_old = Helper.events;
                Helper.ship(orderId);
                ensure(2, "ship", "Shipped.containsKey(orderId)", Helper.Shipped.containsKey(orderId));
                ensure(2, "ship", "!Open.containsKey(orderId)", !Helper.Open.containsKey(orderId));
                ensure(2, "ship", "stockUnits == \\old(stockUnits)", (java.util.Objects.equals(Helper.stockUnits, stockUnits_old)));
                ensure(2, "ship", "orderedUnits == \\old(orderedUnits)", (java.util.Objects.equals(Helper.orderedUnits, orderedUnits_old)));
                ensure(2, "ship", "events == \\old(events) + 1", (java.util.Objects.equals(Helper.events, (events_old + 1))));
            }

            {   // ── block 3: stockLevel ──────────────────────────────────────────
                String sku = "widget"; // CLIENT_INPUT
                require(3, "stockLevel", "Stock.containsKey(sku)", Helper.Stock.containsKey(sku));
                int stockUnits_old = Helper.stockUnits;
                int events_old = Helper.events;
                level = Helper.stockLevel(sku); // SERVER_OUTPUT captured
                ensure(3, "stockLevel", "level == Stock.get(sku)", (java.util.Objects.equals(level, Helper.Stock.get(sku))));
                ensure(3, "stockLevel", "level >= 0", (level >= 0));
                ensure(3, "stockLevel", "stockUnits == \\old(stockUnits)", (java.util.Objects.equals(Helper.stockUnits, stockUnits_old)));
                ensure(3, "stockLevel", "events == \\old(events)", (java.util.Objects.equals(Helper.events, events_old)));
            }

        } catch (PreconditionViolated stop) {
            // Past a broken precondition the spec says nothing about the library's
            // behaviour, so there is nothing meaningful left to check.
            System.out.println();
            System.out.println("run stopped at " + stop.getMessage()
                    + " — the test string calls it outside its contract");
        }

        System.exit(report());
    }

    /** Raised when a precondition does not hold, to stop the run. */
    private static class PreconditionViolated extends RuntimeException {
        PreconditionViolated(String where) { super(where); }
    }

    /**
      * A precondition, checked immediately before its call.
      *
      * A failure stops the run: calling the library outside its contract would
      * measure behaviour the spec never promised, so nothing after it is evidence
      * of anything.  Postcondition failures do NOT stop the run — each one is a
      * real finding, and reporting them all is more useful than reporting one.
      */
    private static void require(int block, String function, String condition, boolean holds) {
        check(block, function, "PRE ", condition, holds);
        if (!holds) {
            throw new PreconditionViolated("block " + block + " (" + function + ")");
        }
    }

    /** A postcondition, checked immediately after its call. */
    private static void ensure(int block, String function, String condition, boolean holds) {
        check(block, function, "POST", condition, holds);
    }

    private static void check(int block, String function, String kind,
                              String condition, boolean holds) {
        checks++;
        if (!holds) failures++;
        System.out.println(String.format("block %d  %-12s %s  %-4s  %s",
                block, function, kind, holds ? "ok" : "FAIL", condition));
    }

    private static int report() {
        System.out.println();
        if (failures == 0) {
            System.out.println(checks
                    + " condition(s) checked, all hold —"
                    + " the test string runs and meets its pre/postconditions");
            return 0;
        }
        System.out.println(failures + " of " + checks + " condition(s) FAILED");
        return 1;
    }
}
