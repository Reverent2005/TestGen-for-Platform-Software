package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.List;

/**
 * EXAMPLE 14, collaborator 3 of 3 — the append-only trail.
 *
 * <p>The furthest class from the façade: {@link Helper} never calls it, only
 * {@link Ledger} does.  That distance is the point of including it.  A fault
 * seeded here is two calls away from anything the specification names directly,
 * and it is still caught, because every operation that must leave a trace says so
 * as {@code events == \old(events) + 1} — and every operation that must not says
 * {@code events == \old(events)}.
 */
class Audit {

    static List<String> Trail = new ArrayList<String>();
    static int events = 0;

    /** Appends one line to the trail. */
    static void record(String event) {
        Trail.add(event);
        events = events + 1;
    }

    static void reset() {
        Trail = new ArrayList<String>();
        events = 0;
    }
}
