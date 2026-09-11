package in.ac.iiitb.plproject.atc.generated;

import java.util.List;
import java.util.Map;

/**
 * EXAMPLE 14 — OrderService, the FAÇADE of a library that is four classes.
 *
 * <p>Every other example in the set is one class.  This one is
 * {@code Helper -> {Catalogue, Ledger}} and {@code Ledger -> Audit}, and it is
 * here to answer a question none of the others can: does a specification written
 * against a façade still catch a fault planted one or two classes further in?
 *
 * <pre>
 *            Helper            the only class the generated ATC ever calls
 *           /      \
 *   Catalogue      Ledger      stock levels        orders and their ids
 *                     |
 *                   Audit      the append-only trail
 * </pre>
 *
 * <h3>Spec-visible state</h3>
 *
 * The generated code addresses library state as {@code Helper.<name>}, so every
 * name the specification uses has to be a field of this class.  They are not
 * copies:
 *
 * <ul>
 *   <li>the collections are <b>aliases</b> of the collaborators' own maps and
 *       lists — {@code Helper.Stock} IS {@code Catalogue.Stock}, the same object,
 *       so there is no mirror in which a fault could hide;</li>
 *   <li>the scalars cannot be aliased, because assigning an {@code int} copies it,
 *       so they are re-read from the collaborators after every call by
 *       {@link #refresh()}.</li>
 * </ul>
 *
 * <h3>The conservation law</h3>
 *
 * {@code stockUnits} comes from the catalogue and {@code orderedUnits} from the
 * ledger, and placing an order moves units from one to the other.  Neither class
 * can see the other's counter, so
 *
 * <pre>
 *   stockUnits + orderedUnits  is unchanged by placeOrder
 * </pre>
 *
 * is a claim about two classes at once that neither could satisfy by cheating.
 * That is the clause a single-class example cannot have, and it is what makes a
 * fault in {@code Catalogue.take} or in {@code Ledger.open} visible from here.
 */
public class Helper {

    // ── aliases: the collaborators' own state, not copies of it ──────────────
    public static Map<String, Integer> Stock = Catalogue.Stock;
    public static Map<Integer, String> Open = Ledger.Open;
    public static Map<Integer, String> Shipped = Ledger.Shipped;
    public static List<String> Trail = Audit.Trail;

    // ── mirrors: scalars cannot be aliased, so they are re-read after each call ──
    public static int stockUnits = 0;
    public static int orderedUnits = 0;
    public static int nextOrderId = 1;
    public static int events = 0;

    /** pre: sku != null &amp;&amp; quantity &gt; 0;  post: stock rises, nothing else moves. */
    public static void restock(String sku, int quantity) {
        Catalogue.restock(sku, quantity);
        refresh();
    }

    /**
     * pre: the SKU is stocked in at least the quantity asked for.
     * post: the units move from the catalogue to the ledger, and the move is logged.
     *
     * @return the order id the ledger chose — the SERVER_OUTPUT, threaded into ship()
     */
    public static Integer placeOrder(String sku, int quantity) {
        Catalogue.take(sku, quantity);
        int orderId = Ledger.open(sku, quantity);
        refresh();
        return orderId;
    }

    /** pre: the order is open;  post: it is shipped, and no units move. */
    public static void ship(Integer orderId) {
        Ledger.ship(orderId);
        refresh();
    }

    /**
     * pre: the SKU is stocked;  post: \result is its level, and nothing changed.
     *
     * A read-only method still has to {@link #refresh()}.  It changes nothing, but
     * the mirrors it leaves behind are what the postcondition
     * {@code events == \old(events)} is checked against, and a stale mirror makes
     * that clause unfalsifiable — it would compare two copies of the same old
     * number and hold however much the collaborators had moved underneath. Skipping
     * the refresh here let ORDER_STOCKLEVEL_LOGS survive until it was put back.
     */
    public static int stockLevel(String sku) {
        int level = Catalogue.level(sku);
        refresh();
        return level;
    }

    /**
     * Re-points the aliases and re-reads the scalars.
     *
     * The re-pointing matters: a collaborator that replaces its map rather than
     * mutating it — {@link Catalogue#reset()} does — would otherwise leave this
     * class holding the old object.
     *
     * <p>EVERY method calls this, queries included.  The collections are aliases
     * and look after themselves; the scalars are copies, and a copy that is not
     * re-read after a call is a number the specification can no longer falsify.
     */
    private static void refresh() {
        Stock = Catalogue.Stock;
        Open = Ledger.Open;
        Shipped = Ledger.Shipped;
        Trail = Audit.Trail;
        stockUnits = Catalogue.units;
        orderedUnits = Ledger.committed;
        nextOrderId = Ledger.nextOrderId;
        events = Audit.events;
    }

    /** Resets all four classes between dry runs. */
    public static void reset() {
        Catalogue.reset();
        Ledger.reset();
        Audit.reset();
        refresh();
    }
}
