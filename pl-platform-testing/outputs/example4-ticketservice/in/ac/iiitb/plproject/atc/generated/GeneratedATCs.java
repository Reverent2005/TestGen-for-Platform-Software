package in.ac.iiitb.plproject.atc.generated;

import java.util.*;
import gov.nasa.jpf.symbc.Debug;

public class GeneratedATCs {

    public Integer numSeatsAvailable_helper() {
        Debug.assume((Helper.available >= 0));
        int available_old = Helper.available;
        Integer freeCount = Debug.makeSymbolicInteger("freeCount");
        freeCount = Helper.numSeatsAvailable();
        assert(java.util.Objects.equals(freeCount, Helper.available) && java.util.Objects.equals(Helper.available, available_old));
        return freeCount;
    }

    public Integer findAndHoldSeats_helper() {
        Integer numSeats = Debug.makeSymbolicInteger("numSeats");
        String customerEmail = Debug.makeSymbolicString("customerEmail");
        Debug.assume(numSeats > 0 && customerEmail != null && Helper.available >= numSeats);
        int available_old = Helper.available;
        int nextHoldId_old = Helper.nextHoldId;
        Integer seatHoldId = Debug.makeSymbolicInteger("seatHoldId");
        seatHoldId = Helper.findAndHoldSeats(numSeats, customerEmail);
        assert(Helper.Holds.containsKey(seatHoldId) && java.util.Objects.equals(Helper.available, (available_old - numSeats)) && java.util.Objects.equals(Helper.nextHoldId, (nextHoldId_old + 1)));
        return seatHoldId;
    }

    public String reserveSeats_helper(Integer seatHoldId) {
        seatHoldId = Debug.makeSymbolicInteger("seatHoldId");
        String customerEmail = Debug.makeSymbolicString("customerEmail");
        Debug.assume(Helper.Holds.containsKey(seatHoldId) && java.util.Objects.equals(customerEmail, Helper.Holds.get(seatHoldId)));
        int available_old = Helper.available;
        String confirmationCode = Debug.makeSymbolicString("confirmationCode");
        confirmationCode = Helper.reserveSeats(seatHoldId, customerEmail);
        assert(Helper.Reservations.containsKey(seatHoldId) && !Helper.Holds.containsKey(seatHoldId) && java.util.Objects.equals(Helper.available, available_old));
        return confirmationCode;
    }

    public static void main(String[] args) {
        GeneratedATCs instance = new GeneratedATCs();
        Integer freeCount_0 = instance.numSeatsAvailable_helper();
        Integer seatHoldId = instance.findAndHoldSeats_helper();
        String confirmationCode = instance.reserveSeats_helper(seatHoldId);
        Integer freeCount_3 = instance.numSeatsAvailable_helper();
    }
}
