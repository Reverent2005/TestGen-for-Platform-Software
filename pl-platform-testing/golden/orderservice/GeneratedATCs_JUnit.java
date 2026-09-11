package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public void restock_helper() {
        String sku = "sku" /* CLIENT_INPUT: replace with the literal SPF solves for "sku" */;
        int quantity = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "quantity" */;
        assumeTrue(sku != null && quantity > 0);
        int stockUnits_old = Helper.stockUnits;
        int orderedUnits_old = Helper.orderedUnits;
        int events_old = Helper.events;
        Helper.restock(sku, quantity);
        assert(Helper.Stock.containsKey(sku) && java.util.Objects.equals(Helper.stockUnits, (stockUnits_old + quantity)) && java.util.Objects.equals(Helper.orderedUnits, orderedUnits_old) && java.util.Objects.equals(Helper.events, events_old));
    }

    public Integer placeOrder_helper() {
        String sku = "sku" /* CLIENT_INPUT: replace with the literal SPF solves for "sku" */;
        int quantity = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "quantity" */;
        assumeTrue(quantity > 0 && Helper.Stock.containsKey(sku) && Helper.Stock.get(sku) >= quantity);
        int stockUnits_old = Helper.stockUnits;
        int orderedUnits_old = Helper.orderedUnits;
        int nextOrderId_old = Helper.nextOrderId;
        int events_old = Helper.events;
        Response orderIdResponse = executeApiCall(Helper.placeOrder(sku, quantity));
        Integer orderId = extractFromResponse(orderIdResponse, "orderId");
        assert(Helper.Open.containsKey(orderId) && java.util.Objects.equals(Helper.stockUnits, (stockUnits_old - quantity)) && java.util.Objects.equals(Helper.orderedUnits, (orderedUnits_old + quantity)) && java.util.Objects.equals(Helper.nextOrderId, (nextOrderId_old + 1)) && java.util.Objects.equals(Helper.events, (events_old + 1)));
        return orderId;
    }

    public void ship_helper(Integer orderId) {
        // orderId is bound dynamically: it arrives as the SERVER_OUTPUT captured by placeOrder (block 1)
        assumeTrue(Helper.Open.containsKey(orderId));
        int stockUnits_old = Helper.stockUnits;
        int orderedUnits_old = Helper.orderedUnits;
        int events_old = Helper.events;
        Helper.ship(orderId);
        assert(Helper.Shipped.containsKey(orderId) && !Helper.Open.containsKey(orderId) && java.util.Objects.equals(Helper.stockUnits, stockUnits_old) && java.util.Objects.equals(Helper.orderedUnits, orderedUnits_old) && java.util.Objects.equals(Helper.events, (events_old + 1)));
    }

    public int stockLevel_helper() {
        String sku = "sku" /* CLIENT_INPUT: replace with the literal SPF solves for "sku" */;
        assumeTrue(Helper.Stock.containsKey(sku));
        int stockUnits_old = Helper.stockUnits;
        int events_old = Helper.events;
        Response levelResponse = executeApiCall(Helper.stockLevel(sku));
        int level = extractFromResponse(levelResponse, "level");
        assert(java.util.Objects.equals(level, Helper.Stock.get(sku)) && level >= 0 && java.util.Objects.equals(Helper.stockUnits, stockUnits_old) && java.util.Objects.equals(Helper.events, events_old));
        return level;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        instance.restock_helper();
        Integer orderId = instance.placeOrder_helper();
        instance.ship_helper(orderId);
        int level = instance.stockLevel_helper();
    }

    // ── Dynamic data binding support ──────────────────────────────────
    // SERVER_OUTPUT values are read back from the call at RUNTIME rather
    // than solved for, which is what separates this flavour from the SPF one.
    static class Response {
        private final Object payload;
        Response(Object payload) { this.payload = payload; }
        Object payload() { return payload; }
    }

    static Response executeApiCall(Object returnedValue) {
        return new Response(returnedValue);
    }

    @SuppressWarnings("unchecked")
    static <T> T extractFromResponse(Response response, String name) {
        return (T) response.payload();
    }

    @Test
    public void testSequence() {
        main(new String[0]);
    }
}
