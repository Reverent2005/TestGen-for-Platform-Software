package in.ac.iiitb.plproject.atc.generated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EXAMPLE 4 — TicketService.
 *
 * The library under test for the
 * numSeatsAvailable -&gt; findAndHoldSeats -&gt; reserveSeats -&gt; numSeatsAvailable dry run.
 *
 * <p>Modelled on the ticket-service coding challenge at
 * {@code github.com/yingw787/walmart_challenge_07_07_2017}: a performance venue
 * is a grid of seats, a customer holds the best seats currently free, and a hold
 * is later committed into a reservation that returns a confirmation code.  The
 * three public operations are the ones {@code PROBLEM.md} specifies —
 * {@code numSeatsAvailable}, {@code findAndHoldSeats}, {@code reserveSeats} —
 * and the seat lifecycle (free → held → reserved) is the one {@code BasicSeat}
 * implements.
 *
 * <p>Global state (mirrors specs/TicketService.spec):
 * <pre>
 *   Holds        : Map&lt;Integer,String&gt; = {}   // seatHoldId -&gt; holder's email
 *   Reservations : Map&lt;Integer,String&gt; = {}   // seatHoldId -&gt; confirmation code
 *   available    : int                  = 12   // seats neither held nor reserved
 *   nextHoldId   : int                  = 1
 * </pre>
 *
 * <p>Two deliberate departures from the original, both so the example stays
 * inside the .spec grammar rather than outside it:
 *
 * <ul>
 *   <li>The original derives a hold's id by multiplying the ids of its seats
 *       together.  Seat ids there start at 0, so the first hold's id is always 0
 *       and two different holds can collide.  Here the id is an auto-incrementing
 *       counter, the same shape as TaskQueue's {@code nextId} — which keeps the
 *       SERVER_OUTPUT propagation honest.  The original's scheme is preserved as
 *       the mutant {@code TICKETSERVICE_HOLD_ID_FROM_SEAT_PRODUCT}.</li>
 *   <li>The seat grid, and which seats a hold got, are private: the .spec grammar
 *       cannot index a list by a computed index, so the specification can only
 *       talk about the counts and the two maps.  What that costs is measurable —
 *       see the mutation set.</li>
 * </ul>
 *
 * <p>Structurally this is the same SERVER_OUTPUT pattern as TaskQueue's
 * submit -&gt; getResult: {@code seatHoldId} is invented by the callee and threaded
 * forward.  What is new is a postcondition whose arithmetic mentions a
 * CLIENT_INPUT ({@code available == \old(available) - numSeats}) rather than only
 * a constant.
 */
public class Helper {

    /** Venue layout. Row 0 is the most valuable row, as in BasicPerformanceVenue. */
    private static final int ROWS = 3;
    private static final int COLS = 4;

    /** Seat lifecycle, mirroring BasicSeat's isHeld/isReserved pair. */
    private static final int FREE = 0;
    private static final int HELD = 1;
    private static final int RESERVED = 2;

    public static Map<Integer, String> Holds = new HashMap<Integer, String>();
    public static Map<Integer, String> Reservations = new HashMap<Integer, String>();
    public static int available = ROWS * COLS;
    public static int nextHoldId = 1;

    /** Seat state by seat id; ids run 0..ROWS*COLS-1, ascending from the best row. */
    private static int[] seats = new int[ROWS * COLS];

    /** Which seats each hold is holding, so reserveSeats can commit exactly those. */
    private static Map<Integer, List<Integer>> heldSeats =
            new HashMap<Integer, List<Integer>>();

    /**
     * pre: available &gt;= 0;  post: \result = freeCount = available, state unchanged.
     *
     * Read-only. The count it returns is a SERVER_OUTPUT nothing downstream
     * consumes — captured and returned, never forwarded.
     */
    public static Integer numSeatsAvailable() {
        return available;
    }

    /**
     * pre: numSeats &gt; 0, customerEmail != null, available &gt;= numSeats;
     * post: \result = seatHoldId, seatHoldId in dom(Holds),
     *       available' = available - numSeats, nextHoldId' = nextHoldId + 1.
     *
     * seatHoldId is the archetypal SERVER_OUTPUT: the venue assigns it, and the
     * caller cannot know it before the call.
     */
    public static Integer findAndHoldSeats(Integer numSeats, String customerEmail) {
        List<Integer> best = mostValuableFreeSeats(numSeats);
        for (Integer seatId : best) {
            seats[seatId] = HELD;
        }
        Integer seatHoldId = nextHoldId;
        nextHoldId = nextHoldId + 1;
        Holds.put(seatHoldId, customerEmail);
        heldSeats.put(seatHoldId, best);
        available = available - numSeats;
        return seatHoldId;
    }

    /**
     * pre: seatHoldId in dom(Holds), customerEmail = Holds[seatHoldId];
     * post: \result = confirmationCode, seatHoldId in dom(Reservations),
     *       seatHoldId not in dom(Holds), available' = available.
     *
     * Committing a hold does not change availability: the seats stopped being
     * available when they were held, which is where BasicPerformanceVenue also
     * decrements, to avoid counting them twice.
     */
    public static String reserveSeats(Integer seatHoldId, String customerEmail) {
        for (Integer seatId : heldSeats.get(seatHoldId)) {
            seats[seatId] = RESERVED;
        }
        String confirmationCode = "R-" + seatHoldId;
        Reservations.put(seatHoldId, confirmationCode);
        Holds.remove(seatHoldId);
        heldSeats.remove(seatHoldId);
        return confirmationCode;
    }

    /**
     * The best seats currently free, in descending order of value — the venue's
     * seat-picking policy, and the part of the problem statement the .spec
     * grammar cannot see.
     */
    private static List<Integer> mostValuableFreeSeats(int numSeats) {
        List<Integer> picked = new ArrayList<Integer>();
        for (int seatId = 0; seatId < seats.length; seatId++) {
            if (seats[seatId] == FREE) {
                picked.add(seatId);
                if (picked.size() == numSeats) return picked;
            }
        }
        throw new IllegalStateException("Not enough seats available.");
    }

    /** Resets the library between dry runs. */
    public static void reset() {
        Holds = new HashMap<Integer, String>();
        Reservations = new HashMap<Integer, String>();
        available = ROWS * COLS;
        nextHoldId = 1;
        seats = new int[ROWS * COLS];
        heldSeats = new HashMap<Integer, List<Integer>>();
    }
}
