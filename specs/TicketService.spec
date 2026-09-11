/*
 * EXAMPLE 4 — TicketService
 * Pattern: multi-hop propagation alongside a read-only query, and the first
 * postcondition whose arithmetic mentions a CLIENT_INPUT.
 *
 * Test string: numSeatsAvailable -> findAndHoldSeats -> reserveSeats -> numSeatsAvailable
 *
 * From the ticket-service coding challenge at
 * github.com/yingw787/walmart_challenge_07_07_2017 (PROBLEM.md): a venue holds
 * seats for a customer, and a hold is later committed into a reservation that
 * returns a confirmation code.
 *
 * seatHoldId is the SERVER_OUTPUT: findAndHoldSeats invents it and reserveSeats
 * takes it as a formal parameter, so it is threaded forward exactly as taskId is
 * in Example 3.  freeCount and confirmationCode are SERVER_OUTPUTs nothing
 * downstream names — captured and returned, never forwarded.
 *
 * customerEmail is NOT propagated even though both findAndHoldSeats and
 * reserveSeats declare a parameter of that name: propagation carries
 * SERVER_OUTPUTs, and customerEmail is a CLIENT_INPUT in both blocks.  The two
 * are independent values that happen to share a name, and the test string binds
 * each of them separately.
 *
 * NEW HERE: findAndHoldSeats' postcondition subtracts a CLIENT_INPUT rather than
 * a constant — available' = available - numSeats.  Examples 1 to 3 only ever
 * offset the old state by a literal.
 *
 * SIMPLIFICATION: the problem statement's real subject is "the BEST available
 * seats" and "a hold expires within a set number of seconds".  The .spec grammar
 * can neither index the seat grid by a computed index nor talk about time, so
 * neither clause is asserted here rather than asserted wrongly.  What that
 * silence costs is measured, not just noted: see mutations/ticketservice.mutants.
 */

state {
    Map<Integer,String> Holds;
    Map<Integer,String> Reservations;
    int available;
    int nextHoldId;
}

spec numSeatsAvailable {
    signature: Integer numSeatsAvailable();
    requires:  available >= 0;
    ensures:   \result == freeCount && freeCount == available && available == \old(available);
}

spec findAndHoldSeats {
    signature: Integer findAndHoldSeats(Integer numSeats, String customerEmail);
    requires:  numSeats > 0 && customerEmail != null && available >= numSeats;
    ensures:   \result == seatHoldId && Holds.containsKey(seatHoldId) && available == \old(available) - numSeats && nextHoldId == \old(nextHoldId) + 1;
}

spec reserveSeats {
    signature: String reserveSeats(Integer seatHoldId, String customerEmail);
    requires:  Holds.containsKey(seatHoldId) && customerEmail == Holds.get(seatHoldId);
    ensures:   \result == confirmationCode && Reservations.containsKey(seatHoldId) && !Holds.containsKey(seatHoldId) && available == \old(available);
}
