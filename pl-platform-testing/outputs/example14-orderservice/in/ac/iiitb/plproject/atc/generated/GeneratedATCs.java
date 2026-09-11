package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public void restock_helper() {
        String sku = Debug.makeSymbolicString("sku");
        int quantity = Debug.makeSymbolicInteger("quantity");
        Debug.assume(sku != null && quantity > 0);
        int stockUnits_old = Helper.stockUnits;
        int orderedUnits_old = Helper.orderedUnits;
        int events_old = Helper.events;
        Helper.restock(sku, quantity);
        assert(Helper.Stock.containsKey(sku) && java.util.Objects.equals(Helper.stockUnits, (stockUnits_old + quantity)) && java.util.Objects.equals(Helper.orderedUnits, orderedUnits_old) && java.util.Objects.equals(Helper.events, events_old));
    }

    public Integer placeOrder_helper() {
        String sku = Debug.makeSymbolicString("sku");
        int quantity = Debug.makeSymbolicInteger("quantity");
        Debug.assume(quantity > 0 && Helper.Stock.containsKey(sku) && Helper.Stock.get(sku) >= quantity);
        int stockUnits_old = Helper.stockUnits;
        int orderedUnits_old = Helper.orderedUnits;
        int nextOrderId_old = Helper.nextOrderId;
        int events_old = Helper.events;
        Integer orderId = Debug.makeSymbolicInteger("orderId");
        orderId = Helper.placeOrder(sku, quantity);
        assert(Helper.Open.containsKey(orderId) && java.util.Objects.equals(Helper.stockUnits, (stockUnits_old - quantity)) && java.util.Objects.equals(Helper.orderedUnits, (orderedUnits_old + quantity)) && java.util.Objects.equals(Helper.nextOrderId, (nextOrderId_old + 1)) && java.util.Objects.equals(Helper.events, (events_old + 1)));
        return orderId;
    }

    public void ship_helper(Integer orderId) {
        orderId = Debug.makeSymbolicInteger("orderId");
        Debug.assume(Helper.Open.containsKey(orderId));
        int stockUnits_old = Helper.stockUnits;
        int orderedUnits_old = Helper.orderedUnits;
        int events_old = Helper.events;
        Helper.ship(orderId);
        assert(Helper.Shipped.containsKey(orderId) && !Helper.Open.containsKey(orderId) && java.util.Objects.equals(Helper.stockUnits, stockUnits_old) && java.util.Objects.equals(Helper.orderedUnits, orderedUnits_old) && java.util.Objects.equals(Helper.events, (events_old + 1)));
    }

    public int stockLevel_helper() {
        String sku = Debug.makeSymbolicString("sku");
        Debug.assume(Helper.Stock.containsKey(sku));
        int stockUnits_old = Helper.stockUnits;
        int events_old = Helper.events;
        int level = Debug.makeSymbolicInteger("level");
        level = Helper.stockLevel(sku);
        assert(java.util.Objects.equals(level, Helper.Stock.get(sku)) && level >= 0 && java.util.Objects.equals(Helper.stockUnits, stockUnits_old) && java.util.Objects.equals(Helper.events, events_old));
        return level;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        instance.restock_helper();
        Integer orderId = instance.placeOrder_helper();
        instance.ship_helper(orderId);
        int level = instance.stockLevel_helper();
    }
}
