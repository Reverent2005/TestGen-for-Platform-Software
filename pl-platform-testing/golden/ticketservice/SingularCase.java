package in.ac.iiitb.plproject.atc.generated;

/**
 * SINGULAR CASE — generated, do not edit.
 *
 * Runs the test string once with concrete inputs and checks every
 * precondition before its call and every postcondition after it.
 *
 *   test string: numSeatsAvailable -> findAndHoldSeats -> reserveSeats -> numSeatsAvailable
 *
 * Exits 0 when every condition holds, 1 otherwise.
 */
public class SingularCase {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("singular case : TicketServiceDryRun");
        System.out.println("test string   : numSeatsAvailable -> findAndHoldSeats -> reserveSeats -> numSeatsAvailable");
        System.out.println();

        Helper.reset(); // start from the initial state the spec declares

        // SERVER_OUTPUT captures — declared here so later blocks can consume them
        Integer freeCount_0;
        Integer seatHoldId;
        String confirmationCode;
        Integer freeCount_3;

        try {
            {   // ── block 0: numSeatsAvailable ───────────────────────────────────
                require(0, "numSeatsAvailable", "available >= 0", (Helper.available >= 0));
                int available_old = Helper.available;
                freeCount_0 = Helper.numSeatsAvailable(); // SERVER_OUTPUT captured
                Integer freeCount = freeCount_0; // the name the postcondition uses
                ensure(0, "numSeatsAvailable", "freeCount == available", (java.util.Objects.equals(freeCount, Helper.available)));
                ensure(0, "numSeatsAvailable", "available == \\old(available)", (java.util.Objects.equals(Helper.available, available_old)));
            }

            {   // ── block 1: findAndHoldSeats ────────────────────────────────────
                Integer numSeats = 3; // CLIENT_INPUT
                String customerEmail = "alice@example.com"; // CLIENT_INPUT
                require(1, "findAndHoldSeats", "numSeats > 0 && customerEmail != null && available >= numSeats", numSeats > 0 && customerEmail != null && Helper.available >= numSeats);
                int available_old = Helper.available;
                int nextHoldId_old = Helper.nextHoldId;
                seatHoldId = Helper.findAndHoldSeats(numSeats, customerEmail); // SERVER_OUTPUT captured
                ensure(1, "findAndHoldSeats", "Holds.containsKey(seatHoldId)", Helper.Holds.containsKey(seatHoldId));
                ensure(1, "findAndHoldSeats", "available == \\old(available) - numSeats", (java.util.Objects.equals(Helper.available, (available_old - numSeats))));
                ensure(1, "findAndHoldSeats", "nextHoldId == \\old(nextHoldId) + 1", (java.util.Objects.equals(Helper.nextHoldId, (nextHoldId_old + 1))));
            }

            {   // ── block 2: reserveSeats ────────────────────────────────────────
                // seatHoldId is the SERVER_OUTPUT captured by findAndHoldSeats, propagated into this block
                String customerEmail = "alice@example.com"; // CLIENT_INPUT
                require(2, "reserveSeats", "Holds.containsKey(seatHoldId) && customerEmail == Holds.get(seatHoldId)", Helper.Holds.containsKey(seatHoldId) && java.util.Objects.equals(customerEmail, Helper.Holds.get(seatHoldId)));
                int available_old = Helper.available;
                confirmationCode = Helper.reserveSeats(seatHoldId, customerEmail); // SERVER_OUTPUT captured
                ensure(2, "reserveSeats", "Reservations.containsKey(seatHoldId)", Helper.Reservations.containsKey(seatHoldId));
                ensure(2, "reserveSeats", "!Holds.containsKey(seatHoldId)", !Helper.Holds.containsKey(seatHoldId));
                ensure(2, "reserveSeats", "available == \\old(available)", (java.util.Objects.equals(Helper.available, available_old)));
            }

            {   // ── block 3: numSeatsAvailable ───────────────────────────────────
                require(3, "numSeatsAvailable", "available >= 0", (Helper.available >= 0));
                int available_old = Helper.available;
                freeCount_3 = Helper.numSeatsAvailable(); // SERVER_OUTPUT captured
                Integer freeCount = freeCount_3; // the name the postcondition uses
                ensure(3, "numSeatsAvailable", "freeCount == available", (java.util.Objects.equals(freeCount, Helper.available)));
                ensure(3, "numSeatsAvailable", "available == \\old(available)", (java.util.Objects.equals(Helper.available, available_old)));
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
