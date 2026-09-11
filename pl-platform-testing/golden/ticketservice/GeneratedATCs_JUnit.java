package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;

public class GeneratedATCs_JUnit {

    public Integer numSeatsAvailable_helper() {
        assumeTrue((Helper.available >= 0));
        int available_old = Helper.available;
        Response freeCountResponse = executeApiCall(Helper.numSeatsAvailable());
        Integer freeCount = extractFromResponse(freeCountResponse, "freeCount");
        assert(java.util.Objects.equals(freeCount, Helper.available) && java.util.Objects.equals(Helper.available, available_old));
        return freeCount;
    }

    public Integer findAndHoldSeats_helper() {
        Integer numSeats = 0 /* CLIENT_INPUT: replace with the literal SPF solves for "numSeats" */;
        String customerEmail = "customerEmail" /* CLIENT_INPUT: replace with the literal SPF solves for "customerEmail" */;
        assumeTrue(numSeats > 0 && customerEmail != null && Helper.available >= numSeats);
        int available_old = Helper.available;
        int nextHoldId_old = Helper.nextHoldId;
        Response seatHoldIdResponse = executeApiCall(Helper.findAndHoldSeats(numSeats, customerEmail));
        Integer seatHoldId = extractFromResponse(seatHoldIdResponse, "seatHoldId");
        assert(Helper.Holds.containsKey(seatHoldId) && java.util.Objects.equals(Helper.available, (available_old - numSeats)) && java.util.Objects.equals(Helper.nextHoldId, (nextHoldId_old + 1)));
        return seatHoldId;
    }

    public String reserveSeats_helper(Integer seatHoldId) {
        // seatHoldId is bound dynamically: it arrives as the SERVER_OUTPUT captured by findAndHoldSeats (block 1)
        String customerEmail = "customerEmail" /* CLIENT_INPUT: replace with the literal SPF solves for "customerEmail" */;
        assumeTrue(Helper.Holds.containsKey(seatHoldId) && java.util.Objects.equals(customerEmail, Helper.Holds.get(seatHoldId)));
        int available_old = Helper.available;
        Response confirmationCodeResponse = executeApiCall(Helper.reserveSeats(seatHoldId, customerEmail));
        String confirmationCode = extractFromResponse(confirmationCodeResponse, "confirmationCode");
        assert(Helper.Reservations.containsKey(seatHoldId) && !Helper.Holds.containsKey(seatHoldId) && java.util.Objects.equals(Helper.available, available_old));
        return confirmationCode;
    }

    public static void main(String[] args) {
        GeneratedATCs_JUnit instance = new GeneratedATCs_JUnit();
        Integer freeCount_0 = instance.numSeatsAvailable_helper();
        Integer seatHoldId = instance.findAndHoldSeats_helper();
        String confirmationCode = instance.reserveSeats_helper(seatHoldId);
        Integer freeCount_3 = instance.numSeatsAvailable_helper();
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
